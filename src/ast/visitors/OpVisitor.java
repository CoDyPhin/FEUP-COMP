package ast.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import table.Table;
import ast.NodeParser;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;


public class OpVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table table;
    private NodeParser nodeParser;

    public OpVisitor(Table table, String functionName) {
        this.table = table;
        this.nodeParser = new NodeParser(table, functionName);
        addVisit("Equals", this::visitEquals);
        addVisit("If", this::visitCondition);
        addVisit("While", this::visitCondition);
    }

    private Boolean visitCondition(JmmNode node, List<Report> reports) {
        String condition = this.nodeParser.parseNodeType(node.getChildren().get(0).getChildren().get(0), reports);

        if (!condition.equals("Boolean") && !condition.equals("Int") && !condition.equals("Integer")) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Condition is not of Boolean Type!"));
            return false;
        }
        return true;
    }

    private Boolean visitEquals(JmmNode node, List<Report> reports) {
        String type1 = this.nodeParser.parseNodeType(node.getChildren().get(0), reports);
        if (type1.equals("bool"))
            type1 = "Boolean";
        String type2 = this.nodeParser.parseNodeType(node.getChildren().get(1), reports);
        if (type2.equals("bool"))
            type2 = "Boolean";

        if (type1.equals("") || type2.equals(""))
            return false;

        if (!type1.equals(type2)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Cannot assign variable of type " + type2 + " to variable of type " + type1 + "!"));
            return false;
        }

        if (node.getChildren().get(0).getKind().equals("Variable"))
            this.table.wasInitialized(this.nodeParser.getFunctionName(), node.getChildren().get(0));

        return true;
    }
}
