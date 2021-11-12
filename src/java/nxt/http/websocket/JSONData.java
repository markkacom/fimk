package nxt.http.websocket;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nxt.Account;
import nxt.Appendix;
import nxt.Asset;
import nxt.Account.AccountIdentifier;
import nxt.Account.AccountInfo;
import nxt.Attachment.MonetarySystemAttachment;
import nxt.Attachment.ColoredCoinsAssetTransfer;
import nxt.Attachment.ColoredCoinsAskOrderPlacement;
import nxt.Attachment.ColoredCoinsBidOrderPlacement;
import nxt.Attachment.ColoredCoinsAskOrderCancellation;
import nxt.Attachment.ColoredCoinsBidOrderCancellation;
import nxt.AccountColor;
import nxt.Block;
import nxt.Currency;
import nxt.DigitalGoodsStore;
import nxt.Nxt;
import nxt.Order;
import nxt.RewardsImpl;
import nxt.Trade;
import nxt.Transaction;
import nxt.peer.Peer;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class JSONData {

    @SuppressWarnings("unchecked")
    public static JSONObject accountBalance(Account account) {
        JSONObject json = new JSONObject();
        if (account == null) {
            json.put("balanceNQT", "0");
            json.put("unconfirmedBalanceNQT", "0");
            json.put("effectiveBalanceNXT", "0");
            json.put("forgedBalanceNQT", "0");
            json.put("guaranteedBalanceNQT", "0");
        } else {
            json.put("balanceNQT", String.valueOf(account.getBalanceNQT()));
            json.put("unconfirmedBalanceNQT", String.valueOf(account.getUnconfirmedBalanceNQT()));
            json.put("effectiveBalanceNXT", account.getEffectiveBalanceNXT());
            json.put("forgedBalanceNQT", String.valueOf(account.getForgedBalanceNQT()));
            json.put("guaranteedBalanceNQT", String.valueOf(account.getGuaranteedBalanceNQT()));
        }
        return json;
    }

    @SuppressWarnings("unchecked")
    public static void putAccount(JSONObject json, String name, long accountId) {
        Account account = Account.getAccount(accountId);
        if (account != null) {
            AccountInfo info = account.getAccountInfo();
            if (info != null) {
                json.put(name + "Name", info.getName());
            }
            if (account.getAccountColorId() != 0) {
                AccountColor accountColor = AccountColor.getAccountColor(account.getAccountColorId());
                if (accountColor != null) {
                    json.put(name + "ColorId", Long.toUnsignedString(accountColor.getId()));
                    json.put(name + "ColorName", accountColor.getName());
                }
            }
            AccountIdentifier identifier = Account.getEmailAccountIdentifier(accountId);
            if (identifier != null) {
                json.put(name + "Email", identifier.getEmail());
            }
            else {
                json.put(name + "Email", Convert.rsAccount(accountId));
            }
        }
        json.put(name, Long.toUnsignedString(accountId));
        json.put(name + "RS", Convert.rsAccount(accountId));
    }

    @SuppressWarnings("unchecked")
    public static JSONArray transactions(List<? extends Transaction> transactions, boolean unconfirmed) {
        JSONArray result = new JSONArray();
        for (Transaction t : transactions) {
            result.add(transaction(t, unconfirmed));
        }
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static JSONObject transaction(Transaction transaction, boolean unconfirmed) {
        JSONObject json = new JSONObject();
        json.put("transaction", transaction.getStringId());
        json.put("type", transaction.getType().getType());
        json.put("subtype", transaction.getType().getSubtype());
        json.put("timestamp", transaction.getTimestamp());
        json.put("amountNQT", transaction.getAmountNQT());
        json.put("feeNQT", transaction.getFeeNQT());
        json.put("deadline", transaction.getDeadline());

        if (unconfirmed) {
          json.put("confirmations", -1);
        }
        else {
          json.put("confirmations", Nxt.getBlockchain().getHeight() - transaction.getHeight());
        }
        json.put("height", transaction.getHeight());
        JSONData.putAccount(json, "sender", transaction.getSenderId());
        if (transaction.getRecipientId() != 0) {
            JSONData.putAccount(json, "recipient", transaction.getRecipientId());
        }
        boolean publicKeysIncluded = false;
        JSONObject attachmentJSON = new JSONObject();
        for (Appendix appendage : transaction.getAppendages()) {
            attachmentJSON.putAll(appendage.getJSONObject());
            if (publicKeysIncluded == false && appendage instanceof Appendix.EncryptedMessage) {
                publicKeysIncluded = true;
                Account sender = Account.getAccount(transaction.getSenderId());
                if (sender != null && sender.getPublicKey() != null) {
                    attachmentJSON.put("senderPublicKey", Convert.toHexString(sender.getPublicKey()));
                }
                Account recipient = Account.getAccount(transaction.getRecipientId());
                if (recipient != null && recipient.getPublicKey() != null) {
                    attachmentJSON.put("recipientPublicKey", Convert.toHexString(recipient.getPublicKey()));
                }
            }
            if (transaction.getType().getType() == 2) {
                long assetId = 0L;
                if (transaction.getType().getSubtype() == 0) { // SUBTYPE_COLORED_COINS_ASSET_ISSUANCE
                    assetId = transaction.getId();
                }
                else if (transaction.getType().getSubtype() == 1 && appendage instanceof ColoredCoinsAssetTransfer) { // SUBTYPE_COLORED_COINS_ASSET_TRANSFER
                    assetId = ((ColoredCoinsAssetTransfer) appendage).getAssetId();
                }
                else if (transaction.getType().getSubtype() == 2 && appendage instanceof ColoredCoinsAskOrderPlacement) { // SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT
                    assetId = ((ColoredCoinsAskOrderPlacement) appendage).getAssetId();
                }
                else if (transaction.getType().getSubtype() == 3 && appendage instanceof ColoredCoinsBidOrderPlacement) { // SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT
                    assetId = ((ColoredCoinsBidOrderPlacement) appendage).getAssetId();
                }
                else {
                    if (transaction.getType().getSubtype() == 4 && appendage instanceof ColoredCoinsAskOrderCancellation) { // SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION
                        final long orderId = ((ColoredCoinsAskOrderCancellation) appendage).getOrderId();
                        Order order = Order.Ask.getAskOrder(orderId);
                        if (order == null) {
                            continue;
                        }
                        assetId = order.getAssetId();
                    }
                    else if (transaction.getType().getSubtype() == 5 && appendage instanceof ColoredCoinsBidOrderCancellation) { // SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION
                        final long orderId = ((ColoredCoinsBidOrderCancellation) appendage).getOrderId();
                        Order order = Order.Bid.getBidOrder(orderId);
                        if (order == null) {
                            continue;
                        }
                        assetId = order.getAssetId();
                    }
                    else {
                        continue;
                    }
                }
                if (assetId != 0L) {
                    JSONData.putAssetInfo(attachmentJSON, assetId);
                }
            }
            else if (transaction.getType().getType() == 5 && appendage instanceof MonetarySystemAttachment) {
                final long currencyId = ((MonetarySystemAttachment) appendage).getCurrencyId();
                JSONData.putCurrencyInfo(attachmentJSON, currencyId);
            }
        }
        if (! attachmentJSON.isEmpty()) {
            for (Map.Entry entry : (Iterable<Map.Entry>) attachmentJSON.entrySet()) {
                if (entry.getValue() instanceof Long) {
                    entry.setValue(String.valueOf(entry.getValue()));
                }
            }
            json.put("attachment", attachmentJSON);
        }
        return json;
    }

    @SuppressWarnings("unchecked")
    private static void putCurrencyInfo(JSONObject json, long currencyId) {
        Currency currency = Currency.getCurrency(currencyId);
        if (currency == null) {
            return;
        }
        json.put("name", currency.getName());
        json.put("currentSupply", currency.getCurrentSupply());
        json.put("code", currency.getCode());
        json.put("type", currency.getType());
        json.put("decimals", currency.getDecimals());
        json.put("issuanceHeight", currency.getIssuanceHeight());
        putAccount(json, "issuerAccount", currency.getAccountId());
    }

    @SuppressWarnings("unchecked")
    public static void putAssetInfo(JSONObject json, long assetId) {
        Asset asset = Asset.getAsset(assetId);
        if (asset != null) {
            json.put("name", asset.getName());
            json.put("decimals", asset.getDecimals());
            json.put("type", asset.getType());
        }
    }

    public static int generateTradeHash(Trade trade) {
        return Objects.hash(Long.valueOf(trade.getAssetId()),
                            Long.valueOf(trade.getAskOrderId()),
                            Long.valueOf(trade.getBidOrderId()),
                            Long.valueOf(trade.getSellerId()),
                            Long.valueOf(trade.getBuyerId()),
                            Long.valueOf(trade.getBlockId()),
                            Long.valueOf(trade.getPriceNQT()),
                            Long.valueOf(trade.getQuantityQNT()) );
    }

    @SuppressWarnings("unchecked")
    public static JSONObject trade(Trade trade, boolean includeAssetInfo) {
        JSONObject json = new JSONObject();

        json.put("id", generateTradeHash(trade));
        json.put("timestamp", trade.getTimestamp());
        json.put("quantityQNT", String.valueOf(trade.getQuantityQNT()));
        json.put("priceNQT", String.valueOf(trade.getPriceNQT()));
        json.put("asset", Long.toUnsignedString(trade.getAssetId()));
        json.put("askOrder", Long.toUnsignedString(trade.getAskOrderId()));
        json.put("bidOrder", Long.toUnsignedString(trade.getBidOrderId()));
        json.put("askOrderHeight", trade.getAskOrderHeight());
        json.put("bidOrderHeight", trade.getBidOrderHeight());
        putAccount(json, "seller", trade.getSellerId());
        putAccount(json, "buyer", trade.getBuyerId());
        json.put("block", Long.toUnsignedString(trade.getBlockId()));
        json.put("height", trade.getHeight());
        json.put("tradeType", trade.isBuy() ? "buy" : "sell");
        json.put("confirmations", Nxt.getBlockchain().getHeight() - trade.getHeight());
        if (includeAssetInfo) {
            putAssetInfo(json, trade.getAssetId());
        }
        return json;
    }

    @SuppressWarnings("unchecked")
    public static JSONArray trades(List<? extends Trade> trades) {
        JSONArray result = new JSONArray();
        for (Trade t : trades) {
            result.add(trade(t, true));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject minimalBlock(Block block) {
        JSONObject json = new JSONObject();
        json.put("block", block.getStringId());
        json.put("height", block.getHeight());
        json.put("generator", Convert.rsAccount(block.getGeneratorId()));
        json.put("timestamp", block.getTimestamp());
        json.put("numberOfTransactions", block.getTransactions().size());
        json.put("totalAmountNQT", String.valueOf(block.getTotalAmountNQT()));
        json.put("totalFeeNQT", String.valueOf(block.getTotalFeeNQT()));
        json.put("totalPOSRewardNQT", RewardsImpl.calculatePOSRewardNQT(block));
        return json;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject accountAsset(Account.AccountAsset accountAsset, boolean includeAccount, boolean includeAssetInfo) {
        JSONObject json = new JSONObject();
        if (includeAccount) {
            putAccount(json, "account", accountAsset.getAccountId());
        }
        json.put("asset", Long.toUnsignedString(accountAsset.getAssetId()));
        json.put("quantityQNT", String.valueOf(accountAsset.getQuantityQNT()));
        json.put("unconfirmedQuantityQNT", String.valueOf(accountAsset.getUnconfirmedQuantityQNT()));
        if (includeAssetInfo) {
            Asset asset = Asset.getAsset(accountAsset.getAssetId());
            json.put("name", asset.getName());
            json.put("decimals", asset.getDecimals());
            json.put("totalQuantityQNT", asset.getQuantityQNT());
            putAccount(json, "issuer", asset.getAccountId());
            json.put("type", asset.getType());
            json.put("expiry", asset.getExpiry());
        }
        return json;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject accountCurrency(Account.AccountCurrency accountCurrency, boolean includeCurrencyInfo) {
        JSONObject json = new JSONObject();
        json.put("currency", Long.toUnsignedString(accountCurrency.getCurrencyId()));
        json.put("units", String.valueOf(accountCurrency.getUnits()));
        json.put("unconfirmedUnits", String.valueOf(accountCurrency.getUnconfirmedUnits()));
        if (includeCurrencyInfo) {
            putCurrencyInfo(json, accountCurrency.getCurrencyId());
        }
        return json;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject order(Order order) {
        JSONObject json = new JSONObject();
        json.put("order", Long.toUnsignedString(order.getId()));
        json.put("quantityQNT", String.valueOf(order.getQuantityQNT()));
        json.put("priceNQT", String.valueOf(order.getPriceNQT()));
        json.put("height", order.getHeight());
        json.put("confirmations", Nxt.getBlockchain().getHeight() - order.getHeight());
        json.put("accountRS", Convert.rsAccount(order.getAccountId()));
        json.put("type", (order instanceof Order.Ask) ? "sell" : "buy");
        return json;
    }

    @SuppressWarnings("unchecked")
    static JSONObject peer(Peer peer) {
        JSONObject json = new JSONObject();
        json.put("address", peer.getHost());
        json.put("state", peer.getState().ordinal());
        json.put("announcedAddress", peer.getAnnouncedAddress());
        json.put("shareAddress", peer.shareAddress());
        if (peer.getHallmark() != null) {
            json.put("hallmark", peer.getHallmark().getHallmarkString());
        }
        json.put("weight", peer.getWeight());
        json.put("downloadedVolume", peer.getDownloadedVolume());
        json.put("uploadedVolume", peer.getUploadedVolume());
        json.put("application", peer.getApplication());
        json.put("version", peer.getVersion());
        json.put("platform", peer.getPlatform());
        json.put("blacklisted", peer.isBlacklisted());
        json.put("lastUpdated", peer.getLastUpdated());
        if (peer.isBlacklisted()) {
            json.put("blacklistingCause", peer.getBlacklistingCause());
        }
        return json;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject goods(DigitalGoodsStore.Goods goods, boolean includeCounts) {
        JSONObject json = new JSONObject();
        json.put("goods", Long.toUnsignedString(goods.getId()));
        json.put("name", goods.getName());
        json.put("description", goods.getDescription());
        json.put("quantity", goods.getQuantity());
        json.put("priceNQT", String.valueOf(goods.getPriceNQT()));
        putAccount(json, "seller", goods.getSellerId());
        json.put("tags", goods.getTags());
        JSONArray tagsJSON = new JSONArray();
        Collections.addAll(tagsJSON, goods.getParsedTags());
        json.put("parsedTags", tagsJSON);
        json.put("delisted", goods.isDelisted());
        json.put("timestamp", goods.getTimestamp());
        if (includeCounts) {
            json.put("numberOfPurchases", DigitalGoodsStore.Purchase.getGoodsPurchaseCount(goods.getId(), false, true));
            json.put("numberOfPublicFeedbacks", DigitalGoodsStore.Purchase.getGoodsPurchaseCount(goods.getId(), true, true));
        }
        return json;
    }
}
