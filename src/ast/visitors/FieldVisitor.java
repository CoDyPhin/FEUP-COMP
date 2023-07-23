package ast.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import table.Table;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

public class FieldVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table table;
    public FieldVisitor(Table table) {
        this.table = table;
        addVisit("Variable", this::visitField);
    }

    public Table getTable() {
        return table;
    }

    private Boolean visitField(JmmNode node, List<Report> reports) {
        boolean isArray = false;

        if (node.getChildren().get(0).getChildren().size() > 0)
            isArray = true;
        if (node.getChildren().size() < 2 || !node.getParent().getKind().equals("Class"))
            return true;

        String kind = node.getChildren().get(0).getKind();
        if (kind.equals("Type"))
            kind = node.getChildren().get(0).get("Name");

        if (!this.table.addField(new Symbol(new Type(kind, isArray), node.getChildren().get(1).get("Name")))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Redefinition of variable " + node.getChildren().get(1).get("Name")));
            return false;
        }

        return true;
    }
}