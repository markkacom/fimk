package nxt;

import nxt.http.PublicKeySuite;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;

public class RunPublicKeyTests {
  
    public static void main(String[] args) {
        NxtProperties.setup();
        JUnitCore runner = new JUnitCore();
        runner.addListener(new org.junit.internal.TextListener(System.out));
        runner.run(new Computer(), PublicKeySuite.class);
    }
}

