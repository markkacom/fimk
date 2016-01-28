/******************************************************************************
 * Copyright © 2014-2016 Krypto Fin ry and FIMK Developers.                   *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * FIMK software, including this file, may be copied, modified, propagated,   *
 * or distributed except according to the terms contained in the LICENSE.txt  *
 * file.                                                                      *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package nxt;

import nxt.db.DbClause;
import nxt.db.DbIterator;
import nxt.db.DbKey;
import nxt.db.DbUtils;
import nxt.db.EntityDbTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class AccountColor {

    /* @called-from nxt.TransactionImpl.validate() */
    public static void validate(Transaction transaction) throws NxtException.ValidationException {
        if (!AccountColor.getAccountColorEnabled()) {
            return;
        }

        Account senderAccount = Account.getAccount(transaction.getSenderId());
        Account recipientAccount = Account.getAccount(transaction.getRecipientId());

        /* Not reached ever! */
        if (senderAccount == null) {
            throw new NxtException.NotValidException("Sender account does not exist, should not happen");
        }

        /* Sender and recipient both exist and have same color */
        if (senderAccount != null && recipientAccount != null) {
            if (senderAccount.getAccountColorId() == recipientAccount.getAccountColorId()) {
                return;
            }
        }

        /* Tests all transaction types, does a 'return' if transaction is valid */
        switch (transaction.getType().getType()) {
            case TransactionType.TYPE_PAYMENT: {
                switch (transaction.getType().getSubtype()) {
                    case TransactionType.SUBTYPE_PAYMENT_ORDINARY_PAYMENT: {

                        /* All accounts can send payments to new accounts */
                        if (recipientAccount == null) {
                            return;
                        }

                        /* Colored account can send payment to color creator */
                        if (senderAccount.getAccountColorId() == 0 && recipientAccount.getAccountColorId() != 0) {
                            AccountColor recipientAccountColor = AccountColor.getAccountColor(recipientAccount.getAccountColorId());
                            if (recipientAccountColor != null && recipientAccountColor.getAccountId() == senderAccount.getId()) {
                                return;
                            }
                        }

                        /* Color creator can send payment to colored account */
                        if (senderAccount.getAccountColorId() != 0 && recipientAccount.getAccountColorId() == 0) {
                            AccountColor senderAccountColor = AccountColor.getAccountColor(senderAccount.getAccountColorId());
                            if (senderAccountColor != null && senderAccountColor.getAccountId() == recipientAccount.getId()) {
                                return;
                            }
                        }

                        throw new NxtException.NotValidException("Payment between colored account and non-colored account not allowed");
                    }
                }
            }
            case TransactionType.TYPE_COLORED_COINS: {
                switch (transaction.getType().getSubtype()) {
                    case TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_CANCELLATION:
                    case TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_CANCELLATION:
                    case TransactionType.SUBTYPE_COLORED_COINS_DIVIDEND_PAYMENT:
                    case TransactionType.SUBTYPE_COLORED_COINS_ASK_ORDER_PLACEMENT:
                    case TransactionType.SUBTYPE_COLORED_COINS_ASSET_ISSUANCE: {

                        /* Not affected by account color */
                        return;
                    }
                    case TransactionType.SUBTYPE_COLORED_COINS_ASSET_TRANSFER: {

                        /* All accounts can transfer assets to new accounts */
                        if (recipientAccount == null) {
                            return;
                        }

                        /* Sender and recipient must have same color */
                        if (senderAccount.getAccountColorId() == recipientAccount.getAccountColorId()) {
                            return;
                        }
                        throw new NxtException.NotValidException("Asset transfer between colored account and non-colored account not allowed");
                    }
                    case TransactionType.SUBTYPE_COLORED_COINS_BID_ORDER_PLACEMENT: {

                        /* Only accounts of same color as asset issuer can place buy orders */
                        Attachment.ColoredCoinsBidOrderPlacement attachment = (Attachment.ColoredCoinsBidOrderPlacement)transaction.getAttachment();
                        Asset asset = Asset.getAsset(attachment.getAssetId());
                        if (asset != null) {

                            Account issuerAccount = Account.getAccount(asset.getAccountId());
                            if (issuerAccount != null) {
                                if (issuerAccount.getAccountColorId() == senderAccount.getAccountColorId()) {
                                    return;
                                }
                            }
                        }
                        throw new NxtException.NotValidException("Buying and selling of assets of different color not allowed");
                    }
                }
            }
            case TransactionType.TYPE_MESSAGING: {
                switch (transaction.getType().getSubtype()) {
                    case TransactionType.SUBTYPE_MESSAGING_ARBITRARY_MESSAGE:
                    case TransactionType.SUBTYPE_MESSAGING_POLL_CREATION:
                    case TransactionType.SUBTYPE_MESSAGING_HUB_ANNOUNCEMENT:
                    case TransactionType.SUBTYPE_MESSAGING_ACCOUNT_INFO: {

                        /* Not affected by account color */
                        return;
                    }
                    case TransactionType.SUBTYPE_MESSAGING_ALIAS_ASSIGNMENT: {

                        /* Not supported for colored accounts */
                        if (senderAccount.getAccountColorId() != 0) {
                            throw new NxtException.NotValidException("Colored accounts cannot assign aliases");
                        }

                        return;
                    }
                    case TransactionType.SUBTYPE_MESSAGING_ALIAS_BUY: {

                        /* Not supported for colored accounts */
                        if (senderAccount.getAccountColorId() != 0) {
                            throw new NxtException.NotValidException("Colored accounts cannot buy aliases");
                        }

                        return;
                    }
                    case TransactionType.SUBTYPE_MESSAGING_ALIAS_SELL: {

                        /* Not supported for colored accounts */
                        if (senderAccount.getAccountColorId() != 0) {
                            throw new NxtException.NotValidException("Colored accounts cannot sell aliases");
                        }

                        return;
                    }
                    case TransactionType.SUBTYPE_MESSAGING_PHASING_VOTE_CASTING: {

                        /* Poll creator must have same color as vote caster */
                        Attachment.MessagingVoteCasting attachment = (Attachment.MessagingVoteCasting) transaction.getAttachment();
                        Poll poll = Poll.getPoll(attachment.getPollId());
                        if (poll != null) {
                            Account pollCreatorAccount = Account.getAccount(poll.getAccountId());
                            if (pollCreatorAccount != null) {
                                if (pollCreatorAccount.getAccountColorId() == senderAccount.getAccountColorId()) {
                                    return;
                                }
                            }
                        }

                        throw new NxtException.NotValidException("Votes cannot be cast to polls from a different color");
                    }
                }
            }
            case TransactionType.TYPE_DIGITAL_GOODS: {
                switch (transaction.getType().getSubtype()) {
                    case TransactionType.SUBTYPE_DIGITAL_GOODS_LISTING:
                    case TransactionType.SUBTYPE_DIGITAL_GOODS_DELISTING:
                    case TransactionType.SUBTYPE_DIGITAL_GOODS_PRICE_CHANGE:
                    case TransactionType.SUBTYPE_DIGITAL_GOODS_QUANTITY_CHANGE:
                    case TransactionType.SUBTYPE_DIGITAL_GOODS_DELIVERY:
                    case TransactionType.SUBTYPE_DIGITAL_GOODS_FEEDBACK:
                    case TransactionType.SUBTYPE_DIGITAL_GOODS_REFUND: {

                        /* Not affected by account color */
                        return;
                    }
                    case TransactionType.SUBTYPE_DIGITAL_GOODS_PURCHASE: {

                        /* Goods seller must have same color as sender */
                        Attachment.DigitalGoodsPurchase attachment = (Attachment.DigitalGoodsPurchase) transaction.getAttachment();
                        DigitalGoodsStore.Goods goods = DigitalGoodsStore.Goods.getGoods(attachment.getGoodsId());
                        if (goods != null) {
                            Account sellerAccount = Account.getAccount(goods.getSellerId());
                            if (sellerAccount != null) {
                                if (sellerAccount.getAccountColorId() == senderAccount.getAccountColorId()) {
                                    return;
                                }
                            }
                        }
                        throw new NxtException.NotValidException("Cannot purchase good of different color");
                    }
                }
            }
            case TransactionType.TYPE_MONETARY_SYSTEM: {

                /* Not supported for colored accounts */
                if (senderAccount.getAccountColorId() != 0) {
                    throw new NxtException.NotValidException("Monetary system not supported for colored accounts");
                }

                return;
            }
            case TransactionType.TYPE_DATA: {
                switch (transaction.getType().getSubtype()) {
                    case TransactionType.SUBTYPE_DATA_TAGGED_DATA_UPLOAD:
                    case TransactionType.SUBTYPE_DATA_TAGGED_DATA_EXTEND: {

                        /* Not affected by account color */
                        return;
                    }
                }
            }
            case MofoTransactions.TYPE_FIMKRYPTO: {
                switch (transaction.getType().getSubtype()) {
                    case MofoTransactions.SUBTYPE_FIMKRYPTO_NAMESPACED_ALIAS_ASSIGNMENT:
                    case MofoTransactions.SUBTYPE_FIMKRYPTO_ACCOUNT_ID_ASSIGNMENT:
                    case MofoTransactions.SUBTYPE_FIMKRYPTO_PRIVATE_ASSET_ADD_ACCOUNT:
                    case MofoTransactions.SUBTYPE_FIMKRYPTO_PRIVATE_ASSET_REMOVE_ACCOUNT:
                    case MofoTransactions.SUBTYPE_FIMKRYPTO_PRIVATE_ASSET_SET_FEE:
                    case MofoTransactions.SUBTYPE_FIMKRYPTO_SET_VERIFICATION_AUTHORITY: {

                        /* Not affected by account color */
                        return;
                    }
                    case MofoTransactions.SUBTYPE_FIMKRYPTO_ACCOUNT_COLOR_CREATE:
                    case MofoTransactions.SUBTYPE_FIMKRYPTO_ACCOUNT_COLOR_ASSIGN: {

                        /* Not supported for colored accounts */
                        if (senderAccount.getAccountColorId() != 0) {
                            throw new NxtException.NotValidException("Transaction not supported for colored accounts");
                        }
                        return;
                    }
                }
            }
            case TransactionType.TYPE_ACCOUNT_CONTROL: {
                switch (transaction.getType().getSubtype()) {
                    case TransactionType.SUBTYPE_ACCOUNT_CONTROL_EFFECTIVE_BALANCE_LEASING: {

                        /* Not supported for colored accounts */
                        if (senderAccount.getAccountColorId() != 0) {
                            throw new NxtException.NotValidException("Balance leasing not supported for colored accounts");
                        }

                        return;
                    }
                }
            }
        }

        throw new NxtException.NotValidException("Unsupported transaction type " +
            transaction.getType().getName() + " (type=" +  transaction.getType().getType() + " subtype=" +
                transaction.getType().getSubtype() + ")");
    }

    private static final DbKey.LongKeyFactory<AccountColor> accountColorDbKeyFactory = new DbKey.LongKeyFactory<AccountColor>("id") {

        @Override
        public DbKey newKey(AccountColor asset) {
            return asset.dbKey;
        }

    };

    private static final EntityDbTable<AccountColor> accountColorTable = new EntityDbTable<AccountColor>("account_color", accountColorDbKeyFactory, "name,description") {

        @Override
        protected AccountColor load(Connection con, ResultSet rs) throws SQLException {
            return new AccountColor(rs);
        }

        @Override
        protected void save(Connection con, AccountColor accountColor) throws SQLException {
            accountColor.save(con);
        }

        public boolean hasForeignKey() {
            return true;
        };
    };

    public static DbIterator<AccountColor> getAllAccountColors(int from, int to) {
        return accountColorTable.getAll(from, to);
    }

    public static int getCount() {
        return accountColorTable.getCount();
    }

    public static AccountColor getAccountColor(long id) {
        return accountColorTable.get(accountColorDbKeyFactory.newKey(id));
    }

    public static DbIterator<AccountColor> getAccountColorsIssuedBy(long accountId, int from, int to) {
        return accountColorTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    /* Searches the account colors table using LIKE search on the name column
     * Optionally results are limited to creator account unless accountId is 0 */
    public static DbIterator<AccountColor> searchAccountColorsByName(String name, long accountId, int from, int to) {
        Connection con = null;
        try {
            String sql = "SELECT * "
                       + "FROM account_color "
                       + "WHERE name_lower LIKE ? "
                       +  (accountId != 0 ? "AND account_id = ? " : "")
                       + "ORDER BY name_lower "
                       +  DbUtils.limitsClause(from, to);

            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql.toString());
            int i=1;
            pstmt.setString(i++, "%" + name.toLowerCase() + "%");
            if (accountId != 0) {
                pstmt.setLong(i++, accountId);
            }
            DbUtils.setLimits(i, pstmt, from, to);
            return accountColorTable.getManyBy(con, pstmt, false);

        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    static void addAccountColor(Transaction transaction, MofoAttachment.AccountColorCreateAttachment attachment) {
        accountColorTable.insert(new AccountColor(transaction, attachment));
    }

    static void init() {}

    private final long accountColorId;
    private final DbKey dbKey;
    private final long accountId;
    private final String name;
    private final String description;

    private AccountColor(Transaction transaction, MofoAttachment.AccountColorCreateAttachment attachment) {
        this.accountColorId = transaction.getId();
        this.dbKey = accountColorDbKeyFactory.newKey(this.accountColorId);
        this.accountId = transaction.getSenderId();
        this.name = attachment.getName();
        this.description = attachment.getDescription();
    }

    private AccountColor(ResultSet rs) throws SQLException {
        this.accountColorId = rs.getLong("id");
        this.dbKey = accountColorDbKeyFactory.newKey(this.accountColorId);
        this.accountId = rs.getLong("account_id");
        this.name = rs.getString("name");
        this.description = rs.getString("description");
    }

    private void save(Connection con) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO account_color (id, account_id, name, "
                + "description, height) VALUES (?, ?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, this.accountColorId);
            pstmt.setLong(++i, this.accountId);
            pstmt.setString(++i, this.name);
            pstmt.setString(++i, this.description);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public long getId() {
        return accountColorId;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public DbIterator<Account> getAccounts(int from, int to) {
        return Account.getAccountColorAccounts(this.accountColorId, from, to);
    }

    public static boolean getAccountColorEnabled() {
        return HardFork.COLORED_ACCOUNTS_BLOCK();
    }
}
