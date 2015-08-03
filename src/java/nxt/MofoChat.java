package nxt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nxt.db.DbIterator;
import nxt.db.DbUtils;
import nxt.util.Listener;


public class MofoChat {
    
    public static class Chat {

        private long accountId;
        private int timestamp;

        public Chat(long accountId, int timestamp) {
            this.accountId = accountId;
            this.timestamp = timestamp;
        }

        public long getAccountId() {
            return accountId;
        }

        public int getTimestamp() {
            return timestamp;
        }
    }

    static {

        /* Confirmed transactions are removed from the transient messages table
         * since they now become available in the transaction table. */
        Nxt.getTransactionProcessor().addListener(new Listener<List<? extends Transaction>>() {
            @Override
            public void notify(List<? extends Transaction> t) {
            }
        }, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);

        /* Unconfirmed transactions are added to the transient messages table, 
         * this allows us to look up messages in this table instead of having  
         *  */
        Nxt.getTransactionProcessor().addListener(new Listener<List<? extends Transaction>>() {
            @Override
            public void notify(List<? extends Transaction> t) {
            }
        }, TransactionProcessor.Event.ADDED_UNCONFIRMED_TRANSACTIONS);

        
      
    }

    public static DbIterator<? extends Transaction> getChatTransactions(long accountOne, long accountTwo, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction "
                    + "WHERE sender_id = ? AND recipient_id = ? AND type = 1 AND subtype = 0 "
                    + "UNION "
                    + "SELECT * FROM transaction "
                    + "WHERE sender_id = ? AND recipient_id = ? AND type = 1 AND subtype = 0 "
                    + "ORDER BY timestamp DESC "
                    +  DbUtils.limitsClause(from, to));
          
            int i = 0;
            pstmt.setLong(++i, accountOne);
            pstmt.setLong(++i, accountTwo);
            pstmt.setLong(++i, accountTwo);
            pstmt.setLong(++i, accountOne);
            DbUtils.setLimits(++i, pstmt, from, to);
          
            return Nxt.getBlockchain().getTransactions(con, pstmt);
        }
        catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static List<Chat> getChatList(long accountId, int from, int to) {
        try (Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT account_id, latest "
                    + "FROM ( "
                    + "  SELECT account_id, MAX(timestamp) AS latest "
                    + "  FROM ( "
                    + "    SELECT sender_id AS account_id, timestamp "
                    + "    FROM transaction "
                    + "    WHERE recipient_id = ? AND sender_id <> ? AND type = 1 AND subtype = 0 "                  
                    + "    UNION ALL "
                    + "    SELECT recipient_id AS account_id, timestamp "
                    + "    FROM transaction "
                    + "    WHERE sender_id = ? AND type = 1 AND subtype = 0"
                    + "  )"
                    + "  GROUP BY account_id " 
                    + ")"
                    + "ORDER BY latest DESC "
                    +  DbUtils.limitsClause(from, to));
        ) {
            int i = 0;
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            pstmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, pstmt, from, to);

            List<Chat> chatList = new ArrayList<Chat>();
            Map<Long, Chat> chatMap = from == 0 ? new HashMap<Long, Chat>() : null;
            
            try (ResultSet rs = pstmt.executeQuery()) {
              
                while (rs.next()) {
                    long id = rs.getLong(1);
                    int timestamp = rs.getInt(2);
                    
                    Chat chat = new Chat(id, timestamp);
                    chatList.add(chat);
                    if (from == 0) {
                        chatMap.put(Long.valueOf(chat.getAccountId()), chat);
                    }
                }
            }
            
            /* Include or update based on unconfirmed transactions */
            if (from == 0) {
                try (DbIterator<? extends Transaction> iterator = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {
                    outerLoop: 
                    while (iterator.hasNext()) {
                        UnconfirmedTransaction transaction = (UnconfirmedTransaction) iterator.next();
                        if (transaction.getType().getType() == 1 && transaction.getType().getSubtype() == 0) {
                            long otherAccountId;
                            if (transaction.getSenderId() == accountId) {
                                otherAccountId = transaction.getRecipientId();
                            }
                            else if (transaction.getRecipientId() == accountId) {
                                otherAccountId = transaction.getSenderId();
                            }
                            else {
                                continue;
                            }
                            
                            /* Either update the existing timestamp or add a new Chat object */
                            Chat chat = chatMap.get(Long.valueOf(otherAccountId));
                            if (chat != null) {
                                chat.timestamp = Integer.max(transaction.getTimestamp(), chat.timestamp);
                            }
                            else {
                                chat = new Chat(otherAccountId, transaction.getTimestamp());
                                for (int j=0; j<chatList.size(); j++) {
                                    if (chatList.get(j).getTimestamp() <= transaction.getTimestamp()) {
                                        chatList.add(j, chat);
                                        continue outerLoop;
                                    }
                                }
                                chatList.add(chat);
                            }
                        }
                    }
                }
            }
            
            return chatList;
            
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }      
    }

}
