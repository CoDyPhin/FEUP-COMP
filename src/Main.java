
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.JmmParser;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.StringReader;

public class Main implements JmmParser {


	public JmmParserResult parse(String jmmCode) {
		
		try {
			Parser myParser = new Parser(new StringReader(jmmCode));
    		SimpleNode root = myParser.Program(); // returns reference to root node
            	
    		root.dump(""); // prints the tree on the screen
    	
    		return new JmmParserResult(root, myParser.getReports());
		} catch(ParseException e) {
			throw new RuntimeException("Error while parsing", e);
		}
	}

    public static void main(String[] args) throws IOException {
        System.out.println("Executing with args: " + Arrays.toString(args));

		File jmmFile = new File(args[0]);
		String file = Files.readString(jmmFile.toPath());

		boolean optimizedWhiles = false;
		boolean rflag = false;
		int n = 0;
		if (args.length > 1) {
			if (args[1].equals("-o") || (args.length > 2 && args[2].equals("-o"))) {
				optimizedWhiles = true;
				System.out.println("Optimized Whiles");
			}
			if (args[1].contains("-r") || (args.length > 2 && args[2].contains("-r"))){
				 try{
				 	n = Integer.parseInt(args[(args[1].contains("-r")?1:2)].split("=")[1]);
				 	if(n <= 0){
				 		throw new RuntimeException("Wrong usage of flag -r. n must be greater than 0");
					}
				 	System.out.println("Local variable check activated");
				 	rflag = true;
				 }catch(ArrayIndexOutOfBoundsException a){
					throw new RuntimeException("Wrong usage of flag -r. Usage -r=n\n");
				 }
			}
		}

		JmmParserResult jmmParserResult = new Main().parse(file);
		JmmSemanticsResult jmmSemanticsResult = new AnalysisStage().semanticAnalysis(jmmParserResult);
		OllirResult ollirResult = new OptimizationStage(optimizedWhiles, rflag).toOllir(jmmSemanticsResult);
		if(rflag){
			System.out.println("\n=======Local Variable Report=======");
			System.out.println("-----------------------------------");
			int minn = 1;
			boolean stopexec = false;
			ClassUnit ollirClass = ollirResult.getOllirClass();
			ollirClass.buildVarTables();
			for(Method method: ollirClass.getMethods()){
				int vartsize = method.isConstructMethod() ? method.getVarTable().size() : method.getVarTable().size()+1;
				minn = Math.max(minn, vartsize);
				if(vartsize > n){
					stopexec = true;
				}
				System.out.println("Method: " + method.getMethodName() + ":\n\tLocal Variables Used in JVM: " + vartsize + "\n\tMaximum Number Requested: " + n + "\n" + "\tOptimization " + (vartsize > n? "failed" : "successful" + "\n-----------------------------------"));
			}
			if(stopexec){
				throw new RuntimeException("The number of local variables provided are not enough to compile the code. Minimum number required: " + minn);
			}
			System.out.println("===================================\n");
		}
		JasminResult jasminResult = new BackendStage().toJasmin(ollirResult);

		Path path = Paths.get(ollirResult.getSymbolTable().getClassName() + "_results/");
		if(!Files.exists(path)) {
			Files.createDirectories(path);
		}

		//AST
		FileWriter fileWriter  = new FileWriter(path + "/ast.json");
		fileWriter.write(jmmParserResult.toJson());
		fileWriter.close();

		// Symbol Table
		FileWriter fileWriter2  = new FileWriter(path + "/symbolTable.txt");
		fileWriter2.write(jmmSemanticsResult.getSymbolTable().print());
		fileWriter2.close();

		// Ollir
		FileWriter fileWriter3  = new FileWriter(path + "/" + ollirResult.getSymbolTable().getClassName() + ".ollir");
		fileWriter3.write(ollirResult.getOllirCode());
		fileWriter3.close();

		// Jasmin
		FileWriter fileWriter4 = new FileWriter(path + "/" + ollirResult.getSymbolTable().getClassName() + ".j");
		fileWriter4.write(jasminResult.getJasminCode());
		fileWriter4.close();

		jasminResult.compile(path.toFile());
		//jasminResult.run();

		if (args[0].contains("fail")) {
			throw new RuntimeException("It's supposed to fail");
		}
    }


}