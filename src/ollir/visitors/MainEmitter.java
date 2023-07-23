package ollir.visitors;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.*;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class MainEmitter extends PreorderJmmVisitor<List<Report>, Boolean> {
    private final boolean optimizedWhiles;
    private final boolean rflag;
    private SymbolTable table;
    private String code, prefix;
    private List<Report> reports;
    public MainEmitter(SymbolTable table, List<Report> reports, String prefix, boolean optimizedWhiles, boolean rflag) {
        this.table = table;
        this.code = "";
        this.reports = reports;
        this.prefix = prefix;
        this.optimizedWhiles = optimizedWhiles;
        this.rflag = rflag;
        addVisit("MainDeclaration", this::visitMain);
    }

    public String getCode() {
        return code;
    }

    public List<Report> getReports() {
        return reports;
    }

    private boolean visitMain(JmmNode node, List<Report> reports) {
        this.code += "\n  .method public static main(args.array.String).V";
        this.code += " {\n";

        var expressionsEmitter = new ExpressionsEmitter(this.table, this.reports, prefix + "  ", "main", optimizedWhiles, rflag);
        expressionsEmitter.visit(node);
        this.code += expressionsEmitter.getCode();

        this.code += "  }\n";
        return true;
    }
}