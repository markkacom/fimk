package uk.co.benjiweber.junitjs;

import static java.util.Arrays.asList;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import nxt.ScriptHelper;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

public class JSRunner extends Runner implements Filterable, Sortable  {
	
	private List<TestClass> tests;
	private final Class<?> cls;

	public JSRunner(Class<?> cls) {
  		this.cls = cls;
  		List<String> testNames = asList(cls.getAnnotation(Tests.class).value());
  		this.tests = findJSTests(testNames);
	}
	
	@Override
	public Description getDescription() {
  		Description suite = Description.createSuiteDescription(cls);
  		for (TestClass testClass : tests) {
    			List<TestCase> tests = testClass.testCases;
    			Description desc = Description.createTestDescription(testClass.junitName(), testClass.junitName());
    			suite.addChild(desc);
    			for (TestCase test : tests) {
      				Description methodDesc = Description.createTestDescription(testClass.junitName(), test.name);
      				desc.addChild(methodDesc);
    			}
  		}
  		return suite;
	}

	@Override
	public void run(RunNotifier notifier) {
      try {
          ScriptHelper.init();
      	  for (TestClass testClass : tests) {
        			List<TestCase> tests = testClass.testCases;
        			for (TestCase test : tests) {
          				Description desc = Description.createTestDescription(testClass.junitName(), test.name);
          				notifier.fireTestStarted(desc);
          				try {
            				  System.out.println("Running test: '"+test.name+"'");  				  
            					test.testCase.run();
            					notifier.fireTestFinished(desc);
          				} catch (Exception | Error e) {
          					  notifier.fireTestFailure(new Failure(desc, bestException(e)));
          				}
        			}
      		}
      } finally {
          ScriptHelper.destroy();
      }
  }
	
	private List<TestClass> findJSTests(List<String> testNames) {
  		try {
    			ScriptEngine engine = getBestJavaScriptEngine();
    			List<TestClass> testClasses = new ArrayList<TestClass>();
    			for (String name : testNames) {
    				testClasses.add(new TestClass(name, load(engine, name)));
    			}
    			return testClasses;
  			
  		} catch (Exception e) {
  			  throw new RuntimeException(e);
  		}
	}

	public static class Loader {
		
  		private final ScriptEngine rhino;
  
  		public Loader(ScriptEngine rhino) {
  			  this.rhino = rhino;
  		}
  		
  		public void load(String filename) {
    			try {
    				  rhino.eval(new FileReader(filename));
    			} catch (FileNotFoundException | ScriptException e) {
    				  throw new RuntimeException(e);
    			}
  		}
	}
	
	private ScriptEngine getBestJavaScriptEngine() throws ScriptException {
  		ScriptEngineManager factory = new ScriptEngineManager();
  		ScriptEngine nashorn = factory.getEngineByName("nashorn");
  		
  		if (nashorn != null) return nashorn;
  		
  		final ScriptEngine rhino = factory.getEngineByName("JavaScript");
  		
  		rhino.put("Loader", new Loader(rhino));
  		rhino.eval("function load(filename) { Loader.load(filename); }");
  		
  		return rhino; 
	}
	
	@SuppressWarnings("unchecked")
	private List<TestCase> load(ScriptEngine engine, String name) throws ScriptException, IOException{
	    return (List<TestCase>) engine.eval(new FileReader(name));
  }

	public void sort(Sorter sorter) {
		  //
	}

	public void filter(Filter filter) throws NoTestsRemainException {
		  //
	}

	private Throwable bestException(Throwable e) {
  		if (nashornException(e))
  			  return e.getCause() != null ? e.getCause() : e;
  		if (rhinoException(e)) {
  			  return extractActualExceptionFromRhino(e);
  		}
  		return e;
	}

	private boolean rhinoException(Throwable e) {
		  return e.getClass().getSimpleName().contains("JavaScript");
	}

	private boolean nashornException(Throwable e) {
		  return e.getClass().getSimpleName().contains("ECMA");
	}

	private Throwable extractActualExceptionFromRhino(Throwable e) {
  		try {
    			Field f = e.getClass().getDeclaredField("value");
    			f.setAccessible(true);
    			Object javascriptWrapper = f.get(e);
    			Field javaThrowable = javascriptWrapper.getClass().getDeclaredField("javaObject");
    			javaThrowable.setAccessible(true);
    			Throwable t = (Throwable) javaThrowable.get(javascriptWrapper);
    			return t;
  		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e1) {
  			  throw new RuntimeException(e);
  		}
	}
	
}
