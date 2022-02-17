/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt;

import nxt.util.Logger;
import nxt.util.Time;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.util.Properties;

public abstract class BlockchainTest extends AbstractBlockchainTest {

    protected static Tester FORGY;
    protected static Tester ALICE;
    protected static Tester BOB;
    protected static Tester CHUCK;
    protected static Tester DAVE;

    protected static int baseHeight;
    
    // FIM-9MAB-AXFN-XXL7-6BHU3
    protected static final String forgerSecretPhrase = "franz dark offer race fuel fake joust waste tensor jk sw 101st";
    private static final String forgerAccountId = "FIM-9MAB-AXFN-XXL7-6BHU3";
    
    // FIM-R4X4-KMHT-RCXD-CLGFZ
    protected static final String secretPhrase1 = "anion harp ere sandal cobol chink bunch tire clare power fogy hump";
    
    // FIM-7MGS-PLDG-ULV4-GHEHB
    protected static final String secretPhrase2 = "astral void larkin era beebe r6 guyana woke hoc dacca cancer await";
    
    // FIM-B842-25LP-FLG8-HLTDH
    protected static final String secretPhrase3 = "mush ripen wharf tub shut nine baldy sk wink epsom batik 6u";
    
    // FIM-LJ94-SPJZ-QHQM-B3VFF
    protected static final String secretPhrase4 = "dublin janus spout lykes tacky gland nice bigot rubric 4v vb peace";

    private static final String aliceSecretPhrase = "hope peace happen touch easy pretend worthless talk them indeed wheel state";
    private static final String bobSecretPhrase2 = "rshw9abtpsa2";
    private static final String chuckSecretPhrase = "eOdBVLMgySFvyiTy8xMuRXDTr45oTzB7L5J";
    private static final String daveSecretPhrase = "t9G2ymCmDsQij7VtYinqrbGCOAtDDA3WiNr";

    protected static boolean isNxtInitted = false;
    protected static boolean needShutdownAfterClass = false;

    public static void initNxt() {
        if (!isNxtInitted) {
            Properties properties = ManualForgingTest.newTestProperties();
            properties.setProperty("fimk.isTestnet", "true");
            properties.setProperty("fimk.isOffline", "true");
            properties.setProperty("fimk.enableFakeForging", "true");
            properties.setProperty("fimk.fakeForgingAccount", forgerAccountId);
            properties.setProperty("fimk.timeMultiplier", "1");
            properties.setProperty("fimk.testnetGuaranteedBalanceConfirmations", "1");
            properties.setProperty("fimk.testnetLeasingDelay", "1");
            properties.setProperty("fimk.disableProcessTransactionsThread", "true");
            AbstractForgingTest.init(properties);
            isNxtInitted = true;
        }
    }
    
    @BeforeClass
    public static void init() {
        needShutdownAfterClass = !isNxtInitted;
        initNxt();
        
        Nxt.setTime(new Time.CounterTime(Nxt.getEpochTime()));
        baseHeight = blockchain.getHeight();
        Logger.logMessage("baseHeight: " + baseHeight);
        
        // id1 = Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getId();
        // id2 = Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getId();
        // id3 = Account.getAccount(Crypto.getPublicKey(secretPhrase3)).getId();
        // id4 = Account.getAccount(Crypto.getPublicKey(secretPhrase4)).getId();
        
        FORGY = new Tester(forgerSecretPhrase);
        ALICE = new Tester(aliceSecretPhrase);
        BOB = new Tester(bobSecretPhrase2);
        CHUCK = new Tester(chuckSecretPhrase);
        DAVE = new Tester(daveSecretPhrase);
    }

    @AfterClass
    public static void shutdown() {
        if (needShutdownAfterClass) {
            Nxt.shutdown();
        }
    }

    @After
    public void destroy() {
        AbstractForgingTest.shutdown();
    }

    public static void generateBlock() {
        try {
            blockchainProcessor.generateBlock(forgerSecretPhrase, Nxt.getEpochTime());
        } catch (BlockchainProcessor.BlockNotAcceptedException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    public static void generateBlocks(int howMany) {
        for (int i = 0; i < howMany; i++) {
            generateBlock();
        }
    }
    public static void rollback(int height) {
        blockchainProcessor.popOffTo(height);
    }
    
}
