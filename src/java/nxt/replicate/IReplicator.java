package nxt.replicate;

import java.util.List;

import nxt.Block;
import nxt.Transaction;

public interface IReplicator {

    boolean rescanBegin(int height);

    boolean blockPopped(Block block);

    boolean beforeBlockApply(Block block);

    boolean updateAccountBalance(long accountId, long balanceNQT, int height);

    boolean updateAccountUnconfirmedBalance(long accountId, long unconfirmedBalanceNQT, int height);

    boolean updateAccountPublicKey(long accountId, byte[] publicKey, int height);

    boolean updateAccountAssetBalance(long accountId, long assetId, long quantityQNT, int height);

    boolean updateAccountAssetUnconfirmedBalance(long accountId, long assetId, long unconfirmedQuantityQNT, int height);

    boolean addedUnconfirmedTransactions(List<? extends Transaction> transactions);

    boolean removedUnconfirmedTransactions(List<? extends Transaction> transactions);

    boolean addedConfirmedTransactions(List<? extends Transaction> transactions);
}
