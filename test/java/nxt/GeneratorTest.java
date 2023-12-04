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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;

public class GeneratorTest extends BlockchainTest {

    /**
     * Simulate the forging process calculations
     */
    @Ignore
    @Test
    public void forge() throws Exception {
        byte[] publicKey = ALICE.getPublicKey();
        BlockImpl lastBlock = blockchain.getLastBlock();
        BigInteger[] hits = Generator.calculateHits(publicKey, lastBlock);
        Account account = Account.getAccount(publicKey);
        BigInteger effectiveBalance = BigInteger.valueOf(account == null || account.getEffectiveBalanceNXT() <= 0 ? 0 : account.getEffectiveBalanceNXT());
        long hitTime = Generator.calculateHitTime(effectiveBalance, hits, lastBlock)[0];
        long deadline = hitTime - lastBlock.getTimestamp();
        Object forgingStartResult = Generator.startForging(ALICE.getSecretPhrase());
        if (forgingStartResult instanceof String) {
            throw new Exception((String) forgingStartResult);
        }
        Generator generator = (Generator) forgingStartResult;
        int i=1;
        try {
            while (i<deadline) {
                Assert.assertFalse(generator.forge(lastBlock, lastBlock.getTimestamp() + i));
                i += 100;
            }
            Assert.assertEquals(true, generator.forge(lastBlock, (int)hitTime + 1));
        } catch (BlockchainProcessor.BlockNotAcceptedException e) {
            e.printStackTrace();
        }

        // Now the block is broadcast to all peers
        // This is what the peer which receives the block does
        lastBlock = blockchain.getLastBlock();
        Assert.assertEquals(hitTime + 1, lastBlock.getTimestamp());
        try {
            Assert.assertTrue(lastBlock.verifyGenerationSignature());
        } catch (BlockchainProcessor.BlockOutOfOrderException e) {
            e.printStackTrace();
        }
    }

}
