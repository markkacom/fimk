package nxt;

import org.junit.runner.RunWith;

import uk.co.benjiweber.junitjs.JSRunner;
import uk.co.benjiweber.junitjs.Tests;

@Tests({
    "src/test/resources/TestSystemTest.js",
    "src/test/resources/PrivateAssetTest.js",
    "src/test/resources/OrderFeeTest.js",
    "src/test/resources/TradeFeeTest.js",
    "src/test/resources/RemoveAllowedTest.js",
    "src/test/resources/InstantExhange.js",
    "src/test/resources/AccountIdentifierTest.js"
})
@RunWith(JSRunner.class)
public class JSTestSuite {
}
