package ast.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import table.Table;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class ClassVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table table;
    public ClassVisitor(Table table) {
        this.table = table;
        addVisit("Class", this::visitClassName);
    }

    public Table getTable() {
        return table;
    }

    private Boolean visitClassName(JmmNode node, List<Report> reports) {
        for (String s: node.getAttributes()) {
            switch(s){
                case "Name":
                    this.table.setClassName(node.get("Name"));
                    break;
                case "Extends":
                    this.table.setSuperClassName(node.get("Extends"));
                    break;
                case "col":
                case"line":
                    break;
                default:
                    throw new RuntimeException("Not Implemented Yet!");
            }
        }
        return true;
    }
}