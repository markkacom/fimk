package nxt;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import nxt.Appendix.Message;
import nxt.Attachment.AbstractAttachment;
import nxt.TransactionType.Messaging;
import nxt.db.DbIterator;
import nxt.db.DbUtils;
import nxt.util.Convert;

public final class MofoQueries {

    static final int SECONDS_IN_DAY = 60 * 60 * 24;
    static final int ONE_MONTH_SECONDS = 31 * SECONDS_IN_DAY;    
  
    public static class TransactionFilter {
        int type;
        int subtype;
        
        public TransactionFilter(int type, int subtype) {
            this.type = type;
            this.subtype = subtype;
        }
        
        @Override
        public String toString() {
            return " NOT(type = "+this.type+" AND subtype = "+this.subtype+") ";
        }
        
        public boolean match(Transaction transaction) {
            return transaction.getType().getType() == this.type && 
                transaction.getType().getSubtype() == this.subtype;
        }
    }
    
    public static class InclusiveTransactionFilter extends TransactionFilter {

        public InclusiveTransactionFilter(int type, int subtype) {
            super(type, subtype);  
        }
        
        @Override
        public String toString() {
            return " (type = "+this.type+" AND subtype = "+this.subtype+") ";
        }      
    }
    
    public static class RewardsStruct {
        int timestamp;
        long sumNQT;
        int count;
        
        public RewardsStruct(int timestamp, long sumNQT, int count) {
            this.timestamp = timestamp;
            this.sumNQT = sumNQT;
            this.count = count;
        }
        
        public long getTotalRewardsNQT() {
            if ("FIMK".equals(Nxt.APPLICATION)) {
              this.sumNQT += (this.count * (200 * Constants.ONE_NXT));
            }          
            return this.sumNQT;
        }
        
        public int getCount() {
          return this.count;
        }
    }
    
    public static class ForgingStatStruct implements Comparable<ForgingStatStruct> {
      
        long accountId;
        int blockCount;
        long totalFeeNQT;
        
        ForgingStatStruct(long accountId) {
            this.accountId    = accountId;
            this.totalFeeNQT  = 0;
            this.blockCount   = 0;
        }
        
        public void add(long amountNQT) {
            totalFeeNQT += amountNQT;
            blockCount += 1;
        }
        
        public long getTotalFeeNQT() {
            long total = 0;
            if ("FIMK".equals(Nxt.APPLICATION)) {
              total = totalFeeNQT + (blockCount * (200 * Constants.ONE_NXT));
            }          
            return total;
        }
        
        public long getAccountId() {
            return accountId;
        }

        public int getBlockCount() {
            return blockCount;
        }
        
        @Override
        public int compareTo(ForgingStatStruct other) {
            if ( this.totalFeeNQT < other.totalFeeNQT ) {
                return 1;
            }
            else if ( this.totalFeeNQT > other.totalFeeNQT ) {
                return -1;
            }
            else {
                if ( this.accountId > other.accountId ) {
                    return 1;
                }
                else {
                    return -1;
                }
            }
        }
    }
    
    static String filterClause(List<TransactionFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return "";
        }
        
        List<TransactionFilter> inclusive = new ArrayList<TransactionFilter>();
        List<TransactionFilter> exclusive = new ArrayList<TransactionFilter>();
        for (int i=0; i<filters.size(); i++) {
            if (filters.get(i) instanceof InclusiveTransactionFilter) {
                inclusive.add(filters.get(i));
            }
            else {
                exclusive.add(filters.get(i));
            }
        }
        
        StringBuilder b = new StringBuilder();        
        if ( ! inclusive.isEmpty()) {
            b.append(" AND (");
            for (int i=0; i<inclusive.size(); i++) {
                if (i > 0) {
                    b.append(" OR ");
                }
                b.append(inclusive.get(i).toString());
            }
            b.append(") ");
        }
        if ( ! exclusive.isEmpty()) {
            b.append(" AND (");
            for (int i=0; i<exclusive.size(); i++) {
                if (i > 0) {
                    b.append(" AND ");
                }
                b.append(exclusive.get(i).toString());
            }
            b.append(") ");
        }          
        return b.toString();
    }
    
    public static List<ForgingStatStruct> getForgingStats24H(int timestamp) {
        try 
           (Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT generator_id, total_fee, height "
                    + " FROM block "
                    + " WHERE timestamp < ? AND timestamp > ? " 
                    + " ORDER BY timestamp");)
            
         {
            int i = 0;
            pstmt.setInt(++i, timestamp);
            pstmt.setInt(++i, timestamp - SECONDS_IN_DAY);

            Map<Long, ForgingStatStruct> map = new HashMap<Long, ForgingStatStruct>();
            try (ResultSet rs = pstmt.executeQuery()) {              
                while (rs.next()) {                  
                    Long account_id = Long.valueOf(rs.getLong(1));
                    long total_fee = rs.getLong(2);
                    int height = rs.getInt(3);
                  
                    ForgingStatStruct stat = map.get(account_id);
                    if (stat == null) {
                        stat = new ForgingStatStruct(account_id);
                        map.put(account_id, stat);
                    }
                    
                    /* Must implement a catch here in case we are on NXT and not on FIMK */
                    //stat.add(total_fee + RewardsImpl.calculatePOSRewardNQT(height));
                    stat.add(total_fee);
                }
            }
            
            List<ForgingStatStruct> list = new ArrayList<ForgingStatStruct>(map.values());
            Collections.sort(list);
            return list;
        } 
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static DbIterator<? extends Block> getBlocks(int timestamp, int limit) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block "
                    + " WHERE timestamp < ? " 
                    + " ORDER BY timestamp DESC LIMIT ?");
            int i = 0;
            pstmt.setInt(++i, timestamp);
            pstmt.setInt(++i, limit);
            return Nxt.getBlockchain().getBlocks(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static DbIterator<? extends Block> getBlocks(Account account, int timestamp, int limit) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block "
                    + " WHERE generator_id = ? AND "
                    + " timestamp < ? "
                    + " ORDER BY timestamp DESC LIMIT ?");
            int i = 0;
            pstmt.setLong(++i, account.getId());
            pstmt.setInt(++i, timestamp);
            pstmt.setInt(++i, limit);
            return Nxt.getBlockchain().getBlocks(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static Block getLastBlock(long account_id) {
        try 
           (Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM block "
                    + " WHERE generator_id = ? " 
                    + " ORDER BY timestamp DESC LIMIT 1");)
        {
            int i = 0;
            pstmt.setLong(++i, account_id);
            try (DbIterator<? extends Block> blocks = Nxt.getBlockchain().getBlocks(con, pstmt)) {
                if (blocks.hasNext()) {
                    return blocks.next();
                }
                return null;
            }
        } 
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<Trade> getAccountAssetTradesBefore(Account account, int timestamp, int limit) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM trade WHERE seller_id = ? AND timestamp <= ?"
                    + " UNION ALL SELECT * FROM trade WHERE buyer_id = ? " 
                    + " AND seller_id <> ? "
                    + " AND timestamp < ? "
                    + " ORDER BY timestamp DESC LIMIT ?");
            int i = 0;
            pstmt.setLong(++i, account.getId());
            pstmt.setInt(++i, timestamp);
            pstmt.setLong(++i, account.getId());
            pstmt.setLong(++i, account.getId());
            pstmt.setInt(++i, timestamp);
            pstmt.setInt(++i, limit);
            return Trade.getTable().getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
  
    public static DbIterator<Trade> getAssetTradesBefore(int timestamp, int limit) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM trade WHERE timestamp < ?"
                    + " ORDER BY timestamp DESC LIMIT ?");
            int i = 0;
            pstmt.setInt(++i, timestamp);
            pstmt.setInt(++i, limit);
            return Trade.getTable().getManyBy(con, pstmt, false);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static List<? extends Transaction> getUnconfirmedAccountPosts(long account_id, int limit) {
        try (DbIterator<? extends Transaction> iterator = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {
            
            List<Transaction> filtered = new ArrayList<Transaction>();
            while (iterator.hasNext() && filtered.size() < limit) {
                
                Transaction transaction = iterator.next();
                if (transaction.getType().getType() == 1 && transaction.getType().getSubtype() == 0) {
                  
                    if (transaction.getSenderId() == transaction.getRecipientId() && account_id == transaction.getSenderId()) {
                      
                        for (Appendix appendage : transaction.getAppendages()) {
                            
                            if (appendage instanceof Message && ((Message) appendage).isText()) {
                                
                                byte type = MofoMessaging.parsePost(((Message) appendage).getMessage());
                                if (type == MofoMessaging.TYPE_ACCOUNT_POST) {
                                  
                                    filtered.add(transaction);
                                }
                                break;
                            }
                        }  
                    }
                }
            }
            return filtered;
        }
    }

    public static JSONArray getAccountPosts(long account_id, int from, int to) throws UnsupportedEncodingException {
        try ( 
            Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT "
                    + "id, height, timestamp, attachment_bytes, ("
                    + "  SELECT COUNT(*) FROM mofo_comment "
                    + "  WHERE post_transaction_id = ts.id"
                    + ") AS comment_count "
                    + "FROM transaction AS ts "
                    + "WHERE id IN ("
                    + "  SELECT transaction_id FROM mofo_post "
                    + "  WHERE sender_account_id = ? "
                    + "  AND type = ? "
                    + "  ORDER BY timestamp DESC "
                    +    DbUtils.limitsClause(from, to)                    
                    + ")");
            ) {
            
            int i = 0;
            pstmt.setLong(++i, account_id);
            pstmt.setByte(++i, MofoMessaging.TYPE_ACCOUNT_POST);
            DbUtils.setLimits(++i, pstmt, from, to);
            
            JSONArray response = new JSONArray();
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                  
                    long transaction_id = rs.getLong("id");
                    int height = rs.getInt("height");
                    int timestamp = rs.getInt("timestamp");
                    byte[] attachmentBytes = rs.getBytes("attachment_bytes");
                    int comment_count = rs.getInt("comment_count");
                  
                    ByteBuffer buffer = null;
                    if (attachmentBytes != null) {
                        buffer = ByteBuffer.wrap(attachmentBytes);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        
                        buffer.get(); // version
                        int messageLength = buffer.getInt(); // content length
                        if (messageLength < 0) {
                            messageLength &= Integer.MAX_VALUE;
                        }
                        byte[] message = new byte[messageLength];
                        buffer.get(message);

                        JSONObject post = new JSONObject();
                        post.put("transaction", Convert.toUnsignedLong(transaction_id));
                        post.put("confirmations", Nxt.getBlockchain().getHeight() - height);
                        post.put("height", height);
                        post.put("timestamp", timestamp);
                        post.put("comment_count", comment_count);
                        
                        post.put("message", new String(message, 6, message.length-6, "UTF-8"));
                        
                        response.add(post);
                    }
                }
            }
            return response;
            
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static List<? extends Transaction> getUnconfirmedAssetPosts(long asset_id, int limit) {
        try (DbIterator<? extends Transaction> iterator = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {
            
            List<Transaction> filtered = new ArrayList<Transaction>();
            while (iterator.hasNext() && filtered.size() < limit) {
                
                Transaction transaction = iterator.next();
                if (transaction.getType().getType() == 1 && transaction.getType().getSubtype() == 0) {
                  
                    if (transaction.getSenderId() == transaction.getRecipientId()) {
                      
                        for (Appendix appendage : transaction.getAppendages()) {
                            
                            if (appendage instanceof Message && ((Message) appendage).isText()) {
                                
                                byte type = MofoMessaging.parsePost(((Message) appendage).getMessage());
                                if (type == MofoMessaging.TYPE_ASSET_POST) {
                                  
                                    long identifier = MofoMessaging.parsePostIdentifier(((Message) appendage).getMessage());
                                    if (identifier == asset_id) {
                                  
                                        filtered.add(transaction);
                                    }
                                }
                                break;
                            }
                        }  
                    }
                }
            }
            return filtered;
        }
    }
      
    public static JSONArray getAssetPosts(long asset_id, int from, int to) throws UnsupportedEncodingException {
        try ( 
            Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT "
                    + "id, height, timestamp, attachment_bytes, ("
                    + "  SELECT COUNT(*) FROM mofo_comment "
                    + "  WHERE post_transaction_id = ts.id"
                    + ") AS comment_count "
                    + "FROM transaction AS ts "
                    + "WHERE id IN ("
                    + "  SELECT transaction_id FROM mofo_post "
                    + "  WHERE referenced_entity_id = ? "
                    + "  AND type = ? "
                    + "  ORDER BY timestamp DESC "
                    +    DbUtils.limitsClause(from, to)                    
                    + ")");
            ) {
            
            int i = 0;
            pstmt.setLong(++i, asset_id);
            pstmt.setByte(++i, MofoMessaging.TYPE_ASSET_POST);
            DbUtils.setLimits(++i, pstmt, from, to);
            
            JSONArray response = new JSONArray();
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                  
                    long transaction_id = rs.getLong("id");
                    int height = rs.getInt("height");
                    int timestamp = rs.getInt("timestamp");
                    byte[] attachmentBytes = rs.getBytes("attachment_bytes");
                    int comment_count = rs.getInt("comment_count");
                  
                    ByteBuffer buffer = null;
                    if (attachmentBytes != null) {
                        buffer = ByteBuffer.wrap(attachmentBytes);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        
                        buffer.get(); // version
                        int messageLength = buffer.getInt(); // content length
                        if (messageLength < 0) {
                            messageLength &= Integer.MAX_VALUE;
                        }
                        byte[] message = new byte[messageLength];
                        buffer.get(message);
  
                        JSONObject post = new JSONObject();
                        post.put("transaction", Convert.toUnsignedLong(transaction_id));
                        post.put("confirmations", Nxt.getBlockchain().getHeight() - height);
                        post.put("height", height);
                        post.put("timestamp", timestamp);
                        post.put("comment_count", comment_count);
                        
                        int index = -1;
                        for (int j=6; j<message.length; j++) {
                            if (message[j] == ':') {
                                index = j+1;
                                break;
                            }
                        }
                        
                        if (index != -1) {
                            post.put("message", new String(message, index, message.length-index, "UTF-8"));
                        }
                        
                        response.add(post);
                    }
                }
            }
            return response;
            
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    } 
    
    public static List<? extends Transaction> getUnconfirmedComments(long post_transaction_id, int limit) {
        try (DbIterator<? extends Transaction> iterator = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {
            
            List<Transaction> filtered = new ArrayList<Transaction>();
            while (iterator.hasNext() && filtered.size() < limit) {
                
                Transaction transaction = iterator.next();
                if (transaction.getType().getType() == 1 && transaction.getType().getSubtype() == 0) {
                    
                    for (Appendix appendage : transaction.getAppendages()) {
                        
                        if (appendage instanceof Message && ((Message) appendage).isText()) {
                            
                            long identifier = MofoMessaging.parseComment(((Message) appendage).getMessage());
                            if (identifier == post_transaction_id) {
                                filtered.add(transaction);
                            }
                            break;
                        }
                    }  
                }
            }
            return filtered;
        }
    }
    
    public static DbIterator<? extends Transaction> getComments(long post_transaction_id, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction "
                    + "WHERE id IN ("
                    + "  SELECT transaction_id FROM mofo_comment "
                    + "  WHERE post_transaction_id = ? "
                    + "  ORDER BY timestamp ASC "
                    +    DbUtils.limitsClause(from, to)
                    + ")");
            
            int i = 0;
            pstmt.setLong(++i, post_transaction_id);
            DbUtils.setLimits(++i, pstmt, from, to);
            return Nxt.getBlockchain().getTransactions(con, pstmt);
            
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static int getCommentCount(long post_transaction_id) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) "
                    + "FROM mofo_comment "
                    + "WHERE post_transaction_id = ?");
            
            pstmt.setLong(1, post_transaction_id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }            
            return 0;
        } 
        catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static DbIterator<? extends Transaction> getTransactions(int timestamp, int limit, List<TransactionFilter> filters) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction "
                    + " WHERE (timestamp BETWEEN ? AND ?) "
                    + filterClause(filters)
                    + " ORDER BY timestamp DESC LIMIT ?");
            
            int i = 0;
            pstmt.setInt(++i, 0 /*Math.max(timestamp - ONE_MONTH_SECONDS, 0)*/);
            pstmt.setInt(++i, timestamp);
            pstmt.setInt(++i, limit);
            
            System.out.println(pstmt.toString());
            
            return Nxt.getBlockchain().getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static DbIterator<? extends Transaction> getTransactions(Account account, int timestamp, int limit, List<TransactionFilter> filters) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
            
            StringBuilder b = new StringBuilder();
            
            b.append("SELECT * FROM (");
            b.append("(SELECT * FROM transaction WHERE (timestamp BETWEEN ? AND ?) "
                    + "AND recipient_id = ? AND sender_id <> ? "
                    + filterClause(filters)
                    + " ORDER BY timestamp DESC LIMIT ?) ");
            b.append("UNION ALL ");
            b.append("(SELECT * FROM transaction WHERE (timestamp BETWEEN ? AND ?) "
                    + "AND sender_id = ? " 
                    + filterClause(filters)
                    + " ORDER BY timestamp DESC LIMIT ?)");
            b.append(")");
            b.append("ORDER BY timestamp DESC LIMIT ?");

            PreparedStatement pstmt = con.prepareStatement(b.toString());
            
            int i = 0;
            pstmt.setInt(++i, 0 /*Math.max(timestamp - ONE_MONTH_SECONDS, 0)*/);
            pstmt.setInt(++i, timestamp);
            pstmt.setLong(++i, account.getId());
            pstmt.setLong(++i, account.getId());
            pstmt.setInt(++i, limit);
            pstmt.setInt(++i, 0 /*Math.max(timestamp - ONE_MONTH_SECONDS, 0)*/);
            pstmt.setInt(++i, timestamp);
            pstmt.setLong(++i, account.getId());
            pstmt.setInt(++i, limit);
            pstmt.setInt(++i, limit);
            
            return Nxt.getBlockchain().getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static List<Transaction> getUnconfirmedTransactions(Account account, int timestamp, int limit, List<TransactionFilter> filters) {
        try (
            DbIterator<? extends Transaction> iterator = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()
        ) {
            List<Transaction> result = new ArrayList<Transaction>();
            List<TransactionFilter> inclusive = null;
            List<TransactionFilter> exclusive = null;
            if (filters != null && !filters.isEmpty()) {
                inclusive = new ArrayList<TransactionFilter>();
                exclusive = new ArrayList<TransactionFilter>();              
                
                /* Split the filters in inclusive and exclusive filters */
                for (int i=0; i<filters.size(); i++) {
                    if (filters.get(i) instanceof InclusiveTransactionFilter) {
                        inclusive.add(filters.get(i));
                    }
                    else {
                        exclusive.add(filters.get(i));
                    }
                }
            }
            
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                
                /* skip those that are younger than timestamp */
                if (transaction.getTimestamp() > timestamp) {
                    continue;
                }
                
                /* skip those not for our account */
                if (account != null) {
                    if (transaction.getSenderId() != account.getId() && transaction.getRecipientId() != account.getId()) {
                        continue;
                    }
                }
                
                /* skip those that don't match the filter */
                if (filters != null && !filters.isEmpty()) {
                  
                    /* Transaction must match one of these */
                    if (inclusive != null && ! inclusive.isEmpty()) {
                        boolean ignore = true;
                        for (TransactionFilter filter : inclusive) {
                            if (filter.match(transaction)) {
                                ignore = false;
                            }
                        }
                        
                        if (ignore) {
                            continue;
                        }
                    }
                    
                    /* Transaction must NOT match any of these */
                    if (exclusive != null && ! exclusive.isEmpty()) {
                        boolean ignore = false;
                        for (TransactionFilter filter : exclusive) {
                            if (filter.match(transaction)) {
                                ignore = true;
                                break;
                            }
                        }
                        
                        if (ignore) {
                            continue;
                        }                        
                    }
                }
                
                result.add(transaction);
                if (result.size() > limit) {
                    break;
                }                
            }
            
            return result;
        }
    }
    
    public static List<Transaction> getUnconfirmedTransactions(int timestamp, int limit, List<TransactionFilter> filters) {
        return getUnconfirmedTransactions(null, timestamp, limit, filters);
    }
    
    /* TODO - Optimize unconfirmed_transaction table, add type and subtype */
    public static List<Transaction> getUnconfirmedTransactions(byte type, byte subtype) {
        try (DbIterator<? extends Transaction> iterator = Nxt.getTransactionProcessor().getAllUnconfirmedTransactions())               
        {
            List<Transaction> result = new ArrayList<Transaction>();
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                if (transaction.getType().getType() == type && transaction.getType().getSubtype() == subtype) {
                    result.add(transaction);
                }
            }
            return result;
        }      
    }
    
    /* Average block time in the past 24 hours - very rude approach */
    public static int getAverageBlockTime24H() {        
        try (Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM block WHERE timestamp >= ?")) {
            pstmt.setInt(1, Nxt.getBlockchain().getLastBlock().getTimestamp() - SECONDS_IN_DAY);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                
                int totalBlocks24H = rs.getInt(1);
                if (totalBlocks24H > 0) {
                    return SECONDS_IN_DAY / totalBlocks24H;
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }      
    }
    
    public static int getTransactionCountSince(int timestamp) {
        try (Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT COUNT(*) FROM transaction WHERE timestamp >= ?")) {
            pstmt.setInt(1, timestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static RewardsStruct getBlockRewardsSince(int timestamp) {
        try (Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT " 
                + "SUM(total_fee) AS sum_a, "
                + "COUNT(*) AS count_a "
                + "FROM block WHERE timestamp >= ?")
        ) {
            pstmt.setInt(1, timestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return new RewardsStruct(timestamp, rs.getLong(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    public static RewardsStruct getBlockRewardsSince(long account_id, int timestamp) {
        try (Connection con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT " 
                + "SUM(total_fee) AS sum_a, "
                + "COUNT(*) AS count_a "
                + "FROM block WHERE generator_id = ? AND timestamp >= ?")
        ) {
            int i = 0;
            pstmt.setLong(++i, account_id);
            pstmt.setInt(++i, timestamp);
            try (ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return new RewardsStruct(timestamp, rs.getLong(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    static String inClause(String column, int count) {
        StringBuilder b = new StringBuilder();
        b.append(column);       
        b.append(" IN(");
        for (int i=0; i<count; i++) {
            if (i>0) {
                b.append(",");
            }
            b.append("?");
        }
        b.append(") ");
        return b.toString();
    }
    
    public static DbIterator<? extends Transaction> getRecentTransactions(List<Long> accounts, int timestamp, List<TransactionFilter> filters, int limit) {
        if (accounts == null || accounts.size() == 0) {
            throw new RuntimeException("Accounts cannot be empty");
        }
      
        int SIX_MONTHS_AGO = timestamp - ((31 * 24 * 60 * 60) * 6);
        Connection con = null;
        try {
            con = Db.db.getConnection();
            
            StringBuilder b = new StringBuilder();
            
            b.append("(SELECT * FROM transaction WHERE (timestamp BETWEEN ? AND ?) "
                   + "AND " + inClause("recipient_id", accounts.size())
                   + filterClause(filters)
                   + "ORDER BY timestamp DESC LIMIT ?) ");
            b.append("UNION ");
            b.append("(SELECT * FROM transaction WHERE (timestamp BETWEEN ? AND ?) "
                   + "AND " + inClause("sender_id", accounts.size()) 
                   + filterClause(filters)
                   + "ORDER BY timestamp DESC LIMIT ?)");
            
            PreparedStatement pstmt = con.prepareStatement(b.toString());
            
            int i = 0;
            pstmt.setInt(++i, SIX_MONTHS_AGO);
            pstmt.setInt(++i, timestamp);            
            for (int j=0; j<accounts.size(); j++) {
                pstmt.setLong(++i, accounts.get(j));
            }
            pstmt.setInt(++i, limit);    

            pstmt.setInt(++i, SIX_MONTHS_AGO);
            pstmt.setInt(++i, timestamp);            
            for (int j=0; j<accounts.size(); j++) {
                pstmt.setLong(++i, accounts.get(j));
            }
            pstmt.setInt(++i, limit);    
            
            System.out.println(pstmt.toString());
            
            return Nxt.getBlockchain().getTransactions(con, pstmt);
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static JSONArray getAssetOpenOrders(List<Long> accounts, long asset_id, int from, int to) {
        if (accounts == null || accounts.size() == 0) {
            throw new RuntimeException("Accounts cannot be empty");
        }

        StringBuilder b = new StringBuilder();
        b.append("SELECT * FROM ( ");
        b.append("(SELECT id, price, quantity, creation_height, account_id, 'sell' AS TYPE FROM ask_order WHERE LATEST = TRUE "
               + "AND asset_id = ? AND " + inClause("account_id", accounts.size()) + ") ");
        b.append("UNION ");
        b.append("(SELECT id, price, quantity, creation_height, account_id, 'buy' AS TYPE FROM bid_order WHERE LATEST = TRUE "
               + "AND asset_id = ? AND " + inClause("account_id", accounts.size()) + ") ");
        b.append(") ORDER BY creation_height DESC ");
        b.append(DbUtils.limitsClause(from, to));        
        
        try 
            (Connection con = Db.db.getConnection();              
             PreparedStatement pstmt = con.prepareStatement(b.toString());)
        {            
            int i = 0;
            pstmt.setLong(++i, asset_id);
            for (int j=0; j<accounts.size(); j++) {
                pstmt.setLong(++i, accounts.get(j));
            }
            pstmt.setLong(++i, asset_id);
            for (int j=0; j<accounts.size(); j++) {
                pstmt.setLong(++i, accounts.get(j));
            }
            DbUtils.setLimits(++i, pstmt, from, to);

            JSONArray response = new JSONArray();
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int height = rs.getInt("creation_height");
                  
                    JSONObject order = new JSONObject();
                    order.put("order", Convert.toUnsignedLong(rs.getLong("id")));
                    order.put("priceNQT", String.valueOf(rs.getLong("price")));
                    order.put("quantityQNT", String.valueOf(rs.getLong("quantity")));
                    order.put("height", height);
                    order.put("accountRS", Convert.rsAccount(rs.getLong("account_id")));
                    order.put("type", rs.getString("TYPE"));
                    order.put("confirmations", Nxt.getBlockchain().getHeight() - height);
                    
                    response.add(order);
                }               
            }
            return response;        
        } 
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
