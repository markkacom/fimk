package uk.co.benjiweber.junitjs;

import java.util.List;

import nxt.BlockchainTest;

public class TestClass extends BlockchainTest {
  
	public final List<TestCase> testCases;
	public final String name;

	public TestClass(String name, List<TestCase> testCases) {
		this.testCases = testCases;
		this.name = name;
	}

  public String junitName() {
      return name.replaceAll("(.*)\\.(.*)","$2.$1");
  }
	
}
