package ast.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import table.Table;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class MethodOpVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table table;
    public MethodOpVisitor(Table table) {
        this.table = table;
        addVisit("Function", this::visitMethod);
    }

    public Table getTable() {
        return table;
    }

    private Boolean visitMethod(JmmNode node, List<Report> reports) {
        var opVisitor = new OpVisitor(this.table, node.get("Name"));
        opVisitor.visit(node, reports);

        var returnVisitor = new ReturnVisitor(this.table,node.get("Name"));
        returnVisitor.visit(node, reports);

        return true;
    }
}