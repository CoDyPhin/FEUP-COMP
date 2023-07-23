package ast.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import table.Method;
import table.Table;
import ast.NodeParser;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CheckFunctionCallsVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    private Table table;
    private NodeParser nodeParser;
    public CheckFunctionCallsVisitor(Table table) {
        this.table = table;
        this.nodeParser = new NodeParser(table);
        addVisit("FunctionCall", this::visitFunctionCall);
    }

    private Boolean visitFunctionCall(JmmNode node, List<Report> reports) {
        for (JmmNode child: node.getChildren()) {
            if (child.getKind().equals("FunctionName")) {
                this.nodeParser.setFunctionName(child.get("Name"));
                if (!table.getMethods().contains(child.get("Name"))) {
                    switch (node.getChildren().get(0).getKind()) {
                        case "This":
                            if (table.getSuper() != null)
                                return true;
                            break;
                        case "Variable":
                            /*if (node.getChildren().get(2).getChildren().size() != 0) {
                                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Static functions don't take arguments!"));
                                return false;
                            }*/
                            return true;
                        default:
                            break;
                    }

                    if (node.getChildren().get(0).getKind().equals("This")) {
                        boolean found = false;
                        for (Method m: this.table.getMethodsObjects()) {
                            if (m.getName().equals(child.get("Name")))
                                found = true;
                        }
                        if (!found) {
                            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Undefined function " + child.get("Name") + "!"));
                            return false;
                        }
                    }

                    if (!node.getChildren().get(0).getKind().equals("NewObject") && !node.getChildren().get(0).get("Name").equals(this.table.getClassName()) && !node.getChildren().get(0).get("Name").equals(this.table.getSuper()) && this.table.getImports().isEmpty()) {
                        reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Undefined function " + child.get("Name") + "!"));
                    }
                    return false;
                }
                break;
            }
        }
        List<String> vars = this.table.getParametersString(this.nodeParser.getFunctionName());
        List<String> functionCallVars = new ArrayList<>();

        for (JmmNode child: node.getChildren()) {
            if (child.getKind().equals("Arguments")) {
                for (JmmNode argument: child.getChildren()) {
                    functionCallVars.add(this.nodeParser.parseNodeType(argument, reports));
                    }
                break;
            }
        }

        if (!functionCallVars.isEmpty())
            functionCallVars = new ArrayList<>(Arrays.asList(functionCallVars.get(0).split(",")));

        List<List<String>> aux = new ArrayList<>();
        List<String> aux2 = new ArrayList<>();
        for (String s: vars) {
            if (s.equals("|")) {
                aux.add(aux2);
                aux2 = new ArrayList<>();
            }
            else
                aux2.add(s);
        }
        if (!aux2.isEmpty())
            aux.add(aux2);

        List<String> tempAux = new ArrayList<>();
        for (int x = 0; x < functionCallVars.size(); x++) {
            if (functionCallVars.get(x).equals("-"))
                tempAux.add(vars.get(x));
            else
                tempAux.add(functionCallVars.get(x));
        }
        functionCallVars = tempAux;

        boolean different = true;
        for (List<String> list: aux) {
            vars = list;
            if (functionCallVars.size() == vars.size()) {
                different = false;
                break;
            }
        }

        if (different) {
            if (this.table.getSuper() == null && this.table.getImports().size() == 0) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Wrong usage of arguments for this function!"));
                return false;
            }
            else return true;
        }

        for (List<String> list: aux) {
            for (String s : functionCallVars) {
                list.remove(s);
            }
        }

        boolean same = false;
        for (List<String> l: aux) {
            if (l.isEmpty())
                same = true;
        }

        if (!same) {
            if (this.table.getSuper() == null && this.table.getImports().size() == 0) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Wrong usage of arguments for this function!"));
                return false;
            }
            else return true;
        }

        return true;
    }
}
