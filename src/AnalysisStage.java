
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import table.Table;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import ast.visitors.*;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

public class AnalysisStage implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        if (TestUtils.getNumReports(parserResult.getReports(), ReportType.ERROR) > 0) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but there are errors from previous stage");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        if (parserResult.getRootNode() == null) {
            var errorReport = new Report(ReportType.ERROR, Stage.SEMANTIC, -1,
                    "Started semantic analysis but AST root node is null");
            return new JmmSemanticsResult(parserResult, null, Arrays.asList(errorReport));
        }

        JmmNode node = parserResult.getRootNode();
        String json = node.toJson();
        JmmNode root = JmmNodeImpl.fromJson(json);
        Table symbolTable = new Table();
        List<Report> reports = new ArrayList<>();

        // Imports
        var importVisitor = new ImportVisitor(symbolTable);
        importVisitor.visit(root, reports);

        // Class Name
        var classNameVisitor = new ClassVisitor(symbolTable);
        classNameVisitor.visit(root, reports);

        // Fields
        var fieldVisitor = new FieldVisitor(symbolTable);
        fieldVisitor.visit(root, reports);

        // Methods
        var methodVisitor = new MethodVisitor(symbolTable);
        methodVisitor.visit(root, reports);

        // Methods
        var methodOpVisitor = new MethodOpVisitor(symbolTable);
        methodOpVisitor.visit(root, reports);

        // Main Function
        var mainVisitor = new MainVisitor(symbolTable);
        mainVisitor.visit(root, reports);

        // Function Calls
        var functionCallVisitor = new FunctionCallVisitor(symbolTable);
        functionCallVisitor.visit(root, reports);

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}