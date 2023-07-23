package ast.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import table.Method;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import table.Table;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;

public class MethodVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table table;
    public MethodVisitor(Table table) {
        this.table = table;
        addVisit("Function", this::visitMethod);
    }

    public Table getTable() {
        return table;
    }

    private Boolean visitMethod(JmmNode node, List<Report> reports) {
        Type returnType = null;
        Symbol symbol = null;
        List<Symbol> parameters = new ArrayList<Symbol>();
        var fieldVisitor = new MethodFieldVisitor(this.table);
        for (JmmNode child: node.getChildren()) {
            switch(child.getKind()) {
                case "Type":
                    if (child.getChildren().get(0).getChildren().size() > 0)
                        returnType = new Type(child.getChildren().get(0).getKind(), true);
                    else
                        returnType = new Type(child.getChildren().get(0).getKind(), false);
                    break;
                case "Parameters":
                    for (JmmNode parameter: child.getChildren()) {
                        if (parameter.getChildren().get(0).getChildren().size() > 0)
                            symbol = new Symbol(new Type(parameter.getChildren().get(0).getKind(), true), parameter.get("Name"));
                        else {
                            if (parameter.getChildren().get(0).getKind().equals("Type"))
                                symbol = new Symbol(new Type(parameter.getChildren().get(0).get("Name"), false), parameter.get("Name"));
                            else { symbol = new Symbol(new Type(parameter.getChildren().get(0).getKind(), false), parameter.get("Name")); }
                        }

                        parameters.add(symbol);
                    }
                    break;
                case "Body":
                    fieldVisitor.visit(child, reports);
                    break;
                default:
                    throw new RuntimeException("Not Implemented Yet!");
            }
        }

        if (!this.table.addMethod(new Method(node.get("Name"), returnType, parameters, fieldVisitor.getFields()))) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Same exact redefinition of Method " + node.get("Name")));
            return false;
        }
        
        return true;
    }
}