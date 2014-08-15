package nxt;

import java.util.Calendar;
import java.util.TimeZone;

public final class Constants {
  
    public static final boolean isTestnet = true; /* Nxt.getBooleanProperty("nxt.isTestnet"); */
  
    public static final long ONE_NXT = 100000000;
    
    /* XXX - MINIMUM FEE IS 0.1 FIM */
    public static final long MIN_FEE_NQT = ONE_NXT / 10;

    public static final int BLOCK_HEADER_LENGTH = 232;
    
    /* XXX - max transactions is 512 */
    public static final int MAX_NUMBER_OF_TRANSACTIONS = 512;
    public static final int MAX_PAYLOAD_LENGTH = MAX_NUMBER_OF_TRANSACTIONS * 160;
    
    /* XXX - number of seconds between blocks */
    public static final int SECONDS_BETWEEN_BLOCKS = 30;
    
    /* XXX - POS reward amounts and halving */
    public static final long FORGER_FEE_STAGE_CHANGE_AT_BLOCK = 889920;
    public static final long[] FORGER_FEE_AMOUNT_NQT_STAGES = { 200 * ONE_NXT, 100 * ONE_NXT, 50 * ONE_NXT, 25 * ONE_NXT };
    
    /* XXX - max balance is calculated from */
    public static final long MAX_BALANCE_NXT = isTestnet ? 999965465 : 999455619;
    public static final long MAX_BALANCE_NQT = MAX_BALANCE_NXT * ONE_NXT;
    
    /* XXX - adjust base target for MAX_BALANCE_NXT (192153584) */
    public static final long INITIAL_BASE_TARGET = isTestnet ? 307456352 : 307613193;
    
    public static final long MAX_BASE_TARGET = MAX_BALANCE_NXT * INITIAL_BASE_TARGET;

    public static final int MAX_ALIAS_URI_LENGTH = 1000;
    public static final int MAX_ALIAS_LENGTH = 100;

    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;

    public static final int MAX_ACCOUNT_NAME_LENGTH = 100;
    public static final int MAX_ACCOUNT_DESCRIPTION_LENGTH = 1000;

    public static final long MAX_ASSET_QUANTITY_QNT = 1000000000L * 100000000L;
    public static final long ASSET_ISSUANCE_FEE_NQT = 1000 * ONE_NXT;
    public static final int MIN_ASSET_NAME_LENGTH = 3;
    public static final int MAX_ASSET_NAME_LENGTH = 10;
    public static final int MAX_ASSET_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_ASSET_TRANSFER_COMMENT_LENGTH = 1000;

    public static final int MAX_POLL_NAME_LENGTH = 100;
    public static final int MAX_POLL_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_POLL_OPTION_LENGTH = 100;
    public static final int MAX_POLL_OPTION_COUNT = 100;

    public static final int MAX_DIGITAL_GOODS_QUANTITY = 1000000000;
    public static final int MAX_DIGITAL_GOODS_LISTING_NAME_LENGTH = 100;
    public static final int MAX_DIGITAL_GOODS_LISTING_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_DIGITAL_GOODS_LISTING_TAGS_LENGTH = 100;
    public static final int MAX_DIGITAL_GOODS_NOTE_LENGTH = 1000;
    public static final int MAX_DIGITAL_GOODS_LENGTH = 1000;

    public static final int MAX_HUB_ANNOUNCEMENT_URIS = 100;
    public static final int MAX_HUB_ANNOUNCEMENT_URI_LENGTH = 1000;
    public static final long MIN_HUB_EFFECTIVE_BALANCE = 100000;
    
    public static final int SECOND_BIRTH_BLOCK = 1;

    public static final int ALIAS_SYSTEM_BLOCK = SECOND_BIRTH_BLOCK;
    public static final int TRANSPARENT_FORGING_BLOCK = SECOND_BIRTH_BLOCK;
    public static final int ARBITRARY_MESSAGES_BLOCK = SECOND_BIRTH_BLOCK;
    public static final int TRANSPARENT_FORGING_BLOCK_2 = SECOND_BIRTH_BLOCK;
    public static final int TRANSPARENT_FORGING_BLOCK_3 = SECOND_BIRTH_BLOCK;
    public static final int TRANSPARENT_FORGING_BLOCK_4 = SECOND_BIRTH_BLOCK;
    public static final int TRANSPARENT_FORGING_BLOCK_5 = SECOND_BIRTH_BLOCK;
    public static final int TRANSPARENT_FORGING_BLOCK_6 = SECOND_BIRTH_BLOCK;
    
    /* XXX - Disable HUB_ANNOUNCEMENT for now */
    public static final int TRANSPARENT_FORGING_BLOCK_7 = Integer.MAX_VALUE;
    
    public static final int NQT_BLOCK = SECOND_BIRTH_BLOCK;
    public static final int FRACTIONAL_BLOCK = SECOND_BIRTH_BLOCK;
    public static final int ASSET_EXCHANGE_BLOCK = SECOND_BIRTH_BLOCK;
    public static final int REFERENCED_TRANSACTION_FULL_HASH_BLOCK = 0;
    
    /* XXX - Set REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP to 0 */    
    public static final int REFERENCED_TRANSACTION_FULL_HASH_BLOCK_TIMESTAMP = 0;
    
    /* XXX - Disable VOTING for now */
    public static final int VOTING_SYSTEM_BLOCK = Integer.MAX_VALUE;
    
    /* XXX - Disable Digital Goods Store for now */
    public static final int DIGITAL_GOODS_STORE_BLOCK = Integer.MAX_VALUE;

    /* XXX - Make UNCONFIRMED_POOL_DEPOSIT_NQT same on testnet as main net */
    public static final long UNCONFIRMED_POOL_DEPOSIT_NQT = 100 * ONE_NXT;
    
    /* XXX - FORGER_FEE_BLOCK, FORGER_FEE_AMOUNT_NQT */
    public static final int FORGER_FEE_BLOCK = SECOND_BIRTH_BLOCK + 1;

    public static final long EPOCH_BEGINNING;
    static {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, 2013);
        calendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        calendar.set(Calendar.DAY_OF_MONTH, 24);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        EPOCH_BEGINNING = calendar.getTimeInMillis();
    }

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";

    private Constants() {} // never

}
