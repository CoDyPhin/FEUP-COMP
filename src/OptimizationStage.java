import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import ollir.OllirEmitter;
import pt.up.fe.comp.jmm.ollir.OllirResult;

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

public class OptimizationStage implements JmmOptimization {
    private boolean optimizedWhiles;
    private boolean rflag;

    public OptimizationStage() {
    }

    public OptimizationStage(boolean optimizedWhiles, boolean rflag) {
        this.optimizedWhiles = optimizedWhiles;
        this.rflag = rflag;
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        JmmNode node = semanticsResult.getRootNode();

        // Convert the AST to a String containing the equivalent OLLIR code
        var emitter = new OllirEmitter(semanticsResult.getSymbolTable(), optimizedWhiles, rflag);
        emitter.visit(node); // Convert node ...
        String ollirCode = emitter.getCode();

        //ollirCode = "";

        System.out.println(ollirCode);

        return new OllirResult(semanticsResult, ollirCode, emitter.getReports());
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        // THIS IS JUST FOR CHECKPOINT 3
        return semanticsResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        // THIS IS JUST FOR CHECKPOINT 3
        return ollirResult;
    }

}
