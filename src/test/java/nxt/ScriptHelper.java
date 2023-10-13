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

import java.util.Properties;

import nxt.crypto.Crypto;
import nxt.util.Listener;
import nxt.util.Logger;
import nxt.util.Time;

import org.junit.After;
import org.junit.Assert;

public class ScriptHelper {
  
    public static BlockchainImpl blockchain;
    public static BlockchainProcessorImpl blockchainProcessor;
    private static final Object doneLock = new Object();
    private static boolean done = false;  
    public static final String forgerSecretPhrase = "franz dark offer race fuel fake joust waste tensor jk sw 101st";
    public static final String secretPhrase1 = "anion harp ere sandal cobol chink bunch tire clare power fogy hump";
    public static final String secretPhrase2 = "astral void larkin era beebe r6 guyana woke hoc dacca cancer await";
    public static final String secretPhrase3 = "mush ripen wharf tub shut nine baldy sk wink epsom batik 6u";
    public static final String secretPhrase4 = "dublin janus spout lykes tacky gland nice bigot rubric 4v vb peace";  
  
    public static void init() {
        Properties properties = new Properties();
        properties = new Properties();
        properties.setProperty("fimk.shareMyAddress", "false");
        properties.setProperty("fimk.savePeers", "false");
        properties.setProperty("fimk.disableGenerateBlocksThread", "true");
        properties.setProperty("fimk.testUnconfirmedTransactions", "true");
        properties.setProperty("fimk.debugTraceAccounts", "");
        properties.setProperty("fimk.debugLogUnconfirmed", "false");
        properties.setProperty("fimk.debugTraceQuote", "\"");
        properties.setProperty("fimk.numberOfForkConfirmations", "0");
        properties.setProperty("fimk.isTestnet", "true");
        properties.setProperty("fimk.testDbUrl", "jdbc:h2:fim_unit_tests_db/fim;DB_CLOSE_ON_EXIT=FALSE;MVCC=TRUE");
        properties.setProperty("fimk.isOffline", "true");      
        properties.setProperty("fimk.enableFakeForging", "true");
        properties.setProperty("fimk.timeMultiplier", "1");
        properties.setProperty("fimk.fakeForgingAccount", "FIM-9MAB-AXFN-XXL7-6BHU3");
  
        Nxt.init(properties);
        blockchain = BlockchainImpl.getInstance();
        blockchainProcessor = BlockchainProcessorImpl.getInstance();
        blockchainProcessor.setGetMoreBlocks(false);
        Listener<Block> countingListener = new Listener<Block>() {
            @Override
            public void notify(Block block) {
                if (block.getHeight() % 1000 == 0) {
                    Logger.logMessage("downloaded block " + block.getHeight());
                }
            }
        };
        blockchainProcessor.addListener(countingListener, BlockchainProcessor.Event.BLOCK_PUSHED);
        blockchainProcessor.addListener(new Helper.BlockListener(), BlockchainProcessor.Event.BLOCK_GENERATED);    
        Nxt.setTime(new Time.CounterTime(Nxt.getEpochTime()));
    }
  
    @After
    public static void destroy() {
        Nxt.getTransactionProcessor().clearUnconfirmedTransactions();
        Assert.assertEquals(0, Helper.getCount("unconfirmed_transaction"));
        Nxt.shutdown(); 
    }
    
    public static void clearUnconfirmedTransactions() {
        Nxt.getTransactionProcessor().clearUnconfirmedTransactions();
    }
    
    public static void generateBlock(String secretPhrase) {
        try {
            blockchainProcessor.generateBlock(secretPhrase, Nxt.getEpochTime());
        } catch (BlockchainProcessor.BlockNotAcceptedException e) {
            e.printStackTrace();
            Assert.fail();
        }
    }
  
    public static BlockchainImpl getBlockchain() {
        return blockchain;
    }
    
    public static BlockchainProcessorImpl getBlockchainProcessor() {
        return blockchainProcessor;
    }
    
    public static void rollback(int height) {
        blockchainProcessor.popOffTo(height);
    }
    
    public static void downloadTo(final int endHeight) {
        if (blockchain.getHeight() == endHeight) {
            return;
        }
        Assert.assertTrue(blockchain.getHeight() < endHeight);
        Listener<Block> stopListener = new Listener<Block>() {
            @Override
            public void notify(Block block) {
                if (blockchain.getHeight() == endHeight) {
                    synchronized (doneLock) {
                        done = true;
                        blockchainProcessor.setGetMoreBlocks(false);
                        doneLock.notifyAll();
                        throw new NxtException.StopException("Reached height " + endHeight);
                    }
                }
            }
        };
        blockchainProcessor.addListener(stopListener, BlockchainProcessor.Event.BLOCK_PUSHED);
        synchronized (doneLock) {
            done = false;
            Logger.logMessage("Starting download from height " + blockchain.getHeight());
            blockchainProcessor.setGetMoreBlocks(true);
            while (! done) {
                try {
                    doneLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        Assert.assertEquals(endHeight, blockchain.getHeight());
        blockchainProcessor.removeListener(stopListener, BlockchainProcessor.Event.BLOCK_PUSHED);
    }
  
    public static void forgeTo(final int endHeight, final String secretPhrase) {
        if (blockchain.getHeight() == endHeight) {
            return;
        }
        Assert.assertTrue(blockchain.getHeight() < endHeight);
        Listener<Block> stopListener = new Listener<Block>() {
            @Override
            public void notify(Block block) {
                if (blockchain.getHeight() == endHeight) {
                    synchronized (doneLock) {
                        done = true;
                        Generator.stopForging(secretPhrase);
                        doneLock.notifyAll();
                    }
                }
            }
        };
        blockchainProcessor.addListener(stopListener, BlockchainProcessor.Event.BLOCK_PUSHED);
        synchronized (doneLock) {
            done = false;
            Logger.logMessage("Starting forging from height " + blockchain.getHeight());
            Generator.startForging(secretPhrase);
            while (! done) {
                try {
                    doneLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        Assert.assertTrue(blockchain.getHeight() >= endHeight);
        Assert.assertArrayEquals(Crypto.getPublicKey(secretPhrase), blockchain.getLastBlock().getGeneratorPublicKey());
        blockchainProcessor.removeListener(stopListener, BlockchainProcessor.Event.BLOCK_PUSHED);
    }  
  
}
