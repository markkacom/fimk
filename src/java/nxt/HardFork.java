package nxt;

import nxt.util.Listener;

public class HardFork {

    /*
     * Hardforks are set through aliases. Normal flow of operation is to declare
     * an alias which DOES NOT hold a valid integer.
     *
     * When you decide the right block height to enable the fork set this height
     * to the alias you declared earlier.
     *
     * You can only set a fork height once! This ensures that it is not possible
     * to change a fork height after it was set.
     */

    // static long ACCOUNT_IDENTIFIER_BLOCK_2_ID = Long.parseUnsignedLong("10133298006531225081"); // 957300
    static long NAMESPACED_ALIAS_FIX_ID = Long.parseUnsignedLong("16302510207109928211");

    static ForkHeight[] forks = {
        // new ForkHeight(ACCOUNT_IDENTIFIER_BLOCK_2_ID),
        new ForkHeight(NAMESPACED_ALIAS_FIX_ID)
    };

    static void init() {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                for (ForkHeight fork : forks) {
                    if (fork.timestamp >= block.getTimestamp()) {
                        fork.reset();
                    }
                }
            }
        }, BlockchainProcessor.Event.BLOCK_POPPED);

        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                for (ForkHeight fork : forks) {
                    fork.reset();
                }
            }
        }, BlockchainProcessor.Event.RESCAN_BEGIN);
    }

    static class ForkHeight {
        protected long aliasId;
        protected int timestamp = 0;
        protected int height = Integer.MAX_VALUE;

        public ForkHeight(long aliasId) {
            this.aliasId = aliasId;
        }

        public void reset() {
            height = Integer.MAX_VALUE;
            timestamp = 0;
        }
    }

    static int getForkHeight(long aliasId) {
        for (ForkHeight fork : forks) {
            if (fork.aliasId == aliasId) {
                int height = fork.height;
                if (height == Integer.MAX_VALUE) {
                    Alias alias = Alias.getAlias(aliasId);
                    if (alias != null) {
                        try {
                            height = Integer.parseInt(alias.getAliasURI());
                            fork.height = height;
                            fork.timestamp = alias.getTimestamp();

                        } catch (NumberFormatException e) { /* fall through */ }
                    }
                }
                return height;
            }
        }
        return Integer.MAX_VALUE;
    }

    public static boolean POS_POP_REWARD_BLOCK(int height) {
        return (height == -1 ? Nxt.getBlockchain().getHeight() : height) > Constants.POS_POP_REWARD_BLOCK;
    }

    public static boolean MARKETPLACE_PRICE_IN_ASSET_BLOCK(int height) {
        return (height == -1 ? Nxt.getBlockchain().getHeight() : height) > Constants.MARKETPLACE_PRICE_IN_ASSET_BLOCK;
    }

    public static boolean PRIVATE_ASSETS_BLOCK() {
        return Nxt.getBlockchain().getHeight() > Constants.PRIVATE_ASSETS_BLOCK;
    }

    public static boolean ACCOUNT_IDENTIFIER_BLOCK() {
        return Nxt.getBlockchain().getHeight() > Constants.ACCOUNT_IDENTIFIER_BLOCK;
    }

    public static boolean ACCOUNT_IDENTIFIER_BLOCK_2() {
        return Nxt.getBlockchain().getHeight() > Constants.ACCOUNT_IDENTIFIER_BLOCK_2;
    }

    public static boolean COLORED_ACCOUNTS_BLOCK() {
        return Nxt.getBlockchain().getHeight() > Constants.COLORED_ACCOUNTS_BLOCK;
    }

    public static boolean NAMESPACED_ALIAS_FIX() {
        return Nxt.getBlockchain().getHeight() > getForkHeight(NAMESPACED_ALIAS_FIX_ID);
    }
}