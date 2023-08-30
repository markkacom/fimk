package nxt.txn.extension;

import nxt.*;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Specifying image for Asset or Marketplace Item.
 */
public class ItemImageExtension extends TransactionTypeExtension {

    public static final String MARK = "(FTR.3.0)";

    public enum Item {ASSET, GOODS}

    public enum Image {IMAGE_URL, IMAGE_DATA}

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

        // possible values "ASSET IMAGE_URL"  "ASSET IMAGE_DATA"  "GOODS IMAGE_URL"  "GOODS IMAGE_DATA"
        String s = a.getType();
        String[] resourceDescriptor;
        Item item = null;
        Image imageKind = null;
        String error = "Wrong type of resource";
        if (s != null && !s.trim().isEmpty()) {
            resourceDescriptor = s.split(" ");
            if (resourceDescriptor.length == 2) {
                try {
                    // check is the value is expected
                    item = Item.valueOf(resourceDescriptor[0]);
                    imageKind = Image.valueOf(resourceDescriptor[1]);
                    error = null;
                } catch (IllegalArgumentException e) {
                    // error string is set above
                }
            }
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
            if (asset == null) return "Asset id is wrong";
            if (sender.getId() != asset.getAccountId()) return "Only asset issuer is allowed to set image for asset";
        } else if (Item.GOODS == item) {
            DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(itemId);
            if (goods == null) return "Goods id is wrong";
            if (sender.getId() != goods.getSellerId()) return "Only goods seller is allowed to set image for goods";
        }

        String dataStr = new String(a.getData());
        boolean disableImage = dataStr.trim().isEmpty();

        if (!disableImage) {
            if (imageKind == Image.IMAGE_URL) {
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

}
