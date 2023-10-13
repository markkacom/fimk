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

