import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.SpecsIo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class AnalysisTest {
    @Test
    public void arr_index_not_jmm_Test() throws IOException {
        //String jmmCode  = SpecsIo.getResource("fixtures/public/fail/semantic/extra/miss_type.jmm");
        String jmmCode  = SpecsIo.getResource("fixtures/public/fail/syntactical/CompleteWhileTest.jmm");
        FileWriter file  = new FileWriter("./JSON.txt");
        JmmSemanticsResult result = TestUtils.analyse(jmmCode);
        assertEquals("Program", result.getRootNode().getKind());
        file.write(result.getRootNode().toJson());
        List<Report> reports = result.getReports();
        for (Report report: reports)
            System.out.println(report);
    }

    @Test
    public void arr_size_not_int_Test() throws IOException {
        String jmmCode  = SpecsIo.getResource("fixtures/public/fail/semantic/arr_size_not_int.jmm");
        FileWriter file  = new FileWriter("./JSON.txt");
        JmmSemanticsResult result = TestUtils.analyse(jmmCode);
        assertEquals("Program", result.getRootNode().getKind());
        file.write(result.getRootNode().toJson());
        List<Report> reports = result.getReports();
        for (Report report: reports)
            System.out.println(report);
    }

    @Test
    public void badArguments_Test() throws IOException {
        String jmmCode  = SpecsIo.getResource("fixtures/public/fail/semantic/badArguments.jmm");
        FileWriter file  = new FileWriter("./JSON.txt");
        JmmSemanticsResult result = TestUtils.analyse(jmmCode);
        assertEquals("Program", result.getRootNode().getKind());
        file.write(result.getRootNode().toJson());
        List<Report> reports = result.getReports();
        for (Report report: reports)
            System.out.println(report);
    }

    @Test
    public void binop_incomp_Test() throws IOException {
        String jmmCode  = SpecsIo.getResource("fixtures/public/fail/semantic/binop_incomp.jmm");
        FileWriter file  = new FileWriter("./JSON.txt");
        JmmSemanticsResult result = TestUtils.analyse(jmmCode);
        assertEquals("Program", result.getRootNode().getKind());
        file.write(result.getRootNode().toJson());
        List<Report> reports = result.getReports();
        for (Report report: reports)
            System.out.println(report);
    }

    @Test
    public void funcNotFound_Test() throws IOException {
        String jmmCode  = SpecsIo.getResource("fixtures/public/fail/semantic/funcNotFound.jmm");
        FileWriter file  = new FileWriter("./JSON.txt");
        JmmSemanticsResult result = TestUtils.analyse(jmmCode);
        assertEquals("Program", result.getRootNode().getKind());
        file.write(result.getRootNode().toJson());
        List<Report> reports = result.getReports();
        for (Report report: reports)
            System.out.println(report);
    }

    @Test
    public void simple_length_Test() throws IOException {
        String jmmCode  = SpecsIo.getResource("fixtures/public/fail/semantic/simple_length.jmm");
        FileWriter file  = new FileWriter("./JSON.txt");
        JmmSemanticsResult result = TestUtils.analyse(jmmCode);
        assertEquals("Program", result.getRootNode().getKind());
        file.write(result.getRootNode().toJson());
        List<Report> reports = result.getReports();
        for (Report report: reports)
            System.out.println(report);
    }

    @Test
    public void var_exp_incomp_Test() throws IOException {
        String jmmCode  = SpecsIo.getResource("fixtures/public/fail/semantic/var_exp_incomp.jmm");
        FileWriter file  = new FileWriter("./JSON.txt");
        JmmSemanticsResult result = TestUtils.analyse(jmmCode);
        assertEquals("Program", result.getRootNode().getKind());
        file.write(result.getRootNode().toJson());
        List<Report> reports = result.getReports();
        for (Report report: reports)
            System.out.println(report);
    }

    @Test
    public void var_lit_incomp_Test() throws IOException {
        String jmmCode  = SpecsIo.getResource("fixtures/public/fail/semantic/var_lit_incomp.jmm");
        FileWriter file  = new FileWriter("./JSON.txt");
        JmmSemanticsResult result = TestUtils.analyse(jmmCode);
        assertEquals("Program", result.getRootNode().getKind());
        file.write(result.getRootNode().toJson());
        List<Report> reports = result.getReports();
        for (Report report: reports)
            System.out.println(report);
    }

    @Test
    public void var_undef_Test() throws IOException {
        String jmmCode  = SpecsIo.getResource("fixtures/public/fail/semantic/var_undef.jmm");
        FileWriter file  = new FileWriter("./JSON.txt");
        JmmSemanticsResult result = TestUtils.analyse(jmmCode);
        assertEquals("Program", result.getRootNode().getKind());
        file.write(result.getRootNode().toJson());
        List<Report> reports = result.getReports();
        for (Report report: reports)
            System.out.println(report);
    }

    @Test
    public void varNotInit_Test() throws IOException {
        String jmmCode  = SpecsIo.getResource("fixtures/public/fail/semantic/var_undef.jmm");
        FileWriter file  = new FileWriter("./JSON.txt");
        JmmSemanticsResult result = TestUtils.analyse(jmmCode);
        assertEquals("Program", result.getRootNode().getKind());
        file.write(result.getRootNode().toJson());
        List<Report> reports = result.getReports();
        for (Report report: reports)
            System.out.println(report);
    }
}

