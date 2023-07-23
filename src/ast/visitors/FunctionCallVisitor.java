package ast.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import table.Table;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class FunctionCallVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table table;
    public FunctionCallVisitor(Table table) {
        this.table = table;
        addVisit("Function", this::visitFunctionCall);
        addVisit("MainDeclaration", this::visitFunctionCall);
    }

    private Boolean visitFunctionCall(JmmNode node, List<Report> reports) {
        var checkFunctionCalls = new CheckFunctionCallsVisitor(this.table);
        checkFunctionCalls.visit(node, reports);

        String functionName = "";
        if (node.getKind().equals("MainDeclaration"))
            functionName = "main";
        else
            functionName = node.get("Name");
        var redefinedFieldsVisitor = new RedefinedFieldsVisitor(this.table, functionName);
        redefinedFieldsVisitor.visit(node, reports);

        return true;
    }
}
