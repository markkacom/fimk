package nxt;

import java.nio.ByteBuffer;
import java.util.Map;

import org.json.simple.JSONObject;

public class MofoTransactions {

    public static final byte TYPE_FIMKRYPTO = 40;
    
    private static final byte SUBTYPE_FIMKRYPTO_NAMESPACED_ALIAS_ASSIGNMENT = 0;
    private static final byte SUBTYPE_FIMKRYPTO_PRIVATE_ASSET_ADD_ACCOUNT = 1;
    private static final byte SUBTYPE_FIMKRYPTO_PRIVATE_ASSET_REMOVE_ACCOUNT = 2;
    private static final byte SUBTYPE_FIMKRYPTO_PRIVATE_ASSET_SET_FEE = 3;
    
    public static abstract class NamespacedAliasAssignmentTransaction extends TransactionType {

        private NamespacedAliasAssignmentTransaction() {
        }
  
        @Override
        public final byte getType() {
            return TYPE_FIMKRYPTO;
        }
  
        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }
  
        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }
  
        public static final TransactionType NAMESPACED_ALIAS_ASSIGNMENT = new NamespacedAliasAssignmentTransaction() {
  
            @Override
            public final byte getSubtype() {
                return SUBTYPE_FIMKRYPTO_NAMESPACED_ALIAS_ASSIGNMENT;
            }
  
            @Override
            MofoAttachment.NamespacedAliasAssignmentAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new MofoAttachment.NamespacedAliasAssignmentAttachment(buffer, transactionVersion);
            }
  
            @Override
            MofoAttachment.NamespacedAliasAssignmentAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new MofoAttachment.NamespacedAliasAssignmentAttachment(attachmentData);
            }
  
            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                MofoAttachment.NamespacedAliasAssignmentAttachment attachment = (MofoAttachment.NamespacedAliasAssignmentAttachment) transaction.getAttachment();
                NamespacedAlias.addOrUpdateAlias(transaction, attachment);
            }
  
            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String,Boolean>> duplicates) {
                MofoAttachment.NamespacedAliasAssignmentAttachment attachment = (MofoAttachment.NamespacedAliasAssignmentAttachment) transaction.getAttachment();
                StringBuilder key = new StringBuilder();
                key.append(transaction.getSenderId());
                key.append(attachment.getAliasName().toLowerCase());
                return isDuplicate(NAMESPACED_ALIAS_ASSIGNMENT, key.toString(), duplicates, true);
            }
  
            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                MofoAttachment.NamespacedAliasAssignmentAttachment attachment = (MofoAttachment.NamespacedAliasAssignmentAttachment) transaction.getAttachment();
                if (attachment.getAliasName().length() == 0
                        || attachment.getAliasName().length() > Constants.MAX_ALIAS_LENGTH
                        || attachment.getAliasURI().length() > Constants.MAX_ALIAS_URI_LENGTH) {
                    throw new NxtException.NotValidException("Invalid alias assignment: " + attachment.getJSONObject());
                }
                String normalizedAlias = attachment.getAliasName().toLowerCase();
                for (int i = 0; i < normalizedAlias.length(); i++) {
                    if (Constants.NAMESPACED_ALPHABET.indexOf(normalizedAlias.charAt(i)) < 0) {
                        throw new NxtException.NotValidException("Invalid alias name: " + normalizedAlias);
                    }
                }
                if (!NamespacedAlias.isEnabled()) {
                    throw new NxtException.NotYetEnabledException("Namespaced Alias not yet enabled at height " +  Nxt.getBlockchain().getLastBlock().getHeight());
                }
            }
  
            @Override
            public boolean canHaveRecipient() {
                return false;
            }  
        };
    }
    
    public static abstract class PrivateAssetAddAccountTransaction extends TransactionType {
  
        private PrivateAssetAddAccountTransaction() {
        }
  
        @Override
        public final byte getType() {
            return TYPE_FIMKRYPTO;
        }
  
        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }
  
        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }
  
        public static final TransactionType PRIVATE_ASSET_ADD_ACCOUNT = new PrivateAssetAddAccountTransaction() {
  
            @Override
            public final byte getSubtype() {
                return SUBTYPE_FIMKRYPTO_PRIVATE_ASSET_ADD_ACCOUNT;
            }
  
            @Override
            MofoAttachment.AddPrivateAssetAccountAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new MofoAttachment.AddPrivateAssetAccountAttachment(buffer, transactionVersion);
            }
  
            @Override
            MofoAttachment.AddPrivateAssetAccountAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new MofoAttachment.AddPrivateAssetAccountAttachment(attachmentData);
            }
  
            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                MofoAttachment.AddPrivateAssetAccountAttachment attachment = (MofoAttachment.AddPrivateAssetAccountAttachment) transaction.getAttachment();
                MofoAsset.setAccountAllowed(attachment.getAssetId(), transaction.getRecipientId(), true);
            }
  
            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String,Boolean>> duplicates) {
                MofoAttachment.AddPrivateAssetAccountAttachment attachment = (MofoAttachment.AddPrivateAssetAccountAttachment) transaction.getAttachment();
                StringBuilder key = new StringBuilder();
                key.append(transaction.getSenderId());
                key.append(transaction.getRecipientId());
                key.append(attachment.getAssetId());
                return isDuplicate(PRIVATE_ASSET_ADD_ACCOUNT, key.toString(), duplicates, true);
            }
  
            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                MofoAttachment.AddPrivateAssetAccountAttachment attachment = (MofoAttachment.AddPrivateAssetAccountAttachment) transaction.getAttachment();
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (asset == null) {
                    throw new NxtException.NotValidException("Asset does not exist");
                }
                Account senderAccount = Account.getAccount(transaction.getSenderId());
                if (senderAccount == null) {
                    throw new NxtException.NotValidException("Sender account does not exist");
                }
                if (asset.getAccountId() != senderAccount.getId()) {
                    throw new NxtException.NotValidException("Only asset issuer can add private accounts");
                }
                Account recipientAccount = Account.getAccount(transaction.getRecipientId());
                if (recipientAccount == null) {
                    throw new NxtException.NotValidException("Recipient account does not exist");
                }
                if (recipientAccount.getId() == senderAccount.getId()) {
                    throw new NxtException.NotValidException("Issuer account can not be added as private account");
                }
                if ( ! Asset.privateEnabled()) {
                    throw new NxtException.NotYetEnabledException("Private Assets not yet enabled at height " +  Nxt.getBlockchain().getLastBlock().getHeight());
                }
            }
  
            @Override
            public boolean canHaveRecipient() {
                return true;
            }  
        };
    }

    public static abstract class PrivateAssetRemoveAccountTransaction extends TransactionType {
        
        private PrivateAssetRemoveAccountTransaction() {
        }
  
        @Override
        public final byte getType() {
            return TYPE_FIMKRYPTO;
        }
  
        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }
  
        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }
  
        public static final TransactionType PRIVATE_ASSET_REMOVE_ACCOUNT = new PrivateAssetRemoveAccountTransaction() {
  
            @Override
            public final byte getSubtype() {
                return SUBTYPE_FIMKRYPTO_PRIVATE_ASSET_REMOVE_ACCOUNT;
            }
  
            @Override
            MofoAttachment.RemovePrivateAssetAccountAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new MofoAttachment.RemovePrivateAssetAccountAttachment(buffer, transactionVersion);
            }
  
            @Override
            MofoAttachment.RemovePrivateAssetAccountAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new MofoAttachment.RemovePrivateAssetAccountAttachment(attachmentData);
            }
  
            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                MofoAttachment.RemovePrivateAssetAccountAttachment attachment = (MofoAttachment.RemovePrivateAssetAccountAttachment) transaction.getAttachment();
                MofoAsset.setAccountAllowed(attachment.getAssetId(), transaction.getRecipientId(), false);
            }
  
            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String,Boolean>> duplicates) {
                MofoAttachment.RemovePrivateAssetAccountAttachment attachment = (MofoAttachment.RemovePrivateAssetAccountAttachment) transaction.getAttachment();
                StringBuilder key = new StringBuilder();
                key.append(transaction.getSenderId());
                key.append(transaction.getRecipientId());
                key.append(attachment.getAssetId());
                return isDuplicate(PRIVATE_ASSET_REMOVE_ACCOUNT, key.toString(), duplicates, true);
            }
  
            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                MofoAttachment.RemovePrivateAssetAccountAttachment attachment = (MofoAttachment.RemovePrivateAssetAccountAttachment) transaction.getAttachment();
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (asset == null) {
                    throw new NxtException.NotValidException("Asset does not exist");
                }
                Account senderAccount = Account.getAccount(transaction.getSenderId());
                if (senderAccount == null) {
                    throw new NxtException.NotValidException("Sender account does not exist");
                }
                if (asset.getAccountId() != senderAccount.getId()) {
                    throw new NxtException.NotValidException("Only asset issuer can remove private accounts");
                }

                Account recipientAccount = Account.getAccount(transaction.getRecipientId());
                if (recipientAccount == null) {
                    throw new NxtException.NotValidException("Recipient account does not exist");
                }
                if (recipientAccount.getId() == senderAccount.getId()) {
                    throw new NxtException.NotValidException("Issuer account can not be removed as private account");
                }                
                
                if ( ! MofoAsset.getAccountAllowed(attachment.getAssetId(), recipientAccount.getId())) {
                    throw new NxtException.NotValidException("Cannot remove account as private account that is not a private account");
                }
                if ( ! Asset.privateEnabled()) {
                    throw new NxtException.NotYetEnabledException("Private Assets not yet enabled at height " +  Nxt.getBlockchain().getLastBlock().getHeight());
                }
            }
  
            @Override
            public boolean canHaveRecipient() {
                return true;
            }  
        };
    }

    public static abstract class PrivateAssetSetFeeTransaction extends TransactionType {
        
        private PrivateAssetSetFeeTransaction() {
        }
  
        @Override
        public final byte getType() {
            return TYPE_FIMKRYPTO;
        }
  
        @Override
        final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
            return true;
        }
  
        @Override
        final void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        }
  
        public static final TransactionType PRIVATE_ASSET_SET_FEE = new PrivateAssetSetFeeTransaction() {
  
            @Override
            public final byte getSubtype() {
                return SUBTYPE_FIMKRYPTO_PRIVATE_ASSET_SET_FEE;
            }
  
            @Override
            MofoAttachment.PrivateAssetSetFeeAttachment parseAttachment(ByteBuffer buffer, byte transactionVersion) throws NxtException.NotValidException {
                return new MofoAttachment.PrivateAssetSetFeeAttachment(buffer, transactionVersion);
            }
  
            @Override
            MofoAttachment.PrivateAssetSetFeeAttachment parseAttachment(JSONObject attachmentData) throws NxtException.NotValidException {
                return new MofoAttachment.PrivateAssetSetFeeAttachment(attachmentData);
            }
  
            @Override
            void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
                MofoAttachment.PrivateAssetSetFeeAttachment attachment = (MofoAttachment.PrivateAssetSetFeeAttachment) transaction.getAttachment();
                MofoAsset.setFee(attachment.getAssetId(), attachment.getOrderFeePercentage(), attachment.getTradeFeePercentage());
            }
  
            @Override
            boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String,Boolean>> duplicates) {
                MofoAttachment.PrivateAssetSetFeeAttachment attachment = (MofoAttachment.PrivateAssetSetFeeAttachment) transaction.getAttachment();
                StringBuilder key = new StringBuilder();
                key.append(transaction.getSenderId());
                key.append(transaction.getRecipientId());
                key.append(attachment.getAssetId());
                key.append(attachment.getOrderFeePercentage());
                key.append(attachment.getTradeFeePercentage());
                return isDuplicate(PRIVATE_ASSET_SET_FEE, key.toString(), duplicates, true);
            }
  
            @Override
            void validateAttachment(Transaction transaction) throws NxtException.ValidationException {
                MofoAttachment.PrivateAssetSetFeeAttachment attachment = (MofoAttachment.PrivateAssetSetFeeAttachment) transaction.getAttachment();
                Asset asset = Asset.getAsset(attachment.getAssetId());
                if (asset == null) {
                    throw new NxtException.NotValidException("Asset does not exist");
                }
                Account senderAccount = Account.getAccount(transaction.getSenderId());
                if (senderAccount == null) {
                    throw new NxtException.NotValidException("Sender account does not exist");
                }
                if (asset.getAccountId() != senderAccount.getId()) {
                    throw new NxtException.NotValidException("Only asset issuer can set private asset fee");
                }
                if (attachment.getOrderFeePercentage() < Constants.MIN_PRIVATE_ASSET_FEE_PERCENTAGE || 
                    attachment.getOrderFeePercentage() > Constants.MAX_PRIVATE_ASSET_FEE_PERCENTAGE) {
                    throw new NxtException.NotValidException("Out of range order fee percentage");
                }
                if (attachment.getTradeFeePercentage() < Constants.MIN_PRIVATE_ASSET_FEE_PERCENTAGE || 
                    attachment.getTradeFeePercentage() > Constants.MAX_PRIVATE_ASSET_FEE_PERCENTAGE) {
                    throw new NxtException.NotValidException("Out of range trade fee percentage");
                }
                if (asset.getType() != Asset.TYPE_PRIVATE_ASSET) {
                    throw new NxtException.NotValidException("Asset is not private");
                }
                if ( ! Asset.privateEnabled()) {
                    throw new NxtException.NotYetEnabledException("Private Assets not yet enabled at height " +  Nxt.getBlockchain().getLastBlock().getHeight());
                }
            }
  
            @Override
            public boolean canHaveRecipient() {
                return false;
            }
        };
    }

    public static TransactionType findTransactionType(byte subtype) {      
        switch (subtype) {
            case SUBTYPE_FIMKRYPTO_NAMESPACED_ALIAS_ASSIGNMENT:
                return NamespacedAliasAssignmentTransaction.NAMESPACED_ALIAS_ASSIGNMENT;
            case SUBTYPE_FIMKRYPTO_PRIVATE_ASSET_ADD_ACCOUNT:
                return PrivateAssetAddAccountTransaction.PRIVATE_ASSET_ADD_ACCOUNT;
            case SUBTYPE_FIMKRYPTO_PRIVATE_ASSET_REMOVE_ACCOUNT:
                return PrivateAssetRemoveAccountTransaction.PRIVATE_ASSET_REMOVE_ACCOUNT;
            case SUBTYPE_FIMKRYPTO_PRIVATE_ASSET_SET_FEE:
                return PrivateAssetSetFeeTransaction.PRIVATE_ASSET_SET_FEE;
            default:
                return null;
        }
    }

}
