package ast.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import table.Table;
import pt.up.fe.comp.jmm.analysis.table.Type;
import ast.NodeParser;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;

public class ReturnVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table table;
    private String functionName;
    public ReturnVisitor(Table table, String functionName) {
        this.table = table;
        this.functionName = functionName;

        addVisit("returnDeclaration", this::visitReturn);
    }

    public Table getTable() {
        return table;
    }

    private Boolean visitReturn(JmmNode node, List<Report> reports) {
        String type = "Void";
        String returnType = "";
        Type t = this.table.getReturnType(functionName);

        if (t.isArray())
            returnType = "Int Array";
        else
            returnType = t.getName();

        NodeParser nodeParser = new NodeParser(this.table, this.functionName);
        if (!node.getChildren().isEmpty()) {
            type = nodeParser.parseNodeType(node.getChildren().get(0), reports);
        }

        if (type.equals(""))
            return false;

        if (!type.equals(returnType)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Return Type doesn't match Function Type!"));
            return false;
        }

        return true;
    }
}