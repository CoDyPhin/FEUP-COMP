package ast;

import pt.up.fe.comp.jmm.JmmNode;
import table.Method;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import table.Table;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class NodeParser {
    private Table table;
    private List<String> parents = Arrays.asList("Add", "Sub", "Mul", "Div", "Equals");
    private List<String> ops = Arrays.asList("Add", "Sub", "Mul", "Div");
    private String functionName;

    public NodeParser(Table table, String functionName) {
        this.table = table;
        this.functionName = functionName;
    }

    public NodeParser(Table table) {
        this.table = table;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String parseNodeType(JmmNode node, List<Report> reports) {
        String type = "";

        boolean rightChild = false;

        if (node.getKind().equals("FunctionCall") || node.getParent().getKind().equals("Length") || node.getKind().equals("LessThan") || node.getKind().equals("And") || node.getKind().equals("Colon") || node.getKind().equals("Not") || node.getKind().equals("True") || node.getKind().equals("False") || node.getKind().equals("Integer") || node.getKind().equals("Int") || node.getKind().equals("Variable") || node.getKind().equals("Boolean") || ops.contains(node.getParent().getKind()) || ops.contains(node.getKind()))
            rightChild = true;

        if (node.getParent().getChildren().size() < 1) {
            if (node.getParent().getChildren().get(1).equals(node) && (node.getKind().equals("Variable") || node.getKind().equals("Index")))
                rightChild = true;
        }

        switch(node.getKind()) {
            case "Integer":
            case "Int":
                if (node.getChildren().isEmpty())
                    return "Int";
                return "Int Array";
            case "True":
            case "False":
            case "Boolean":
                return "Boolean";
            case "Variable":
                if (node.getParent().getKind().equals("Equals")) {
                    if (node.getParent().getChildren().get(1).getKind().equals("FunctionCall")) {
                        if (!this.table.getMethods().contains(node.getParent().getChildren().get(1).getChildren().get(1).get("Name"))) {
                            if (this.functionName == null) {
                                JmmNode funcNode = node;
                                while (!funcNode.getKind().equals("Function") && !funcNode.getKind().equals("MainDeclaration")) {
                                    funcNode = funcNode.getParent();
                                }
                                if (funcNode.getKind().equals("MainDeclaration"))
                                    this.functionName = "main";
                                else this.functionName = funcNode.get("Name");
                            }
                            List<Symbol> params = this.table.getParameters(this.functionName);
                            List<Symbol> vars = this.table.getLocalVariables(this.functionName);

                            for (Symbol s2: params) {
                                if (s2 == null)
                                    continue;
                                if (s2.getName().equals(node.getChildren().get(0).get("Name"))) {
                                    if (s2.getType().isArray())
                                        return "Int Array";
                                    else
                                        return s2.getType().getName();
                                }
                            }

                            for (Symbol s2: vars) {
                                if (s2.getName().equals(node.getChildren().get(0).get("Name"))) {
                                    if (s2.getType().isArray())
                                        return "Int Array";
                                    else
                                        return s2.getType().getName();
                                }
                            }
                        }
                    }
                }
                Symbol s = this.getValue(node.getChildren().get(0));
                if (s != null) {
                    List<Symbol> local = new ArrayList<>();
                    JmmNode funcNode = node;
                    JmmNode rootNode = node;

                    while (!funcNode.getKind().equals("MainDeclaration") && !funcNode.getKind().equals("Function")) {
                        funcNode = funcNode.getParent();
                    }

                    while (!rootNode.getKind().equals("Class")) {
                        rootNode = rootNode.getParent();
                    }

                    String funcName;
                    if (funcNode.getKind().equals("MainDeclaration"))
                        funcName = "main";
                    else
                        funcName = funcNode.get("Name");

                    List<JmmNode> rootNodes = new ArrayList<>();
                    for (JmmNode child: rootNode.getChildren()) {
                        if (child.getKind().equals("Function")) {
                            if (child.get("Name").equals(funcName))
                                rootNodes.add(child);
                        }
                    }

                    int indexCnt = 0;
                    for (JmmNode child: rootNodes) {
                        if (child.equals(funcNode))
                            break;
                        indexCnt++;
                    }

                    if (funcNode.getKind().equals("MainDeclaration"))
                        local = this.table.getLocalVariables("main");
                    else {
                        local = this.table.getLocalVariables(funcNode.get("Name"));
                    }

                    List<Symbol> params = this.table.getParameters(funcName);
                    List<Symbol> fields = this.table.getFields();

                    List<List<Symbol>> paramsAux = new ArrayList<>();
                    List<List<Symbol>> localsAux = new ArrayList<>();
                    List<Symbol> auxList = new ArrayList<>();

                    for (Symbol symbol: params) {
                        if (symbol == null) {
                            paramsAux.add(auxList);
                            auxList = new ArrayList<>();
                        }
                        else auxList.add(symbol);
                    }
                    paramsAux.add(auxList);

                    auxList = new ArrayList<>();
                    for (Symbol symbol: local) {
                        if (symbol == null) {
                            localsAux.add(auxList);
                            auxList = new ArrayList<>();
                        }
                        else auxList.add(symbol);
                    }
                    localsAux.add(auxList);

                    local = localsAux.get(indexCnt);
                    params = paramsAux.get(indexCnt);

                    for (Symbol s2: params) {
                        if (s2 == null)
                            continue;
                        if (s2.getName().equals(node.getChildren().get(0).get("Name"))) {
                            if (s2.getType().isArray())
                                return "Int Array";
                            else
                                return s2.getType().getName();
                        }
                    }

                    for (Symbol s2: local) {
                        if (s2.getName().equals(node.getChildren().get(0).get("Name"))) {
                            if (s2.getType().isArray())
                                return "Int Array";
                            else
                                return s2.getType().getName();
                        }
                    }

                    if (s.getType().isArray())
                        return "Int Array";
                    else {
                        if (rightChild)
                            this.checkIfInitialized(s, node, reports);

                        JmmNode argumentsNode = node;
                        while (!argumentsNode.getKind().equals("Arguments") && argumentsNode.getParent() != null)
                            argumentsNode = argumentsNode.getParent();

                        if (argumentsNode.getKind().equals("Arguments")) {
                            return "-";
                        }

                        return s.getType().getName();
                    }
                }
                else {
                    if (node.getParent().getKind().equals("FunctionCall") && !this.table.getImports().isEmpty())
                        return node.getChildren().get(0).get("Name");
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Variable " + node.getChildren().get(0).get("Name") + " is undefined"));
                    return "";
                }
                //node.getChildren().get(0).get("Name");
                //else
                //    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Variable " + node.getChildren().get(0).get("Name") + " hasn't been declared yet!"));
            case "Index":
                String leftIndex = this.parseNodeType(node.getChildren().get(0), reports);
                String rightIndex = this.parseNodeType(node.getChildren().get(1), reports);

                if (leftIndex.equals("-") || rightIndex.equals("-"))
                    return "-";

                if (!leftIndex.equals("Int Array")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Accessed variable is not an array!"));
                    return "";
                }

                if (!rightIndex.equals("Int")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Cannot access array through " + rightIndex + " variable!"));
                    return "";
                }

                if (rightChild) {
                    this.checkIfInitialized(Objects.requireNonNull(this.getValue(node.getChildren().get(0).getChildren().get(0))), node, reports);
                }

                return "Int";
            case "NewArray":
                String index = this.parseNodeType(node.getChildren().get(0), reports);
                if ((!node.getChildren().get(0).getKind().equals("Integer") && !index.equals("Int")) && !node.getChildren().get(0).getKind().equals("Length") && !index.equals("-")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Cannot initialize array with " + node.getChildren().get(0).getKind() + " variable!"));
                    return "";
                }

                /*JmmNode parent = node;
                while (!parent.getKind().equals("Function"))
                    parent = parent.getParent();

                System.out.println(parent);

                this.table.addArrayInitVal(parent.get("Name") + "_" + node)*/

                return "Int Array";
            case "NewObject":
            case "Type":
                return node.get("Name");
            case "Length":
                if (!this.parseNodeType(node.getChildren().get(0), reports).equals("Int Array")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Length cannot be used on variables which are not of Array Type!"));
                    return "";
                }
                return "Int";
            case "Add":
            case "Sub":
            case "Mul":
            case "Div":
                leftIndex = this.parseNodeType(node.getChildren().get(0), reports);
                rightIndex = this.parseNodeType(node.getChildren().get(1), reports);

                if (leftIndex.equals("") || rightIndex.equals(""))
                    return "";

                if (leftIndex.equals("-") || rightIndex.equals("-"))
                    return "-";

                if (leftIndex.equals("Int Array") || rightIndex.equals("Int Array")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Cannot use arrays directly for operations!"));
                    return "";
                }
                else if (!leftIndex.equals(rightIndex)) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Variables are not of the same type: " + leftIndex + " and " + rightIndex));
                    return "";
                }

                return leftIndex;
            case "Colon":
                leftIndex = this.parseNodeType(node.getChildren().get(0), reports);
                rightIndex = this.parseNodeType(node.getChildren().get(1), reports);

                return leftIndex + "," + rightIndex;
            case "LessThan":
            case "And":
                leftIndex = this.parseNodeType(node.getChildren().get(0), reports);
                rightIndex = this.parseNodeType(node.getChildren().get(1), reports);

                if (leftIndex.equals("") || rightIndex.equals(""))
                    return "";

                if (leftIndex.equals("-") || rightIndex.equals("-"))
                    return "-";

                if (!leftIndex.equals(rightIndex)) {
                    reports.add(new Report(ReportType.WARNING, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid Comparison: variables of type " + leftIndex + " with variables of type " + rightIndex));
                    return "";
                }
                return "Boolean";
            case "Not":
                leftIndex = this.parseNodeType(node.getChildren().get(0), reports);

                if (leftIndex.equals(""))
                    return "";

                if (!leftIndex.equals("Boolean")) {
                    reports.add(new Report(ReportType.WARNING, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Invalid operation - cannot use Not operation with " + leftIndex));
                    return "";
                }
                return "Boolean";
            case "This":
                return "This";
            case "FunctionCall":
                String functionName = "";

                for (JmmNode child: node.getChildren()) {
                    if (child.getKind().equals("FunctionName"))
                        functionName = child.get("Name");
                }

                for (Method method: this.table.getMethodsObjects()) {
                    if (method.getName().equals(functionName)) {
                        if ("Integer".equals(method.getReturnType().getName()))
                            return "Int";
                        else {
                            if (method.getReturnType().isArray())
                                return "Int Array";
                            return method.getReturnType().getName();
                        }
                    }
                }

                JmmNode argumentsNode = node;
                while (!argumentsNode.getKind().equals("Arguments") && argumentsNode.getParent() != null)
                    argumentsNode = argumentsNode.getParent();

                if (argumentsNode.getKind().equals("Arguments")) {
                    return "-";
                }

                return this.checkIfImportedMember(node, reports);
            default:
                throw new RuntimeException("No Implementation yet!");
        }
    }

    private void checkIfInitialized(Symbol s, JmmNode node, List<Report> reports) {
        //if (!s.getInitialized() && (parents.contains(node.getParent().getKind())))
        //    reports.add(new Report(ReportType.WARNING, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Variable " + s.getName() + " hasn't been initialized yet!"));
    }

    private Symbol getValue(JmmNode node) {
        List<Symbol> local = new ArrayList<>();
        JmmNode funcNode = node;
        JmmNode rootNode = node;

        while (!funcNode.getKind().equals("MainDeclaration") && !funcNode.getKind().equals("Function")) {
            funcNode = funcNode.getParent();
        }

        while (!rootNode.getKind().equals("Class")) {
            rootNode = rootNode.getParent();
        }

        String funcName;
        if (funcNode.getKind().equals("MainDeclaration"))
            funcName = "main";
        else
            funcName = funcNode.get("Name");

        List<JmmNode> rootNodes = new ArrayList<>();
        for (JmmNode child: rootNode.getChildren()) {
            if (child.getKind().equals("Function")) {
                if (child.get("Name").equals(funcName))
                    rootNodes.add(child);
            }
        }

        int indexCnt = 0;
        for (JmmNode child: rootNodes) {
            if (child.equals(funcNode))
                break;
            indexCnt++;
        }

        if (funcNode.getKind().equals("MainDeclaration"))
            local = this.table.getLocalVariables("main");
        else {
            local = this.table.getLocalVariables(funcNode.get("Name"));
        }

        List<Symbol> params = this.table.getParameters(funcName);
        List<Symbol> fields = this.table.getFields();

        List<List<Symbol>> paramsAux = new ArrayList<>();
        List<List<Symbol>> localsAux = new ArrayList<>();
        List<Symbol> auxList = new ArrayList<>();

        auxList = new ArrayList<>();
        for (Symbol s: params) {
            if (s == null) {
                paramsAux.add(auxList);
                auxList = new ArrayList<>();
            }
            else auxList.add(s);
        }

        auxList = new ArrayList<>();
        for (Symbol s: local) {
            if (s == null) {
                localsAux.add(auxList);
                auxList = new ArrayList<>();
            }
            else auxList.add(s);
        }

        local = localsAux.get(indexCnt);
        params = paramsAux.get(indexCnt);

        for (Symbol var: local) {
            if (var.getName().equals(node.get("Name")))
                return var;
        }

        for (Symbol var: params) {
            if (var == null)
                continue;
            if (var.getName().equals(node.get("Name")))
                return var;
        }

        for (Symbol var: fields) {
            if (var.getName().equals(node.get("Name")))
                return var;
        }

        return null;
    }

    public String checkIfImportedMember(JmmNode node, List<Report> reports) {
        String identifier;

        if (this.table.getMethods().contains(node.getChildren().get(1).get("Name")))
            return "";

        if (node.getChildren().get(0).getKind().equals("This"))
            identifier = "this";
        else {
            if (node.getChildren().get(0).getKind().equals("NewObject")) {
                identifier = node.getChildren().get(0).get("Name");
            }
            else
                identifier = node.getChildren().get(0).getChildren().get(0).get("Name");
        }

        for (String imp: this.table.getImports()) {
            List<String> auxList = Arrays.asList(imp.split("\\."));
            if (auxList.contains(identifier)) {
                if (node.getParent().getKind().equals("Equals") || node.getParent().getKind().equals("FunctionCall"))
                    return this.parseNodeType(node.getParent().getChildren().get(0), reports);
                if (node.getParent().getKind().equals("Colon") || node.getParent().getKind().equals("Body"))
                    return identifier;
            }
        }

        if (identifier.equals(this.table.getClassName()))
            return identifier;

        if (identifier.equals(this.table.getSuper()))
            return identifier;

        return "";
    }
}
