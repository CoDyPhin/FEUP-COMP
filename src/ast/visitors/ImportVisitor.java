package ast.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import table.Table;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class ImportVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table table;
    public ImportVisitor(Table table) {
        this.table = table;
        addVisit("Import", this::visitImport);
    }

    public Table getTable() {
        return table;
    }

    private Boolean visitImport(JmmNode node, List<Report> reports) {
        this.table.addImport(node.get("Name"));
        return true;
    }
}
