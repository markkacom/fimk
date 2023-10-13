package nxt.http.websocket;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import nxt.Transaction;
import nxt.TransactionType;
import nxt.util.Convert;

/**
 * TransactionTopicBuilder generates topic for use in subscribing to websocket events.
 * The topic string is a JSON array serialized to string.
 * The serialized array has the following structure.
 *
 * [
 *    # Topic type identifier. This first slot always contains the number 101
 *    101,
 *
 *    # The second slot contains an array of arrays, each sub array has the
 *    # transaction type as its first element and the subtype as the second element.
 *    # If no filtering should be performed on matched transaction types leave the
 *    # root array empty.
 *    [[1,1],[2,3],[3,4]],
 *
 *    # The third slot contains the account filter. An account filter will only
 *    # notify about transactions where EITHER sender AND/OR recipient matches
 *    # the provided account. The account can be in RS or numeric notation.
 *    # Pass an empty string to indicate there is no account.
 *    " FIM-Z38B-MAXH-ZHXC-DWXYX",
 *
 *    # The fourth slot contains the recipient filter. A recipient filter will
 *    # only notify about transactions where recipient EQUALS the recipient
 *    # you provided.
 *    # Pass an empty string to indicate there is no recipient.
 *    "",
 *
 *    # The fifth slot contains the sender filter. A sender filter will
 *    # only notify about transactions where sender EQUALS the sender
 *    # you provided.
 *    # Pass an empty string to indicate there is no sender.
 *    ""
 * ]
 */
public class TransactionTopicBuilder {

    static class TransactionTopicType {
        final public byte type;
        final public byte subtype;

        TransactionTopicType(JSONArray typeData) {
            if (typeData == null || typeData.size() != 2) {
                throw new RuntimeException("Invalid transaction type data");
            }
            if (!(typeData.get(0) instanceof Long)) {
                throw new RuntimeException("Invalid type value");
            }
            if (!(typeData.get(1) instanceof Long)) {
                throw new RuntimeException("Invalid subtype value");
            }

            type = ((Long)typeData.get(0)).byteValue();
            subtype = ((Long)typeData.get(0)).byteValue();
        }

        public boolean match(TransactionType transactionType) {
            return transactionType.getType() == type && transactionType.getSubtype() == subtype;
        }
    }

    final List<TransactionTopicType> types;
    final long account;
    final long recipient;
    final long sender;
    final String topicValue;

    private MofoWebSocketAdapter adapter;

    public TransactionTopicBuilder(MofoWebSocketAdapter adapter, String topicValue) {
        this.topicValue = topicValue;

        JSONArray topicData = (JSONArray) JSONValue.parse(topicValue);
        if (topicData.get(1) instanceof JSONArray && ((JSONArray) topicData.get(1)).size() > 0) {
            types = new ArrayList<TransactionTopicType>();
            for (Object typeData : (JSONArray) topicData.get(1)) {
                types.add(new TransactionTopicType((JSONArray) typeData));
            }
        }
        else {
            types = null;
        }

        account = Convert.parseAccountId((String) topicData.get(2));
        recipient = Convert.parseAccountId((String) topicData.get(3));
        sender = Convert.parseAccountId((String) topicData.get(4));

        this.adapter = adapter;
    }

    public MofoWebSocketAdapter getAdapter() {
        return adapter;
    }

    public String getTopic() {
        return topicValue;
    }

    public boolean matchesTransaction(Transaction transaction) {
        if (types != null) {
            boolean typesMatch = false;
            for (TransactionTopicType type : types) {
                if (type.match(transaction.getType())) {
                    typesMatch = true;
                    break;
                }
            }
            if (!typesMatch) {
                return false;
            }
        }

        /* If account is provided match if either sender OR recipient matches */
        if (account != 0) {
            if (transaction.getSenderId() == account || transaction.getRecipientId() == account) {
                return true;
            }
            return false;
        }

        /* If sender AND recipient is provided match if BOTH matches */
        if (sender != 0 && recipient != 0) {
            if (transaction.getSenderId() == sender && transaction.getRecipientId() == recipient) {
                return true;
            }
            return false;
        }

        /* If sender is provided match if IT matches */
        if (sender != 0) {
            if (transaction.getSenderId() == sender) {
                return true;
            }
            return false;
        }

        /* If recipient is provided match if IT matches */
        if (recipient != 0) {
            if (transaction.getRecipientId() == sender) {
                return true;
            }
            return false;
        }
        return true;
    }
}