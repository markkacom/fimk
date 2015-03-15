package nxt.http;

import org.junit.Test;

import nxt.Account;
import nxt.BlockchainTest;
import nxt.Constants;
import nxt.Nxt;
import nxt.crypto.Crypto;
import nxt.http.APICall.Builder;
import nxt.util.Convert;

public class TestPublicKey extends BlockchainTest {
  
    /**
     * To enable public key less account set the alias 
     * "publickeyannouncementrequired" to "false".
     */  
  
    static final int BEFORE_PUBLIC_KEY_ENABLED_HEIGHT = 43000;
    static final String ALIAS_NAME = "publickeyannouncementrequired";
  
    @Test
    public void testPublicKeyRequired() {
        rollback(BEFORE_PUBLIC_KEY_ENABLED_HEIGHT);

        long recipientId = Account.getId(Crypto.getPublicKey("hello-good-bye"));
        String recipientPublicKey = Convert.toHexString(Crypto.getPublicKey("hello-good-bye"));
        
        // sending money without published public key will fail 
        MofoHelper.sendMoney(secretPhrase1, recipientId, 100 * Constants.ONE_NXT, "Recipient account does not have a public key, must attach a public key announcement");
        
        int height = Nxt.getBlockchain().getHeight();            
        
        // sending money while providing the recipient public key will succeed
        MofoHelper.sendMoney(secretPhrase1, recipientId, recipientPublicKey, 100 * Constants.ONE_NXT);
        
        // rollback to previous height
        rollback(height);
        
        // now sending without public key will fail again
        MofoHelper.sendMoney(secretPhrase1, recipientId, 100 * Constants.ONE_NXT, "Recipient account does not have a public key, must attach a public key announcement");        
    }
  
    @Test
    public void testAliasEnablesPublicKeyNotNeeded() {
        rollback(BEFORE_PUBLIC_KEY_ENABLED_HEIGHT);
  
        long recipientId = Account.getId(Crypto.getPublicKey("hello-good-bye"));
        String recipientPublicKey = Convert.toHexString(Crypto.getPublicKey("hello-good-bye"));
        
        // sending money without published public key will fail 
        MofoHelper.sendMoney(secretPhrase1, recipientId, 100 * Constants.ONE_NXT, "Recipient account does not have a public key, must attach a public key announcement");
        
        int height = Nxt.getBlockchain().getHeight();            

        // enable the lifting of the public key requirement by setting the alias
        MofoHelper.setAlias(secretPhrase1, ALIAS_NAME, "false");
        
        // sending money without providing the recipient public key will now succeed
        MofoHelper.sendMoney(secretPhrase1, recipientId, 100 * Constants.ONE_NXT);        
        
        // rollback to previous height
        rollback(height);
        
        // now sending without public key will fail again
        MofoHelper.sendMoney(secretPhrase1, recipientId, 100 * Constants.ONE_NXT, "Recipient account does not have a public key, must attach a public key announcement");                
    }
    
    @Test
    public void testUnconfirmed() {
        rollback(BEFORE_PUBLIC_KEY_ENABLED_HEIGHT);
        
        long recipientId = Account.getId(Crypto.getPublicKey("hello-good-bye"));
        String recipientPublicKey = Convert.toHexString(Crypto.getPublicKey("hello-good-bye"));      
      
        // sending money without published public key will fail 
        MofoHelper.sendMoney(secretPhrase1, recipientId, 100 * Constants.ONE_NXT, "Recipient account does not have a public key, must attach a public key announcement");

        // create setAlias transaction but leave it unconfirmed
        MofoHelper.invoke(MofoHelper.call(secretPhrase1, "setAlias").param("aliasName", ALIAS_NAME).param("aliasURI", "false").build(), true, null);

        // sending money still fails 
        MofoHelper.sendMoney(secretPhrase1, recipientId, 100 * Constants.ONE_NXT, "Recipient account does not have a public key, must attach a public key announcement");
        
        // make the alias confirmed
        BlockchainTest.generateBlock();
        
        // sending money without providing the recipient public key will now succeed
        MofoHelper.sendMoney(secretPhrase1, recipientId, 100 * Constants.ONE_NXT);   
    }
}
