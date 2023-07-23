import org.junit.Test;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

import java.io.FileWriter;
import java.io.IOException;

public class SyntaticalTest {
	@Test
	public void FindMaximumTest() throws IOException {
		String jmmCode  = SpecsIo.getResource("fixtures/public/FindMaximum.jmm");
		FileWriter file  = new FileWriter("./JSON.txt");
		try{
			file.write(TestUtils.parse(jmmCode).getRootNode().toJson());
		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally {
			file.flush();
			file.close();
		}
	}

	@Test
	public void HelloWorldTest() throws IOException {
		String jmmCode  = SpecsIo.getResource("fixtures/public/HelloWorld.jmm");
		FileWriter file  = new FileWriter("./JSON.txt");
		try{
			file.write(TestUtils.parse(jmmCode).getRootNode().toJson());
		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally {
			file.flush();
			file.close();
		}
	}

	@Test
	public void LazySortTest() throws IOException {
		String jmmCode  = SpecsIo.getResource("fixtures/public/LazySort.jmm");
		FileWriter file  = new FileWriter("./JSON.txt");
		try{
			file.write(TestUtils.parse(jmmCode).getRootNode().toJson());
		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally {
			file.flush();
			file.close();
		}
	}

	@Test
	public void LifeTest() throws IOException {
		String jmmCode  = SpecsIo.getResource("fixtures/public/Life.jmm");
		FileWriter file  = new FileWriter("./JSON.txt");
		try{
			file.write(TestUtils.parse(jmmCode).getRootNode().toJson());
		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally {
			file.flush();
			file.close();
		}
	}

	@Test
	public void QuickSortTest() throws IOException {
		String jmmCode  = SpecsIo.getResource("fixtures/public/QuickSort.jmm");
		FileWriter file  = new FileWriter("./JSON.txt");
		try{
			file.write(TestUtils.parse(jmmCode).getRootNode().toJson());
		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally {
			file.flush();
			file.close();
		}
	}

	@Test
	public void SimpleTest() throws IOException {
		String jmmCode  = SpecsIo.getResource("fixtures/public/Simple.jmm");
		FileWriter file  = new FileWriter("./JSON.txt");
		try{
			file.write(TestUtils.parse(jmmCode).getRootNode().toJson());
		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally {
			file.flush();
			file.close();
		}
	}

	@Test
	public void TicTacToeTest() throws IOException {
		String jmmCode  = SpecsIo.getResource("fixtures/public/TicTacToe.jmm");
		FileWriter file  = new FileWriter("./JSON.txt");
		try{
			file.write(TestUtils.parse(jmmCode).getRootNode().toJson());
		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally {
			file.flush();
			file.close();
		}
	}

	@Test
	public void WhileAndIFTest() throws IOException {
		String jmmCode  = SpecsIo.getResource("fixtures/public/fail/semantic/funcNotFound.jmm");
		FileWriter file  = new FileWriter("./JSON.txt");
		try{
			file.write(TestUtils.parse(jmmCode).getRootNode().toJson());
		}
		catch(IOException e){
			e.printStackTrace();
		}
		finally {
			file.flush();
			file.close();
		}
	}
}
