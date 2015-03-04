package nxt;

import nxt.crypto.Crypto;
import nxt.util.Logger;
import nxt.util.Time;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.util.Properties;

public abstract class BlockchainTest extends AbstractBlockchainTest {

    protected static int baseHeight;
    
    // FIM-9MAB-AXFN-XXL7-6BHU3
    protected static final String forgerSecretPhrase = "franz dark offer race fuel fake joust waste tensor jk sw 101st";
    
    // FIM-R4X4-KMHT-RCXD-CLGFZ
    protected static final String secretPhrase1 = "anion harp ere sandal cobol chink bunch tire clare power fogy hump";
    
    // FIM-7MGS-PLDG-ULV4-GHEHB
    protected static final String secretPhrase2 = "astral void larkin era beebe r6 guyana woke hoc dacca cancer await";
    
    // FIM-B842-25LP-FLG8-HLTDH
    protected static final String secretPhrase3 = "mush ripen wharf tub shut nine baldy sk wink epsom batik 6u";
    
    // FIM-LJ94-SPJZ-QHQM-B3VFF
    protected static final String secretPhrase4 = "dublin janus spout lykes tacky gland nice bigot rubric 4v vb peace";

    protected static long id1;
    protected static long id2;
    protected static long id3;
    protected static long id4;

    @Before
    public void init() {
        id1 = Account.getAccount(Crypto.getPublicKey(secretPhrase1)).getId();
        id2 = Account.getAccount(Crypto.getPublicKey(secretPhrase2)).getId();
        id3 = Account.getAccount(Crypto.getPublicKey(secretPhrase3)).getId();
        id4 = Account.getAccount(Crypto.getPublicKey(secretPhrase4)).getId();

        Properties properties = ManualForgingTest.newTestProperties();
        properties.setProperty("nxt.isTestnet", "true");
        properties.setProperty("nxt.isOffline", "true");
        properties.setProperty("nxt.enableFakeForging", "true");
        properties.setProperty("nxt.timeMultiplier", "1");
        AbstractForgingTest.init(properties);
        Nxt.setTime(new Time.CounterTime(Nxt.getEpochTime()));
        baseHeight = blockchain.getHeight();
        Logger.logMessage("baseHeight: " + baseHeight);
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

    public static void rollback(int height) {
        blockchainProcessor.popOffTo(height);
    }
    
}
