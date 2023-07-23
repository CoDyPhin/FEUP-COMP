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
import java.util.Optional;

public class RedefinedFieldsVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table table;
    private List<Symbol> params;
    private List<Symbol> fields;

    public RedefinedFieldsVisitor(Table table, String functionName) {
        this.table = table;
        params = this.table.getParameters(functionName);
        fields = this.table.getFields();
        addVisit("Variable", this::visitVariable);
    }

    private Boolean visitVariable(JmmNode node, List<Report> reports) {
        Optional<JmmNode> returnParent = node.getAncestor("returnDeclaration");
        if (returnParent.isPresent() || !node.getParent().getKind().equals("Body"))
            return true;
        boolean isArray = false;

        if (node.getChildren().size() < 2)
            return true;
        if (node.getChildren().get(0).getChildren().size() > 0)
            isArray = true;

        String kind = node.getChildren().get(0).getKind();
        if (kind.equals("Type"))
            kind = node.getChildren().get(0).get("Name");

        Symbol variable = new Symbol(new Type(kind, isArray), node.getChildren().get(1).get("Name"));

        if (this.params.contains(variable) && !this.params.contains(null)) {
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Redefinition of variable " + node.getChildren().get(1).get("Name")));
            return false;
        }

        return true;
    }
}
