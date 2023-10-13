package nxt;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import nxt.Appendix.Message;
import nxt.util.Convert;
import nxt.util.Listener;

/** 
 * MofoMessaging is a layer on top of Arbitrary Messages, if arbitrary messages
 * are prepended with a specific header they will count as MofoMessaging 
 * headers.
 * 
 * There are two types of MofoMessaging messages:
 * 
 *  1. posts
 *  2. comments
 *  
 * Posts defined.
 * 
 * A post is a message that has the same sender and recipient (message send to 
 * self) and which starts with the text 'post' followed by a single byte folowed 
 * by a ':' followed by an optional second identifier or followed by the post 
 * contents.
 * 
 * A so called ACCOUNT_POST has the following signature.
 * 
 * ['post']['1'][':'][.+]
 * 
 * While an ASSET_POST or CURRENCY_POST has an extra identifier for the asset or 
 * currency.
 * 
 * ['post']['2'-'127'][':'][asset or currency id][':'][.+] 
 * 
 * Comments defined.
 * 
 * A comment is a public message send to a post author and in direct reply to a 
 * post.
 * On disk a comment looks like 'comm' followed by a post id (transaction id)
 * followed by a ':' followed by the comment contents.
 * 
 * ['comm'][transaction id][':'][.+]
 */

public class MofoMessaging {
  
    public static final byte TYPE_ACCOUNT_POST        = '1';
    public static final byte TYPE_ASSET_POST          = '2';
    public static final byte TYPE_CURRENCY_POST       = '3';
  
    private static final byte   COLON                 = ':';    
    private static final byte[] POST                  = {'p','o','s','t'};
    private static final byte[] COMMENT               = {'c','o','m','m'};
    
    private static final int MIN_POST_LENGTH          = 5;
    private static final int MAX_UNSIGNEDLONG_LENGTH  = 40;  /* TODO - determine accurate max transaction id length ( + start offset ) */
    private static final int POST_IDENTIFIER_OFFSET   = 6;
    
    static void init() {
        Nxt.getTransactionProcessor().addListener(new Listener<List<? extends Transaction>>() {

            @Override
            public void notify(List<? extends Transaction> _transactions) {
                
                for (Transaction transaction : _transactions) {
                    
                    if (transaction.getType().getType() == 1 && transaction.getType().getSubtype() == 0) {                      
                        
                        for (Appendix appendage : transaction.getAppendages()) {
                        
                            if (appendage instanceof Message && ((Message) appendage).isText()) {
                                
                                if (transaction.getSenderId() == transaction.getRecipientId()) {
                                  
                                    byte type = parsePost(((Message) appendage).getMessage());
                                    if (type != 0) {
                                      
                                        if (type == TYPE_ACCOUNT_POST) {
                                          
                                            persistPost(transaction, TYPE_ACCOUNT_POST, 0);
                                        }
                                        else if (type == TYPE_ASSET_POST || type == TYPE_CURRENCY_POST) {
                                          
                                            long identifier = parsePostIdentifier(((Message) appendage).getMessage());
                                            if (identifier != 0) {
                                                persistPost(transaction, type, identifier);
                                            }
                                        }
                                        break;
                                    }
                                }
                                
                                long transaction_id = parseComment(((Message) appendage).getMessage());
                                if (transaction_id != 0) {

                                    persistComment(transaction, transaction_id);
                                }
                            }
                        }
                    }
                }
            }
        }, TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
    }

    static void persistPost(Transaction transaction, byte type, long referenced_entity_id) {
        savePost(type, transaction.getTimestamp(), transaction.getSenderId(), referenced_entity_id, transaction.getId());
    }
    
    static void persistComment(Transaction transaction, long post_transaction_id) {
        saveComment(transaction.getTimestamp(), transaction.getSenderId(), post_transaction_id, transaction.getId());
    }    
    
    static void savePost(byte type, int timestamp, long sender_account_id, long referenced_entity_id, long transaction_id) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                 "MERGE INTO mofo_post (type, timestamp, sender_account_id, referenced_entity_id, transaction_id) "
               + "KEY (transaction_id) "
               + "VALUES (?, ?, ?, ?, ?)")) 
        {
            int i = 0;
            pstmt.setByte(++i, type);
            pstmt.setInt(++i, timestamp);
            pstmt.setLong(++i, sender_account_id);
            pstmt.setLong(++i, referenced_entity_id);
            pstmt.setLong(++i, transaction_id);
  
            pstmt.executeUpdate();
        } 
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    
    static void saveComment(int timestamp, long sender_account_id, long post_transaction_id, long transaction_id) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                 "MERGE INTO mofo_comment (timestamp, sender_account_id, post_transaction_id, transaction_id) "
               + "KEY (transaction_id) "
               + "VALUES (?, ?, ?, ?)")) 
        {
            int i = 0;
            pstmt.setInt(++i, timestamp);
            pstmt.setLong(++i, sender_account_id);
            pstmt.setLong(++i, post_transaction_id);
            pstmt.setLong(++i, transaction_id);
  
            pstmt.executeUpdate();
        } 
        catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * Parse a post
     * - returns the post type or 0 if not a post 
     * */
    public static byte parsePost(byte[] message) {
        if (message.length > MIN_POST_LENGTH && startsWith(message, POST)) {
            byte type = message[POST.length];
            if (message[MIN_POST_LENGTH] == COLON) {
                return type;
            }
        }
        return 0;
    }

    /**
     * Parse a comment
     * - returns the referenced transaction id or 0 if not a comment 
     * */
    public static long parseComment(byte[] message) {
        if (startsWith(message, COMMENT)) {
            int colon_index = indexOf(message, COLON, COMMENT.length, MAX_UNSIGNEDLONG_LENGTH);
            if (colon_index != -1) {
                try {
                    String id = new String(message, COMMENT.length, colon_index - COMMENT.length, "UTF-8");
                    return Convert.parseUnsignedLong(id);
                }
                catch (UnsupportedEncodingException | IllegalArgumentException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
    
    /**
     * Parse an optional post identifier
     * - returns the referenced identifier or 0 if not found 
     * */
    public static long parsePostIdentifier(byte[] message) {
        int colon_index = indexOf(message, COLON, POST_IDENTIFIER_OFFSET, MAX_UNSIGNEDLONG_LENGTH);
        if (colon_index != -1) {
            try {
                String id = new String(message, POST_IDENTIFIER_OFFSET, colon_index - POST_IDENTIFIER_OFFSET, "UTF-8");
                return Convert.parseUnsignedLong(id);
            }
            catch (UnsupportedEncodingException | IllegalArgumentException e) {
                return 0;
            }
        }
        return 0;
    }

    static boolean startsWith(byte[] source, byte[] match) {
        if (match.length > source.length) {
            return false;
        }
        for (int i = 0; i < match.length; i++) {
            if (source[i] != match[i]) {
                return false;
            }
        }
        return true;
    }

    static int indexOf(byte[] haystack, byte needle, int offset, int max_length) {
        int index = offset;
        while (index < max_length && index < haystack.length) {
            if (haystack[index] == needle) {
                return index;
            }
            index++;
        }
        return -1;
    }
}