package nxt;

import java.nio.ByteBuffer;

import nxt.Attachment.AbstractAttachment;
import nxt.util.Convert;

import org.json.simple.JSONObject;


public class MofoAttachment {

    public final static class NamespacedAliasAssignmentAttachment extends AbstractAttachment {
  
        private final String aliasName;
        private final String aliasURI;
    
        NamespacedAliasAssignmentAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            aliasName = Convert.readString(buffer, buffer.get(), Constants.MAX_ALIAS_LENGTH).trim().intern();
            aliasURI = Convert.readString(buffer, buffer.getShort(), Constants.MAX_ALIAS_URI_LENGTH).trim().intern();
        }
    
        NamespacedAliasAssignmentAttachment(JSONObject attachmentData) {
            super(attachmentData);
            aliasName = (Convert.nullToEmpty((String) attachmentData.get("alias"))).trim().intern();
            aliasURI = (Convert.nullToEmpty((String) attachmentData.get("uri"))).trim().intern();
        }
    
        public NamespacedAliasAssignmentAttachment(String aliasName, String aliasURI) {
            super();
            this.aliasName = aliasName.trim().intern();
            this.aliasURI = aliasURI.trim().intern();
        }
    
        @Override
        String getAppendixName() {
            return "NamespacedAliasAssignment";
        }
    
        @Override
        int getMySize() {
            return 1 + Convert.toBytes(aliasName).length + 2 + Convert.toBytes(aliasURI).length;
        }
    
        @Override
        void putMyBytes(ByteBuffer buffer) {
            byte[] alias = Convert.toBytes(this.aliasName);
            byte[] uri = Convert.toBytes(this.aliasURI);
            buffer.put((byte)alias.length);
            buffer.put(alias);
            buffer.putShort((short) uri.length);
            buffer.put(uri);
        }
    
        @Override
        void putMyJSON(JSONObject attachment) {
            attachment.put("alias", aliasName);
            attachment.put("uri", aliasURI);
        }
    
        @Override
        public TransactionType getTransactionType() {
            return MofoTransactions.NamespacedAliasAssignmentTransaction.NAMESPACED_ALIAS_ASSIGNMENT;
        }
    
        public String getAliasName() {
            return aliasName;
        }
    
        public String getAliasURI() {
            return aliasURI;
        }
    }
  
    public final static class PrivateAssetAddAccountAttachment extends AbstractAttachment {
        
        private final long assetId;
    
        PrivateAssetAddAccountAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
            super(buffer, transactionVersion);
            assetId = buffer.getLong();
        }
    
        PrivateAssetAddAccountAttachment(JSONObject attachmentData) {
            super(attachmentData);
            this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
        }
    
        public PrivateAssetAddAccountAttachment(long assetId) {
            super();
            this.assetId = assetId;
        }
    
        @Override
        String getAppendixName() {
            return "PrivateAssetAddAccount";
        }
    
        @Override
        int getMySize() {
            return 8;
        }
    
        @Override
        void putMyBytes(ByteBuffer buffer) {
            buffer.putLong(assetId);
        }
    
        @Override
        void putMyJSON(JSONObject attachment) {
            Asset.putAsset(attachment, assetId);
        }
    
        @Override
        public TransactionType getTransactionType() {
            return MofoTransactions.PrivateAssetAddAccountTransaction.PRIVATE_ASSET_ADD_ACCOUNT;
        }
        
        public long getAssetId() {
            return assetId;
        }        
    }
}
