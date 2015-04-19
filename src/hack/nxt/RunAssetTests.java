package nxt;

import nxt.http.AssetSuite;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;

public class RunAssetTests {
  
    public static void main(String[] args) {
        NxtProperties.setup();
        JUnitCore runner = new JUnitCore();
        runner.addListener(new org.junit.internal.TextListener(System.out));
        runner.run(new Computer(), AssetSuite.class);
    }
}

