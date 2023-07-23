package ollir.visitors;

import ollir.Pair;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.*;
import ast.NodeParser;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import table.Table;

import java.util.*;

public class ExpressionsEmitter extends PreorderJmmVisitor<List<Report>, Boolean> {
    private final boolean optimizedWhiles;
    private final boolean rflag;
    private SymbolTable table;
    private String code, prefix, functionName;
    private List<Report> reports;
    private List<Pair<JmmNode, Integer>> pairs = new ArrayList<>();
    private NodeParser nodeParser;
    HashMap<JmmNode, String> alreadyDone = new HashMap<>();
    private List<JmmNode> alreadyParsedIf = new ArrayList<>();
    private List<List<Pair<JmmNode, Integer>>> ifPairs = new ArrayList<>();
    private List<Pair<JmmNode, Integer>> auxIfPairs = new ArrayList<>();
    private JmmNode rootIfNode;
    List<String> arguments;
    private int rootLabelCnt = 0;

    public ExpressionsEmitter(SymbolTable table, List<Report> reports, String prefix, String functionName, boolean optimizedWhiles, boolean rflag) {
        this.table = table;
        this.code = "";
        this.reports = reports;
        this.prefix = prefix;
        this.arguments = new ArrayList<>();
        this.functionName = functionName;
        this.nodeParser = new NodeParser((Table) table, functionName);
        this.optimizedWhiles = optimizedWhiles;
        this.rflag = rflag;
        List<Symbol> aux = this.table.getParameters(functionName);

        for (Symbol s: aux)
            if (s == null)
                this.arguments.add(null);
            else
                this.arguments.add(s.getName());

        addVisit("Equals", this::visitAssignments);
        addVisit("returnDeclaration", this::visitReturn);
        addVisit("FunctionCall", this::visitFunctionCall);
        addVisit("If", this::visitIf);
        addVisit("While", this::visitWhile);
    }

    public String getCode() {
        return code;
    }

    public List<Report> getReports() {
        return reports;
    }

    private boolean visitIf(JmmNode node, List<Report> reports) {
        if (this.rflag) alreadyDone = new HashMap<>();
        if (!node.getParent().getKind().equals("If") && !node.getParent().getKind().equals("Else") && !node.getParent().getKind().equals("While")) {
            this.parseIf(node, rootLabelCnt);
        }
        return true;
    }

    private boolean visitWhile(JmmNode node, List<Report> reports) {
        if (this.rflag) alreadyDone = new HashMap<>();
        if (!node.getParent().getKind().equals("If") && !node.getParent().getKind().equals("Else") && !node.getParent().getKind().equals("While")) {
            this.parseWhile(node, rootLabelCnt);
        }
        this.prefix += "  ";
        return true;
    }

    private void parseWhile(JmmNode node, int labelCnt) {
        rootLabelCnt++;
        labelCnt = rootLabelCnt;
        this.code += this.prefix + "Loop" + labelCnt + ":\n";
        this.prefix += "  ";
        String oldPrefix = this.prefix;
        this.prefix += "  ";
        this.rootIfNode = node;
        for (JmmNode child: node.getChildren()) {
            if (child.getKind().equals("Condition")) {
                this.pairs = new ArrayList<>();
                this.buildIf(child);
                if (this.optimizedWhiles)
                    this.code += "goto EndLoop" + labelCnt + ";\n";
                else {
                    this.code += "goto Body" + labelCnt + ";\n";
                    this.code += this.prefix + "goto EndLoop" + labelCnt + ";\n";
                }
                break;
            }
        }
        this.ifPairs = new ArrayList<>();
        this.pairs = new ArrayList<>();
        this.auxIfPairs = new ArrayList<>();
        this.generateCode();
        this.prefix = oldPrefix;
        this.alreadyParsedIf.add(node);
        rootIfNode = node;
        this.getIfList(node, -1);
        oldPrefix = this.prefix;
        if (!this.optimizedWhiles)
            this.code += this.prefix + "Body" + labelCnt + ":\n";
        //rootLabelCnt = labelCnt;
        this.prefix += "  ";

        List<List<Pair<JmmNode, Integer>>> ifPairsTemp;
        List<Pair<JmmNode, Integer>> previousPair = null;
        for (List<Pair<JmmNode, Integer>> p: this.ifPairs) {
            this.pairs = p;
            if (previousPair != null && this.pairs.size() == 1 && previousPair.size() == 1) {
                JmmNode curr = this.pairs.get(0).getKey();
                JmmNode prev = previousPair.get(0).getKey();
                if (curr.getKind().equals("FunctionCall") && prev.getKind().equals("Index"))
                    continue;
            }
            if (p.get(0).getKey().getKind().equals("If")) {
                this.pairs = new ArrayList<>();
                ifPairsTemp = this.ifPairs;
                this.ifPairs = new ArrayList<>();
                this.parseIf(p.get(0).getKey(), rootLabelCnt);
                this.ifPairs = ifPairsTemp;
            }
            else if (p.get(0).getKey().getKind().equals("While")) {
                this.pairs = new ArrayList<>();
                ifPairsTemp = this.ifPairs;
                this.ifPairs = new ArrayList<>();
                this.parseWhile(p.get(0).getKey(), rootLabelCnt);
                this.ifPairs = ifPairsTemp;
            }
            else
                this.generateCode();
            previousPair = p;
        }
        this.prefix = oldPrefix;
        this.ifPairs = new ArrayList<>();
        this.pairs = new ArrayList<>();

        this.code += this.prefix + "  goto Loop" + labelCnt + ";\n";
        this.prefix = oldPrefix;
        this.code += this.prefix + "EndLoop" + labelCnt + ":\n";

        Optional<JmmNode> checkMain = node.getAncestor("MainDeclaration");
        if (checkMain.isPresent()) {
            JmmNode bodyNode = node.getParent();
            while (!bodyNode.getKind().equals("Body"))
                bodyNode = bodyNode.getParent();

            if (bodyNode.getChildren().indexOf(node) == bodyNode.getChildren().size() - 1)
                this.code += this.prefix + "  ret.V;\n";

        }

        this.pairs = new ArrayList<>();
    }

    private void parseIf(JmmNode node, int labelCnt) {
        rootLabelCnt++;
        labelCnt = rootLabelCnt;
        this.buildIf(node.getChildren().get(0));
        this.code += "goto else" + labelCnt + ";\n";
        this.alreadyParsedIf.add(node);
        rootIfNode = node;
        this.pairs = new ArrayList<>();
        this.ifPairs = new ArrayList<>();
        this.getIfList(node, -1);

        String oldPrefix = this.prefix;
        this.prefix += "  ";
        List<List<Pair<JmmNode, Integer>>> ifPairsTemp;
        for (List<Pair<JmmNode, Integer>> p: this.ifPairs) {
            this.pairs = p;
            if (p.get(0).getKey().getKind().equals("If")) {
                this.pairs = new ArrayList<>();
                ifPairsTemp = this.ifPairs;
                this.ifPairs = new ArrayList<>();
                this.parseIf(p.get(0).getKey(), labelCnt);
                this.ifPairs = ifPairsTemp;
            }
            else if (p.get(0).getKey().getKind().equals("While")) {
                this.pairs = new ArrayList<>();
                ifPairsTemp = this.ifPairs;
                this.ifPairs = new ArrayList<>();
                this.parseWhile(p.get(0).getKey(), labelCnt);
                this.ifPairs = ifPairsTemp;
            }
            else
                this.generateCode();
        }
        this.prefix = oldPrefix;
        this.ifPairs = new ArrayList<>();
        this.pairs = new ArrayList<>();

        this.code += this.prefix + "  goto endif" + labelCnt + ";\n";

        for (JmmNode child: node.getChildren()) {
            if (child.getKind().equals("Else")) {
                this.parseElse(child, labelCnt);
                return;
            }
        }
        this.pairs = new ArrayList<>();
    }

    private void parseElse(JmmNode node, int labelCnt) {
        this.code += this.prefix + "else" + labelCnt + ":\n";
        rootIfNode = node;
        this.getIfList(node, -1);

        String oldPrefix = this.prefix;
        this.prefix += "  ";
        List<List<Pair<JmmNode, Integer>>> ifPairsTemp;
        for (List<Pair<JmmNode, Integer>> p: this.ifPairs) {
            this.pairs = p;
            if (p.get(0).getKey().getKind().equals("If")) {
                this.pairs = new ArrayList<>();
                ifPairsTemp = this.ifPairs;
                this.ifPairs = new ArrayList<>();
                this.parseIf(p.get(0).getKey(), labelCnt);
                this.ifPairs = ifPairsTemp;
            }
            else if (p.get(0).getKey().getKind().equals("While")) {
                this.pairs = new ArrayList<>();
                ifPairsTemp = this.ifPairs;
                this.ifPairs = new ArrayList<>();
                this.parseWhile(p.get(0).getKey(), labelCnt);
                this.ifPairs = ifPairsTemp;
            }
            else
                this.generateCode();
        }
        this.prefix = oldPrefix;
        this.ifPairs = new ArrayList<>();
        this.pairs = new ArrayList<>();

        this.pairs = new ArrayList<>();

        this.generateCode();
        this.prefix = oldPrefix;

        this.code += this.prefix + "endif" + labelCnt + ":\n";

        //rootLabelCnt = labelCnt;
        this.prefix += " ";
        this.pairs = new ArrayList<>();
    }

    public void buildIf(JmmNode node) {
        String type = "";
        this.getIfList(node, -1);

        if (node.getChildren().size() == 1 && node.getChildren().get(0).getKind().equals("Variable")) {
            this.parseVar(node.getChildren().get(0));
        }

        for (List<Pair<JmmNode, Integer>> p: this.ifPairs) {
            this.pairs = p;
            this.generateCode();
        }
        this.auxIfPairs = new ArrayList<>();
        List<String> aux = new ArrayList<>();
        switch (node.getChildren().get(0).getKind()) {
            case "LessThan":
                aux = Arrays.asList(this.parseVar(node.getChildren().get(0).getChildren().get(0)).split("\\."));
                boolean isParameter = false;
                for (String s: aux) {
                    if (s.contains("$")) {
                        isParameter = true;
                        continue;
                    }
                    if (isParameter) {
                        isParameter = false;
                        continue;
                    }
                    if (aux.indexOf(s) == 0 || s.contains("[") || s.contains("]"))
                        continue;
                    type += "." + s;
                }

                JmmNode checkIfWhileOrIf = node;
                while (!checkIfWhileOrIf.getKind().equals("If") && !checkIfWhileOrIf.getKind().equals("While"))
                    checkIfWhileOrIf = checkIfWhileOrIf.getParent();

                String left = this.parseVar(node.getChildren().get(0).getChildren().get(0));
                String right = this.parseVar(node.getChildren().get(0).getChildren().get(1));

                if (left.contains("[") && left.contains("]")) {
                    alreadyDone.put(node.getChildren().get(0).getChildren().get(0), "aux" + (alreadyDone.size() + 1) + ".i32");
                    this.code += this.prefix + "aux" + alreadyDone.size() + ".i32 :=.i32 " + left + ";\n";
                    left = "aux" + alreadyDone.size() + ".i32";
                }

                if (right.contains("[") && right.contains("]")) {
                    alreadyDone.put(node.getChildren().get(0).getChildren().get(1), "aux" + (alreadyDone.size() + 1) + ".i32");
                    this.code += this.prefix + "aux" + alreadyDone.size() + ".i32 :=.i32 " + right + ";\n";
                    right = "aux" + alreadyDone.size() + ".i32";
                }

                if (checkIfWhileOrIf.getKind().equals("If") || this.optimizedWhiles)
                    this.code += this.prefix + "if (" + left + " >=" + type + " " + right + ") ";
                else
                    this.code += this.prefix + "if (" + left + " <" + type + " " + right + ") ";
                break;
            case "Variable":
                this.code += this.prefix + "if (" + this.parseVar(node.getChildren().get(0)) + "== .bool 0.bool) ";
                break;
            case "And":
                if (!node.getParent().getKind().equals("While")) {
                    alreadyDone.put(node.getChildren().get(0), "aux" + (alreadyDone.size() + 1) + ".bool");
                    this.code += this.prefix + "aux" + alreadyDone.size() + ".bool :=.bool " + this.parseVar(node.getChildren().get(0).getChildren().get(0)) + " &&.bool " + this.parseVar(node.getChildren().get(0).getChildren().get(1)) + ";\n";
                    this.code += this.prefix + "if (" + alreadyDone.get(node.getChildren().get(0)) + " ==.bool 0.bool) ";
                }
                else {
                    alreadyDone.put(node.getChildren().get(0), "aux" + (alreadyDone.size() + 1) + ".bool");
                    this.code += this.prefix + "aux" + alreadyDone.size() + ".bool :=.bool " + this.parseVar(node.getChildren().get(0).getChildren().get(0)) + " &&.bool " + this.parseVar(node.getChildren().get(0).getChildren().get(1)) + ";\n";
                    this.code += this.prefix + "if (" + alreadyDone.get(node.getChildren().get(0)) + " ==.bool 1.bool) ";
                }
                break;
            case "FunctionCall":
                this.code += this.prefix + "if (" + this.parseVar(node.getChildren().get(0)) + " ==.bool 1.bool) ";
                break;
            case "Not":
                String notNode = this.parseVar(node.getChildren().get(0).getChildren().get(0));
                alreadyDone.put(node, "aux" + (alreadyDone.size() + 1) + ".bool");
                if (node.getParent().getKind().equals("While")) {
                    if (this.optimizedWhiles)
                        this.code += this.prefix + "if (" + notNode + " ==.bool 1.bool) ";
                    else
                        this.code += this.prefix + "if (" + notNode + " ==.bool 0.bool) ";
                }
                else
                    this.code += this.prefix + "if (" + notNode + " ==.bool 1.bool) ";
                break;
            case "True":
                this.code += this.prefix + "if (1.bool ==.bool 1.bool" + ") ";
                break;
            case "False":
                this.code += this.prefix + "if (0.bool ==.bool 1.bool" + ") ";
                break;
            default:
                break;
        }
    }

    public void getIfList(JmmNode node, int depth) {
        for (JmmNode child2 : node.getChildren()) {
            if (child2.getKind().equals("Condition") || child2.getKind().equals("Else"))
                continue;

            if (!child2.getKind().equals("If") && !child2.getKind().equals("Else") && !child2.getKind().equals("While"))
                this.getIfList(child2, depth + 1);
            else if (child2.getKind().equals("If") || child2.getKind().equals("While")) {
                Pair p = new Pair<>(child2, depth);
                if (!auxIfPairs.contains(p)) {
                    auxIfPairs.add(p);
                }
                this.addIfPair(auxIfPairs);
                auxIfPairs = new ArrayList<>();
            }
        }

        if (!node.equals(rootIfNode)) {
            if (!node.getKind().equals("Arguments") && !node.getKind().equals("Variable") && !node.getKind().equals("Integer") && !node.getKind().equals("Colon") && !node.getKind().equals("FunctionName") && !node.getKind().equals("Identifier") && !node.getKind().equals("This") && !node.getKind().equals("False") && !node.getKind().equals("True") && !node.getKind().equals("Condition"))
                if (depth == 0) {
                    Pair p = new Pair<>(node, depth);
                    if (!auxIfPairs.contains(p)) {
                        auxIfPairs.add(p);
                    }
                    this.addIfPair(auxIfPairs);
                    auxIfPairs = new ArrayList<>();
                }
                else {
                    Pair p = new Pair<>(node, depth);
                    if (!auxIfPairs.contains(p))
                        auxIfPairs.add(p);
                }
        }
    }

    private boolean visitAssignments(JmmNode node, List<Report> reports) {
        if (this.rflag) alreadyDone = new HashMap<>();
        Optional<JmmNode> ifStatement = node.getAncestor("If");
        Optional<JmmNode> whileStatement = node.getAncestor("While");

        if (ifStatement.isPresent() || whileStatement.isPresent())
            return true;

        this.getPairs(node);
        return true;
    }

    private boolean visitReturn(JmmNode node, List<Report> reports) {
        if (this.rflag) alreadyDone = new HashMap<>();
        List<JmmNode> children = node.getChildren();
        String kind = "";
        kind = children.get(0).getKind();
        if (children.isEmpty() || (!kind.equals("Add") && !kind.equals("Sub") && !kind.equals("Mul") && !kind.equals("Div") && !kind.equals("FunctionCall") && !kind.equals("NewObject") && !kind.equals("Index") && !kind.equals("NewArray") && !kind.equals("Length") && !kind.equals("Not") && !kind.equals("And") && !kind.equals("LessThan"))) {
            String s = "";
            if (alreadyDone.containsKey(node.getChildren().get(0)))
                s = alreadyDone.get(node.getChildren().get(0));
            else s = this.parseVar(node.getChildren().get(0));
            List<String> aux = Arrays.asList(this.parseVar(node.getChildren().get(0)).split("\\."));
            String type = aux.get(aux.size()-1);
            type = "." + type;
            this.code += this.prefix + "ret" + type + " " + s + ";\n";

            return true;
        }
        this.getPairs(node);
        this.generateCode();

        JmmNode func = node.getParent();
        while (!func.getKind().equals("Function"))
            func = func.getParent();

        this.code += this.prefix + "ret";
        if (this.table.getReturnType(func.get("Name")) != null) {
            if (this.table.getReturnType(func.get("Name")).isArray()) this.code += ".array";
        }
        this.code += this.getType(this.table.getReturnType(func.get("Name")).getName()) + " " + alreadyDone.get(node.getChildren().get(0)) + ";\n";

        return true;
    }

    private boolean visitFunctionCall(JmmNode node, List<Report> reports) {
        if (this.rflag) alreadyDone = new HashMap<>();
        if (node.getParent().getKind().equals("Body")) {
            this.getPairs(node);
            this.generateCode();
        }

        return true;
    }

    private String parseVar(JmmNode node) {
        if (alreadyDone.get(node) != null) {
            return alreadyDone.get(node);
        }
        switch(node.getKind()) {
            case "Integer":
            case "Int":
                return node.get("Value") + ".i32";
            case "Identifier":
                return node.get("Name");
            case "True":
                return "1.bool";
            case "False":
                return "0.bool";
            case "Variable":
                String name = this.sanitize(node.getChildren().get(0).get("Name"));
                if (this.arguments.contains(node.getChildren().get(0).get("Name"))) {
                    if (node.getParent().getKind().equals("Index"))
                        return "$" + (this.arguments.indexOf(node.getChildren().get(0).get("Name")) + 1) + "." + name + ".i32";
                    else {
                        for (Symbol parameter: this.table.getParameters(this.functionName)) {
                            if (parameter == null)
                                continue;
                            if (parameter.getName().equals(node.getChildren().get(0).get("Name"))) {
                                if (parameter.getType().isArray())
                                    return "$" + (this.arguments.indexOf(node.getChildren().get(0).get("Name")) + 1) + "." + name + ".array.i32";
                                else {
                                    return "$" + (this.arguments.indexOf(node.getChildren().get(0).get("Name")) + 1) + "." + name + this.getType(parameter.getType().getName());
                                }
                            }
                        }
                    }
                }

                for (Symbol child: this.table.getLocalVariables(this.functionName)) {
                    if (child == null)
                        continue;
                    if (child.getName().equals(node.getChildren().get(0).get("Name"))) {
                        if (child.getType().isArray()) {
                            if (node.getParent().getKind().equals("Index"))
                                return name;
                            else
                                return name + ".array.i32";
                        }
                        else
                            return this.parseVar(node.getChildren().get(0)) + this.getType(child.getType().getName());
                    }
                }

                for (Symbol child: this.table.getFields()) {
                    if (child.getName().equals(node.getChildren().get(0).get("Name"))) {
                        String type = this.getType(child.getType().getName());
                        if (child.getType().isArray())
                            type = ".array" + type;
                        if ((node.getParent().getKind().equals("Equals")) && node.getParent().getChildren().get(0).getKind().equals("Variable") && node.getParent().getChildren().get(1).getKind().equals("Variable") && this.checkIfField(node.getParent().getChildren().get(0)) && this.checkIfField(node.getParent().getChildren().get(1))) {
                            alreadyDone.put(node.getParent().getChildren().get(1).getChildren().get(0), "aux" + (alreadyDone.size() + 1) + type);
                            return "aux" + alreadyDone.size() + type + " :=" + type + " getfield(this, " + name + type + ")" + type + ";\n"
                                    + this.prefix + "putfield(this, " + name + type + ", " + node.getParent().getChildren().get(1).getChildren().get(0).get("Name") + type + ").V;\n";
                        }
                        else if (node.getParent().getKind().equals("Equals") && node.getParent().getChildren().get(0).getKind().equals("Variable") && !node.getParent().getChildren().get(1).getKind().equals("FunctionCall") && this.checkIfField(node.getParent().getChildren().get(0))) {
                            String putfieldAux = this.parseVar(node.getParent().getChildren().get(1));
                            if (putfieldAux == null) {
                                alreadyDone.put(null, "aux" + (alreadyDone.size() + 1) + type);
                                return "aux" + alreadyDone.size() + type;
                            }
                            return "putfield(this, " + name + type + ", " + this.parseVar(node.getParent().getChildren().get(1)) + ").V;\n";
                        }
                        else if (node.getParent().getKind().equals("Equals") && node.getParent().getChildren().get(1).getKind().equals("Variable") && this.checkIfField(node.getParent().getChildren().get(1)))
                            return "getfield(this, " + name + type + ")" + type;
                        else if (node.getParent().getKind().equals("Index") && this.checkIfField(node)) {
                            return "getfield(this, " + name + type + ")" + type;
                        }
                        else {
                            if (alreadyDone.containsKey(node))
                                return alreadyDone.get(node);

                            alreadyDone.put(node, "aux" + (alreadyDone.size() + 1) + type);
                            this.code += this.prefix + "aux" + alreadyDone.size() + type + " :=" + type + " getfield(this, " + name + type + ")" + type + ";\n";

                            if ((node.getParent().getKind().equals("Equals") || node.getParent().getKind().equals("returnDeclaration")) && node.getParent().getChildren().get(0).getKind().equals("Variable") && this.checkIfField(node.getParent().getChildren().get(0)))
                                this.code += this.prefix + "aux" + alreadyDone.size() + type + " :=" + type + " getfield(this, " + name + type + ")" + type + ";\n";
                            else if ((node.getParent().getKind().equals("LessThan")) && node.getKind().equals("Variable") && this.checkIfField(node))
                                this.code += this.prefix + "aux" + alreadyDone.size() + type + " :=" + type + " getfield(this, " + name + type + ")" + type + ";\n";
                            if (child.getType().isArray()) {
                                if (node.getParent().getKind().equals("Index"))
                                    return "aux" + alreadyDone.size();
                                else
                                    return "aux" + alreadyDone.size() + ".array.i32";
                            } else {
                                return "aux" + alreadyDone.size() + this.getType(child.getType().getName());
                            }
                        }
                    }
                }

                if (node.getChildren().get(0).get("Name").equals("io") || node.getChildren().get(0).get("Name").equals("ioPlus"))
                    return name;

                return name;
            case "Colon":
                return this.parseVar(node.getChildren().get(0)) + " ," + this.parseVar(node.getChildren().get(1));
            case "Index":
                String firstChild = this.parseVar(node.getChildren().get(0));
                List<String> splitFirstChild = Arrays.asList(firstChild.split("\\."));
                if (!firstChild.contains("getfield")) {
                    if (firstChild.contains("$"))
                        firstChild = splitFirstChild.get(0) + "." + splitFirstChild.get(1);
                    else
                        firstChild = splitFirstChild.get(0);
                }
                String secondChild = this.parseVar(node.getChildren().get(1));
                if (!secondChild.contains("aux") && node.getChildren().get(1).getKind().equals("Integer")) {
                    alreadyDone.put(node.getChildren().get(1), "aux" + (alreadyDone.size() + 1) + ".i32");
                    this.code += this.prefix + "aux" + (alreadyDone.size()) + ".i32 :=.i32 " + secondChild + ";\n";
                    secondChild = alreadyDone.get(node.getChildren().get(1));
                }

                if (firstChild.contains("getfield")) {
                    String getfieldType = "";
                    if (secondChild.contains("getfield")) {
                        List<String> getfieldTypeList = Arrays.asList(secondChild.split("\\)"));
                        getfieldType = getfieldTypeList.get(getfieldTypeList.size() - 1);
                        alreadyDone.put(node.getChildren().get(1), "aux" + (alreadyDone.size() + 1) + getfieldType);
                        this.code += this.prefix + "aux" + alreadyDone.size() + getfieldType + " :=" + getfieldType + " " + secondChild + ";\n";
                        secondChild = "aux" + (alreadyDone.size()) + getfieldType;
                    }
                    List<String> auxGetFieldType = Arrays.asList(firstChild.split("\\)"));
                    getfieldType = auxGetFieldType.get(auxGetFieldType.size() - 1);
                    alreadyDone.put(node.getChildren().get(0), "aux" + (alreadyDone.size() + 1) + getfieldType);
                    this.code += this.prefix + "aux" + alreadyDone.size() + getfieldType + " :=" + getfieldType + " " + firstChild + ";\n";
                    return "aux" + alreadyDone.size() + "[" + secondChild +  "].i32";
                }

                return firstChild + "[" + secondChild +  "].i32";
            case "NewObject":
                if (alreadyDone.get(node) != null)
                    return alreadyDone.get(node);
                return "." + node.get("Name");
            case "FunctionCall":
                if (alreadyDone.containsKey(node))
                    return alreadyDone.get(node);
                return "";
            default:
                return alreadyDone.get(node);
        }
    }

    private String sanitize(String name) {
        List<String> reserved = new ArrayList<>(List.of("field", "ret", "invoke", "putfield", "getfield", "array", "i32", "bool", "array.i32"));
        for (String s: reserved) {
            if (name.contains(s))
                return "sanitized_" + name + "_sanitized";
        }

        return name;
    }

    private String getType(String type) {
        switch(type) {
            case "Integer":
            case "Int":
            case "Index":
                return ".i32";
            case "Boolean":
                return ".bool";
            case "This":
                return "this";
            default:
                return "." + type;
        }
    }

    private void getPairs(JmmNode node) {
        this.getList(node, 0);
        this.pairs.sort(Comparator.comparing(p -> -p.getValue()));

        this.generateCode();
        this.pairs = new ArrayList<>();
    }

    public void generateCode() {
        if (this.pairs.isEmpty())
            return;
        this.pairs.sort(Comparator.comparing(p -> -p.getValue()));
        int maxVal = this.pairs.get(0).getValue();

        String type = "", auxString = "";
        List<String> aux = new ArrayList<>();
        JmmNode n;
        boolean alreadyPut = false;

        while (maxVal >= 0) {
            for (Pair<JmmNode, Integer> child : this.pairs) {
                if (child.getValue() == maxVal) {
                    auxString = "";
                    Optional<JmmNode> ifNode = child.getKey().getAncestor("If");
                    Optional<JmmNode> elseNode = child.getKey().getAncestor("Else");
                    Optional<JmmNode> whileNode = child.getKey().getAncestor("While");
                    auxString = "";
                    n = child.getKey();
                    switch (n.getKind()) {
                        case "Add":
                        case "Sub":
                        case "Mul":
                        case "Div":
                        case "And":
                        case "LessThan":
                            auxString = "";
                            aux = Arrays.asList(this.parseVar(n.getChildren().get(0)).split("\\."));
                            type = aux.get(aux.size()-1);
                            type = "." + type;

                            String leftChild = this.parseVar(n.getChildren().get(0));
                            if (n.getChildren().get(0).getKind().equals("Index")) {
                                alreadyDone.put(n.getChildren().get(0), "aux" + (alreadyDone.size() + 1) + ".i32");
                                this.code += this.prefix + alreadyDone.get(n.getChildren().get(0)) + " :=.i32 " + leftChild + ";\n";
                                leftChild = alreadyDone.get(n.getChildren().get(0));
                            }

                            if (n.getKind().equals("And") || n.getKind().equals("LessThan"))
                                auxString += ".bool " + leftChild;
                            else
                                auxString += type + " " + leftChild;

                            switch (n.getKind()) {
                                case "Add":
                                    auxString += " +";
                                    break;
                                case "Sub":
                                    auxString += " -";
                                    break;
                                case "Mul":
                                    auxString += " *";
                                    break;
                                case "Div":
                                    auxString += " /";
                                    break;
                                case "And":
                                    auxString += " &&";
                                    break;
                                case "LessThan":
                                    auxString += " <";
                                    break;
                            }

                            String rightChild = this.parseVar(n.getChildren().get(1));
                            if (n.getChildren().get(1).getKind().equals("Index")) {
                                alreadyDone.put(n.getChildren().get(1), "aux" + (alreadyDone.size() + 1) + ".i32");
                                this.code += this.prefix + alreadyDone.get(n.getChildren().get(1)) + " :=.i32 " + rightChild + ";\n";
                                rightChild = alreadyDone.get(n.getChildren().get(1));
                            }

                            auxString += type + " " + rightChild + ";\n";

                            if ((ifNode.isPresent() || elseNode.isPresent() || whileNode.isPresent()) && !n.getParent().getKind().equals("Equals")) {
                                if (!n.getParent().getKind().equals("Condition")) {
                                    if (!n.getKind().equals("Add") && !n.getKind().equals("Sub") && !n.getKind().equals("Mul") && !n.getKind().equals("Div"))
                                        type = ".bool";
                                    alreadyDone.put(n, "aux" + (this.alreadyDone.size() + 1) + type);
                                    this.code += this.prefix + alreadyDone.get(n) + ":=" + auxString;
                                }
                                else
                                    return;
                            }
                            break;
                        case "FunctionCall":
                            if (!this.functionName.equals("main")) {
                                if (!this.table.getMethods().contains(n.getChildren().get(1).get("Name"))) {
                                    if (n.getParent().getKind().equals("Equals")) {
                                        type += this.getType(n.getParent().getChildren().get(0).getKind());
                                    }
                                }
                                else {
                                    type = "";
                                    if (this.table.getReturnType(n.getChildren().get(1).get("Name")) != null) {
                                        if (this.table.getReturnType(n.getChildren().get(1).get("Name")).isArray()) type += ".array";
                                    }
                                    type += this.getType(this.table.getReturnType(n.getChildren().get(1).get("Name")).getName());
                                }
                                auxString += this.invokeFunctionCall(n);

                            }
                            else {
                                auxString += this.invokeFunctionCall(n);
                                aux = Arrays.asList(auxString.split("\""));
                                if (n.getChildren().get(0).getKind().equals("Variable")) {
                                    if (this.getType(n.getChildren().get(0).getChildren().get(0).get("Name")).substring(1).equals(n.getChildren().get(0).getChildren().get(0).get("Name")))
                                        type = this.getType(n.getChildren().get(0).getChildren().get(0).get("Name"));
                                    else {
                                        if (this.table.getReturnType(aux.get(1)).isArray())
                                            type = ".array.i32";
                                        else
                                            type = this.getType(this.table.getReturnType(aux.get(1)).getName());
                                    }
                                }
                            }

                            if (!auxString.contains("invokestatic")) {
                                List<String> auxStringList = Arrays.asList(auxString.split(" "));
                                type = auxStringList.get(0);
                            }


                            if (n.getParent().getKind().equals("Equals") || n.getParent().getKind().equals("Condition")) {
                                if (n.getParent().getChildren().get(0).getKind().equals("Index") && !auxString.contains("invokestatic")) {
                                    if (n.getParent().getKind().equals("Equals") && n.getParent().getChildren().get(0).getKind().equals("Index")) {
                                        type = Arrays.asList(auxString.split(" ")).get(0);
                                        alreadyDone.put(n, "aux" + (this.alreadyDone.size() + 1) + type);
                                        auxString = this.prefix + alreadyDone.get(n) + " :=" + auxString + ";\n";
                                        auxString += this.prefix + this.parseVar(n.getParent().getChildren().get(0)) + " :=" + type + " " + alreadyDone.get(n) + ";\n";
                                        this.code += auxString;
                                    }
                                    else
                                        this.code += this.prefix + this.parseVar(n.getParent().getChildren().get(0)) + ":=" + auxString + ";\n";
                                    return;
                                }
                                else {
                                    for (JmmNode equalsChild : n.getParent().getChildren()) {
                                        if (equalsChild.getKind().equals("Index")) {
                                            if (n.getParent().getKind().equals("Equals") && n.getParent().getChildren().get(0).getKind().equals("Index")) {
                                                List<String> divideAuxString = Arrays.asList(auxString.split(":="));
                                                type = Arrays.asList(divideAuxString.get(1).split(" ")).get(0);
                                                alreadyDone.put(n, "aux" + (this.alreadyDone.size() + 1) + type);
                                                auxString = this.prefix + alreadyDone.get(n) + " :=" + divideAuxString.get(1);
                                                auxString += this.prefix + divideAuxString.get(0) + ":=" + type + " " + alreadyDone.get(n) + ";\n";
                                                this.code += auxString;
                                            }
                                            else
                                                this.code += this.prefix + auxString;
                                            return;
                                        }
                                    }
                                }
                            }

                            if (n.getParent().getKind().equals("Equals") && n.getParent().getChildren().get(1).getKind().equals("FunctionCall")) {
                                if (!type.equals(".i32") && !type.equals(".array.i32") && !type.equals(".bool")) {
                                    type = Arrays.asList(auxString.split(" ")).get(0);
                                }
                                alreadyDone.put(n, "aux" + (alreadyDone.size() + 1) + type);
                            }
                            break;
                        case "NewObject":
                            type = this.parseVar(n);
                            if (n.getParent().getKind().equals("Equals") && n.getParent().getParent().getKind().equals("Body"))
                                auxString += type + " new(" + type.substring(1) + ")." + type.substring(1) + ";\n";
                            else
                                auxString += "new(" + type.substring(1) + ")." + type.substring(1) + ";\n";
                            if (n.getParent().getKind().equals("Equals"))
                                alreadyDone.put(n, this.parseVar(n.getParent().getChildren().get(0)));
                            else
                                alreadyDone.put(n, "aux" + (this.alreadyDone.size() + 1) + type);
                            alreadyPut = true;
                            auxString += this.invokeNewObject(n);
                            break;
                        case "Equals":
                            String s = this.parseVar(n.getChildren().get(0));
                            if (n.getParent().getKind().equals("Body") && s.contains("putfield(")) {
                                this.code += this.prefix + s;
                                return;
                            }
                            if (!s.contains("putfield(") && !s.contains("getfield(") && maxVal > 0 && ifNode.isEmpty() && whileNode.isEmpty() && elseNode.isEmpty()) {
                                auxString = "";
                                auxString += this.prefix + s;
                                auxString += " :=";
                                if (maxVal == 0) {
                                    aux = Arrays.asList(s.split("\\."));
                                    type = aux.get(aux.size() - 1);
                                    type = "." + type;
                                }
                            }
                            else {
                                String firstChild, secondChild;
                                firstChild = this.prefix + this.parseVar(n.getChildren().get(0)) + " :=";
                                if (maxVal == 0 || ifNode.isPresent() || whileNode.isPresent() || elseNode.isPresent()) {
                                    aux = Arrays.asList(this.parseVar(n.getChildren().get(0)).split("\\."));
                                    type = aux.get(aux.size()-1);
                                    type = "." + type;
                                }

                                if (n.getChildren().get(1).getKind().equals("Integer"))
                                    firstChild = this.prefix + s + " :=";

                                secondChild = type + " " + this.parseVar(n.getChildren().get(1)) + ";\n";

                                if (n.getChildren().get(1).getKind().equals("Index")) {
                                    alreadyDone.put(n, "aux" + (alreadyDone.size() + 1) + ".i32");
                                    this.code += this.prefix + alreadyDone.get(n) + " :=" + secondChild;
                                    this.code += firstChild + ".i32 aux" + (alreadyDone.size()) + ".i32;\n";
                                    return;
                                }

                                this.code += firstChild + secondChild;

                                return;

                            }
                            this.code += auxString;
                            return;
                        case "Index":
                            if (n.getParent().getKind().equals("Equals") && n.getParent().getChildren().get(0).getKind().equals("Index")) {
                                List<Pair<JmmNode, Integer>> pairsAux = new ArrayList<>();
                                for (int x = this.pairs.indexOf(child) + 1; x < this.pairs.size(); x++)
                                    pairsAux.add(this.pairs.get(x));
                                this.pairs = pairsAux;
                                this.generateCode();
                                return;
                            }

                            aux = Arrays.asList(this.parseVar(n.getChildren().get(1)).split("\\."));
                            type = aux.get(aux.size()-1);
                            type = ".i32";
                            List<String> splitString;
                            boolean hasInteger = false;
                            if (maxVal <= 1 && n.getParent().getKind().equals("Equals")) {
                                for (JmmNode childNode: n.getChildren()) {
                                    if (childNode.getKind().equals("Integer")) {
                                        hasInteger = true;
                                        String number = this.parseVar(childNode);
                                        if (n.getKind().equals("Index") && n.getChildren().get(0).equals(childNode))
                                            alreadyDone.put(childNode, "aux" + (alreadyDone.size() + 1) + ".array.i32");
                                        else
                                            alreadyDone.put(childNode, "aux" + (alreadyDone.size() + 1) + ".i32");
                                        this.code += this.prefix + alreadyDone.get(childNode) + " :=.i32 " + number + ";\n";
                                        if (n.getParent().getChildren().get(1).getKind().equals("FunctionCall"))
                                            this.code += this.prefix + this.parseVar(n.getParent().getChildren().get(0)) + ":=";
                                        else {
                                            if (this.checkIfField(n.getChildren().get(0))) {
                                                if (n.getKind().equals("Index"))
                                                    type = ".array.i32";
                                                this.code += this.prefix + "aux" + (alreadyDone.size() + 1) + type + " :=" + type + " getfield(this, " + n.getChildren().get(0).getChildren().get(0).get("Name") + type + ")" + type + ";\n";
                                            }
                                            this.code += this.prefix + this.parseVar(n.getParent().getChildren().get(0)) + ":=.i32 " + this.parseVar(n.getParent().getChildren().get(1)) + ";\n";
                                        }
                                    }
                                }

                                if (!hasInteger) {
                                    String aux1 = this.parseVar(n.getParent().getChildren().get(0));
                                    String aux2 = this.parseVar(n.getParent().getChildren().get(1));

                                    if (aux1 != null && aux2 != null && !aux1.equals("") && !aux2.equals(""))
                                        this.code += this.prefix + aux1 + ":=.i32 " + aux2 + ";\n";
                                }

                                for (JmmNode childEquals: n.getParent().getChildren()) {
                                    if (childEquals.getKind().equals("FunctionCall")) {
                                        String funcAux = this.invokeFunctionCall(childEquals);
                                        if (hasInteger)
                                            this.code += funcAux + ";\n";
                                        else
                                            this.code += this.prefix + funcAux;
                                        return;
                                    }
                                }
                            }
                            else if (n.getParent().getKind().equals("Index") || n.getParent().getKind().equals("Colon")) {
                                String before = this.parseVar(n.getChildren().get(1));
                                alreadyDone.put(n.getChildren().get(1), "aux" + (alreadyDone.size() + 1) + ".i32");
                                this.code += this.prefix + "aux" + alreadyDone.size() + ".i32 :=.i32 " + before + ";\n";
                            }

                            if (n.getParent().getKind().equals("Equals"))
                                return;

                            break;
                        case "NewArray":
                            auxString += ".array.i32 new(array, " + this.parseVar(n.getChildren().get(0)) + ")";
                            type = ".array.i32";
                            break;
                        case "Length":
                            aux = Arrays.asList((this.parseVar(n.getChildren().get(0)) + ")").split("\\."));
                            if (aux.get(aux.size() - 1).contains(")"))
                                type = "." + aux.get(aux.size() - 1).substring(0, aux.get(aux.size() - 1).length() - 1);
                            else
                                type = aux.get(aux.size() - 1).substring(0, aux.size() - 1);
                            if (type.contains("\\."))
                                if (n.getParent().getKind().equals("Equals"))
                                    auxString += "." + type + " ";
                            auxString += type + " " + "arraylength(" + this.parseVar(n.getChildren().get(0)) + ")";
                            break;
                        case "Not":
                            type = ".bool";
                            auxString =".bool !.bool " + this.parseVar(n.getChildren().get(0)) + ";\n";
                            if ((ifNode.isPresent() || elseNode.isPresent() || whileNode.isPresent()) && !n.getParent().getKind().equals("Equals")) {
                                if (!n.getParent().getKind().equals("Condition")) {
                                    alreadyDone.put(n, "aux" + (this.alreadyDone.size() + 1) + ".bool");
                                    this.code += this.prefix + "aux" + this.alreadyDone.size() + ".bool :=" + auxString;
                                }
                                else
                                    return;
                            }
                            if (n.getParent().getKind().equals("Equals") && maxVal <= 1) {
                                this.code += this.prefix + this.parseVar(n.getParent().getChildren().get(0)) + ":=" + auxString;
                                return;
                            }
                            else if ( n.getParent().getKind().equals("returnDeclaration")) {
                                alreadyDone.put(n, "aux" + (this.alreadyDone.size() + 1) + ".bool");
                                this.code += this.prefix + this.parseVar(n.getParent().getChildren().get(0)) + ":=" + auxString;
                                return;
                            }
                            else if (n.getParent().getKind().equals("Equals"))
                                return;
                            break;
                        default:
                            break;
                    }
                    if (n.getKind().equals("LessThan") || n.getKind().equals("And"))
                        type = ".bool";
                    if ((n.getParent().getKind().equals("Condition") || ifNode.isPresent() || elseNode.isPresent() || whileNode.isPresent()) && (!n.getParent().getKind().equals("Equals"))) {
                        if (n.getKind().equals("FunctionCall")) {
                            if (!n.getParent().getKind().equals("If") && !n.getParent().getKind().equals("Else") && !n.getParent().getKind().equals("While")) {
                                if (auxString.contains("invokestatic") && type.equals("")) {
                                    JmmNode parent = n.getParent();
                                    while(!parent.getKind().equals("Equals"))
                                        parent = parent.getParent();

                                    List<String> auxParsedVarList = Arrays.asList(this.parseVar(parent.getChildren().get(0)).split("\\."));
                                    boolean containsBracket = false;
                                    for (String sAux: auxParsedVarList) {
                                        if (containsBracket) {
                                            if (sAux.contains("]"))
                                                containsBracket = false;
                                            continue;
                                        }
                                        if (auxParsedVarList.indexOf(sAux) == 0) {
                                            if (sAux.contains("[")) {
                                                containsBracket = true;
                                                continue;
                                            }
                                            continue;
                                        }
                                        if (sAux.contains("[")) {
                                            containsBracket = true;
                                            continue;
                                        }
                                        type += sAux;
                                        if (auxParsedVarList.indexOf(sAux) != auxParsedVarList.size()-1)
                                            type += ".";
                                    }
                                    type = "." + type;
                                    auxString += type;
                                }
                                alreadyDone.put(n, "aux" + (this.alreadyDone.size() + 1) + type);
                                auxString = this.prefix + alreadyDone.get(n) + " :=" + type + " " + auxString + ";\n";
                            }
                            else if (auxString.contains("invokevirtual"))
                                auxString = this.prefix + auxString + ";\n";
                            else
                            auxString = this.prefix + auxString ;
                            this.code += auxString;
                        }
                        else if (n.getKind().equals("Length")) {
                            alreadyDone.put(n, "aux" + (this.alreadyDone.size() + 1) + type);
                            auxString = this.prefix + alreadyDone.get(n) + " :=" + auxString + ".i32;\n";
                            this.code += auxString;
                        }
                        else if (!n.getParent().getKind().equals("Condition") && !n.getParent().getKind().equals("Arguments") && ifNode.isEmpty() && elseNode.isEmpty() && whileNode.isEmpty()){
                            this.code += this.prefix + alreadyDone.get(n) + " :=" + auxString;
                        }
                        continue;
                    }
                    else if (maxVal <= 1 && this.pairs.size() != 1 && maxVal - this.colonRemover() <= 1) {
                        if (maxVal > 0 && ((!n.getKind().equals("FunctionCall") || !n.getParent().getKind().equals("Body"))) && (!auxString.contains("invokestatic"))) {
                            JmmNode root = n;
                            while(!root.getKind().equals("Program") && !root.getKind().equals("returnDeclaration") && !root.getKind().equals("Condition")) {
                                root = root.getParent();
                            }
                            if (root.getKind().equals("returnDeclaration") || root.getKind().equals("Condition")) {
                                if (n.getChildren().get(0).getKind().equals("This")) {
                                    alreadyDone.put(n, "aux" + (this.alreadyDone.size() + 1) + type);
                                    auxString = this.prefix + alreadyDone.get(n) + " :=" + auxString + ";\n";
                                    if (!root.getKind().equals("Condition"))
                                        auxString += this.prefix + "ret" + type + " " + alreadyDone.get(n) + ";\n";
                                    this.code += auxString;
                                    return;
                                }
                                else {
                                    aux = Arrays.asList(this.parseVar(n.getChildren().get(0)).split("\\."));
                                    type = aux.get(aux.size() - 1);
                                    type = "." + type;
                                    if (n.getKind().equals("NewArray"))
                                        auxString += ".array.i32;\n";

                                    if (auxString.contains("arraylength"))
                                        auxString += ".i32;\n";

                                    alreadyDone.put(n, "aux" + (this.alreadyDone.size() + 1) + type);
                                    if (!n.getParent().getKind().equals("If") && !n.getParent().getKind().equals("Else") && !n.getParent().getKind().equals("While"))
                                        auxString = this.prefix + "aux" + (this.alreadyDone.size()) + type + " :=" + auxString;
                                    else
                                        auxString = this.prefix + "aux" + (this.alreadyDone.size()) + type + " :=" + auxString + this.prefix + "ret" + type + " " + "aux" + (this.alreadyDone.size() + 1) + type + ";\n";
                                }
                            }
                            else {
                                if (n.getParent().getKind().equals("If") || n.getParent().getKind().equals("Else"))
                                    aux = Arrays.asList(this.parseVar(n.getChildren().get(0)).split("\\."));
                                else
                                    aux = Arrays.asList(this.parseVar(n.getParent().getChildren().get(0)).split("\\."));
                                if (!type.equals(".i32") && !type.equals(".array.i32") && !type.equals(".bool")) {
                                    type = "";
                                    for (String sAux : aux) {
                                        if ((aux.size() > 2 && aux.indexOf(sAux) == 0) || (aux.size() > 1 && aux.indexOf(sAux) == 0 && (n.getKind().equals("NewObject") || n.getKind().equals("Length"))))
                                            continue;
                                        type += sAux;
                                        if (aux.indexOf(sAux) != aux.size() - 1)
                                            type += ".";
                                    }
                                    type = "." + type;
                                }
                                if (n.getKind().equals("NewObject"))
                                    auxString = this.prefix + aux.get(0) + type + " :=" + auxString;
                                else if (n.getKind().equals("FunctionCall")) {
                                    auxString = this.prefix + alreadyDone.get(n) + " :=" + auxString + ";\n";
                                    auxString += this.prefix + this.parseVar(n.getParent().getChildren().get(0)) + " :=" + type + " " + alreadyDone.get(n);
                                }
                                else {
                                    if (n.getParent().getKind().equals("Equals") && n.getParent().getChildren().get(0).getKind().equals("Index")) {
                                        type = Arrays.asList(auxString.split(" ")).get(0);
                                        alreadyDone.put(n, "aux" + (this.alreadyDone.size() + 1) + type);
                                        auxString = this.prefix + alreadyDone.get(n) + " :=" + auxString;
                                        auxString += this.prefix + this.parseVar(n.getParent().getChildren().get(0)) + " :=" + type + " " + alreadyDone.get(n) + ";\n";
                                    }
                                    else
                                        auxString = this.prefix + this.parseVar(n.getParent().getChildren().get(0)) + " :=" + auxString;
                                }
                                if (n.getKind().equals("FunctionCall") && n.getParent().getKind().equals("Equals"))
                                    auxString += ";\n";
                                else if (!n.getKind().equals("Add") && !n.getKind().equals("Sub") && !n.getKind().equals("Mul") && !n.getKind().equals("Div") && !n.getKind().equals("And") && !n.getKind().equals("LessThan") && !auxString.contains("invokespecial"))
                                    auxString += type + ";\n";
                            }
                        }
                        else auxString = this.prefix + auxString;

                        if (n.getParent().getKind().equals("Body") && auxString.contains("invokevirtual"))
                            auxString += ";\n";

                        this.code += auxString;

                        if (n.getParent().getKind().equals("Equals") && n.getParent().getChildren().get(0).getKind().equals("Variable") && this.checkIfField(n.getParent().getChildren().get(0))) {
                            aux = Arrays.asList(auxString.split(":="));
                            String ident = aux.get(0).replaceAll(" ", "");
                            List<String> identAux = Arrays.asList(ident.split("\\."));
                            type = ".";
                            for (String identS: identAux) {
                                if (identS.contains("aux"))
                                    continue;
                                else
                                    type += identS;
                                if (identAux.indexOf(identS) != identAux.size() - 1)
                                    type += ".";
                            }
                            this.code += this.prefix + "putfield(this, " + this.sanitize(n.getParent().getChildren().get(0).getChildren().get(0).get("Name")) + type + ", " + ident + ").V;\n";
                        }

                        return;
                    }
                    else {
                        if ((!type.equals(".V") || !n.getKind().equals("NewObject")) && !(n.getParent().getKind().equals("Body") && n.getKind().equals("FunctionCall"))) {
                            if (!n.getParent().getKind().equals("If") && !n.getParent().getKind().equals("Else") && !n.getParent().getKind().equals("While")) {
                                if ((!n.getParent().equals("Body") && auxString.contains("invokestatic") || (n.getKind().equals("NewObject") && !n.getParent().getKind().equals("Body")))) {
                                    if (type.equals("")) {
                                        JmmNode parent = n.getParent();
                                        while(!parent.getKind().equals("Equals"))
                                            parent = parent.getParent();

                                        List<String> auxParsedVarList = Arrays.asList(this.parseVar(parent.getChildren().get(0)).split("\\."));
                                        boolean containsBracket = false;
                                        for (String sAux: auxParsedVarList) {
                                            if (containsBracket) {
                                                if (sAux.contains("]"))
                                                    containsBracket = false;
                                                continue;
                                            }
                                            if (auxParsedVarList.indexOf(sAux) == 0) {
                                                if (sAux.contains("[")) {
                                                    containsBracket = true;
                                                    continue;
                                                }
                                                continue;
                                            }
                                            if (sAux.contains("[")) {
                                                containsBracket = true;
                                                continue;
                                            }
                                            type += sAux;
                                            if (auxParsedVarList.indexOf(sAux) != auxParsedVarList.size()-1)
                                                type += ".";
                                        }
                                        type = "." + type;
                                    }
                                    auxString = type + " " + auxString;
                                }
                                else if (!n.getParent().getKind().equals("Equals") && n.getKind().equals("Index") && !n.getParent().getKind().equals("If") && !n.getParent().getKind().equals("Else") && !n.getParent().getKind().equals("While"))
                                    auxString = type + " " + auxString;
                            }

                            if (n.getKind().equals("NewObject"))
                                auxString = this.prefix + "aux" + (this.alreadyDone.size()) + type + " :=" + auxString;
                            else {
                                if (type.equals("")) {
                                    List<String> temp = Arrays.asList(auxString.split("\\."));
                                    type = "." + temp.get(temp.size() - 1);
                                }
                                if (!n.getParent().getKind().equals("If")) {
                                    if (n.getKind().equals("Index"))
                                        auxString = this.prefix + "aux" + (this.alreadyDone.size() + 1) + type + " :=.i32 " + auxString;
                                    else {
                                        if (!n.getParent().getKind().equals("If") && !n.getParent().getKind().equals("Else") && !n.getParent().getKind().equals("While")) {
                                            if (!type.equals(".i32") && !type.equals(".array.i32") && !type.equals(".bool")) {
                                                type = Arrays.asList(auxString.split(" ")).get(0);
                                            }
                                            auxString = this.prefix + "aux" + (this.alreadyDone.size() + 1) + type + " :=" + auxString;
                                        }
                                        else
                                            auxString = this.prefix + auxString;
                                    }
                                    if (auxString.contains("invokestatic"))
                                        if (!auxString.contains(".V;\n"))
                                            auxString += ".V;\n";
                                }
                                else {
                                    List<String> splitArr;
                                    splitArr = Arrays.asList(auxString.split(" "));
                                    if (!n.getParent().getKind().equals("If") && !n.getParent().getKind().equals("Else") && !n.getParent().getKind().equals("While")) {
                                        auxString = "";
                                        for (String splitArrS : splitArr) {
                                            if (splitArr.indexOf(splitArrS) == 0)
                                                continue;
                                            else
                                                auxString += splitArrS;
                                        }
                                    }
                                    auxString = this.prefix + auxString;
                                }
                            }

                            if ((auxString.contains("invoke") || n.getKind().equals("NewArray")) && !auxString.contains("invokestatic") && !(n.getKind().equals("NewObject") && !n.getParent().getKind().equals("Body"))) {
                                if (n.getKind().equals("NewArray"))
                                    auxString += type;
                                auxString += ";\n";
                            }
                            else if (n.getKind().equals("Length") && !n.getParent().getKind().equals("Equals"))
                                auxString += type + ";\n";
                            if (!alreadyPut && !n.getParent().getKind().equals("If") && !n.getParent().getKind().equals("Else") && !n.getParent().getKind().equals("While"))
                                alreadyDone.put(n, "aux" + (this.alreadyDone.size() + 1) + type);

                            alreadyPut = false;
                        }
                        else {
                            if (n.getKind().equals("FunctionCall")) {
                                aux = Arrays.asList(auxString.split("\\."));
                                type = aux.get(1);
                                aux = Arrays.asList(type.split(","));
                                type = aux.get(0);
                                if (!n.getParent().getKind().equals("Body")) {
                                    type = "." + type;
                                    auxString = this.prefix + auxString + type + ";\n";
                                }
                                else {
                                    if (!auxString.contains("invokestatic"))
                                        auxString = this.prefix + auxString + ";\n";
                                    else
                                        auxString = this.prefix + auxString;
                                }
                            }
                            else
                                auxString = this.prefix + auxString;
                        }
                    }
                    this.code += auxString;
                }
            }
            maxVal--;
        }
    }

    public void getList(JmmNode node, int depth) {
        for (JmmNode child2 : node.getChildren()) {
            this.getList(child2, depth + 1);
        }

        if (!node.getKind().equals("Arguments") && !node.getKind().equals("Variable") && !node.getKind().equals("Integer") && !node.getKind().equals("Colon") && !node.getKind().equals("FunctionName") && !node.getKind().equals("Identifier") && !node.getKind().equals("This") && !node.getKind().equals("False") && !node.getKind().equals("True"))
            this.addPair(new Pair<>(node, depth));
    }

    public int colonRemover() {
        int cnt = 0;
        for (int x = 0; x < this.pairs.size(); x++) {
            if (this.pairs.get(x).getKey().getKind().equals("Colon"))
                cnt++;
        }
        return cnt;
    }

    public void addPair(Pair<JmmNode, Integer> p) {
        if (!this.pairs.contains(p))
            this.pairs.add(p);
    }

    public void addIfPair(List<Pair<JmmNode, Integer>> p) {
        if (!this.ifPairs.contains(p))
            this.ifPairs.add(p);
    }

    public String invokeFunctionCall(JmmNode n) {
        String res = "", name = n.getChildren().get(1).get("Name"), leftChild = "";

        switch (n.getChildren().get(0).getKind()) {
            case "NewObject":
            case "Variable":
                leftChild = this.parseVar(n.getChildren().get(0));
                break;
            case "This":
                leftChild = "this";
                break;
        }

        if (name.equals(this.table.getClassName())) {
            res += "invokespecial(";
            if (this.alreadyDone.containsKey(n.getChildren().get(0)))
                res += this.alreadyDone.get(n.getChildren().get(0));
            res += ", \"<init>\").V;\n";
        }
        else if (!nodeParser.checkIfImportedMember(n, reports).equals("") || this.checkIfFromImported(n)) {
            String type = "";
            if (n.getParent().getKind().equals("Equals")) {

                res += this.parseVar(n.getParent().getChildren().get(0));
                List<String> aux = Arrays.asList(this.parseVar(n.getParent().getChildren().get(0)).split("\\."));
                boolean containsBracket = false;
                for (String sAux: aux) {
                    if (containsBracket) {
                        if (sAux.contains("]"))
                            containsBracket = false;
                        continue;
                    }
                    if (aux.indexOf(sAux) == 0) {
                        if (sAux.contains("[")) {
                            containsBracket = true;
                            continue;
                        }
                        continue;
                    }
                    if (sAux.contains("[")) {
                        containsBracket = true;
                        continue;
                    }
                    type += sAux;
                    if (aux.indexOf(sAux) != aux.size()-1)
                        type += ".";
                }
                type = "." + type;
                res += " :=" + type + " ";
                aux = Arrays.asList(leftChild.split("\\."));
                leftChild = aux.get(aux.size() - 1);
            }

            res += "invokestatic(" + leftChild + ",\"" + n.getChildren().get(1).get("Name") + "\"";

            if (!n.getChildren().get(2).getChildren().isEmpty())
                res += ",";

            String parsedChild = "";
            for (JmmNode child: n.getChildren().get(2).getChildren()) {
                parsedChild = this.parseVar(child);
                if (parsedChild.contains("[")) {
                    this.code += this.prefix + "aux" + (alreadyDone.size() + 1) + ".i32 :=.i32 " + parsedChild + ";\n";
                    res += "aux" + (alreadyDone.size() + 1) + ".i32";
                }
                else
                    res += parsedChild;
                if (n.getChildren().get(2).getChildren().indexOf(child) < n.getChildren().get(2).getChildren().size()-1)
                    res += ",";
            }

            res += ")";
            if (n.getParent().getKind().equals("Equals"))
                res += type + ";\n";
            else if (n.getParent().getKind().equals("Body") || n.getParent().getKind().equals("If") || n.getParent().getKind().equals("Else") || n.getParent().getKind().equals("While") || (n.getParent().getKind().equals("Equals") && n.getParent().getParent().getKind().equals("Body")))
                res += ".V;\n";
        }
        else {
            Type t = this.table.getReturnType(n.getChildren().get(1).get("Name"));
            String type = "";
            if (t != null) {
                if (t.isArray()) type = ".array";
                type += this.getType(t.getName());
            }

            if (n.getParent().getKind().equals("Equals")) {
                List<String> aux = new ArrayList<>();
                String aux2 = "";
                type = ".";
                aux2 = this.parseVar(n.getParent().getChildren().get(0));
                aux = Arrays.asList(aux2.split("\\."));
                if (aux2.contains("$")) {
                    for (int x = 2; x < aux.size(); x++) {
                        type += aux.get(x);
                        if (x < aux.size() - 1)
                            type += ".";
                    }
                }
                else {
                     for (int x = 1; x < aux.size(); x++) {
                         if (aux.get(x).contains("]"))
                             continue;
                        type += aux.get(x);
                        if (x < aux.size() - 1)
                            type += ".";
                    }
                }
            }


            if (n.getParent().getKind().equals("Body") || n.getParent().getKind().equals("If") || n.getParent().getKind().equals("Else") || n.getParent().getKind().equals("While"))
                res += "invokevirtual(" + leftChild + ",\"" + name + "\"";
            else
                res += type +  " " + "invokevirtual(" + leftChild + ",\"" + name + "\"";

            if (!n.getChildren().get(2).getChildren().isEmpty()) {
                res += ",";
                String auxArg;
                for (JmmNode child : n.getChildren().get(2).getChildren()) {
                    auxArg = this.parseVar(child);
                    List<String> auxArgList = Arrays.asList(auxArg.split(","));
                    int auxArgStringCnt = -1;
                    for (String auxArgString: auxArgList) {
                        auxArgStringCnt++;
                        if (auxArgString.contains("[")) {
                            alreadyDone.put(child.getChildren().get(auxArgStringCnt), "aux" + (alreadyDone.size() + 1) + type);
                            this.code += this.prefix + "aux" + alreadyDone.size() + ".i32" + " :=.i32 " + auxArgString + ";\n";
                            res += "aux" + alreadyDone.size() + ".i32";
                        } else
                            res += auxArgString;
                        if (auxArgList.indexOf(auxArgString) < auxArgList.size() - 1)
                            res += ",";
                    }
                }
            }

            res += ")" + type;
        }

        return res;
    }

    public String invokeNewObject(JmmNode n) {
        String res = this.prefix;

        res += "invokespecial(";
        if (this.alreadyDone.containsKey(n))
            res += this.alreadyDone.get(n);
        res += ", \"<init>\").V;\n";

        return res;
    }

    private boolean checkIfField(JmmNode node) {
        JmmNode parentFunc = node;
        while(!parentFunc.getKind().equals("MainDeclaration") && !parentFunc.getKind().equals("Function"))
            parentFunc = parentFunc.getParent();

        String funcName;
        if (parentFunc.getKind().equals("MainDeclaration"))
            funcName = "main";
        else funcName = parentFunc.get("Name");

        List<List<Symbol>> splitLocalVars = new ArrayList<>();
        List<Symbol> localVars = this.table.getLocalVariables(funcName);
        List<Symbol> aux = new ArrayList<>();

        for (Symbol s: localVars) {
            if (s == null) {
                splitLocalVars.add(aux);
                aux = new ArrayList<>();
            }
            else {
                aux.add(s);
            }
        }



        for (Symbol child: this.table.getLocalVariables(funcName)) {
            if (child == null)
                continue;
            if (child.getName().equals(node.getChildren().get(0).get("Name")))
                return false;
        }

        for (Symbol child: this.table.getParameters(funcName)) {
            if (child == null)
                continue;
            if (child.getName().equals(node.getChildren().get(0).get("Name")))
                return false;
        }

        for (Symbol child: this.table.getFields()) {
            if (child.getName().equals(node.getChildren().get(0).get("Name")))
                return true;
        }
        return false;
    }

    private boolean checkIfFromImported(JmmNode node) {
        if (this.table.getMethods().contains(node.getChildren().get(1).get("Name")))
            return false;

        if (node.getChildren().get(0).getKind().equals("Variable")) {
            JmmNode parent = node;
            while(!parent.getKind().equals("Function") && !parent.getKind().equals("MainDeclaration"))
                parent = parent.getParent();
            String funcName;
            if (parent.getKind().equals("MainDeclaration"))
                funcName = "main";
            else
                funcName = parent.get("Name");
            for (Symbol s: this.table.getLocalVariables(funcName)) {
                if (s == null)
                    continue;
                if (s.getName().equals(node.getChildren().get(0).getChildren().get(0).get("Name")))
                    return false;
            }
            for (Symbol s: this.table.getFields()) {
                if (s.getName().equals(node.getChildren().get(0).getChildren().get(0).get("Name")))
                    return false;
            }
            for (Symbol s: this.table.getParameters(funcName)) {
                if (s == null)
                    continue;
                if (s.getName().equals(node.getChildren().get(0).getChildren().get(0).get("Name")))
                    return false;
            }
            if (this.nodeParser.parseNodeType(node.getChildren().get(0), reports).equals(this.table.getClassName()))
                return false;
            if (this.nodeParser.parseNodeType(node.getChildren().get(0), reports).equals(this.table.getSuper()))
                return false;
        }

        if (node.getChildren().get(0).getKind().equals("NewObject"))
            return false;

        if (this.table.getClassName().equals(node.getChildren().get(1).get("Name")))
            return false;

        if (this.table.getSuper() == null)
            return true;

        return !this.table.getSuper().equals(node.getChildren().get(1).get("Name"));
    }
}

