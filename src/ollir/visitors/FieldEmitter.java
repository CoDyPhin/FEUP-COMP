package ollir.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.*;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class FieldEmitter extends PreorderJmmVisitor<List<Report>, Boolean> {
    private SymbolTable table;
    private String code;
    private List<Report> reports;
    public FieldEmitter(SymbolTable table, List<Report> reports) {
        this.table = table;
        this.code = "";
        this.reports = reports;
        addVisit("Function", this::visitMethod);
    }

    public String getCode() {
        return code;
    }

    public List<Report> getReports() {
        return reports;
    }

    private boolean visitMethod(JmmNode node, List<Report> reports) {
        this.code += "  .method public " + this.sanitize(node.get("Name")) + "(";

        List<Symbol> parameters = this.table.getParameters(node.get("Name"));

        for (Symbol parameter: parameters) {
            this.code += this.sanitize(parameter.getName());
            switch (parameter.getType().getName()) {
                case "Int":
                    if (parameter.getType().isArray())
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

            if (parameters.indexOf(parameter) != parameters.size() - 1)
                this.code += ",";
        }

        this.code += ")";

        switch (this.table.getReturnType(node.get("Name")).getName()) {
            case "void":
                this.code += "V";
                break;
            case "Int":
                this.code += ".i32";
                break;
            case "Int Array":
                this.code += "array.i32";
                break;
            case "Boolean":
                this.code += ".bool";
                break;
            default:
                this.code += this.table.getReturnType(node.get("Name")).getName();
                break;
        }

        this.code += " {\n";
        this.code += "  }\n";
        return true;
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