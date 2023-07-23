
/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
import pt.up.fe.specs.util.SpecsIo;

import java.util.ArrayList;

public class BackendTest {

    @Test
    public void testHelloWorld() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/private/Turing.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run("1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n1\n");
        //assertEquals("Hello, World!", output.trim());
    }

    @Test
    public void testFindMaximum() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/FindMaximum.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("Result: 28", output.trim());
    }

    @Test
    public void testQuickSort() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/QuickSort.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("1\r\n2\r\n3\r\n4\r\n5\r\n6\r\n7\r\n8\r\n9\r\n10", output.trim());
    }

    @Test
    public void testLazysort() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Lazysort.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
    }

    @Test
    public void testLife() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Life.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
    }

    @Test
    public void testMonteCarloPi() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run("12");
    }

    @Test
    public void testSimple() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/Simple.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("30", output.trim());
    }

    @Test
    public void testTicTacToe() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/TicTacToe.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run(SpecsIo.getResource("fixtures/public/TicTacToe.input"));
        //assertEquals(SpecsIo.getResource("fixtures/public/TicTacToe.txt"), output);

    }

    @Test
    public void testWhileAndIf() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/WhileAndIf.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("10\r\n10\r\n10\r\n10\r\n10\r\n10\r\n10\r\n10\r\n10\r\n10", output.trim());
    }

    @Test
    public void testCheckpoint3Test1() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/checkpoint3/File1.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("Result: 50", output.trim());
    }

    @Test
    public void testCheckpoint3Test2() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/checkpoint3/File2.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("Result: 861", output.trim());
    }

    @Test
    public void testCheckpoint3Test3() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/checkpoint3/File3.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("120", output.trim());
    }

    @Test
    public void testCheckpoint3Test4() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/checkpoint3/File4.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("Result: 392", output.trim());
    }

    @Test
    public void testCheckpoint3Test5() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/checkpoint3/File5.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("13", output.trim());
    }

    @Test
    public void testCheckpoint3Test6() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/checkpoint3/File6.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("5\r\n" +
                "4\r\n" +
                "3\r\n" +
                "2\r\n" +
                "1", output.trim());
    }

    @Test
    public void IIncOptimizationTest() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/IIncOptimization.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("128\r\n" +
                "230\r\n" +
                "-712\r\n" +
                "37000\r\n" +
                "395\r\n" +
                "38256", output.trim());
    }

    @Test
    public void IfCondOptimizationTest() {
        var result = TestUtils.backend(SpecsIo.getResource("fixtures/public/IfCondOptimization.jmm"));
        TestUtils.noErrors(result.getReports());

        var output = result.run();
        assertEquals("1\r\n" +
                "-2\r\n" +
                "-3\r\n" +
                "1\r\n" +
                "2\r\n" +
                "1\r\n" +
                "-2\r\n" +
                "3\r\n" +
                "4\r\n" +
                "5\r\n" +
                "1\r\n" +
                "2\r\n" +
                "1\r\n" +
                "-2\r\n" +
                "-3\r\n" +
                "1\r\n" +
                "2\r\n" +
                "1\r\n" +
                "-2\r\n" +
                "3\r\n" +
                "-4\r\n" +
                "1\r\n" +
                "2\r\n" +
                "1\r\n" +
                "-2\r\n" +
                "-3\r\n" +
                "1\r\n" +
                "2\r\n" +
                "1\r\n" +
                "-2\r\n" +
                "3\r\n" +
                "4\r\n" +
                "-5\r\n" +
                "-1", output.trim());
    }

}
