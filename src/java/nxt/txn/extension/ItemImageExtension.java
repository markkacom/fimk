package nxt.txn.extension;

import nxt.*;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specifying image for Asset or Marketplace Item.
 */
public class ItemImageExtension extends TransactionTypeExtension {

    public static final String MARK = "(FTR.3.0)";

    public enum Item {ASSET, GOODS}

    @Override
    protected String getMark() {
        return MARK;
    }

    @Override
    public String getName() {
        return "Image for asset";
    }

    public String process(boolean validateOnly, Transaction transaction, Account sender, Account recipient) {
        Attachment att = transaction.getAttachment();
        Attachment.TaggedDataAttachment a;
        if (att instanceof Attachment.TaggedDataAttachment) {
            a = (Attachment.TaggedDataAttachment) att;
            if (!MARK.equals(a.getChannel())) return "Wrong mark";
        } else {
            return "Attachment type is not suitable";
        }

        /* expected result:
         1) for what the image (asset or goods);
         2) image data or url to image;
         3) sender id;
        */

        // possible values "ASSET"  "GOODS"
        Item item = null;
        String error = "Wrong type of resource";
        try {
            // check is the value is expected
            item = Item.valueOf(a.getType());
            error = null;
        } catch (IllegalArgumentException e) {
            // error string is set above
        }
        if (error != null) {
            return error;
        }

        // id of asset or goods, for example "230587438543221"
        long itemId;
        try {
            itemId = Long.parseUnsignedLong(a.getName());
        } catch (NumberFormatException e) {
            return item + " id is wrong";
        }
        if (Item.ASSET == item) {
            Asset asset = Asset.getAsset(itemId);
            if (asset == null) return "Asset is wrong";
            if (sender.getId() != asset.getAccountId()) return "Only asset issuer is allowed to set image for asset";
        } else if (Item.GOODS == item) {
            DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(itemId);
            if (goods == null) return "Goods is wrong";
            if (sender.getId() != goods.getSellerId()) return "Only goods seller is allowed to set image for goods";
        }

        String dataStr = new String(a.getData());
        boolean disableImage = dataStr.trim().isEmpty();

        if (!disableImage) {
            if (dataStr.startsWith("data:")) {
                // sample "data:image/gif;base64,R0lGODlhEgAZAJEAAODg4AAAAP///////yH5BAEAAAMALAAAAAASABkAAAJLnI+py+0YoowtgItvWEH4/wFb0oGgqJSmh5JsJ77pK8T1CNF2m9/+PfPtcIdSbBg8GomG5Y9X1C2TsKlrJbtioVEsEzKRPMbkcqMAADs="
                final Matcher m = DATA_URL_PATTERN.matcher(dataStr);
                if (! m.find()) return "Wrong image data";
            } else {
                try {
                    new URL(dataStr).toURI();
                } catch (MalformedURLException e) {
                    return "Malformed URL";
                } catch (URISyntaxException e) {
                    return "Wrong URL";
                }
            }
        }

        if (validateOnly) return null;

        //apply

        TaggedData.updateItemImageExtensionStatus(transaction.getId(), disableImage ? (byte) 0 : (byte) 1, itemId);

        return null;
    }

    private final Pattern DATA_URL_PATTERN = Pattern.compile("^data:image/(.+?);base64,\\s*", Pattern.CASE_INSENSITIVE);

}
