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

import nxt.db.DbVersion;

class NxtDbVersion extends DbVersion {

    protected void update(int nextUpdate) {
        switch (nextUpdate) {
            case 1:
                apply("CREATE TABLE IF NOT EXISTS block (db_id IDENTITY, id BIGINT NOT NULL, version INT NOT NULL, "
                        + "timestamp INT NOT NULL, previous_block_id BIGINT, "
                        + "FOREIGN KEY (previous_block_id) REFERENCES block (id) ON DELETE CASCADE, total_amount BIGINT NOT NULL, "
                        + "total_fee BIGINT NOT NULL, payload_length INT NOT NULL, "
                        + "previous_block_hash BINARY(32), cumulative_difficulty VARBINARY NOT NULL, base_target BIGINT NOT NULL, "
                        + "next_block_id BIGINT, FOREIGN KEY (next_block_id) REFERENCES block (id) ON DELETE SET NULL, "
                        + "height INT NOT NULL, generation_signature BINARY(64) NOT NULL, "
                        + "block_signature BINARY(64) NOT NULL, payload_hash BINARY(32) NOT NULL, generator_id BIGINT NOT NULL)");
            case 2:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_id_idx ON block (id)");
            case 3:
                apply("CREATE TABLE IF NOT EXISTS transaction (db_id IDENTITY, id BIGINT NOT NULL, "
                        + "deadline SMALLINT NOT NULL, recipient_id BIGINT, "
                        + "amount BIGINT NOT NULL, fee BIGINT NOT NULL, full_hash BINARY(32) NOT NULL, "
                        + "height INT NOT NULL, block_id BIGINT NOT NULL, FOREIGN KEY (block_id) REFERENCES block (id) ON DELETE CASCADE, "
                        + "signature BINARY(64) NOT NULL, timestamp INT NOT NULL, type TINYINT NOT NULL, subtype TINYINT NOT NULL, "
                        + "sender_id BIGINT NOT NULL, block_timestamp INT NOT NULL, referenced_transaction_full_hash BINARY(32), "
                        + "attachment_bytes VARBINARY, version TINYINT NOT NULL, has_message BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "has_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE, has_public_key_announcement BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "ec_block_height INT DEFAULT NULL, ec_block_id BIGINT DEFAULT NULL, has_encrypttoself_message BOOLEAN NOT NULL DEFAULT FALSE)");
            case 4:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS transaction_id_idx ON transaction (id)");
            case 5:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_height_idx ON block (height)");
            case 6:
                apply(null);
            case 7:
                apply("CREATE INDEX IF NOT EXISTS block_generator_id_idx ON block (generator_id)");
            case 8:
                apply("CREATE INDEX IF NOT EXISTS transaction_sender_id_idx ON transaction (sender_id)");
            case 9:
                apply("CREATE INDEX IF NOT EXISTS transaction_recipient_id_idx ON transaction (recipient_id)");
            case 10:
                apply(null);
            case 11:
                apply(null);
            case 12:
                apply(null);
            case 13:
                apply(null);
            case 14:
                apply(null);
            case 15:
                apply(null);
            case 16:
                apply(null);
            case 17:
                apply(null);
            case 18:
                apply(null);
            case 19:
                apply(null);
            case 20:
                apply(null);
            case 21:
                apply(null);
            case 22:
                apply(null);
            case 23:
                apply(null);
            case 24:
                apply(null);
            case 25:
                apply(null);
            case 26:
                apply(null);
            case 27:
                apply(null);
            case 28:
                apply(null);
            case 29:
                apply(null);
            case 30:
                apply(null);
            case 31:
                apply(null);
            case 32:
                apply(null);
            case 33:
                apply(null);
            case 34:
                apply(null);
            case 35:
                apply(null);
            case 36:
                apply("CREATE TABLE IF NOT EXISTS peer (address VARCHAR PRIMARY KEY)");
            case 37:
                apply(null);
            case 38:
                apply(null);
            case 39:
                apply(null);
            case 40:
                apply(null);
            case 41:
                apply(null);
            case 42:
                apply(null);
            case 43:
                apply(null);
            case 44:
                apply(null);
            case 45:
                apply(null);
            case 46:
                apply(null);
            case 47:
                apply(null);
            case 48:
                apply(null);
            case 49:
                apply(null);
            case 50:
                apply(null);
            case 51:
                apply(null);
            case 52:
                apply(null);
            case 53:
                apply(null);
            case 54:
                apply(null);
            case 55:
                apply(null);
            case 56:
                apply(null);
            case 57:
                apply(null);
            case 58:
                apply(null);
            case 59:
                apply(null);
            case 60:
                apply(null);
            case 61:
                apply(null);
            case 62:
                apply(null);
            case 63:
                apply(null);
            case 64:
                apply(null);
            case 65:
                apply(null);
            case 66:
                apply(null);
            case 67:
                apply(null);
            case 68:
                apply(null);
            case 69:
                apply(null);
            case 70:
                if (!Constants.isTestnet) {
                    apply("INSERT INTO peer (address) VALUES " +
                        "('5.101.102.197'), " +
                        "('5.101.102.199'), ('5.101.102.200'), ('5.101.102.201'), " +
                        "('107.170.73.9'), ('107.170.123.54'), ('144.76.3.50')");
                } else {
                    apply("INSERT INTO peer (address) VALUES " + "('188.166.36.203'), ('188.166.0.145')");
                }
            case 71:
                apply(null);
            case 72:
                apply(null);
            case 73:
                apply("CREATE TABLE IF NOT EXISTS namespaced_alias (db_id IDENTITY, id BIGINT NOT NULL, "
                    + "account_id BIGINT NOT NULL, alias_name VARCHAR NOT NULL, "
                    + "alias_name_lower VARCHAR AS LOWER (alias_name) NOT NULL, "
                    + "alias_uri VARCHAR NOT NULL, timestamp INT NOT NULL, "
                    + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 74:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_id_height_idx ON namespaced_alias (id, height DESC)");
            case 75:
                apply("CREATE INDEX IF NOT EXISTS alias_account_id_idx ON namespaced_alias (account_id, height DESC)");
            case 76:
                apply("CREATE INDEX IF NOT EXISTS alias_name_lower_idx ON namespaced_alias (alias_name_lower)");
            case 77:
                apply("CREATE INDEX IF NOT EXISTS transaction_block_timestamp_idx ON transaction (block_timestamp DESC)");
            case 78:
                apply("DROP INDEX IF EXISTS transaction_timestamp_idx");
            case 79:
                apply("CREATE TABLE IF NOT EXISTS alias (db_id IDENTITY, id BIGINT NOT NULL, "
                    + "account_id BIGINT NOT NULL, alias_name VARCHAR NOT NULL, "
                    + "alias_name_lower VARCHAR AS LOWER (alias_name) NOT NULL, "
                    + "alias_uri VARCHAR NOT NULL, timestamp INT NOT NULL, "
                    + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 80:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_id_height_idx ON alias (id, height DESC)");
            case 81:
                apply("CREATE INDEX IF NOT EXISTS alias_account_id_idx ON alias (account_id, height DESC)");
            case 82:
                apply("CREATE INDEX IF NOT EXISTS alias_name_lower_idx ON alias (alias_name_lower)");
            case 83:
                apply("CREATE TABLE IF NOT EXISTS alias_offer (db_id IDENTITY, id BIGINT NOT NULL, "
                        + "price BIGINT NOT NULL, buyer_id BIGINT, "
                        + "height INT NOT NULL, latest BOOLEAN DEFAULT TRUE NOT NULL)");
            case 84:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS alias_offer_id_height_idx ON alias_offer (id, height DESC)");
            case 85:
                apply("CREATE TABLE IF NOT EXISTS asset (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "name VARCHAR NOT NULL, description VARCHAR, quantity BIGINT NOT NULL, decimals TINYINT NOT NULL, "
                        + "height INT NOT NULL)");
            case 86:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_id_idx ON asset (id)");
            case 87:
                apply("CREATE INDEX IF NOT EXISTS asset_account_id_idx ON asset (account_id)");
            case 88:
                apply("CREATE TABLE IF NOT EXISTS trade (db_id IDENTITY, asset_id BIGINT NOT NULL, block_id BIGINT NOT NULL, "
                        + "ask_order_id BIGINT NOT NULL, bid_order_id BIGINT NOT NULL, ask_order_height INT NOT NULL, "
                        + "bid_order_height INT NOT NULL, seller_id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, "
                        + "quantity BIGINT NOT NULL, price BIGINT NOT NULL, timestamp INT NOT NULL, height INT NOT NULL)");
            case 89:
                apply(null);
            case 90:
                apply("CREATE INDEX IF NOT EXISTS trade_asset_id_idx ON trade (asset_id, height DESC)");
            case 91:
                apply("CREATE INDEX IF NOT EXISTS trade_seller_id_idx ON trade (seller_id, height DESC)");
            case 92:
                apply("CREATE INDEX IF NOT EXISTS trade_buyer_id_idx ON trade (buyer_id, height DESC)");
            case 93:
                apply("CREATE TABLE IF NOT EXISTS ask_order (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "asset_id BIGINT NOT NULL, price BIGINT NOT NULL, "
                        + "quantity BIGINT NOT NULL, creation_height INT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 94:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS ask_order_id_height_idx ON ask_order (id, height DESC)");
            case 95:
                apply("CREATE INDEX IF NOT EXISTS ask_order_account_id_idx ON ask_order (account_id, height DESC)");
            case 96:
                apply("CREATE INDEX IF NOT EXISTS ask_order_asset_id_price_idx ON ask_order (asset_id, price)");
            case 97:
                apply("CREATE TABLE IF NOT EXISTS bid_order (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "asset_id BIGINT NOT NULL, price BIGINT NOT NULL, "
                        + "quantity BIGINT NOT NULL, creation_height INT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 98:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS bid_order_id_height_idx ON bid_order (id, height DESC)");
            case 99:
                apply("CREATE INDEX IF NOT EXISTS bid_order_account_id_idx ON bid_order (account_id, height DESC)");
            case 100:
                apply("CREATE INDEX IF NOT EXISTS bid_order_asset_id_price_idx ON bid_order (asset_id, price DESC)");
            case 101:
                apply("CREATE TABLE IF NOT EXISTS goods (db_id IDENTITY, id BIGINT NOT NULL, seller_id BIGINT NOT NULL, "
                        + "name VARCHAR NOT NULL, description VARCHAR, "
                        + "tags VARCHAR, timestamp INT NOT NULL, quantity INT NOT NULL, price BIGINT NOT NULL, "
                        + "delisted BOOLEAN NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 102:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS goods_id_height_idx ON goods (id, height DESC)");
            case 103:
                apply("CREATE INDEX IF NOT EXISTS goods_seller_id_name_idx ON goods (seller_id, name)");
            case 104:
                apply("CREATE INDEX IF NOT EXISTS goods_timestamp_idx ON goods (timestamp DESC, height DESC)");
            case 105:
                apply("CREATE TABLE IF NOT EXISTS purchase (db_id IDENTITY, id BIGINT NOT NULL, buyer_id BIGINT NOT NULL, "
                        + "goods_id BIGINT NOT NULL, "
                        + "seller_id BIGINT NOT NULL, quantity INT NOT NULL, "
                        + "price BIGINT NOT NULL, deadline INT NOT NULL, note VARBINARY, nonce BINARY(32), "
                        + "timestamp INT NOT NULL, pending BOOLEAN NOT NULL, goods VARBINARY, goods_nonce BINARY(32), "
                        + "refund_note VARBINARY, refund_nonce BINARY(32), has_feedback_notes BOOLEAN NOT NULL DEFAULT FALSE, "
                        + "has_public_feedbacks BOOLEAN NOT NULL DEFAULT FALSE, discount BIGINT NOT NULL, refund BIGINT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 106:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS purchase_id_height_idx ON purchase (id, height DESC)");
            case 107:
                apply("CREATE INDEX IF NOT EXISTS purchase_buyer_id_height_idx ON purchase (buyer_id, height DESC)");
            case 108:
                apply("CREATE INDEX IF NOT EXISTS purchase_seller_id_height_idx ON purchase (seller_id, height DESC)");
            case 109:
                apply("CREATE INDEX IF NOT EXISTS purchase_deadline_idx ON purchase (deadline DESC, height DESC)");
            case 110:
                apply("CREATE TABLE IF NOT EXISTS account (db_id IDENTITY, id BIGINT NOT NULL, creation_height INT NOT NULL, "
                        + "key_height INT, balance BIGINT NOT NULL, unconfirmed_balance BIGINT NOT NULL, "
                        + "forged_balance BIGINT NOT NULL, current_leasing_height_from INT, "
                        + "current_leasing_height_to INT, current_lessee_id BIGINT NULL, next_leasing_height_from INT, "
                        + "next_leasing_height_to INT, next_lessee_id BIGINT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 111:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_id_height_idx ON account (id, height DESC)");
            case 112:
                apply(null);
            case 113:
                apply("CREATE TABLE IF NOT EXISTS account_asset (db_id IDENTITY, account_id BIGINT NOT NULL, "
                        + "asset_id BIGINT NOT NULL, quantity BIGINT NOT NULL, unconfirmed_quantity BIGINT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 114:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_asset_id_height_idx ON account_asset (account_id, asset_id, height DESC)");
            case 115:
                apply("CREATE TABLE IF NOT EXISTS account_guaranteed_balance (db_id IDENTITY, account_id BIGINT NOT NULL, "
                        + "additions BIGINT NOT NULL, height INT NOT NULL)");
            case 116:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_guaranteed_balance_id_height_idx ON account_guaranteed_balance "
                        + "(account_id, height DESC)");
            case 117:
                apply("CREATE TABLE IF NOT EXISTS purchase_feedback (db_id IDENTITY, id BIGINT NOT NULL, feedback_data VARBINARY NOT NULL, "
                        + "feedback_nonce BINARY(32) NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 118:
                apply("CREATE INDEX IF NOT EXISTS purchase_feedback_id_height_idx ON purchase_feedback (id, height DESC)");
            case 119:
                apply("CREATE TABLE IF NOT EXISTS purchase_public_feedback (db_id IDENTITY, id BIGINT NOT NULL, public_feedback "
                        + "VARCHAR NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 120:
                apply("CREATE INDEX IF NOT EXISTS purchase_public_feedback_id_height_idx ON purchase_public_feedback (id, height DESC)");
            case 121:
                apply("CREATE TABLE IF NOT EXISTS unconfirmed_transaction (db_id IDENTITY, id BIGINT NOT NULL, expiration INT NOT NULL, "
                        + "transaction_height INT NOT NULL, fee_per_byte BIGINT NOT NULL, "
                        + "transaction_bytes VARBINARY NOT NULL, height INT NOT NULL)");
            case 122:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS unconfirmed_transaction_id_idx ON unconfirmed_transaction (id)");
            case 123:
                apply(null);
            case 124:
                apply("CREATE TABLE IF NOT EXISTS asset_transfer (db_id IDENTITY, id BIGINT NOT NULL, asset_id BIGINT NOT NULL, "
                        + "sender_id BIGINT NOT NULL, recipient_id BIGINT NOT NULL, quantity BIGINT NOT NULL, timestamp INT NOT NULL, "
                        + "height INT NOT NULL)");
            case 125:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_transfer_id_idx ON asset_transfer (id)");
            case 126:
                apply("CREATE INDEX IF NOT EXISTS asset_transfer_asset_id_idx ON asset_transfer (asset_id, height DESC)");
            case 127:
                apply("CREATE INDEX IF NOT EXISTS asset_transfer_sender_id_idx ON asset_transfer (sender_id, height DESC)");
            case 128:
                apply("CREATE INDEX IF NOT EXISTS asset_transfer_recipient_id_idx ON asset_transfer (recipient_id, height DESC)");
            case 129:
                apply(null);
            case 130:
                apply("CREATE INDEX IF NOT EXISTS account_asset_quantity_idx ON account_asset (quantity DESC)");
            case 131:
                apply("CREATE INDEX IF NOT EXISTS purchase_timestamp_idx ON purchase (timestamp DESC, id)");
            case 132:
                apply("CREATE INDEX IF NOT EXISTS ask_order_creation_idx ON ask_order (creation_height DESC)");
            case 133:
                apply("CREATE INDEX IF NOT EXISTS bid_order_creation_idx ON bid_order (creation_height DESC)");
            case 134:
                apply(null);
            case 135:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS block_timestamp_idx ON block (timestamp DESC)");
            case 136:
                apply(null);
            case 137:
                apply("ALTER TABLE goods ADD COLUMN IF NOT EXISTS parsed_tags ARRAY");
            case 138:
                apply("CREATE ALIAS IF NOT EXISTS FTL_INIT FOR \"org.h2.fulltext.FullTextLucene.init\"");
            case 139:
                apply("CALL FTL_INIT()");
            case 140:
                apply(null);
            case 141:
                apply(null);
            case 142:
                apply("CREATE TABLE IF NOT EXISTS tag (db_id IDENTITY, tag VARCHAR NOT NULL, in_stock_count INT NOT NULL, "
                        + "total_count INT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 143:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS tag_tag_idx ON tag (tag, height DESC)");
            case 144:
                apply("CREATE INDEX IF NOT EXISTS tag_in_stock_count_idx ON tag (in_stock_count DESC, height DESC)");
            case 145:
                apply(null);
            case 146:
                apply("CREATE TABLE IF NOT EXISTS currency (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "name VARCHAR NOT NULL, name_lower VARCHAR AS LOWER (name) NOT NULL, code VARCHAR NOT NULL, "
                        + "description VARCHAR, type INT NOT NULL, initial_supply BIGINT NOT NULL DEFAULT 0, "
                        + "reserve_supply BIGINT NOT NULL, max_supply BIGINT NOT NULL, creation_height INT NOT NULL, issuance_height INT NOT NULL, "
                        + "min_reserve_per_unit_nqt BIGINT NOT NULL, min_difficulty TINYINT NOT NULL, "
                        + "max_difficulty TINYINT NOT NULL, ruleset TINYINT NOT NULL, algorithm TINYINT NOT NULL, "
                        + "decimals TINYINT NOT NULL DEFAULT 0,"
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 147:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_id_height_idx ON currency (id, height DESC)");
            case 148:
                apply("CREATE INDEX IF NOT EXISTS currency_account_id_idx ON currency (account_id)");
            case 149:
                apply("CREATE TABLE IF NOT EXISTS account_currency (db_id IDENTITY, account_id BIGINT NOT NULL, "
                        + "currency_id BIGINT NOT NULL, units BIGINT NOT NULL, unconfirmed_units BIGINT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 150:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_currency_id_height_idx ON account_currency (account_id, currency_id, height DESC)");
            case 151:
                apply("CREATE TABLE IF NOT EXISTS currency_founder (db_id IDENTITY, currency_id BIGINT NOT NULL, "
                        + "account_id BIGINT NOT NULL, amount BIGINT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 152:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_founder_currency_id_idx ON currency_founder (currency_id, account_id, height DESC)");
            case 153:
                apply("CREATE TABLE IF NOT EXISTS currency_mint (db_id IDENTITY, currency_id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "counter BIGINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 154:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_mint_currency_id_account_id_idx ON currency_mint (currency_id, account_id, height DESC)");
            case 155:
                apply("CREATE TABLE IF NOT EXISTS buy_offer (db_id IDENTITY, id BIGINT NOT NULL, currency_id BIGINT NOT NULL, account_id BIGINT NOT NULL,"
                        + "rate BIGINT NOT NULL, unit_limit BIGINT NOT NULL, supply BIGINT NOT NULL, expiration_height INT NOT NULL,"
                        + "creation_height INT NOT NULL, transaction_index SMALLINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 156:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS buy_offer_id_idx ON buy_offer (id, height DESC)");
            case 157:
                apply("CREATE INDEX IF NOT EXISTS buy_offer_currency_id_account_id_idx ON buy_offer (currency_id, account_id, height DESC)");
            case 158:
                apply("CREATE TABLE IF NOT EXISTS sell_offer (db_id IDENTITY, id BIGINT NOT NULL, currency_id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "rate BIGINT NOT NULL, unit_limit BIGINT NOT NULL, supply BIGINT NOT NULL, expiration_height INT NOT NULL, "
                        + "creation_height INT NOT NULL, transaction_index SMALLINT NOT NULL, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 159:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS sell_offer_id_idx ON sell_offer (id, height DESC)");
            case 160:
                apply("CREATE INDEX IF NOT EXISTS sell_offer_currency_id_account_id_idx ON sell_offer (currency_id, account_id, height DESC)");
            case 161:
                apply("CREATE TABLE IF NOT EXISTS exchange (db_id IDENTITY, transaction_id BIGINT NOT NULL, currency_id BIGINT NOT NULL, block_id BIGINT NOT NULL, "
                        + "offer_id BIGINT NOT NULL, seller_id BIGINT NOT NULL, "
                        + "buyer_id BIGINT NOT NULL, units BIGINT NOT NULL, "
                        + "rate BIGINT NOT NULL, timestamp INT NOT NULL, height INT NOT NULL)");
            case 162:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS exchange_offer_idx ON exchange (transaction_id, offer_id)");
            case 163:
                apply("CREATE INDEX IF NOT EXISTS exchange_currency_id_idx ON exchange (currency_id, height DESC)");
            case 164:
                apply("CREATE INDEX IF NOT EXISTS exchange_seller_id_idx ON exchange (seller_id, height DESC)");
            case 165:
                apply("CREATE INDEX IF NOT EXISTS exchange_buyer_id_idx ON exchange (buyer_id, height DESC)");
            case 166:
                apply("CREATE TABLE IF NOT EXISTS currency_transfer (db_id IDENTITY, id BIGINT NOT NULL, currency_id BIGINT NOT NULL, "
                        + "sender_id BIGINT NOT NULL, recipient_id BIGINT NOT NULL, units BIGINT NOT NULL, timestamp INT NOT NULL, "
                        + "height INT NOT NULL)");
            case 167:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_transfer_id_idx ON currency_transfer (id)");
            case 168:
                apply("CREATE INDEX IF NOT EXISTS currency_transfer_currency_id_idx ON currency_transfer (currency_id, height DESC)");
            case 169:
                apply("CREATE INDEX IF NOT EXISTS currency_transfer_sender_id_idx ON currency_transfer (sender_id, height DESC)");
            case 170:
                apply("CREATE INDEX IF NOT EXISTS currency_transfer_recipient_id_idx ON currency_transfer (recipient_id, height DESC)");
            case 171:
                apply("CREATE INDEX IF NOT EXISTS account_currency_units_idx ON account_currency (units DESC)");
            case 172:
                apply("CREATE INDEX IF NOT EXISTS currency_name_idx ON currency (name_lower, height DESC)");
            case 173:
                apply("CREATE INDEX IF NOT EXISTS currency_code_idx ON currency (code, height DESC)");
            case 174:
                apply("CREATE INDEX IF NOT EXISTS buy_offer_rate_height_idx ON buy_offer (rate DESC, creation_height ASC)");
            case 175:
                apply("CREATE INDEX IF NOT EXISTS sell_offer_rate_height_idx ON sell_offer (rate ASC, creation_height ASC)");
            case 176:
                apply(null);
            case 177:
                apply(null);
            case 178:
                apply("DROP INDEX IF EXISTS unconfirmed_transaction_height_fee_timestamp_idx");
            case 179:
                apply("ALTER TABLE unconfirmed_transaction DROP COLUMN IF EXISTS timestamp");
            case 180:
                apply("ALTER TABLE unconfirmed_transaction ADD COLUMN IF NOT EXISTS arrival_timestamp BIGINT NOT NULL DEFAULT 0");
            case 181:
                apply("CREATE INDEX IF NOT EXISTS unconfirmed_transaction_height_fee_timestamp_idx ON unconfirmed_transaction "
                        + "(transaction_height ASC, fee_per_byte DESC, arrival_timestamp ASC)");
            case 182:
                /* unfortunately so .. */
                BlockDb.deleteAll();
                apply(null);
            case 183:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS transaction_index SMALLINT NOT NULL");
            case 184:
                apply(null);
            case 185:
                apply("TRUNCATE TABLE ask_order");
            case 186:
                apply("ALTER TABLE ask_order ADD COLUMN IF NOT EXISTS transaction_index SMALLINT NOT NULL");
            case 187:
                apply(null);
            case 188:
                apply("TRUNCATE TABLE bid_order");
            case 189:
                apply("ALTER TABLE bid_order ADD COLUMN IF NOT EXISTS transaction_index SMALLINT NOT NULL");
            case 190:
                apply(null);
            case 191:
                apply(null);
            case 192:
                apply("CREATE TABLE IF NOT EXISTS scan (rescan BOOLEAN NOT NULL DEFAULT FALSE, height INT NOT NULL DEFAULT 0, "
                        + "validate BOOLEAN NOT NULL DEFAULT FALSE)");
            case 193:
                apply("INSERT INTO scan (rescan, height, validate) VALUES (false, 0, false)");
            case 194:
                apply("CREATE INDEX IF NOT EXISTS currency_creation_height_idx ON currency (creation_height DESC)");
            case 195:
                apply(null);
            case 196:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS transaction_timestamp_desc_idx ON transaction (timestamp DESC)");
            case 197:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS trade_timestamp_desc_idx ON trade (timestamp DESC)");
            case 198:
                /* FIMKrypto */
                apply(null);
            case 199:
                /* FIMKrypto */
                apply(null);
            case 200:
                /* FIMKrypto */
                apply(null);
            case 201:
                /* FIMKrypto */
                apply(null);
            case 202:
                /* FIMKrypto */
                apply(null);
            case 203:
                /* FIMKrypto */
                apply("CREATE TABLE IF NOT EXISTS mofo_post ( "
                    + "type TINYINT NOT NULL, timestamp INT NOT NULL, sender_account_id BIGINT NOT NULL, "
                    + "referenced_entity_id BIGINT NOT NULL, transaction_id BIGINT NOT NULL, "
                    + "FOREIGN KEY (transaction_id) REFERENCES transaction (id) ON DELETE CASCADE)");
            case 204:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS mofo_post_timestamp_desc_idx ON mofo_post (timestamp DESC)");
            case 205:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS mofo_post_type_idx ON mofo_post (type)");
            case 206:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS mofo_post_sender_account_id_idx ON mofo_post (sender_account_id)");
            case 207:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS mofo_post_referenced_entity_id_idx ON mofo_post (referenced_entity_id)");
            case 208:
                /* FIMKrypto */
                apply("CREATE TABLE IF NOT EXISTS mofo_comment ( "
                    + "timestamp INT NOT NULL, post_transaction_id BIGINT NOT NULL, transaction_id BIGINT NOT NULL, "
                    + "sender_account_id BIGINT NOT NULL, "
                    + "FOREIGN KEY (transaction_id) REFERENCES transaction (id) ON DELETE CASCADE)");
            case 209:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS mofo_comment_timestamp_idx ON mofo_comment (timestamp)");
            case 210:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS mofo_comment_sender_account_id_idx ON mofo_comment (sender_account_id)");
            case 211:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS mofo_comment_post_transaction_id_idx ON mofo_comment (post_transaction_id)");
            case 212:
                apply(null);
            case 213:
                apply("CREATE TABLE IF NOT EXISTS currency_supply (db_id IDENTITY, id BIGINT NOT NULL, "
                        + "current_supply BIGINT NOT NULL, current_reserve_per_unit_nqt BIGINT NOT NULL, height INT NOT NULL, "
                        + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 214:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS currency_supply_id_height_idx ON currency_supply (id, height DESC)");
            case 215:
                apply("TRUNCATE TABLE currency");
            case 216:
                apply("ALTER TABLE currency DROP COLUMN IF EXISTS current_supply");
            case 217:
                apply("ALTER TABLE currency DROP COLUMN IF EXISTS current_reserve_per_unit_nqt");
            case 218:
                apply(null);
            case 219:
                apply(null);
            case 220:
                apply("CREATE TABLE IF NOT EXISTS public_key (db_id IDENTITY, account_id BIGINT NOT NULL, "
                        + "public_key BINARY(32), height INT NOT NULL, FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE)");
            case 221:
                apply(null);
            case 222:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS public_key_account_id_idx ON public_key (account_id)");
            case 223:
                apply("ALTER TABLE account DROP COLUMN IF EXISTS public_key");
            case 224:
                apply("ALTER TABLE block DROP COLUMN IF EXISTS generator_public_key");
            case 225:
                apply("ALTER TABLE transaction DROP COLUMN IF EXISTS sender_public_key");
            case 226:
                apply(null);
            case 227:
                apply(null);
            case 228:
                apply(null);
            case 229:
                apply("CREATE INDEX IF NOT EXISTS account_guaranteed_balance_height_idx ON account_guaranteed_balance(height)");
            case 230:
                apply(null);
            case 231:
                apply(null);
            case 232:
                apply(null);
            case 233:
                apply("CREATE INDEX IF NOT EXISTS asset_height_idx ON asset(height)");
            case 234:
                apply("CREATE INDEX IF NOT EXISTS asset_transfer_height_idx ON asset_transfer(height)");
            case 235:
                apply(null);
            case 236:
                apply(null);
            case 237:
                apply(null);
            case 238:
                apply(null);
            case 239:
                apply(null);
            case 240:
                apply(null);
            case 241:
                apply("CREATE INDEX IF NOT EXISTS currency_transfer_height_idx ON currency_transfer(height)");
            case 242:
                apply("CREATE INDEX IF NOT EXISTS exchange_height_idx ON exchange(height)");
            case 243:
                apply(null);
            case 244:
                apply(null);
            case 245:
                apply(null);
            case 246:
                apply(null);
            case 247:
                apply(null);
            case 248:
                apply(null);
            case 249:
                apply(null);
            case 250:
                apply("CREATE INDEX IF NOT EXISTS trade_height_idx ON trade(height)");
            case 251:
                apply(null);
            case 252:
                apply(null);
            case 253:
                apply(null);
            case 254:
                apply(null);
            case 255:
                /* FIMKrypto */
                apply("ALTER TABLE asset ADD COLUMN IF NOT EXISTS type TINYINT");
            case 256:
                /* FIMKrypto */
                apply("UPDATE asset SET type = 0");
            case 257:
                /* FIMKrypto */
                apply("CREATE TABLE IF NOT EXISTS private_asset (db_id IDENTITY, asset_id BIGINT NOT NULL, "
                    + "order_fee_percentage INT NOT NULL, trade_fee_percentage INT NOT NULL, height INT NOT NULL, "
                    + "FOREIGN KEY (asset_id) REFERENCES asset (id) ON DELETE CASCADE, "
                    + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 258:
                /* FIMKrypto */
                apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_id_height_idx ON private_asset (asset_id, height DESC)");
            case 259:
                /* FIMKrypto */
                apply("CREATE TABLE IF NOT EXISTS private_asset_account (db_id IDENTITY, account_id BIGINT NOT NULL, "
                    + "asset_id BIGINT NOT NULL, allowed BOOLEAN NOT NULL DEFAULT TRUE, height INT NOT NULL, "
                    + "latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 260:
                /* FIMKrypto */
                apply("CREATE UNIQUE INDEX IF NOT EXISTS asset_id_account_id_height_idx ON private_asset_account (asset_id, account_id, height DESC)");
            case 261:
                /* FIMKrypto */
                apply("CREATE TABLE IF NOT EXISTS account_identifier (db_id IDENTITY, "
                    + "account_id BIGINT NOT NULL, email VARCHAR NOT NULL, "
                    + "email_lower VARCHAR AS LOWER (email) NOT NULL, height INT NOT NULL)");
            case 262:
                /* FIMKrypto */
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_identifier_email_idx ON account_identifier (email_lower)");
            case 263:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS account_identifier_account_id_idx ON account_identifier (account_id)");
            case 264:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS account_identifier_height_idx ON account_identifier (height DESC)");
            case 265:
                /* FIMKrypto */
                apply(null);
            case 266:
                /* FIMKrypto */
                apply("CREATE TABLE IF NOT EXISTS verification_authority (db_id IDENTITY, "
                    + "account_id BIGINT NOT NULL, period INT NOT NULL, "
                    + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 267:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS verification_authority_account_id_idx ON verification_authority (account_id, height DESC)");
            case 268:
                apply("DROP TABLE IF EXISTS poll");
            case 269:
                apply("DROP TABLE IF EXISTS vote");
            case 270:
                apply("CREATE TABLE IF NOT EXISTS vote (db_id IDENTITY, id BIGINT NOT NULL, " +
                        "poll_id BIGINT NOT NULL, voter_id BIGINT NOT NULL, vote_bytes VARBINARY NOT NULL, height INT NOT NULL)");
            case 271:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS vote_id_idx ON vote (id)");
            case 272:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS vote_poll_id_idx ON vote (poll_id, voter_id)");
            case 273:
                apply("CREATE TABLE IF NOT EXISTS poll (db_id IDENTITY, id BIGINT NOT NULL, "
                        + "account_id BIGINT NOT NULL, name VARCHAR NOT NULL, "
                        + "description VARCHAR, options ARRAY NOT NULL, min_num_options TINYINT, max_num_options TINYINT, "
                        + "min_range_value TINYINT, max_range_value TINYINT, "
                        + "finish_height INT NOT NULL, voting_model TINYINT NOT NULL, min_balance BIGINT, "
                        + "min_balance_model TINYINT, holding_id BIGINT, height INT NOT NULL)");
            case 274:
                apply("CREATE TABLE IF NOT EXISTS poll_result (db_id IDENTITY, poll_id BIGINT NOT NULL, "
                        + "result BIGINT, weight BIGINT NOT NULL, height INT NOT NULL)");
            case 275:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS phased BOOLEAN NOT NULL DEFAULT FALSE");
            case 276:
                apply("CREATE TABLE IF NOT EXISTS phasing_poll (db_id IDENTITY, id BIGINT NOT NULL, "
                        + "account_id BIGINT NOT NULL, whitelist_size TINYINT NOT NULL DEFAULT 0, "
                        + "finish_height INT NOT NULL, voting_model TINYINT NOT NULL, quorum BIGINT, "
                        + "min_balance BIGINT, holding_id BIGINT, min_balance_model TINYINT, "
                        + "linked_full_hashes ARRAY, hashed_secret VARBINARY, algorithm TINYINT, height INT NOT NULL)");
            case 277:
                apply("CREATE TABLE IF NOT EXISTS phasing_vote (db_id IDENTITY, vote_id BIGINT NOT NULL, "
                        + "transaction_id BIGINT NOT NULL, voter_id BIGINT NOT NULL, "
                        + "height INT NOT NULL)");
            case 278:
                apply("CREATE TABLE IF NOT EXISTS phasing_poll_voter (db_id IDENTITY, "
                        + "transaction_id BIGINT NOT NULL, voter_id BIGINT NOT NULL, "
                        + "height INT NOT NULL)");
            case 279:
                apply("CREATE INDEX IF NOT EXISTS vote_height_idx ON vote(height)");
            case 280:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS poll_id_idx ON poll(id)");
            case 281:
                apply("CREATE INDEX IF NOT EXISTS poll_height_idx ON poll(height)");
            case 282:
                apply("CREATE INDEX IF NOT EXISTS poll_account_idx ON poll(account_id)");
            case 283:
                apply("CREATE INDEX IF NOT EXISTS poll_finish_height_idx ON poll(finish_height DESC)");
            case 284:
                apply("CREATE INDEX IF NOT EXISTS poll_result_poll_id_idx ON poll_result(poll_id)");
            case 285:
                apply("CREATE INDEX IF NOT EXISTS poll_result_height_idx ON poll_result(height)");
            case 286:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS phasing_poll_id_idx ON phasing_poll(id)");
            case 287:
                apply("CREATE INDEX IF NOT EXISTS phasing_poll_height_idx ON phasing_poll(height)");
            case 288:
                apply("CREATE INDEX IF NOT EXISTS phasing_poll_account_id_idx ON phasing_poll(account_id, height DESC)");
            case 289:
                apply("CREATE INDEX IF NOT EXISTS phasing_poll_holding_id_idx ON phasing_poll(holding_id, height DESC)");
            case 290:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS phasing_vote_transaction_voter_idx ON phasing_vote(transaction_id, voter_id)");
            case 291:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS phasing_poll_voter_transaction_voter_idx ON phasing_poll_voter(transaction_id, voter_id)");
            case 292:
                apply("CREATE TABLE IF NOT EXISTS phasing_poll_result (db_id IDENTITY, id BIGINT NOT NULL, "
                        + "result BIGINT NOT NULL, approved BOOLEAN NOT NULL, height INT NOT NULL)");
            case 293:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS phasing_poll_result_id_idx ON phasing_poll_result(id)");
            case 294:
                apply("CREATE INDEX IF NOT EXISTS phasing_poll_result_height_idx ON phasing_poll_result(height)");
            case 295:
                apply("CREATE INDEX IF NOT EXISTS currency_founder_account_id_idx ON currency_founder (account_id, height DESC)");
            case 296:
                apply("TRUNCATE TABLE trade");
            case 297:
                apply("ALTER TABLE trade ADD COLUMN IF NOT EXISTS is_buy BOOLEAN NOT NULL");
            case 298:
                apply("CREATE INDEX IF NOT EXISTS phasing_poll_voter_height_idx ON phasing_poll_voter(height)");
            case 299:
                apply("TRUNCATE TABLE ask_order");
            case 300:
                apply("ALTER TABLE ask_order ADD COLUMN IF NOT EXISTS transaction_height INT NOT NULL");
            case 301:
                apply("TRUNCATE TABLE bid_order");
            case 302:
                apply("ALTER TABLE bid_order ADD COLUMN IF NOT EXISTS transaction_height INT NOT NULL");
            case 303:
                apply("TRUNCATE TABLE buy_offer");
            case 304:
                apply("ALTER TABLE buy_offer ADD COLUMN IF NOT EXISTS transaction_height INT NOT NULL");
            case 305:
                apply("TRUNCATE TABLE sell_offer");
            case 306:
                apply("ALTER TABLE sell_offer ADD COLUMN IF NOT EXISTS transaction_height INT NOT NULL");
            case 307:
                apply("CREATE INDEX IF NOT EXISTS phasing_vote_height_idx ON phasing_vote(height)");
            case 308:
                apply("DROP INDEX IF EXISTS transaction_full_hash_idx");
            case 309:
                apply("DROP INDEX IF EXISTS trade_ask_bid_idx");
            case 310:
                apply("CREATE INDEX IF NOT EXISTS trade_ask_idx ON trade (ask_order_id, height DESC)");
            case 311:
                apply("CREATE INDEX IF NOT EXISTS trade_bid_idx ON trade (bid_order_id, height DESC)");
            case 312:
                apply("CREATE TABLE IF NOT EXISTS account_info (db_id IDENTITY, account_id BIGINT NOT NULL, "
                        + "name VARCHAR, description VARCHAR, height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 313:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_info_id_height_idx ON account_info (account_id, height DESC)");
            case 314:
                apply(null);
            case 315:
                apply("ALTER TABLE account DROP COLUMN IF EXISTS name");
            case 316:
                apply("ALTER TABLE account DROP COLUMN IF EXISTS description");
            case 317:
                apply("ALTER TABLE account DROP COLUMN IF EXISTS message_pattern_regex");
            case 318:
                apply("ALTER TABLE account DROP COLUMN IF EXISTS message_pattern_flags");
            case 319:
                apply(null);
            case 320:
                apply("TRUNCATE TABLE poll");
            case 321:
                apply("ALTER TABLE poll ADD COLUMN IF NOT EXISTS timestamp INT NOT NULL");
            case 322:
                apply(null);
            case 323:
                apply("CREATE TABLE IF NOT EXISTS prunable_message (db_id IDENTITY, id BIGINT NOT NULL, sender_id BIGINT NOT NULL, "
                        + "recipient_id BIGINT, message VARBINARY NOT NULL, is_text BOOLEAN NOT NULL, is_compressed BOOLEAN NOT NULL, "
                        + "is_encrypted BOOLEAN NOT NULL, timestamp INT NOT NULL, expiration INT NOT NULL, height INT NOT NULL, "
                        + "FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE)");
            case 324:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS prunable_message_id_idx ON prunable_message (id)");
            case 325:
                apply(null);
            case 326:
                apply("CREATE INDEX IF NOT EXISTS prunable_message_expiration_idx ON prunable_message (expiration DESC)");
            case 327:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_prunable_message BOOLEAN NOT NULL DEFAULT FALSE");
            case 328:
                apply("TRUNCATE TABLE unconfirmed_transaction");
            case 329:
                apply("ALTER TABLE unconfirmed_transaction ADD COLUMN IF NOT EXISTS prunable_json VARCHAR");
            case 330:
                apply("CREATE INDEX IF NOT EXISTS prunable_message_sender_idx ON prunable_message (sender_id)");
            case 331:
                apply("CREATE INDEX IF NOT EXISTS prunable_message_recipient_idx ON prunable_message (recipient_id)");
            case 332:
                apply(null);
            case 333:
                apply("ALTER TABLE transaction ADD COLUMN IF NOT EXISTS has_prunable_encrypted_message BOOLEAN NOT NULL DEFAULT FALSE");
            case 334:
                apply(null);
            case 335:
                apply("ALTER TABLE prunable_message ALTER COLUMN expiration RENAME TO transaction_timestamp");
            case 336:
                apply("UPDATE prunable_message SET transaction_timestamp = SELECT timestamp FROM transaction WHERE prunable_message.id = transaction.id");
            case 337:
                apply("ALTER INDEX prunable_message_expiration_idx RENAME TO prunable_message_transaction_timestamp_idx");
            case 338:
                apply("ALTER TABLE prunable_message ALTER COLUMN timestamp RENAME TO block_timestamp");
            case 339:
                apply("DROP INDEX IF EXISTS prunable_message_timestamp_idx");
            case 340:
                apply("CREATE INDEX IF NOT EXISTS prunable_message_block_timestamp_dbid_idx ON prunable_message (block_timestamp DESC, db_id DESC)");
            case 341:
                apply("DROP INDEX IF EXISTS prunable_message_height_idx");
            case 342:
                apply("DROP INDEX IF EXISTS public_key_height_idx");
            case 343:
                apply("CREATE TABLE IF NOT EXISTS tagged_data (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                        + "name VARCHAR NOT NULL, description VARCHAR, tags VARCHAR, parsed_tags ARRAY, type VARCHAR, data VARBINARY NOT NULL, "
                        + "is_text BOOLEAN NOT NULL, filename VARCHAR, block_timestamp INT NOT NULL, transaction_timestamp INT NOT NULL, "
                        + "height INT NOT NULL, FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 344:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS tagged_data_id_height_idx ON tagged_data (id, height DESC)");
            case 345:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_expiration_idx ON tagged_data (transaction_timestamp DESC)");
            case 346:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_account_id_height_idx ON tagged_data (account_id, height DESC)");
            case 347:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_block_timestamp_height_db_id_idx ON tagged_data (block_timestamp DESC, height DESC, db_id DESC)");
            case 348:
                apply("CALL FTL_CREATE_INDEX('PUBLIC', 'TAGGED_DATA', 'NAME,DESCRIPTION,TAGS')");
            case 349:
                apply("CREATE TABLE IF NOT EXISTS data_tag (db_id IDENTITY, tag VARCHAR NOT NULL, tag_count INT NOT NULL, "
                        + "height INT NOT NULL, FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 350:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS data_tag_tag_height_idx ON data_tag (tag, height DESC)");
            case 351:
                apply("CREATE INDEX IF NOT EXISTS data_tag_count_height_idx ON data_tag (tag_count DESC, height DESC)");
            case 352:
                apply("CREATE TABLE IF NOT EXISTS tagged_data_timestamp (db_id IDENTITY, id BIGINT NOT NULL, timestamp INT NOT NULL, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 353:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS tagged_data_timestamp_id_height_idx ON tagged_data_timestamp (id, height DESC)");
            case 354:
                apply(null);
            case 355:
                apply(null);
            case 356:
                apply(null);
            case 357:
                apply("ALTER TABLE tagged_data ADD COLUMN IF NOT EXISTS channel VARCHAR");
            case 358:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_channel_idx ON tagged_data (channel, height DESC)");
            case 359:
                apply("ALTER TABLE peer ADD COLUMN IF NOT EXISTS last_updated INT");
            case 360:
                apply("DROP INDEX IF EXISTS account_current_lessee_id_leasing_height_idx");
            case 361:
                apply("TRUNCATE TABLE account");
            case 362:
                apply("ALTER TABLE account ADD COLUMN IF NOT EXISTS active_lessee_id BIGINT");
            case 363:
                apply("ALTER TABLE account DROP COLUMN IF EXISTS current_leasing_height_from");
            case 364:
                apply("ALTER TABLE account DROP COLUMN IF EXISTS current_leasing_height_to");
            case 365:
                apply("ALTER TABLE account DROP COLUMN IF EXISTS current_lessee_id");
            case 366:
                apply("ALTER TABLE account DROP COLUMN IF EXISTS next_leasing_height_from");
            case 367:
                apply("ALTER TABLE account DROP COLUMN IF EXISTS next_leasing_height_to");
            case 368:
                apply("ALTER TABLE account DROP COLUMN IF EXISTS next_lessee_id");
            case 369:
                apply("CREATE INDEX IF NOT EXISTS account_active_lessee_id_idx ON account (active_lessee_id)");
            case 370:
                apply("CREATE TABLE IF NOT EXISTS account_lease (db_id IDENTITY, lessor_id BIGINT NOT NULL, "
                        + "current_leasing_height_from INT, current_leasing_height_to INT, current_lessee_id BIGINT, "
                        + "next_leasing_height_from INT, next_leasing_height_to INT, next_lessee_id BIGINT, "
                        + "height INT NOT NULL, latest BOOLEAN NOT NULL DEFAULT TRUE)");
            case 371:
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_lease_lessor_id_height_idx ON account_lease (lessor_id, height DESC)");
            case 372:
                apply("CREATE INDEX IF NOT EXISTS account_lease_current_leasing_height_from_idx ON account_lease (current_leasing_height_from)");
            case 373:
                apply("CREATE INDEX IF NOT EXISTS account_lease_current_leasing_height_to_idx ON account_lease (current_leasing_height_to)");
            case 374:
                apply("CREATE INDEX IF NOT EXISTS account_lease_height_id_idx ON account_lease (height, lessor_id)");
            case 375:
                apply("CREATE INDEX IF NOT EXISTS account_asset_asset_id_idx ON account_asset (asset_id)");
            case 376:
                apply("CREATE INDEX IF NOT EXISTS account_currency_currency_id_idx ON account_currency (currency_id)");
            case 377:
                apply("CREATE INDEX IF NOT EXISTS currency_issuance_height_idx ON currency (issuance_height)");
            case 378:
                apply("CREATE INDEX IF NOT EXISTS unconfirmed_transaction_expiration_idx ON unconfirmed_transaction (expiration DESC)");
            case 379:
                apply("DROP INDEX IF EXISTS account_height_idx");
            case 380:
                apply("CREATE INDEX IF NOT EXISTS account_height_id_idx ON account (height, id)");
            case 381:
                apply("DROP INDEX IF EXISTS account_asset_height_idx");
            case 382:
                apply("CREATE INDEX IF NOT EXISTS account_asset_height_id_idx ON account_asset (height, account_id, asset_id)");
            case 383:
                apply("DROP INDEX IF EXISTS account_currency_height_idx");
            case 384:
                apply("CREATE INDEX IF NOT EXISTS account_currency_height_id_idx ON account_currency (height, account_id, currency_id)");
            case 385:
                apply("DROP INDEX IF EXISTS alias_height_idx");
            case 386:
                apply("CREATE INDEX IF NOT EXISTS alias_height_id_idx ON alias (height, id)");
            case 387:
                apply("DROP INDEX IF EXISTS alias_offer_height_idx");
            case 388:
                apply("CREATE INDEX IF NOT EXISTS alias_offer_height_id_idx ON alias_offer (height, id)");
            case 389:
                apply("DROP INDEX IF EXISTS ask_order_height_idx");
            case 390:
                apply("CREATE INDEX IF NOT EXISTS ask_order_height_id_idx ON ask_order (height, id)");
            case 391:
                apply("DROP INDEX IF EXISTS bid_order_height_idx");
            case 392:
                apply("CREATE INDEX IF NOT EXISTS bid_order_height_id_idx ON bid_order (height, id)");
            case 393:
                apply("DROP INDEX IF EXISTS buy_offer_height_idx");
            case 394:
                apply("CREATE INDEX IF NOT EXISTS buy_offer_height_id_idx ON buy_offer (height, id)");
            case 395:
                apply("DROP INDEX IF EXISTS currency_height_idx");
            case 396:
                apply("CREATE INDEX IF NOT EXISTS currency_height_id_idx ON currency (height, id)");
            case 397:
                apply("DROP INDEX IF EXISTS currency_founder_height_idx");
            case 398:
                apply("CREATE INDEX IF NOT EXISTS currency_founder_height_id_idx ON currency_founder (height, currency_id, account_id)");
            case 399:
                apply("DROP INDEX IF EXISTS currency_mint_height_idx");
            case 400:
                apply("CREATE INDEX IF NOT EXISTS currency_mint_height_id_idx ON currency_mint (height, currency_id, account_id)");
            case 401:
                apply("DROP INDEX IF EXISTS currency_supply_height_idx");
            case 402:
                apply("CREATE INDEX IF NOT EXISTS currency_supply_height_id_idx ON currency_supply (height, id)");
            case 403:
                apply("DROP INDEX IF EXISTS goods_height_idx");
            case 404:
                apply("CREATE INDEX IF NOT EXISTS goods_height_id_idx ON goods (height, id)");
            case 405:
                apply("DROP INDEX IF EXISTS purchase_height_idx");
            case 406:
                apply("CREATE INDEX IF NOT EXISTS purchase_height_id_idx ON purchase (height, id)");
            case 407:
                apply("DROP INDEX IF EXISTS purchase_feedback_height_idx");
            case 408:
                apply("CREATE INDEX IF NOT EXISTS purchase_feedback_height_id_idx ON purchase_feedback (height, id)");
            case 409:
                apply("DROP INDEX IF EXISTS purchase_public_feedback_height_idx");
            case 410:
                apply("CREATE INDEX IF NOT EXISTS purchase_public_feedback_height_id_idx ON purchase_public_feedback (height, id)");
            case 411:
                apply("DROP INDEX IF EXISTS sell_offer_height_idx");
            case 412:
                apply("CREATE INDEX IF NOT EXISTS sell_offer_height_id_idx ON sell_offer (height, id)");
            case 413:
                apply("DROP INDEX IF EXISTS tag_height_idx");
            case 414:
                apply("CREATE INDEX IF NOT EXISTS tag_height_tag_idx ON tag (height, tag)");
            case 415:
                apply("DROP INDEX IF EXISTS account_info_height_idx");
            case 416:
                apply("CREATE INDEX IF NOT EXISTS account_info_height_id_idx ON account_info (height, account_id)");
            case 417:
                apply("DROP INDEX IF EXISTS tagged_data_timestamp_height_idx");
            case 418:
                apply("CREATE INDEX IF NOT EXISTS tagged_data_timestamp_height_id_idx ON tagged_data_timestamp (height, id)");
            case 419:
                apply("CREATE INDEX IF NOT EXISTS trade_height_db_id_idx ON trade (height DESC, db_id DESC)");
            case 420:
                apply("CREATE INDEX IF NOT EXISTS asset_height_db_id_idx ON asset (height DESC, db_id DESC)");
            case 421:
                apply("CREATE INDEX IF NOT EXISTS exchange_height_db_id_idx ON exchange (height DESC, db_id DESC)");
            case 422:
                /* FIMKrypto */
                apply(null);
            case 423:
                /* FIMKrypto */
                apply("ALTER TABLE account_identifier DROP COLUMN IF EXISTS transaction_id");
            case 424:
                /* FIMKrypto */
                apply("DROP INDEX IF EXISTS account_identifier_transaction_id_idx");
            case 425:
                apply(null);
            case 426:
                /* FIMKrypto */
                apply("CREATE TABLE IF NOT EXISTS account_color (db_id IDENTITY, id BIGINT NOT NULL, account_id BIGINT NOT NULL, "
                    + "name VARCHAR NOT NULL, description VARCHAR, height INT NOT NULL, "
                    + "name_lower VARCHAR AS LOWER (name) NOT NULL)");
            case 427:
                /* FIMKrypto */
                apply("CREATE UNIQUE INDEX IF NOT EXISTS account_color_id_idx ON account_color (id)");
            case 428:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS account_color_account_id_idx ON account_color (account_id)");
            case 429:
                /* FIMKrypto */
                apply("ALTER TABLE account ADD COLUMN IF NOT EXISTS account_color_id BIGINT");
            case 430:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS account_account_color_id_idx ON account (account_color_id)");
            case 431:
                /* FIMKrypto */
                apply("CREATE INDEX IF NOT EXISTS name_account_color_idx ON account_color (name_lower)");
            case 432:
                /* FIMKrypto */
                if (!Constants.isTestnet) {
                    BlockDb.deleteBlocksFromHeight(Constants.PRIVATE_ASSETS_BLOCK);
                }
                BlockchainProcessorImpl.getInstance().scheduleScan(0, true);
                apply(null);
            case 433:
                /* FIMKrypto */
                apply("ALTER TABLE asset ADD COLUMN IF NOT EXISTS expiry INT;" +
                        "ALTER TABLE goods ADD COLUMN IF NOT EXISTS expiry INT");
            case 434:
                /* FIMKrypto */
                apply("ALTER TABLE asset ADD COLUMN IF NOT EXISTS block_timestamp INT;");
                BlockchainProcessorImpl.getInstance().scheduleScan(0, false);
            case 435:
                apply("CREATE TABLE IF NOT EXISTS asset_rewarding (db_id IDENTITY, id BIGINT NOT NULL, " +
                        "asset_id BIGINT NOT NULL, height INT NOT NULL, frequency INT NOT NULL, target TINYINT NOT NULL, " +
                        "lotteryType TINYINT NOT NULL, baseAmount BIGINT NOT NULL, balanceDivider BIGINT NOT NULL, targetInfo BIGINT NOT NULL, " +
                        "FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE); " +
                        "CREATE UNIQUE INDEX IF NOT EXISTS asset_rewarding_asset_id_idx ON asset_rewarding (asset_id);");
            case 436:
                apply("CREATE TABLE IF NOT EXISTS reward_candidate (id BIGINT NOT NULL, " +
                        "height INT NOT NULL, asset_id BIGINT NOT NULL, account_id BIGINT NOT NULL, " +
                        "FOREIGN KEY (height) REFERENCES block (height) ON DELETE CASCADE); " +
                        "CREATE UNIQUE INDEX IF NOT EXISTS reward_candidate_asset_id_account_id_idx ON reward_candidate (asset_id, account_id);");
            case 437:
                apply("CREATE TABLE IF NOT EXISTS account_node (db_id IDENTITY, transaction_id BIGINT NOT NULL, height INT NOT NULL, " +
                        "address VARCHAR NOT NULL, account_id BIGINT NOT NULL, token VARCHAR NOT NULL, token_sender_id BIGINT NOT NULL," +
                        "score TINYINT NOT NULL, request_peer_timestamp INT NOT NULL, " +
                        "timestamp INT NOT NULL); " +
                        "CREATE UNIQUE INDEX IF NOT EXISTS account_node_address_account_id_idx " +
                        "ON account_node (address, account_id); " +
                        "CREATE INDEX IF NOT EXISTS account_node_timestamp_idx ON account_node (timestamp);");
            case 438:
                return;
            default:
                throw new RuntimeException("Blockchain database inconsistent with code, at update " + nextUpdate + ", probably trying to run older code on newer database");
        }
    }
}
