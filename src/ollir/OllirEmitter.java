package ollir;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.*;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import ollir.visitors.MainEmitter;
import ollir.visitors.MethodEmitter;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

public class OllirEmitter extends PreorderJmmVisitor<List<Report>, Boolean> {
    private final boolean optimizedWhiles;
    private final boolean rflag;
    private SymbolTable table;
    private String code;
    private List<Report> reports;
    public OllirEmitter(SymbolTable table, boolean optimizedWhiles, boolean rflag) {
        this.table = table;
        this.code = "";
        this.reports = new ArrayList<>();
        this.optimizedWhiles = optimizedWhiles;
        this.rflag = rflag;
        addVisit("Class", this::visitClass);
    }

    public String getCode() {
        return code;
    }

    public List<Report> getReports() {
        return reports;
    }

    private boolean visitClass(JmmNode node, List<Report> reports) {
        for (String imp: this.table.getImports())
            this.code += "import " + imp + ";\n";
        this.code += "\n";

        this.code += this.table.getClassName();

        if (this.table.getSuper() != null) {
            this.code += " extends " + this.table.getSuper();
        }

        this.code += " {\n";

        // Converting Fields
        for (Symbol field: this.table.getFields()) {
            this.code += "  .field private " + this.sanitize(field.getName());

            switch (field.getType().getName()) {
                case "Int":
                    if (field.getType().isArray())
                        this.code += ".array";
                    this.code += ".i32";
                    break;
                case "Boolean":
                    this.code += ".bool";
                    break;
                default:
                    this.code += "." + field.getType().getName();
                    break;
            }

            this.code += ";\n";
        }

        // Initial Constructor
        this.code += "  " + ".construct " + this.table.getClassName() + "().V {\n";
        this.code += "    " + "invokespecial(this, \"<init>\").V;\n  }\n";

        // Converting Methods
        var methodEmitter = new MethodEmitter(this.table, this.reports, "  ", optimizedWhiles, rflag);
        methodEmitter.visit(node);
        this.code += methodEmitter.getCode();

        // Convertitng Main
        var mainEmitter = new MainEmitter(this.table, this.reports, "  ", optimizedWhiles, rflag);
        mainEmitter.visit(node);
        this.code += mainEmitter.getCode();
        this.code += "}";
        return true;
    }

    private String sanitize(String name) {
        List<String> reserved = new ArrayList<>(List.of("field", "ret", "invoke", "putfield", "getfield"));

        for (String s: reserved) {
            if (name.contains(s))
                return "sanitized_" + name + "_sanitized";
        }

        return name;
    }
}