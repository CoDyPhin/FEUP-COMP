package ollir.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.*;
import ast.NodeParser;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import table.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MethodEmitter extends PreorderJmmVisitor<List<Report>, Boolean> {
    private final boolean optimizedWhiles;
    private final boolean rflag;
    private SymbolTable table;
    private String code, prefix;
    private List<Report> reports;
    private NodeParser nodeParser;

    public MethodEmitter(SymbolTable table, List<Report> reports, String prefix, boolean optimizedWhiles, boolean rflag) {
        this.table = table;
        this.code = "";
        this.reports = reports;
        this.prefix = prefix;
        this.nodeParser = new NodeParser((Table) this.table);
        this.optimizedWhiles = optimizedWhiles;
        this.rflag = rflag;
        addVisit("Function", this::visitMethod);
    }

    public String getCode() {
        return code;
    }

    public List<Report> getReports() {
        return reports;
    }

    private boolean visitMethod(JmmNode node, List<Report> reports) {
        this.code += "\n  .method public " + node.get("Name") + "(";

        this.convertMethodHeader(node);

        var expressionsEmitter = new ExpressionsEmitter(this.table, this.reports, prefix + "  ", node.get("Name"), optimizedWhiles, rflag);
        expressionsEmitter.visit(node);
        this.code += expressionsEmitter.getCode();

        // JB: Adds return if last instruction is not a return
//        if(!node.getChildren().get(node.getNumChildren()-1).getKind().equals("returnDeclaration")) {
//            this.code += "ret.V;\n";
//        }
        
        // JB: HACK
//        if(this.code.strip().endsWith(":")) {
//            this.code += "ret.V;\n";
//        }
        
        this.code += "  }\n";
        return true;
    }

    private void convertMethodHeader(JmmNode node) {
        List<JmmNode> params = node.getChildren().get(1).getChildren();
        String aux = "";
        this.nodeParser.setFunctionName(node.get("Name"));
        for (JmmNode param: params) {
            aux += this.nodeParser.parseNodeType(param.getChildren().get(0), reports);
            if (params.indexOf(param) != params.size()-1)
                aux += ",";
        }
        List<String> parameters = Arrays.asList(aux.split(","));
        int cnt = 0;
        List<List<Symbol>> paramSymbolsAux = this.convertParamSymbols(this.table.getParameters(node.get("Name")));
        List<Symbol> paramSymbols = new ArrayList<>();
        if (paramSymbolsAux.size() == 1) {
            paramSymbols = paramSymbolsAux.get(0);
        }
        else {
            List<String> auxArgs = this.convertParamsListToString(paramSymbolsAux);
            cnt = auxArgs.indexOf(aux);
            paramSymbols = paramSymbolsAux.get(cnt);
            cnt = 0;
        }

        if (parameters.size() == 1 && parameters.get(0) == "")
            parameters = new ArrayList<>();

        for (String parameter: parameters) {
            if (paramSymbols.get(cnt) != null) {
                this.code += this.sanitize(paramSymbols.get(cnt).getName());
                switch (parameter) {
                    case "Int":
                        this.code += ".i32";
                        break;
                    case "Int Array":
                        this.code += ".array.i32";
                        break;
                    case "Boolean":
                        this.code += ".bool";
                        break;
                    default:
                        this.code += "." + parameter;
                        break;
                }
                if (cnt != parameters.size() - 1)
                    this.code += ",";
                cnt++;
            }
        }

        this.code += ")";

        switch (this.table.getReturnType(node.get("Name")).getName()) {
            case "void":
                this.code += "V";
                break;
            case "Int":
                if (this.table.getReturnType(node.get("Name")).isArray())
                    this.code += ".array";
                this.code += ".i32";
                break;
            case "Boolean":
                this.code += ".bool";
                break;
            default:
                this.code += this.table.getReturnType(node.get("Name")).getName();
                break;
        }

        this.code += " {\n";
    }

    public List<List<Symbol>> convertParamSymbols(List<Symbol> paramSymbols) {
        List<List<Symbol>> res = new ArrayList<>();
        List<Symbol> aux = new ArrayList<>();

        for (Symbol param: paramSymbols) {
            if (param == null) {
                res.add(aux);
                aux = new ArrayList<>();
            }
            else
                aux.add(param);
        }

        res.add(aux);

        return res;
    }

    public List<String> convertParamsListToString(List<List<Symbol>> paramSymbols) {
        List<String> res = new ArrayList<>();
        String aux = "";

        for (List<Symbol> symbolList: paramSymbols) {
            for (Symbol s: symbolList) {
                switch (s.getType().getName()) {
                    case "Int":
                        if (s.getType().isArray())
                            aux += "Int Array";
                        else
                            aux += "Int";
                        break;
                    case "Boolean":
                        aux += "Boolean";
                        break;
                    default:
                        aux += s.getType().getName();
                        break;
                }
                if (symbolList.indexOf(s) != symbolList.size() - 1)
                    aux += ",";
            }
            res.add(aux);
            aux = "";
        }

        return res;
    }

    private String sanitize(String name) {
        List<String> reserved = new ArrayList<>(List.of("field", "ret", "invoke", "putfield", "getfield"));

        for (String s: reserved) {
            if (name.contains(s))
                return "sanitized_" + name + "_sanitized";
        }

        return name;
    }
}