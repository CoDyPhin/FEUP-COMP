package ast.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import table.Method;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import table.Table;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class MainVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table table;
    public MainVisitor(Table table) {
        this.table = table;
        addVisit("MainDeclaration", this::visitMain);
    }

    public Table getTable() {
        return table;
    }

    private Boolean visitMain(JmmNode node, List<Report> reports) {
        List<Symbol> parameters = new ArrayList<Symbol>();
        var fieldVisitor = new MethodFieldVisitor(this.table);
        parameters.add(new Symbol(new Type("String", true), "args"));

        for (JmmNode child: node.getChildren()) {
            if (child.getKind().equals("Body")) {
                fieldVisitor.visit(child, reports);
            }
        }

        this.table.addMethod(new Method("main", new Type("Void", false), parameters, fieldVisitor.getFields()));

        var opVisitor = new OpVisitor(this.table, "main");
        opVisitor.visit(node, reports);

        return true;
    }
}