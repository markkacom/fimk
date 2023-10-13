/******************************************************************************
 * Copyright © 2013-2015 The Nxt Core Developers.                             *
 *                                                                            *
 * See the AUTHORS.txt, DEVELOPER-AGREEMENT.txt and LICENSE.txt files at      *
 * the top-level directory of this distribution for the individual copyright  *
 * holder information and the developer policies on copyright and licensing.  *
 *                                                                            *
 * Unless otherwise agreed in a custom licensing agreement, no part of the    *
 * Nxt software, including this file, may be copied, modified, propagated,    *
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
import nxt.db.VersionedEntityDbTable;
import nxt.txn.AskOrderPlacementAttachment;
import nxt.txn.BidOrderPlacementAttachment;
import nxt.txn.OrderPlacementAttachment;
import nxt.util.Listener;
import nxt.util.Listeners;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class Order {

    public static enum Event {
        CREATE, UPDATE, REMOVE
    }

    private static void matchOrders(long assetId) {

        Order.Ask askOrder;
        Order.Bid bidOrder;

        while ((askOrder = Ask.getNextOrder(assetId)) != null
                && (bidOrder = Bid.getNextOrder(assetId)) != null) {

            if (askOrder.getPriceNQT() > bidOrder.getPriceNQT()) {
                break;
            }

            Trade trade = Trade.addTrade(assetId, askOrder, bidOrder);

            if (Asset.privateEnabled() && MofoAsset.isPrivateAsset(askOrder.getAssetId())) {
                /**
                 * When a trade fee is imposed that fee (percentage) will be deducted from
                 * the amount of NQT which the asker receives and from the QNT of assets
                 * that the bidder receives.
                 *
                 * The NQT from the asker and the QNT from the bidder are transferred to the
                 * asset issuer account.
                 */

                askOrder.updateQuantityQNT(Math.subtractExact(askOrder.getQuantityQNT(), trade.getQuantityQNT()));

                Account askAccount = Account.getAccount(askOrder.getAccountId());
                long amountNQT = Math.multiplyExact(trade.getQuantityQNT(), trade.getPriceNQT());
                long feeNQT = MofoAsset.calculateTradeFee(assetId, amountNQT);

                askAccount.addToBalanceAndUnconfirmedBalanceNQT(Math.subtractExact(amountNQT, feeNQT));
                askAccount.addToAssetBalanceQNT(assetId, -trade.getQuantityQNT());

                bidOrder.updateQuantityQNT(Math.subtractExact(bidOrder.getQuantityQNT(), trade.getQuantityQNT()));

                Account bidAccount = Account.getAccount(bidOrder.getAccountId());
                long quantityQNT = trade.getQuantityQNT();
                long feeQNT = MofoAsset.calculateTradeFee(assetId, quantityQNT);

                bidAccount.addToAssetAndUnconfirmedAssetBalanceQNT(assetId, Math.subtractExact(quantityQNT, feeQNT));
                bidAccount.addToBalanceNQT(-Math.multiplyExact(trade.getQuantityQNT(), trade.getPriceNQT()));
                bidAccount.addToUnconfirmedBalanceNQT(Math.multiplyExact(trade.getQuantityQNT(), (bidOrder.getPriceNQT() - trade.getPriceNQT())));

                Account issuerAccount = Account.getAccount(Asset.getAsset(assetId).getAccountId());
                issuerAccount.addToBalanceAndUnconfirmedBalanceNQT(feeNQT);
                issuerAccount.addToAssetAndUnconfirmedAssetBalanceQNT(assetId, feeQNT);
            }
            else {
                askOrder.updateQuantityQNT(Math.subtractExact(askOrder.getQuantityQNT(), trade.getQuantityQNT()));
                Account askAccount = Account.getAccount(askOrder.getAccountId());
                askAccount.addToBalanceAndUnconfirmedBalanceNQT(Math.multiplyExact(trade.getQuantityQNT(), trade.getPriceNQT()));
                askAccount.addToAssetBalanceQNT(assetId, -trade.getQuantityQNT());

                bidOrder.updateQuantityQNT(Math.subtractExact(bidOrder.getQuantityQNT(), trade.getQuantityQNT()));
                Account bidAccount = Account.getAccount(bidOrder.getAccountId());
                bidAccount.addToAssetAndUnconfirmedAssetBalanceQNT(assetId, trade.getQuantityQNT());
                bidAccount.addToBalanceNQT(-Math.multiplyExact(trade.getQuantityQNT(), trade.getPriceNQT()));
                bidAccount.addToUnconfirmedBalanceNQT(Math.multiplyExact(trade.getQuantityQNT(), (bidOrder.getPriceNQT() - trade.getPriceNQT())));
            }
        }
    }

    static void init() {
        Ask.init();
        Bid.init();
    }


    private final long id;
    private final long accountId;
    private final long assetId;
    private final long priceNQT;
    private final int creationHeight;
    private final short transactionIndex;
    private final int transactionHeight;

    private long quantityQNT;

    private Order(Transaction transaction, OrderPlacementAttachment attachment) {
        this.id = transaction.getId();
        this.accountId = transaction.getSenderId();
        this.assetId = attachment.getAssetId();
        this.quantityQNT = attachment.getQuantityQNT();
        this.priceNQT = attachment.getPriceNQT();
        this.creationHeight = Nxt.getBlockchain().getHeight();
        this.transactionIndex = transaction.getIndex();
        this.transactionHeight = transaction.getHeight();
    }

    private Order(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.accountId = rs.getLong("account_id");
        this.assetId = rs.getLong("asset_id");
        this.priceNQT = rs.getLong("price");
        this.quantityQNT = rs.getLong("quantity");
        this.creationHeight = rs.getInt("creation_height");
        this.transactionIndex = rs.getShort("transaction_index");
        this.transactionHeight = rs.getInt("transaction_height");
    }

    private void save(Connection con, String table) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO " + table + " (id, account_id, asset_id, "
                + "price, quantity, creation_height, transaction_index, transaction_height, height, latest) KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, this.id);
            pstmt.setLong(++i, this.accountId);
            pstmt.setLong(++i, this.assetId);
            pstmt.setLong(++i, this.priceNQT);
            pstmt.setLong(++i, this.quantityQNT);
            pstmt.setInt(++i, this.creationHeight);
            pstmt.setShort(++i, this.transactionIndex);
            pstmt.setInt(++i, this.transactionHeight);
            pstmt.setInt(++i, Nxt.getBlockchain().getHeight());
            pstmt.executeUpdate();
        }
    }

    public final long getId() {
        return id;
    }

    public final long getAccountId() {
        return accountId;
    }

    public final long getAssetId() {
        return assetId;
    }

    public final long getPriceNQT() {
        return priceNQT;
    }

    public final long getQuantityQNT() {
        return quantityQNT;
    }

    public final int getHeight() {
        return creationHeight;
    }

    public final int getTransactionIndex() {
        return transactionIndex;
    }

    public final int getTransactionHeight() {
        return transactionHeight;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " id: " + Long.toUnsignedString(id) + " account: " + Long.toUnsignedString(accountId)
                + " asset: " + Long.toUnsignedString(assetId) + " price: " + priceNQT + " quantity: " + quantityQNT
                + " height: " + creationHeight + " transactionIndex: " + transactionIndex + " transactionHeight: " + transactionHeight;
    }

    private void setQuantityQNT(long quantityQNT) {
        this.quantityQNT = quantityQNT;
    }

    /*
    private int compareTo(Order o) {
        if (height < o.height) {
            return -1;
        } else if (height > o.height) {
            return 1;
        } else {
            if (id < o.id) {
                return -1;
            } else if (id > o.id) {
                return 1;
            } else {
                return 0;
            }
        }

    }
    */

    public static final class Ask extends Order {

        private static final Listeners<Order,Event> listeners = new Listeners<>();

        public static boolean addListener(Listener<Order> listener, Event eventType) {
            return listeners.addListener(listener, eventType);
        }

        public static boolean removeListener(Listener<Order> listener, Event eventType) {
            return listeners.removeListener(listener, eventType);
        }

        private static final DbKey.LongKeyFactory<Ask> askOrderDbKeyFactory = new DbKey.LongKeyFactory<Ask>("id") {

            @Override
            public DbKey newKey(Ask ask) {
                return ask.dbKey;
            }

        };

        private static final VersionedEntityDbTable<Ask> askOrderTable = new VersionedEntityDbTable<Ask>("ask_order", askOrderDbKeyFactory) {
            @Override
            protected Ask load(Connection con, ResultSet rs) throws SQLException {
                return new Ask(rs);
            }

            @Override
            protected void save(Connection con, Ask ask) throws SQLException {
                ask.save(con, table);
            }

            @Override
            protected String defaultSort() {
                return " ORDER BY creation_height DESC ";
            }

        };

        public static VersionedEntityDbTable<Ask> getTable() {
            return askOrderTable;
        }

        public static int getCount() {
            return askOrderTable.getCount();
        }

        public static Ask getAskOrder(long orderId) {
            return askOrderTable.get(askOrderDbKeyFactory.newKey(orderId));
        }

        public static DbIterator<Ask> getAll(int from, int to) {
            return askOrderTable.getAll(from, to);
        }

        public static int getAskOrdersCountByAccount(long accountId) {
            return askOrderTable.getCount(new DbClause.LongClause("account_id", accountId));
        }

        public static DbIterator<Ask> getAskOrdersByAccount(long accountId, int from, int to) {
            return askOrderTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
        }

        public static DbIterator<Ask> getAskOrdersByAsset(long assetId, int from, int to) {
            return askOrderTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
        }

        public static int getAskOrderCountByAsset(long assetId, long accountId) {
            DbClause clause = new DbClause.LongClause("asset_id", assetId);
            if (accountId != 0) {
                clause = clause.and(new DbClause.LongClause("account_id", accountId));
            }
            return askOrderTable.getCount(clause);
        }

        public static DbIterator<Ask> getAskOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
            DbClause dbClause = new DbClause.LongClause("account_id", accountId).and(new DbClause.LongClause("asset_id", assetId));
            return askOrderTable.getManyBy(dbClause, from, to);
        }

        public static DbIterator<Ask> getSortedOrders(long assetId, int from, int to) {
            return askOrderTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to,
                    " ORDER BY price ASC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
        }

        private static Ask getNextOrder(long assetId) {
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM ask_order WHERE asset_id = ? "
                         + "AND latest = TRUE ORDER BY price ASC, creation_height ASC, transaction_height ASC, transaction_index ASC LIMIT 1")) {
                pstmt.setLong(1, assetId);
                try (DbIterator<Ask> askOrders = askOrderTable.getManyBy(con, pstmt, true)) {
                    return askOrders.hasNext() ? askOrders.next() : null;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        public static void addOrder(Transaction transaction, AskOrderPlacementAttachment attachment) {
            Ask order = new Ask(transaction, attachment);
            askOrderTable.insert(order);
            try {
                listeners.notify(order, Event.CREATE);
            } finally {
                matchOrders(attachment.getAssetId());
            }
        }

        public static void removeOrder(long orderId) {
            Ask order = getAskOrder(orderId);
            try {
                if (order != null) {
                    listeners.notify(order, Event.REMOVE);
                }
            } finally {
                askOrderTable.delete(order);
            }
        }

        static void init() {}


        private final DbKey dbKey;

        private Ask(Transaction transaction, AskOrderPlacementAttachment attachment) {
            super(transaction, attachment);
            this.dbKey = askOrderDbKeyFactory.newKey(super.id);
        }

        private Ask(ResultSet rs) throws SQLException {
            super(rs);
            this.dbKey = askOrderDbKeyFactory.newKey(super.id);
        }

        private void save(Connection con, String table) throws SQLException {
            super.save(con, table);
        }

        private void updateQuantityQNT(long quantityQNT) {
            super.setQuantityQNT(quantityQNT);
            if (quantityQNT > 0) {
                askOrderTable.insert(this);
                listeners.notify(this, Event.UPDATE);
            } else if (quantityQNT == 0) {
                askOrderTable.delete(this);
                listeners.notify(this, Event.REMOVE);
            } else {
                throw new IllegalArgumentException("Negative quantity: " + quantityQNT
                        + " for order: " + Long.toUnsignedString(getId()));
            }
        }

        /*
        @Override
        public int compareTo(Ask o) {
            if (this.getPriceNQT() < o.getPriceNQT()) {
                return -1;
            } else if (this.getPriceNQT() > o.getPriceNQT()) {
                return 1;
            } else {
                return super.compareTo(o);
            }
        }
        */

    }

    public static final class Bid extends Order {

        private static final Listeners<Order,Event> listeners = new Listeners<>();

        public static boolean addListener(Listener<Order> listener, Event eventType) {
            return listeners.addListener(listener, eventType);
        }

        public static boolean removeListener(Listener<Order> listener, Event eventType) {
            return listeners.removeListener(listener, eventType);
        }

        private static final DbKey.LongKeyFactory<Bid> bidOrderDbKeyFactory = new DbKey.LongKeyFactory<Bid>("id") {

            @Override
            public DbKey newKey(Bid bid) {
                return bid.dbKey;
            }

        };

        private static final VersionedEntityDbTable<Bid> bidOrderTable = new VersionedEntityDbTable<Bid>("bid_order", bidOrderDbKeyFactory) {

            @Override
            protected Bid load(Connection con, ResultSet rs) throws SQLException {
                return new Bid(rs);
            }

            @Override
            protected void save(Connection con, Bid bid) throws SQLException {
                bid.save(con, table);
            }

            @Override
            protected String defaultSort() {
                return " ORDER BY creation_height DESC ";
            }

        };

        public static VersionedEntityDbTable<Bid> getTable() {
            return bidOrderTable;
        }

        public static int getCount() {
            return bidOrderTable.getCount();
        }

        public static Bid getBidOrder(long orderId) {
            return bidOrderTable.get(bidOrderDbKeyFactory.newKey(orderId));
        }

        public static DbIterator<Bid> getAll(int from, int to) {
            return bidOrderTable.getAll(from, to);
        }

        public static DbIterator<Bid> getBidOrdersByAccount(long accountId, int from, int to) {
            return bidOrderTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
        }

        public static int getBidOrdersCountByAccount(long accountId) {
            return bidOrderTable.getCount(new DbClause.LongClause("account_id", accountId));
        }

        public static DbIterator<Bid> getBidOrdersByAsset(long assetId, int from, int to) {
            return bidOrderTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to);
        }

        public static int getBidOrderCountByAsset(long assetId, long accountId) {
            DbClause clause = new DbClause.LongClause("asset_id", assetId);
            if (accountId != 0) {
                clause = clause.and(new DbClause.LongClause("account_id", accountId));
            }
            return bidOrderTable.getCount(clause);
        }

        public static DbIterator<Bid> getBidOrdersByAccountAsset(final long accountId, final long assetId, int from, int to) {
            DbClause dbClause = new DbClause.LongClause("account_id", accountId).and(new DbClause.LongClause("asset_id", assetId));
            return bidOrderTable.getManyBy(dbClause, from, to);
        }

        public static DbIterator<Bid> getSortedOrders(long assetId, int from, int to) {
            return bidOrderTable.getManyBy(new DbClause.LongClause("asset_id", assetId), from, to,
                    " ORDER BY price DESC, creation_height ASC, transaction_height ASC, transaction_index ASC ");
        }

        private static Bid getNextOrder(long assetId) {
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmt = con.prepareStatement("SELECT * FROM bid_order WHERE asset_id = ? "
                         + "AND latest = TRUE ORDER BY price DESC, creation_height ASC, transaction_height ASC, transaction_index ASC LIMIT 1")) {
                pstmt.setLong(1, assetId);
                try (DbIterator<Bid> bidOrders = bidOrderTable.getManyBy(con, pstmt, true)) {
                    return bidOrders.hasNext() ? bidOrders.next() : null;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }

        public static void addOrder(Transaction transaction, BidOrderPlacementAttachment attachment) {
            Bid order = new Bid(transaction, attachment);
            bidOrderTable.insert(order);
            try {
                listeners.notify(order, Event.CREATE);
            } finally {
                matchOrders(attachment.getAssetId());
            }
        }

        public static void removeOrder(long orderId) {
            Bid order = getBidOrder(orderId);
            try {
                if (order != null) {
                    listeners.notify(order, Event.REMOVE);
                }
            } finally {
                bidOrderTable.delete(order);
            }
        }

        static void init() {}


        private final DbKey dbKey;

        private Bid(Transaction transaction, BidOrderPlacementAttachment attachment) {
            super(transaction, attachment);
            this.dbKey = bidOrderDbKeyFactory.newKey(super.id);
        }

        private Bid(ResultSet rs) throws SQLException {
            super(rs);
            this.dbKey = bidOrderDbKeyFactory.newKey(super.id);
        }

        private void save(Connection con, String table) throws SQLException {
            super.save(con, table);
        }

        private void updateQuantityQNT(long quantityQNT) {
            super.setQuantityQNT(quantityQNT);
            if (quantityQNT > 0) {
                bidOrderTable.insert(this);
                listeners.notify(this, Event.UPDATE);
            } else if (quantityQNT == 0) {
                bidOrderTable.delete(this);
                listeners.notify(this, Event.REMOVE);
            } else {
                throw new IllegalArgumentException("Negative quantity: " + quantityQNT
                        + " for order: " + Long.toUnsignedString(getId()));
            }
        }

        /*
        @Override
        public int compareTo(Bid o) {
            if (this.getPriceNQT() > o.getPriceNQT()) {
                return -1;
            } else if (this.getPriceNQT() < o.getPriceNQT()) {
                return 1;
            } else {
                return super.compareTo(o);
            }
        }
        */
    }
}
