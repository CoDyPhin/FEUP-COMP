package ast.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import table.Table;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class MethodFieldVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    List<Symbol> fields;
    private Table table;
    public MethodFieldVisitor(Table table) {
        this.fields = new ArrayList<Symbol>();
        this.table = table;
        addVisit("Variable", this::visitField);
    }

    public List<Symbol> getFields() {
        return fields;
    }

    private Boolean visitField(JmmNode node, List<Report> reports) {
        boolean isArray = false;

        if (node.getChildren().size() < 2)
            return true;
        if (node.getChildren().get(0).getChildren().size() > 0)
            isArray = true;

        String kind = node.getChildren().get(0).getKind();
        if (kind.equals("Type")) {
            kind = node.getChildren().get(0).get("Name");
            if (this.table.getImports().isEmpty() && this.table.getSuper() == null && !this.table.getClassName().equals(node.getChildren().get(0).get("Name"))) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Type for object " + node.getChildren().get(1).get("Name") + " is missing!"));
                return false;
            }
        }

        Symbol variable = new Symbol(new Type(kind, isArray), node.getChildren().get(1).get("Name"));

        if (this.fields.contains(variable)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Redefinition of variable " + node.getChildren().get(1).get("Name")));
            return false;
        }
        else this.fields.add(variable);

        return true;
    }
}
