/******************************************************************************
 * Copyright © 2014-2016 Krypto Fin ry and FIMK Developers.                   *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * FIMK software, including this file, may be copied, modified, propagated,   *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

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
    "src/test/resources/AccountIdentifierTest.js",
    "src/test/resources/GossipTest.js"
})
@RunWith(JSRunner.class)
public class JSTestSuite {
}
