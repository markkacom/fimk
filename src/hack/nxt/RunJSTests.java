package nxt;

import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;

public class RunJSTests {
  
    public static void main(String[] args) {
        NxtProperties.setup();
        JUnitCore runner = new JUnitCore();
        runner.addListener(new org.junit.internal.TextListener(System.out));
        runner.run(new Computer(), JSTestSuite.class);
        System.exit(0);
    }
}

