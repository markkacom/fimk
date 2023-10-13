package nxt.http.websocket;

import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import nxt.util.Convert;
import nxt.virtualexchange.VirtualOrder;
import nxt.virtualexchange.VirtualTrade;

public class ExchangeTopicBuilder {

    /**
     * ExchangeTopicBuilder generates topics for use in subscribing to websocket events.
     * The topic string is a JSON array serialized to string.
     * The serialized array has the following structure.
     *
     * [
     *    # Topic type identifier. This first slot always contains the number 102
     *    102,
     *
     *    # The second slot contains an asset identifier, if provided only events
     *    # matching this asset will match and will be forwarded.
     *    # Pass an empty string to indicate there is no asset and must match all assets
     *    "1234567890123456789",
     *
     *    # The third slot contains an account number, if provided events will be filtered
     *    # only if either affected buyer or seller is this account.
     *    # Pass an empty string to indicate there is no account.
     *    "FIM-Z38B-MAXH-ZHXC-DWXYX"
     * ]
     */

    final long account;
    final long asset;
    final String topicValue;

    private MofoWebSocketAdapter adapter;

    public ExchangeTopicBuilder(MofoWebSocketAdapter adapter, String topicValue) {
        this.topicValue = topicValue;

        JSONArray topicData = (JSONArray) JSONValue.parse(topicValue);

        String assetValue = (String) topicData.get(1);
        asset = (assetValue != null && !assetValue.trim().isEmpty()) ? Long.parseUnsignedLong(assetValue) : 0;
        account = Convert.parseAccountId((String) topicData.get(2));

        this.adapter = adapter;
    }

    public MofoWebSocketAdapter getAdapter() {
        return adapter;
    }

    public String getTopic() {
        return topicValue;
    }

    public boolean matchesTrade(VirtualTrade trade) {
        if (asset != 0 && trade.getAssetId() != asset) {
            return false;
        }
        else if (account != 0 && (trade.getBuyerId() != account && trade.getSellerId() != account)) {
            return false;
        }
        return true;
    }

    public boolean matchesOrder(VirtualOrder order) {
        if (asset != 0 && order.getAssetId() != asset) {
            return false;
        }
        else if (account != 0 && order.getAccountId() != account) {
            return false;
        }
        return true;
    }
}
