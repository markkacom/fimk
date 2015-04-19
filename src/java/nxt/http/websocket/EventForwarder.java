package nxt.http.websocket;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import nxt.Block;
import nxt.BlockchainProcessor;
import nxt.Nxt;
import nxt.Trade;
import nxt.Transaction;
import nxt.TransactionProcessor;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.Convert;
import nxt.util.Listener;

public final class EventForwarder {
  
    static final int ONE_DAY_SECONDS = 24 * 60 * 60;
    static List<Trade> currentBlockTradeCache = Collections.synchronizedList(new ArrayList<Trade>());

    static Map<Long, List<Transaction>> groupTransactionsAccount(List<? extends Transaction> _transactions) {
        Map<Long, List<Transaction>> map = new HashMap<Long, List<Transaction>>();
        for (Transaction transaction : _transactions) {       
            Long recipientKey = Long.valueOf(transaction.getRecipientId());
            List<Transaction> recipient = map.get(recipientKey);
            if (recipient == null) {
                recipient = new ArrayList<Transaction>();
                map.put(recipientKey, recipient);
            }
            recipient.add(transaction);
          
            Long senderKey = Long.valueOf(transaction.getSenderId());
            List<Transaction> sender = map.get(senderKey);
            if (sender == null) {
                sender = new ArrayList<Transaction>();
                map.put(senderKey, sender);
            }
            sender.add(transaction);
        }
        return map;
    }
  
    static Map<Long, List<Trade>> groupTradesAccount(List<? extends Trade> trades) {
        Map<Long, List<Trade>> map = new HashMap<Long, List<Trade>>();
        for (Trade trade : trades) {                      
            Long sellerKey = Long.valueOf(trade.getSellerId());
            List<Trade> seller = map.get(sellerKey);
            if (seller == null) {
                seller = new ArrayList<Trade>();
                map.put(sellerKey, seller);
            }
            seller.add(trade);
          
            Long buyerKey = Long.valueOf(trade.getBuyerId());
            List<Trade> buyer = map.get(buyerKey);
            if (buyer == null) {
                buyer = new ArrayList<Trade>();
                map.put(buyerKey, buyer);
            }
            buyer.add(trade);
        }
        return map;
    }
    
    static Map<Long, List<Trade>> groupTradesAsset(List<? extends Trade> trades) {
        Map<Long, List<Trade>> map = new HashMap<Long, List<Trade>>();
        for (Trade trade : trades) {            
            Long assetKey = Long.valueOf(trade.getAssetId());
            List<Trade> asset = map.get(assetKey);
            if (asset == null) {
                asset = new ArrayList<Trade>();
                map.put(assetKey, asset);
            }
            asset.add(trade);
        }
        return map;
    }

    /*static Map<TypeFilter, List<Transaction>> groupTransactionsType(List<? extends Transaction> _transactions) {
        Map<TypeFilter, List<Transaction>> map = new HashMap<TypeFilter, List<Transaction>>();
        for (Transaction transaction : _transactions) {
            TypeFilter filter = TypeFilter.valueOf(transaction);
            
            List<Transaction> type_list = map.get(filter);
            if (type_list == null) {
                type_list = new ArrayList<Transaction>();
            }
            type_list.add(transaction);
        }
        return map;
    }*/
    
    static {
      
        Nxt.getTransactionProcessor().addListener(new Listener<List<? extends Transaction>>() {
            @Override
            public void notify(List<? extends Transaction> _transactions) {
                if ( ! _transactions.isEmpty() && ! Nxt.getBlockchainProcessor().isScanning()) {
                    Transaction youngest = _transactions.get(0);
                    if (youngest != null && (Nxt.getEpochTime() - youngest.getTimestamp()) < ONE_DAY_SECONDS) {
                        MofoSocketServer.notifyTransactions("REMOVEDUNCONFIRMEDTRANSACTIONSNEW", _transactions, true);
                    }                  
                  
                    MofoSocketServer.notifyTransactions("REMOVEDUNCONFIRMEDTRANSACTIONS", _transactions, true);
                    
                    Map<Long, List<Transaction>> grouped = groupTransactionsAccount(_transactions);
                    for (Entry<Long, List<Transaction>> entry : grouped.entrySet()) {
                        String topic = "REMOVEDUNCONFIRMEDTRANSACTIONS-" + Convert.toUnsignedLong(entry.getKey());
                        MofoSocketServer.notifyTransactions(topic, entry.getValue(), true);
                    }
                    /*Map<TypeFilter, List<Transaction>> grouped2 = groupTransactionsType(_transactions);
                    for (Entry<TypeFilter, List<Transaction>> entry : grouped2.entrySet()) {
                        String topic = "REMOVEDUNCONFIRMEDTRANSACTIONS~" + entry.getKey().getKey();
                        MofoSocketServer.notifyTransactions(topic, entry.getValue(), true);
                    }*/                    
                }
            }
        }, TransactionProcessor.Event.REMOVED_UNCONFIRMED_TRANSACTIONS);
  
        Nxt.getTransactionProcessor().addListener(new Listener<List<? extends Transaction>>() {
            @Override
            public void notify(List<? extends Transaction> _transactions) {
                if ( ! _transactions.isEmpty() && ! Nxt.getBlockchainProcessor().isScanning()) {
                    Transaction youngest = _transactions.get(0);
                    if (youngest != null && (Nxt.getEpochTime() - youngest.getTimestamp()) < ONE_DAY_SECONDS) {
                        MofoSocketServer.notifyTransactions("ADDEDUNCONFIRMEDTRANSACTIONSNEW", _transactions, true);
                    }                   
                  
                    MofoSocketServer.notifyTransactions("ADDEDUNCONFIRMEDTRANSACTIONS", _transactions, true);
                    
                    Map<Long, List<Transaction>> grouped = groupTransactionsAccount(_transactions);
                    for (Long accountId : grouped.keySet()) {
                        String topic = "ADDEDUNCONFIRMEDTRANSACTIONS-" + Convert.toUnsignedLong(accountId);
                        MofoSocketServer.notifyTransactions(topic, grouped.get(accountId), true);
                    }
                }              
            }
        }, TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS);
  
        Nxt.getTransactionProcessor().addListener(new Listener<List<? extends Transaction>>() {
            @Override
            public void notify(List<? extends Transaction> _transactions) {
                if ( ! _transactions.isEmpty() && ! Nxt.getBlockchainProcessor().isScanning()) {
                    Transaction youngest = _transactions.get(0);
                    if (youngest != null && (Nxt.getEpochTime() - youngest.getTimestamp()) < ONE_DAY_SECONDS) {
                        MofoSocketServer.notifyTransactions("ADDEDCONFIRMEDTRANSACTIONSNEW", _transactions, true);
                    }                  
                  
                    MofoSocketServer.notifyTransactions("ADDEDCONFIRMEDTRANSACTIONS", _transactions, false);
                    
                    Map<Long, List<Transaction>> grouped = groupTransactionsAccount(_transactions);
                    for (Long accountId : grouped.keySet()) {
                        String topic = "ADDEDCONFIRMEDTRANSACTIONS-" + Convert.toUnsignedLong(accountId);
                        MofoSocketServer.notifyTransactions(topic, grouped.get(accountId), false);
                    }
                }
            }
        }, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
    
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {              
                if ((Nxt.getEpochTime() - block.getTimestamp()) < ONE_DAY_SECONDS) {
                    MofoSocketServer.notifyBlock("BLOCKPOPPEDNEW", block);
                }
              
                MofoSocketServer.notifyBlockMinimal("BLOCKPOPPED", block);
                
                String topic = "BLOCKPOPPED-" + Convert.toUnsignedLong(block.getGeneratorId());
                MofoSocketServer.notifyBlock(topic, block);
            }
        }, BlockchainProcessor.Event.BLOCK_POPPED);        
    
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                currentBlockTradeCache.clear();
            }
        }, BlockchainProcessor.Event.BEFORE_BLOCK_APPLY);
      
        Trade.addListener(new Listener<Trade>() {
            @Override
            public void notify(Trade trade) {
                currentBlockTradeCache.add(trade);
            }
        }, Trade.Event.TRADE);
  
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
              
                if ((Nxt.getEpochTime() - block.getTimestamp()) < ONE_DAY_SECONDS) {
                    MofoSocketServer.notifyBlock("BLOCKPUSHEDNEW", block);
                    
                    if ( ! currentBlockTradeCache.isEmpty()) {
                        MofoSocketServer.notifyTrades("ADDEDTRADES", currentBlockTradeCache);
                    }                    
                }
              
                MofoSocketServer.notifyBlockMinimal("BLOCKPUSHED", block);
                
                String topic = "BLOCKPUSHED-" + Convert.toUnsignedLong(block.getGeneratorId());
                MofoSocketServer.notifyBlock(topic, block);              
                
                if ( ! currentBlockTradeCache.isEmpty()) {
                  
                    Map<Long, List<Trade>> grouped = groupTradesAccount(currentBlockTradeCache);
                    for (Long accountId : grouped.keySet()) {
                        topic = "ADDEDTRADES-" + Convert.toUnsignedLong(accountId);
                        MofoSocketServer.notifyTrades(topic, grouped.get(accountId));
                    }
                    
                    grouped = groupTradesAsset(currentBlockTradeCache);
                    for (Long assetId : grouped.keySet()) {
                        topic = "ADDEDTRADES*" + Convert.toUnsignedLong(assetId);
                        MofoSocketServer.notifyTrades(topic, grouped.get(assetId));
                    }
                }
            }
        }, BlockchainProcessor.Event.BLOCK_PUSHED);
        
        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                MofoSocketServer.notifyPeerEvent("PEER_BLACKLIST", peer);
            }
         }, Peers.Event.BLACKLIST);
        
        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                MofoSocketServer.notifyPeerEvent("PEER_UNBLACKLIST", peer);
            }
         }, Peers.Event.UNBLACKLIST);
        
        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                MofoSocketServer.notifyPeerEvent("PEER_DEACTIVATE", peer);
            }
         }, Peers.Event.DEACTIVATE);
        
        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                MofoSocketServer.notifyPeerEvent("PEER_REMOVE", peer); 
            }
         }, Peers.Event.REMOVE);
        
        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                MofoSocketServer.notifyPeerEvent("PEER_DOWNLOADED_VOLUME", peer);
            }
         }, Peers.Event.DOWNLOADED_VOLUME);
        
        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                MofoSocketServer.notifyPeerEvent("PEER_UPLOADED_VOLUME", peer);
            }
         }, Peers.Event.UPLOADED_VOLUME);
        
        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                MofoSocketServer.notifyPeerEvent("PEER_WEIGHT", peer);
            }
         }, Peers.Event.WEIGHT);
        
        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                MofoSocketServer.notifyPeerEvent("PEER_ADDED_ACTIVE_PEER", peer);
            }
         }, Peers.Event.ADDED_ACTIVE_PEER);
        
        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                MofoSocketServer.notifyPeerEvent("PEER_CHANGED_ACTIVE_PEER", peer);
            }
         }, Peers.Event.CHANGED_ACTIVE_PEER);
        
        Peers.addListener(new Listener<Peer>() {
            @Override
            public void notify(Peer peer) {
                MofoSocketServer.notifyPeerEvent("PEER_NEW_PEER", peer);
            }
         }, Peers.Event.NEW_PEER);        
    }
    
    static void init() {}
}
