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

import nxt.crypto.Crypto;
import nxt.db.DbIterator;
import nxt.db.DerivedDbTable;
import nxt.db.FilteringIterator;
import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.util.*;
import org.h2.fulltext.FullTextLucene;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

final class BlockchainProcessorImpl implements BlockchainProcessor {

    /* Rollback 57M theft */
    static final Long ASSET_FREEZE_57M_THEFT_BLOCK = Convert.parseUnsignedLong("13325683304515417100");
    static final int ASSET_FREEZE_57M_THEFT_HEIGHT = 282570;

    private static final byte[] CHECKSUM_THIRD_BIRTH_BLOCK = Constants.isTestnet ? null : null;
    private static final byte[] CHECKSUM_FOURTH_BIRTH_BLOCK = Constants.isTestnet ? null : null;

    private static final BlockchainProcessorImpl instance = new BlockchainProcessorImpl();

    static BlockchainProcessorImpl getInstance() {
        return instance;
    }

    private final BlockchainImpl blockchain = BlockchainImpl.getInstance();

    private final List<DerivedDbTable> derivedTables = new CopyOnWriteArrayList<>();
    private final boolean trimDerivedTables = Nxt.getBooleanProperty("fimk.trimDerivedTables");
    private final int defaultNumberOfForkConfirmations = Nxt.getIntProperty(Constants.isTestnet
            ? "fimk.testnetNumberOfForkConfirmations" : "fimk.numberOfForkConfirmations");

    private volatile int lastTrimHeight;

    private final Listeners<Block, Event> blockListeners = new Listeners<>();
    private volatile Peer lastBlockchainFeeder;
    private volatile int lastBlockchainFeederHeight;
    private volatile boolean getMoreBlocks = true;

    private volatile boolean isScanning;
    private volatile boolean alreadyInitialized = false;

    private final Runnable getMoreBlocksThread = new Runnable() {

        private final ExecutorService networkService = new ThreadPoolExecutor(0, 20, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());

        private final JSONStreamAware getCumulativeDifficultyRequest;

        {
            JSONObject request = new JSONObject();
            request.put("requestType", "getCumulativeDifficulty");
            getCumulativeDifficultyRequest = JSON.prepareRequest(request);
        }

        private boolean peerHasMore;
        private List<Peer> connectedPublicPeers;
        private List<Long> chainBlockIds;

        @Override
        public void run() {
            try {
                //
                // Download blocks until we are up-to-date
                //
                while (true) {
                    if (!getMoreBlocks)
                        return;
                    int chainHeight = blockchain.getHeight();
                    downloadPeer();
                    if (blockchain.getHeight() == chainHeight)
                        break;
                }
            } catch (InterruptedException e) {
                Logger.logDebugMessage("Blockchain download thread interrupted");
            } catch (Throwable t) {
                Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
                System.exit(1);
            }
        }

        private void downloadPeer() throws InterruptedException {
            try {
                int numberOfForkConfirmations = blockchain.getHeight() > Constants.MONETARY_SYSTEM_BLOCK - 720 ?
                        defaultNumberOfForkConfirmations : Math.min(1, defaultNumberOfForkConfirmations);
                connectedPublicPeers = Peers.getPublicPeers(Peer.State.CONNECTED, true);
                if (connectedPublicPeers.size() <= numberOfForkConfirmations) {
                    return;
                }
                peerHasMore = true;
                final Peer peer = Peers.getWeightedPeer(connectedPublicPeers);
                if (peer == null) {
                    return;
                }
                JSONObject response = peer.send(getCumulativeDifficultyRequest);
                if (response == null) {
                    return;
                }
                BigInteger curCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();
                String peerCumulativeDifficulty = (String) response.get("cumulativeDifficulty");
                if (peerCumulativeDifficulty == null) {
                    return;
                }
                BigInteger betterCumulativeDifficulty = new BigInteger(peerCumulativeDifficulty);
                if (betterCumulativeDifficulty.compareTo(curCumulativeDifficulty) < 0) {
                    return;
                }
                if (response.get("blockchainHeight") != null) {
                    lastBlockchainFeeder = peer;
                    lastBlockchainFeederHeight = ((Long) response.get("blockchainHeight")).intValue();
                }
                if (betterCumulativeDifficulty.equals(curCumulativeDifficulty)) {
                    return;
                }

                long commonMilestoneBlockId = Genesis.GENESIS_BLOCK_ID;

                if (blockchain.getLastBlock().getId() != Genesis.GENESIS_BLOCK_ID) {
                    commonMilestoneBlockId = getCommonMilestoneBlockId(peer);
                }
                if (commonMilestoneBlockId == 0 || !peerHasMore) {
                    return;
                }

                chainBlockIds = getCommonBlockId(peer, commonMilestoneBlockId, false);
                if (chainBlockIds.isEmpty() || !peerHasMore) {
                    return;
                }

                final long commonBlockId = chainBlockIds.get(0);
                final Block commonBlock = blockchain.getBlock(commonBlockId);
                if (commonBlock == null || blockchain.getHeight() - commonBlock.getHeight() >= 720) {
                    return;
                }

                synchronized (blockchain) {
                    if (betterCumulativeDifficulty.compareTo(blockchain.getLastBlock().getCumulativeDifficulty()) <= 0) {
                        return;
                    }
                    long lastBlockId = blockchain.getLastBlock().getId();
                    downloadBlockchain(peer, commonBlock);

                    if (blockchain.getHeight() - commonBlock.getHeight() <= 10) {
                        return;
                    }

                    int confirmations = 0;
                    for (Peer otherPeer : connectedPublicPeers) {
                        if (confirmations >= numberOfForkConfirmations) {
                            break;
                        }
                        if (peer.getHost().equals(otherPeer.getHost())) {
                            continue;
                        }
                        chainBlockIds = getCommonBlockId(otherPeer, commonBlockId, true);
                        if (chainBlockIds.isEmpty())
                            continue;
                        long otherPeerCommonBlockId = chainBlockIds.get(0);
                        if (otherPeerCommonBlockId == blockchain.getLastBlock().getId()) {
                            confirmations++;
                            continue;
                        }
                        if (blockchain.getHeight() - blockchain.getBlock(otherPeerCommonBlockId).getHeight() >= 720) {
                            continue;
                        }
                        String otherPeerCumulativeDifficulty;
                        JSONObject otherPeerResponse = peer.send(getCumulativeDifficultyRequest);
                        if (otherPeerResponse == null || (otherPeerCumulativeDifficulty = (String) response.get("cumulativeDifficulty")) == null) {
                            continue;
                        }
                        if (new BigInteger(otherPeerCumulativeDifficulty).compareTo(blockchain.getLastBlock().getCumulativeDifficulty()) <= 0) {
                            continue;
                        }
                        Logger.logDebugMessage("Found a peer with better difficulty");
                        downloadBlockchain(otherPeer, commonBlock); // not otherPeerCommonBlock
                    }
                    Logger.logDebugMessage("Got " + confirmations + " confirmations");

                    if (blockchain.getLastBlock().getId() != lastBlockId) {
                        Logger.logMessage(String.format(
                                "Downloaded %d blocks from %s, height %d",
                                blockchain.getHeight() - commonBlock.getHeight(),
                                peer.getHost(),
                                blockchain.getLastBlock().getHeight()
                        ));
                    } else {
                        Logger.logDebugMessage("Did not accept peer's blocks, back to our own fork");
                    }
                } // synchronized

            } catch (NxtException.StopException e) {
                Logger.logMessage("Blockchain download stopped: " + e.getMessage());
                throw new InterruptedException("Blockchain download stopped");
            } catch (Exception e) {
                Logger.logMessage("Error in blockchain download thread", e);
            }
        }

        private long getCommonMilestoneBlockId(Peer peer) {

            String lastMilestoneBlockId = null;

            while (true) {
                JSONObject milestoneBlockIdsRequest = new JSONObject();
                milestoneBlockIdsRequest.put("requestType", "getMilestoneBlockIds");
                if (lastMilestoneBlockId == null) {
                    milestoneBlockIdsRequest.put("lastBlockId", blockchain.getLastBlock().getStringId());
                } else {
                    milestoneBlockIdsRequest.put("lastMilestoneBlockId", lastMilestoneBlockId);
                }

                JSONObject response = peer.send(JSON.prepareRequest(milestoneBlockIdsRequest));
                if (response == null) {
                    return 0;
                }
                JSONArray milestoneBlockIds = (JSONArray) response.get("milestoneBlockIds");
                if (milestoneBlockIds == null) {
                    return 0;
                }
                if (milestoneBlockIds.isEmpty()) {
                    return Genesis.GENESIS_BLOCK_ID;
                }
                // prevent overloading with blockIds
                if (milestoneBlockIds.size() > 20) {
                    Logger.logDebugMessage("Obsolete or rogue peer " + peer.getHost() + " sends too many milestoneBlockIds, blacklisting");
                    peer.blacklist("Too many milestoneBlockIds");
                    return 0;
                }
                if (Boolean.TRUE.equals(response.get("last"))) {
                    peerHasMore = false;
                }
                for (Object milestoneBlockId : milestoneBlockIds) {
                    long blockId = Convert.parseUnsignedLong((String) milestoneBlockId);
                    if (BlockDb.hasBlock(blockId)) {
                        if (lastMilestoneBlockId == null && milestoneBlockIds.size() > 1) {
                            peerHasMore = false;
                        }
                        return blockId;
                    }
                    lastMilestoneBlockId = (String) milestoneBlockId;
                }
            }

        }

        private List<Long> getCommonBlockId(final Peer peer, final long startBlockId, final boolean countFromStart) {
            long matchId = startBlockId;
            List<Long> blockList = new ArrayList<>(720);
            while (true) {
                JSONObject request = new JSONObject();
                request.put("requestType", "getNextBlockIds");
                request.put("blockId", Long.toUnsignedString(matchId));
                request.put("limit", countFromStart ? 720 : 1440);
                JSONObject response = peer.send(JSON.prepareRequest(request));
                if (response == null) {
                    return blockList;
                }
                JSONArray nextBlockIds = (JSONArray) response.get("nextBlockIds");
                if (nextBlockIds == null || nextBlockIds.size() == 0) {
                    return blockList;
                }
                // prevent overloading with blockIds
                if (nextBlockIds.size() > 1440) {
                    Logger.logDebugMessage("Obsolete or rogue peer " + peer.getHost() + " sends too many nextBlockIds, blacklisting");
                    peer.blacklist("Too many nextBlockIds");
                    return Collections.emptyList();
                }
                boolean matching = true;
                int count = 0;
                for (Object nextBlockId : nextBlockIds) {
                    long blockId = Convert.parseUnsignedLong((String)nextBlockId);
                    if (matching) {
                        if (BlockDb.hasBlock(blockId)) {
                            matchId = blockId;
                        } else {
                            blockList.add(matchId);
                            blockList.add(blockId);
                            matching = false;
                        }
                    } else {
                        blockList.add(blockId);
                        if (blockList.size() >= 720)
                            break;
                    }
                    if (countFromStart && ++count >= 720) {
                        break;
                    }
                }
                if (!matching || countFromStart)
                    break;
            }
            return blockList;
        }

        /**
         * Download the block chain
         *
         * @param   feederPeer              Peer supplying the blocks list
         * @param   commonBlock             Common block
         * @throws  InterruptedException    Download interrupted
         */
        private void downloadBlockchain(final Peer feederPeer, final Block commonBlock) throws InterruptedException {
            Map<Long, PeerBlock> blockMap = new HashMap<>();
            //
            // Break the download into multiple segments.  The first block in each segment
            // is the common block for that segment.
            //
            List<GetNextBlocks> getList = new ArrayList<>();
            int segSize = (blockchain.getHeight() >= nxt.Constants.PHASING_BLOCK ? 36 : 720);
            int stop = chainBlockIds.size()-1;
            for (int start=0; start<stop; start+=segSize)
                getList.add(new GetNextBlocks(chainBlockIds, start, Math.min(start+segSize, stop)));
            //
            // Issue the getNextBlocks requests and get the results.  We will repeat
            // a request if the peer didn't respond or returned a partial block list.
            // The download will be aborted if we are unable to get a segment after
            // retrying with different peers.
            //
            download: while (!getList.isEmpty()) {
                //
                // Submit threads to issue 'getNextBlocks' requests.  The first segment
                // will always be sent to the feeder peer.  Subsequent segments will
                // be sent to the feeder peer if we failed trying to download the blocks
                // from another peer.  We will stop the download and process any pending
                // blocks if we are unable to download a segment from the feeder peer.
                //
                for (GetNextBlocks nextBlocks : getList) {
                    Peer peer;
                    if (nextBlocks.getRequestCount() > 1)
                        break download;
                    if (nextBlocks.getStart() == 0 || nextBlocks.getRequestCount() != 0) {
                        peer = feederPeer;
                    } else {
                        peer = Peers.getWeightedPeer(connectedPublicPeers);
                        if (peer == null)
                            peer = feederPeer;
                    }
                    if (nextBlocks.getPeer() == peer)
                        break download;
                    nextBlocks.setPeer(peer);
                    Future<List<BlockImpl>> future = networkService.submit(nextBlocks);
                    nextBlocks.setFuture(future);
                }
                //
                // Get the results.  A peer is on a different fork if a returned
                // block is not in the block identifier list.
                //
                Iterator<GetNextBlocks> it = getList.iterator();
                while (it.hasNext()) {
                    GetNextBlocks nextBlocks = it.next();
                    List<BlockImpl> blockList;
                    try {
                        blockList = nextBlocks.getFuture().get();
                    } catch (ExecutionException exc) {
                        throw new RuntimeException(exc.getMessage(), exc);
                    }
                    if (blockList == null)
                        continue;
                    Peer peer = nextBlocks.getPeer();
                    int index = nextBlocks.getStart()+1;
                    for (BlockImpl block : blockList) {
                        if (block.getId() != chainBlockIds.get(index))
                            break;
                        blockMap.put(block.getId(), new PeerBlock(peer, block));
                        index++;
                    }
                    if (index > nextBlocks.getStop())
                        it.remove();
                    else
                        nextBlocks.setStart(index-1);
                }
            }
            //
            // Add the new blocks to the blockchain.  We will stop if we encounter
            // a missing block (this will happen if an invalid block is encountered
            // when downloading the blocks)
            //
            List<BlockImpl> forkBlocks = new ArrayList<>();
            final int startHeight = commonBlock.getHeight();
            for (int index=1; index<chainBlockIds.size() && blockchain.getHeight() - startHeight < 720; index++) {
                PeerBlock peerBlock = blockMap.get(chainBlockIds.get(index));
                if (peerBlock == null)
                    break;
                BlockImpl block = peerBlock.getBlock();
                synchronized (blockchain) {
                    if (blockchain.getLastBlock().getId() == block.getPreviousBlockId()) {
                        try {
                            pushBlock(block, peerBlock.getPeer());
                        } catch (BlockNotAcceptedException e) {
                            peerBlock.getPeer().blacklist(e);
                        }
                    } else {
                        forkBlocks.add(block);
                    }
                }
            }
            //
            // Process a fork
            //
            if (!forkBlocks.isEmpty() && blockchain.getHeight() - startHeight < 720) {
                Logger.logDebugMessage("Will process a fork of " + forkBlocks.size() + " blocks");
                processFork(feederPeer, forkBlocks, commonBlock);
            }
        }

        private void processFork(final Peer peer, final List<BlockImpl> forkBlocks, final Block commonBlock) {

            BigInteger curCumulativeDifficulty = blockchain.getLastBlock().getCumulativeDifficulty();

            List<BlockImpl> myPoppedOffBlocks = popOffTo(commonBlock);

            int pushedForkBlocks = 0;
            if (blockchain.getLastBlock().getId() == commonBlock.getId()) {
                for (BlockImpl block : forkBlocks) {
                    if (blockchain.getLastBlock().getId() == block.getPreviousBlockId()) {
                        try {
                            pushBlock(block);
                            pushedForkBlocks += 1;
                        } catch (BlockNotAcceptedException e) {
                            peer.blacklist(e);
                            break;
                        }
                    }
                }
            }

            if (pushedForkBlocks > 0 && blockchain.getLastBlock().getCumulativeDifficulty().compareTo(curCumulativeDifficulty) < 0) {
                Logger.logDebugMessage("Pop off caused by peer " + peer.getHost() + ", blacklisting");
                peer.blacklist("Pop off");
                List<BlockImpl> peerPoppedOffBlocks = popOffTo(commonBlock);
                pushedForkBlocks = 0;
                for (BlockImpl block : peerPoppedOffBlocks) {
                    TransactionProcessorImpl.getInstance().processLater(block.getTransactions());
                }
            }

            if (pushedForkBlocks == 0) {
                Logger.logDebugMessage("Didn't accept any blocks, pushing back my previous blocks");
                for (int i = myPoppedOffBlocks.size() - 1; i >= 0; i--) {
                    BlockImpl block = myPoppedOffBlocks.remove(i);
                    try {
                        pushBlock(block);
                    } catch (BlockNotAcceptedException e) {
                        Logger.logErrorMessage("Popped off block no longer acceptable: " + block.getJSONObject().toJSONString(), e);
                        break;
                    }
                }
            } else {
                Logger.logDebugMessage("Switched to peer's fork");
                for (BlockImpl block : myPoppedOffBlocks) {
                    TransactionProcessorImpl.getInstance().processLater(block.getTransactions());
                }
            }

        }

    };

    /**
     * Callable method to get the next block segment from the selected peer
     */
    private static class GetNextBlocks implements Callable<List<BlockImpl>> {

        /** Callable future */
        private Future<List<BlockImpl>> future;

        /** Peer */
        private Peer peer;

        /** Block identifier list */
        private final List<Long> blockIds;

        /** Start index */
        private int start;

        /** Stop index */
        private int stop;

        /** Request count */
        private int requestCount;

        /**
         * Create the callable future
         *
         * @param   blockIds            Block identifier list
         * @param   start               Start index within the list
         * @param   stop                Stop index within the list
         */
        public GetNextBlocks(List<Long> blockIds, int start, int stop) {
            this.blockIds = blockIds;
            this.start = start;
            this.stop = stop;
            this.requestCount = 0;
        }

        /**
         * Return the result
         *
         * @return                      List of blocks or null if an error occurred
         */
        @Override
        public List<BlockImpl> call() {
            requestCount++;
            //
            // Build the block request list
            //
            JSONArray idList = new JSONArray();
            for (int i=start+1; i<=stop; i++)
                idList.add(Long.toUnsignedString(blockIds.get(i)));
            //
            // Issue the getNextBlocks request and specify both 'blockIds' and 'blockId'.
            // This will allow the request to be processed by both old and new nodes.
            //
            JSONObject request = new JSONObject();
            request.put("requestType", "getNextBlocks");
            request.put("blockIds", idList);
            request.put("blockId", Long.toUnsignedString(blockIds.get(start)));
            //TODO: after Constants.PHASING_BLOCK has passed, reduce the maxResponseSize
            JSONObject response = peer.send(JSON.prepareRequest(request), 192 * 1024 * 1024);
            if (response == null)
                return null;
            //
            // Get the list of blocks.  We will stop parsing blocks if we encounter
            // an invalid block.  We will return the valid blocks and reset the stop
            // index so no more blocks will be processed.
            //
            List<JSONObject> nextBlocks = (List<JSONObject>)response.get("nextBlocks");
            if (nextBlocks == null)
                return null;
            if (nextBlocks.size() > 720) {
                Logger.logDebugMessage("Obsolete or rogue peer " + peer.getHost() + " sends too many nextBlocks, blacklisting");
                peer.blacklist("Too many nextBlocks");
                return null;
            }
            List<BlockImpl> blockList = new ArrayList<>(nextBlocks.size());
            try {
                int count = stop - start;
                for (JSONObject blockData : nextBlocks) {
                    blockList.add(BlockImpl.parseBlock(blockData));
                    if (--count <= 0)
                        break;
                }
            } catch (RuntimeException | NxtException.NotValidException e) {
                Logger.logDebugMessage("Failed to parse block: " + e.toString(), e);
                peer.blacklist(e);
                stop = start + blockList.size();
            }
            return blockList;
        }

        /**
         * Return the callable future
         *
         * @return                      Callable future
         */
        public Future<List<BlockImpl>> getFuture() {
            return future;
        }

        /**
         * Set the callable future
         *
         * @param   future              Callable future
         */
        public void setFuture(Future<List<BlockImpl>> future) {
            this.future = future;
        }

        /**
         * Return the peer
         *
         * @return                      Peer
         */
        public Peer getPeer() {
            return peer;
        }

        /**
         * Set the peer
         *
         * @param   peer                Peer
         */
        public void setPeer(Peer peer) {
            this.peer = peer;
        }

        /**
         * Return the start index
         *
         * @return                      Start index
         */
        public int getStart() {
            return start;
        }

        /**
         * Set the start index
         *
         * @param   start               Start index
         */
        public void setStart(int start) {
            this.start = start;
        }

        /**
         * Return the stop index
         *
         * @return                      Stop index
         */
        public int getStop() {
            return stop;
        }

        /**
         * Return the request count
         *
         * @return                      Request count
         */
        public int getRequestCount() {
            return requestCount;
        }
    }

    /**
     * Block returned by a peer
     */
    private static class PeerBlock {

        /** Peer */
        private final Peer peer;

        /** Block */
        private final BlockImpl block;

        /**
         * Create the peer block
         *
         * @param   peer                Peer
         * @param   block               Block
         */
        public PeerBlock(Peer peer, BlockImpl block) {
            this.peer = peer;
            this.block = block;
        }

        /**
         * Return the peer
         *
         * @return                      Peer
         */
        public Peer getPeer() {
            return peer;
        }

        /**
         * Return the block
         *
         * @return                      Block
         */
        public BlockImpl getBlock() {
            return block;
        }
    }

    private final Listener<Block> checksumListener = block -> {
        if (CHECKSUM_THIRD_BIRTH_BLOCK != null && block.getHeight() == Constants.THIRD_BIRTH_BLOCK
                && ! verifyChecksum(CHECKSUM_THIRD_BIRTH_BLOCK, 0, Constants.THIRD_BIRTH_BLOCK)) {
            popOffTo(0);
        }
        if (CHECKSUM_FOURTH_BIRTH_BLOCK != null && block.getHeight() == Constants.FOURTH_BIRTH_BLOCK
                && ! verifyChecksum(CHECKSUM_FOURTH_BIRTH_BLOCK, Constants.THIRD_BIRTH_BLOCK, Constants.FOURTH_BIRTH_BLOCK)) {
            popOffTo(Constants.THIRD_BIRTH_BLOCK);
        }
    };

    private BlockchainProcessorImpl() {
        final int trimFrequency = Nxt.getIntProperty("fimk.trimFrequency");
        blockListeners.addListener(block -> {
            if (block.getHeight() % 5000 == 0) {
                Logger.logMessage("processed block " + block.getHeight());
            }
            if (trimDerivedTables && block.getHeight() % trimFrequency == 0) {
                doTrimDerivedTables();
            }
        }, Event.BLOCK_SCANNED);

        //delayed call to add listener to the end of the listeners to be last invoked on event
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        blockListeners.addListener(block -> {
                            if (trimDerivedTables && block.getHeight() % trimFrequency == 0) {
                                trimDerivedTables();
                            }
                            if (block.getHeight() % 5000 == 0) {
                                Logger.logMessage("received block " + block.getHeight());
                                Db.db.analyzeTables();
                            }
                        }, Event.BLOCK_PUSHED);
                    }
                },
                2000
        );

        blockListeners.addListener(checksumListener, Event.BLOCK_PUSHED);

        blockListeners.addListener(block -> Db.db.analyzeTables(), Event.RESCAN_END);

        ThreadPool.runBeforeStart(() -> {
            alreadyInitialized = true;
            if (addGenesisBlock()) {
                scan(0, false);
            } else if (Nxt.getBooleanProperty("fimk.forceScan")) {
                scan(0, Nxt.getBooleanProperty("fimk.forceValidate"));
            } else {
                boolean rescan;
                boolean validate;
                int height;
                try (Connection con = Db.db.getConnection();
                     Statement stmt = con.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM scan")) {
                    rs.next();
                    rescan = rs.getBoolean("rescan");
                    validate = rs.getBoolean("validate");
                    height = rs.getInt("height");
                } catch (SQLException e) {
                    throw new RuntimeException(e.toString(), e);
                }
                if (rescan) {
                    scan(height, validate);
                }
            }
        }, false);

        ThreadPool.scheduleThread("GetMoreBlocks", getMoreBlocksThread, 1);

    }

    @Override
    public boolean addListener(Listener<Block> listener, BlockchainProcessor.Event eventType) {
        return blockListeners.addListener(listener, eventType);
    }

    @Override
    public boolean removeListener(Listener<Block> listener, Event eventType) {
        return blockListeners.removeListener(listener, eventType);
    }

    @Override
    public void registerDerivedTable(DerivedDbTable table) {
        if (alreadyInitialized) {
            throw new IllegalStateException("Too late to register table " + table + ", must have done it in Nxt.Init");
        }
        derivedTables.add(table);
    }

    @Override
    public void trimDerivedTables() {
        synchronized (blockchain) {
            try {
                Db.db.beginTransaction();
                doTrimDerivedTables();
                Db.db.commitTransaction();
            } catch (Exception e) {
                Logger.logMessage(e.toString(), e);
                Db.db.rollbackTransaction();
                throw e;
            } finally {
                Db.db.endTransaction();
            }
        }
    }

    private void doTrimDerivedTables() {
        lastTrimHeight = Math.max(blockchain.getHeight() - Constants.MAX_ROLLBACK, 0);
        if (lastTrimHeight > 0) {
            for (DerivedDbTable table : derivedTables) {
                table.trim(lastTrimHeight);
            }
        }
    }

    List<DerivedDbTable> getDerivedTables() {
        return derivedTables;
    }

    @Override
    public Peer getLastBlockchainFeeder() {
        return lastBlockchainFeeder;
    }

    @Override
    public int getLastBlockchainFeederHeight() {
        return lastBlockchainFeederHeight;
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    @Override
    public int getMinRollbackHeight() {
        return trimDerivedTables ? (lastTrimHeight > 0 ? lastTrimHeight : Math.max(blockchain.getHeight() - Constants.MAX_ROLLBACK, 0)) : 0;
    }

    @Override
    public void processPeerBlock(JSONObject request) throws NxtException {
        BlockImpl block = BlockImpl.parseBlock(request);
        synchronized (blockchain) {
            BlockImpl lastBlock = blockchain.getLastBlock();
            if (block.getPreviousBlockId() == lastBlock.getId()) {
                pushBlock(block);
            } else if (block.getPreviousBlockId() == lastBlock.getPreviousBlockId() && block.getTimestamp() < lastBlock.getTimestamp()) {
                if (lastBlock.getId() != blockchain.getLastBlock().getId()) {
                    return; // blockchain changed, ignore the block
                }
                BlockImpl previousBlock = blockchain.getBlock(lastBlock.getPreviousBlockId());
                lastBlock = popOffTo(previousBlock).get(0);
                try {
                    pushBlock(block);
                    TransactionProcessorImpl.getInstance().processLater(lastBlock.getTransactions());
                    Logger.logDebugMessage("Last block " + lastBlock.getStringId() + " was replaced by " + block.getStringId());
                } catch (BlockNotAcceptedException e) {
                    Logger.logDebugMessage("Replacement block failed to be accepted, pushing back our last block");
                    pushBlock(lastBlock);
                    TransactionProcessorImpl.getInstance().processLater(block.getTransactions());
                }
            } // else ignore the block
        }
    }

    @Override
    public List<BlockImpl> popOffTo(int height) {
        if (height <= 0) {
            fullReset();
        } else if (height < blockchain.getHeight()) {
            return popOffTo(blockchain.getBlockAtHeight(height));
        }
        return Collections.emptyList();
    }

    @Override
    public void fullReset() {
        synchronized (blockchain) {
            try {
                setGetMoreBlocks(false);
                scheduleScan(0, false);
                //BlockDb.deleteBlock(Genesis.GENESIS_BLOCK_ID); // fails with stack overflow in H2
                BlockDb.deleteAll();
                if (addGenesisBlock()) {
                    scan(0, false);
                }
            } finally {
                setGetMoreBlocks(true);
            }
        }
    }

    @Override
    public void setGetMoreBlocks(boolean getMoreBlocks) {
        this.getMoreBlocks = getMoreBlocks;
    }

    private void addBlock(BlockImpl block) {
        try (Connection con = Db.db.getConnection()) {
            BlockDb.saveBlock(con, block);
            blockchain.setLastBlock(block);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    private boolean addGenesisBlock() {
        if (BlockDb.hasBlock(Genesis.GENESIS_BLOCK_ID, 0)) {
            Logger.logMessage("Genesis block already in database");
            BlockImpl lastBlock = BlockDb.findLastBlock();
            blockchain.setLastBlock(lastBlock);
            popOffTo(lastBlock);
            Logger.logMessage("Last block height: " + lastBlock.getHeight());
            return false;
        }
        Logger.logMessage("Genesis block not in database, starting from scratch");
        try {
            List<TransactionImpl> transactions = new ArrayList<>();
            for (int i = 0; i < Genesis.GENESIS_RECIPIENTS.length; i++) {
                TransactionImpl transaction = new TransactionImpl.BuilderImpl((byte) 0, Genesis.CREATOR_PUBLIC_KEY,
                        Genesis.GENESIS_AMOUNTS[i] * Constants.ONE_NXT, 0, (short) 0,
                        Attachment.ORDINARY_PAYMENT)
                        .timestamp(0)
                        .recipientId(Genesis.GENESIS_RECIPIENTS[i])
                        .signature(Genesis.GENESIS_SIGNATURES[i])
                        .height(0)
                        .ecBlockHeight(0)
                        .ecBlockId(0)
                        .build();
                transactions.add(transaction);
            }
            Collections.sort(transactions, Comparator.comparingLong(Transaction::getId));
            MessageDigest digest = Crypto.sha256();
            for (TransactionImpl transaction : transactions) {
                digest.update(transaction.bytes());
            }

            BlockImpl genesisBlock = new BlockImpl(-1, 0, 0, Genesis.TOTAL_GENESIS_AMOUNT_NQT, 0, transactions.size() * 128, digest.digest(),
                    Genesis.CREATOR_PUBLIC_KEY, new byte[64], Genesis.GENESIS_BLOCK_SIGNATURE, null, transactions);
            genesisBlock.setPrevious(null);
            addBlock(genesisBlock);
            return true;
        } catch (NxtException.ValidationException e) {
            Logger.logMessage(e.getMessage());
            throw new RuntimeException(e.toString(), e);
        }
    }

    private void pushBlock(final BlockImpl block) throws BlockNotAcceptedException {
        pushBlock(block, null);
    }

    private void pushBlock(final BlockImpl block, Peer peer) throws BlockNotAcceptedException {

        int curTime = Nxt.getEpochTime();

        synchronized (blockchain) {
            BlockImpl previousLastBlock = null;
            try {
                Db.db.beginTransaction();
                previousLastBlock = blockchain.getLastBlock();

                validate(block, previousLastBlock, curTime);

                long nextHitTime = Generator.getNextHitTime(previousLastBlock.getId(), curTime);
                if (nextHitTime > 0 && block.getTimestamp() > nextHitTime + 1) {
                    String msg = "Rejecting block " + block.getStringId() + " at height " + previousLastBlock.getHeight()
                            + " block timestamp " + block.getTimestamp() + " next hit time " + nextHitTime
                            + " current time " + curTime;
                    Logger.logDebugMessage(msg);
                    Generator.setDelay(-Constants.FORGING_SPEEDUP);
                    throw new BlockOutOfOrderException(msg, block);
                }

                Map<TransactionType, Map<String, Boolean>> duplicates = new HashMap<>();
                List<TransactionImpl> validPhasedTransactions = new ArrayList<>();
                List<TransactionImpl> invalidPhasedTransactions = new ArrayList<>();
                validatePhasedTransactions(previousLastBlock.getHeight(), validPhasedTransactions, invalidPhasedTransactions, duplicates);
                validateTransactions(block, previousLastBlock, curTime, duplicates);

                block.setPrevious(previousLastBlock);
                blockListeners.notify(block, Event.BEFORE_BLOCK_ACCEPT);
                TransactionProcessorImpl.getInstance().requeueAllUnconfirmedTransactions();
                addBlock(block);
                accept(block, validPhasedTransactions, invalidPhasedTransactions);

                Db.db.commitTransaction();

                if (peer != null) peer.setLastBlockIdHeight(block.getId(), block.getHeight());

                //report about pushed block
                if ((Nxt.getEpochTime() - block.getTimestamp()) < 3*24*60*60) {
                    LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(Convert.fromEpochTime(block.getTimestamp())), ZoneId.systemDefault());
                    String feederAddresss = peer == null
                            ? "*"
                            : peer.getAnnouncedAddress() == null ? peer.getHost() : peer.getAnnouncedAddress();
                    int interval = block.getTimestamp() - previousLastBlock.getTimestamp();
                    int txCount = block.getTransactions().size();
                    String logMessage = String.format("Pushed block %s height %d time %s from %s generator %s interval %d %s",
                            Long.toUnsignedString(block.getId()), block.getHeight(),
                            dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            feederAddresss,
                            Long.toUnsignedString(block.getGeneratorId()),
                            interval,
                            txCount > 0 ? "transactions " + txCount : ""
                            );
                    Logger.logDebugMessage(logMessage);
                }

            } catch (Exception e) {
                Logger.logErrorMessage("error on block pushing", e);
                Db.db.rollbackTransaction();
                blockchain.setLastBlock(previousLastBlock);
                throw e;
            } finally {
                Db.db.endTransaction();
            }
        } // synchronized

        if (block.getTimestamp() >= curTime - Constants.MAX_TIMEDRIFT) {
            Peers.sendToSomePeers(block);
        }

        blockListeners.notify(block, Event.BLOCK_PUSHED);

    }

    private void validatePhasedTransactions(int height, List<TransactionImpl> validPhasedTransactions, List<TransactionImpl> invalidPhasedTransactions,
                                            Map<TransactionType, Map<String, Boolean>> duplicates) {
        if (height >= Constants.PHASING_BLOCK) {
            try (DbIterator<TransactionImpl> phasedTransactions = PhasingPoll.getFinishingTransactions(height + 1)) {
                for (TransactionImpl phasedTransaction : phasedTransactions) {
                    try {
                        phasedTransaction.validate();
                        if (!phasedTransaction.isDuplicate(duplicates)) {
                            validPhasedTransactions.add(phasedTransaction);
                        } else {
                            Logger.logDebugMessage("At height " + height + " phased transaction " + phasedTransaction.getStringId() + " is duplicate, will not apply");
                            invalidPhasedTransactions.add(phasedTransaction);
                        }
                    } catch (NxtException.ValidationException e) {
                        Logger.logDebugMessage("At height " + height + " phased transaction " + phasedTransaction.getStringId() + " no longer passes validation: "
                                + e.getMessage() + ", will not apply");
                        invalidPhasedTransactions.add(phasedTransaction);
                    }
                }
            }
        }
    }

    private void validate(BlockImpl block, BlockImpl previousLastBlock, int curTime) throws BlockNotAcceptedException {
        if (previousLastBlock.getId() != block.getPreviousBlockId()) {
            String message = String.format("Previous block id (id %s, height %d) doesn't match",
                    Long.toUnsignedString(previousLastBlock.getId()), previousLastBlock.getHeight());
            throw new BlockOutOfOrderException(message, block);
        }
        if (block.getVersion() != getBlockVersion(previousLastBlock.getHeight())) {
            throw new BlockNotAcceptedException("Invalid version " + block.getVersion(), block);
        }
        if (block.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT || block.getTimestamp() <= previousLastBlock.getTimestamp()) {
            throw new BlockOutOfOrderException("Invalid timestamp: " + block.getTimestamp()
                    + " current time is " + curTime + ", previous block timestamp is " + previousLastBlock.getTimestamp(), block);
        }
        if (block.getVersion() != 1 && !Arrays.equals(Crypto.sha256().digest(previousLastBlock.bytes()), block.getPreviousBlockHash())) {
            throw new BlockNotAcceptedException("Previous block hash doesn't match", block);
        }
        if (block.getId() == 0L || BlockDb.hasBlock(block.getId(), previousLastBlock.getHeight())) {
            throw new BlockNotAcceptedException("Duplicate block or invalid id", block);
        }
        /* Rollback 57M theft */
        if (!Constants.isTestnet) {
            if (ASSET_FREEZE_57M_THEFT_BLOCK.equals(block.getId()) && previousLastBlock.getHeight() == (ASSET_FREEZE_57M_THEFT_HEIGHT - 1)) {
                throw new BlockNotAcceptedException("Asset freeze after 57M theft", block);
            }
        }
        if (!block.verifyGenerationSignature() && !Generator.allowsFakeForging(block.getGeneratorPublicKey())) {
            // Logger.logInfoMessage("Generation signature verification failed");
            // Logger.logInfoMessage("badBlocks.put("+block.getHeight()+", new BadBlock(\""+Long.toUnsignedString(block.getId())+"\", \""+Long.toUnsignedString(block.getGeneratorId())+"\"));");
            throw new BlockNotAcceptedException("Generation signature verification failed", block);
        }
        if (!block.verifyBlockSignature()) {
            throw new BlockNotAcceptedException("Block signature verification failed", block);
        }
        if (block.getTransactions().size() > Constants.MAX_NUMBER_OF_TRANSACTIONS) {
            throw new BlockNotAcceptedException("Invalid block transaction count " + block.getTransactions().size(), block);
        }
        if (block.getPayloadLength() > Constants.MAX_PAYLOAD_LENGTH || block.getPayloadLength() < 0) {
            throw new BlockNotAcceptedException("Invalid block payload length " + block.getPayloadLength(), block);
        }

        if (AccountColor.getAccountColorEnabled()) {
            Account account = Account.getAccount(block.getGeneratorId());
            if (account == null || account.getAccountColorId() != 0) {
                throw new BlockNotAcceptedException("Block forged by colored account", block);
            }
        }
    }

    private void validateTransactions(BlockImpl block, BlockImpl previousLastBlock, int curTime, Map<TransactionType, Map<String, Boolean>> duplicates) throws BlockNotAcceptedException {
        long payloadLength = 0;
        long calculatedTotalAmount = 0;
        long calculatedTotalFee = 0;
        MessageDigest digest = Crypto.sha256();
        for (TransactionImpl transaction : block.getTransactions()) {
            if (transaction.getTimestamp() > curTime + Constants.MAX_TIMEDRIFT) {
                throw new BlockOutOfOrderException("Invalid transaction timestamp: " + transaction.getTimestamp()
                        + ", current time is " + curTime, block);
            }
            if (transaction.getTimestamp() > block.getTimestamp() + Constants.MAX_TIMEDRIFT
                            || transaction.getExpiration() < block.getTimestamp() ) {
                throw new TransactionNotAcceptedException("Invalid transaction timestamp " + transaction.getTimestamp()
                        + ", current time is " + curTime + ", block timestamp is " + block.getTimestamp(), transaction);
            }
            if (TransactionDb.hasTransaction(transaction.getId(), previousLastBlock.getHeight())) {
                throw new TransactionNotAcceptedException("Transaction is already in the blockchain", transaction);
            }
            //TODO: check that referenced transaction, if phased, has been applied?
            if (transaction.referencedTransactionFullHash() != null) {
                if ((previousLastBlock.getHeight() < Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK
                        && !TransactionDb.hasTransaction(Convert.fullHashToId(transaction.referencedTransactionFullHash()), previousLastBlock.getHeight()))
                        || (previousLastBlock.getHeight() >= Constants.REFERENCED_TRANSACTION_FULL_HASH_BLOCK
                        && !hasAllReferencedTransactions(transaction, transaction.getTimestamp(), 0))) {
                    throw new TransactionNotAcceptedException("Missing or invalid referenced transaction "
                            + transaction.getReferencedTransactionFullHash(), transaction);
                }
            }
            if (transaction.getVersion() != getTransactionVersion(previousLastBlock.getHeight())) {
                throw new TransactionNotAcceptedException("Invalid transaction version " + transaction.getVersion()
                        + " at height " + previousLastBlock.getHeight(), transaction);
            }
            if (!transaction.verifySignature()) {
                throw new TransactionNotAcceptedException("Transaction signature verification failed at height " + previousLastBlock.getHeight(), transaction);
            }
                    /*
                    if (!EconomicClustering.verifyFork(transaction)) {
                        Logger.logDebugMessage("Block " + block.getStringId() + " height " + (previousLastBlock.getHeight() + 1)
                                + " contains transaction that was generated on a fork: "
                                + transaction.getStringId() + " ecBlockHeight " + transaction.getECBlockHeight() + " ecBlockId "
                                + Convert.toUnsignedLong(transaction.getECBlockId()));
                        //throw new TransactionNotAcceptedException("Transaction belongs to a different fork", transaction);
                    }
                    */
            if (transaction.getId() == 0L) {
                throw new TransactionNotAcceptedException("Invalid transaction id 0", transaction);
            }
            try {
                transaction.validate();
            } catch (NxtException.ValidationException e) {
                throw new TransactionNotAcceptedException(e.getMessage(), transaction);
            }
            if (transaction.getPhasing() == null && transaction.isDuplicate(duplicates)) {
                throw new TransactionNotAcceptedException("Transaction is a duplicate", transaction);
            }
            calculatedTotalAmount += transaction.getAmountNQT();
            calculatedTotalFee += transaction.getFeeNQT();
            payloadLength += transaction.getFullSize();
            digest.update(transaction.bytes());
        }
        if (calculatedTotalAmount != block.getTotalAmountNQT() || calculatedTotalFee != block.getTotalFeeNQT()) {
            throw new BlockNotAcceptedException("Total amount or fee don't match transaction totals", block);
        }
        if (!Arrays.equals(digest.digest(), block.getPayloadHash())) {
            throw new BlockNotAcceptedException("Payload hash doesn't match", block);
        }
        if (payloadLength > block.getPayloadLength()) {
            throw new BlockNotAcceptedException("Transaction payload length " + payloadLength + " exceeds declared block payload length " + block.getPayloadLength(), block);
        }
    }

    private void accept(BlockImpl block, List<TransactionImpl> validPhasedTransactions, List<TransactionImpl> invalidPhasedTransactions) throws TransactionNotAcceptedException {
        for (TransactionImpl transaction : block.getTransactions()) {
            if (! transaction.applyUnconfirmed()) {
                throw new TransactionNotAcceptedException("Double spending", transaction);
            }
        }
        blockListeners.notify(block, Event.BEFORE_BLOCK_APPLY);
        block.apply();
        for (TransactionImpl transaction : validPhasedTransactions) {
            transaction.getPhasing().countVotes(transaction);
        }
        for (TransactionImpl transaction : invalidPhasedTransactions) {
            transaction.getPhasing().reject(transaction);
        }
        for (TransactionImpl transaction : block.getTransactions()) {
            try {
                transaction.apply();
            } catch (RuntimeException e) {
                Logger.logErrorMessage(e.toString(), e);
                throw new BlockchainProcessor.TransactionNotAcceptedException(e, transaction);
            }
        }
        blockListeners.notify(block, Event.AFTER_BLOCK_APPLY);
        if (block.getTransactions().size() > 0) {
            TransactionProcessorImpl.getInstance().notifyListeners(block.getTransactions(), TransactionProcessor.Event.ADDED_CONFIRMED_TRANSACTIONS);
        }
    }

    private List<BlockImpl> popOffTo(Block commonBlock) {
        synchronized (blockchain) {
            if (!Db.db.isInTransaction()) {
                try {
                    Db.db.beginTransaction();
                    return popOffTo(commonBlock);
                } finally {
                    Db.db.endTransaction();
                }
            }
            if (commonBlock.getHeight() < getMinRollbackHeight()) {
                Logger.logMessage("Rollback to height " + commonBlock.getHeight() + " not supported, will do a full rescan");
                popOffWithRescan(commonBlock.getHeight() + 1);
                return Collections.emptyList();
            }
            if (! blockchain.hasBlock(commonBlock.getId())) {
                Logger.logDebugMessage("Block " + commonBlock.getStringId() + " not found in blockchain, nothing to pop off");
                return Collections.emptyList();
            }
            List<BlockImpl> poppedOffBlocks = new ArrayList<>();
            try {
                BlockImpl block = blockchain.getLastBlock();
                block.loadTransactions();
                Logger.logDebugMessage("Rollback from block " + block.getStringId() + " at height " + block.getHeight()
                        + " to " + commonBlock.getStringId() + " at " + commonBlock.getHeight());
                while (block.getId() != commonBlock.getId() && block.getId() != Genesis.GENESIS_BLOCK_ID) {
                    poppedOffBlocks.add(block);
                    block = popLastBlock();
                }
                for (DerivedDbTable table : derivedTables) {
                    table.rollback(commonBlock.getHeight());
                }
                Db.db.commitTransaction();
            } catch (RuntimeException e) {
                Logger.logErrorMessage("Error popping off to " + commonBlock.getHeight() + ", " + e.toString());
                Db.db.rollbackTransaction();
                BlockImpl lastBlock = BlockDb.findLastBlock();
                blockchain.setLastBlock(lastBlock);
                popOffTo(lastBlock);
                throw e;
            }
            return poppedOffBlocks;
        } // synchronized
    }

    private BlockImpl popLastBlock() {
        BlockImpl block = blockchain.getLastBlock();
        if (block.getId() == Genesis.GENESIS_BLOCK_ID) {
            throw new RuntimeException("Cannot pop off genesis block");
        }
        BlockImpl previousBlock = blockchain.getBlock(block.getPreviousBlockId());
        previousBlock.loadTransactions();
        blockchain.setLastBlock(block, previousBlock);
        BlockDb.deleteBlocksFrom(block.getId());
        blockListeners.notify(block, Event.BLOCK_POPPED);
        return previousBlock;
    }

    private void popOffWithRescan(int height) {
        synchronized (blockchain) {
            try {
                BlockImpl block = BlockDb.findBlockAtHeight(height);
                scheduleScan(0, false);
                BlockDb.deleteBlocksFrom(block.getId());
                Logger.logDebugMessage("Deleted blocks starting from height %s", height);
            } finally {
                scan(0, false);
            }
        }
    }

    private int getBlockVersion(int previousBlockHeight) {
        return 3;
    }

    private int getTransactionVersion(int previousBlockHeight) {
        return previousBlockHeight < Constants.DIGITAL_GOODS_STORE_BLOCK ? 0 : 1;
    }

    private boolean verifyChecksum(byte[] validChecksum, int fromHeight, int toHeight) {
        MessageDigest digest = Crypto.sha256();
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement(
                     "SELECT * FROM transaction WHERE height > ? AND height <= ? ORDER BY id ASC, timestamp ASC")) {
            pstmt.setInt(1, fromHeight);
            pstmt.setInt(2, toHeight);
            try (DbIterator<TransactionImpl> iterator = blockchain.getTransactions(con, pstmt)) {
                while (iterator.hasNext()) {
                    digest.update(iterator.next().bytes());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        byte[] checksum = digest.digest();
        if (validChecksum == null) {
            Logger.logMessage("Checksum calculated:\n" + Arrays.toString(checksum));
            return true;
        } else if (!Arrays.equals(checksum, validChecksum)) {
            Logger.logErrorMessage("Checksum failed at block " + blockchain.getHeight() + ": " + Arrays.toString(checksum));
            return false;
        } else {
            Logger.logMessage("Checksum passed at block " + blockchain.getHeight());
            return true;
        }
    }

    private static final Comparator<UnconfirmedTransaction> transactionArrivalComparator = Comparator
            .comparingLong(UnconfirmedTransaction::getArrivalTimestamp)
            .thenComparingInt(UnconfirmedTransaction::getHeight)
            .thenComparingLong(UnconfirmedTransaction::getId);

    private static final Comparator<UnconfirmedTransaction> transactionIdComparator = Comparator
            .comparingLong(UnconfirmedTransaction::getId);

    public void generateBlock(String secretPhrase, int blockTimestamp) throws BlockNotAcceptedException {

        List<UnconfirmedTransaction> orderedUnconfirmedTransactions = new ArrayList<>();
        try (FilteringIterator<UnconfirmedTransaction> unconfirmedTransactions = new FilteringIterator<>(TransactionProcessorImpl.getInstance().getAllUnconfirmedTransactions(),
                transaction -> hasAllReferencedTransactions(transaction.getTransaction(), transaction.getTimestamp(), 0))) {
            for (UnconfirmedTransaction unconfirmedTransaction : unconfirmedTransactions) {
                orderedUnconfirmedTransactions.add(unconfirmedTransaction);
            }
        }

        BlockImpl previousBlock = blockchain.getLastBlock();

        SortedSet<UnconfirmedTransaction> sortedTransactions = new TreeSet<>(previousBlock.getHeight() < Constants.MONETARY_SYSTEM_BLOCK
                ? transactionIdComparator : transactionArrivalComparator);

        Map<TransactionType, Map<String, Boolean>> duplicates = new HashMap<>();
        if (blockchain.getHeight() >= Constants.PHASING_BLOCK) {
            try (DbIterator<TransactionImpl> phasedTransactions = PhasingPoll.getFinishingTransactions(blockchain.getHeight() + 1)) {
                for (TransactionImpl phasedTransaction : phasedTransactions) {
                    try {
                        phasedTransaction.validate();
                        phasedTransaction.isDuplicate(duplicates);
                    } catch (NxtException.ValidationException ignore) {
                    }
                }
            }
        }

        long totalAmountNQT = 0;
        long totalFeeNQT = 0;
        int payloadLength = 0;

        while (payloadLength <= Constants.MAX_PAYLOAD_LENGTH && sortedTransactions.size() <= Constants.MAX_NUMBER_OF_TRANSACTIONS) {

            int prevNumberOfNewTransactions = sortedTransactions.size();

            for (UnconfirmedTransaction unconfirmedTransaction : orderedUnconfirmedTransactions) {

                int transactionLength = unconfirmedTransaction.getTransaction().getFullSize();
                if (sortedTransactions.contains(unconfirmedTransaction) || payloadLength + transactionLength > Constants.MAX_PAYLOAD_LENGTH) {
                    continue;
                }

                if (unconfirmedTransaction.getVersion() != getTransactionVersion(previousBlock.getHeight())) {
                    continue;
                }

                if (unconfirmedTransaction.getTimestamp() > blockTimestamp + Constants.MAX_TIMEDRIFT || unconfirmedTransaction.getExpiration() < blockTimestamp) {
                    continue;
                }

                try {
                    unconfirmedTransaction.getTransaction().validate();
                } catch (NxtException.NotCurrentlyValidException e) {
                    continue;
                } catch (NxtException.ValidationException e) {
                    TransactionProcessorImpl.getInstance().removeUnconfirmedTransaction(unconfirmedTransaction.getTransaction());
                    continue;
                }

                if (unconfirmedTransaction.getPhasing() == null && unconfirmedTransaction.getTransaction().isDuplicate(duplicates)) {
                    continue;
                }

                /*
                if (!EconomicClustering.verifyFork(transaction)) {
                    Logger.logDebugMessage("Including transaction that was generated on a fork: " + transaction.getStringId()
                            + " ecBlockHeight " + transaction.getECBlockHeight() + " ecBlockId " + Long.toUnsignedString(transaction.getECBlockId()));
                    //continue;
                }
                */

                sortedTransactions.add(unconfirmedTransaction);
                payloadLength += transactionLength;
                totalAmountNQT += unconfirmedTransaction.getAmountNQT();
                totalFeeNQT += unconfirmedTransaction.getFeeNQT();

            }

            if (sortedTransactions.size() == prevNumberOfNewTransactions) {
                break;
            }
        }

        List<TransactionImpl> blockTransactions = new ArrayList<>();

        MessageDigest digest = Crypto.sha256();
        for (UnconfirmedTransaction unconfirmedTransaction : sortedTransactions) {
            blockTransactions.add(unconfirmedTransaction.getTransaction());
            digest.update(unconfirmedTransaction.getTransaction().bytes());
        }

        byte[] payloadHash = digest.digest();

        digest.update(previousBlock.getGenerationSignature());
        final byte[] publicKey = Crypto.getPublicKey(secretPhrase);
        byte[] generationSignature = digest.digest(publicKey);

        byte[] previousBlockHash = Crypto.sha256().digest(previousBlock.bytes());

        BlockImpl block = new BlockImpl(getBlockVersion(previousBlock.getHeight()), blockTimestamp, previousBlock.getId(), totalAmountNQT, totalFeeNQT, payloadLength,
                payloadHash, publicKey, generationSignature, previousBlockHash, blockTransactions, secretPhrase);

        try {
            pushBlock(block);
            blockListeners.notify(block, Event.BLOCK_GENERATED);
            Logger.logDebugMessage("Account " + Long.toUnsignedString(block.getGeneratorId()) + " generated block " + block.getStringId()
                    + " at height " + block.getHeight() + " timestamp " + block.getTimestamp() + " fee " + ((float)block.getTotalFeeNQT())/Constants.ONE_NXT);
        } catch (TransactionNotAcceptedException e) {
            Logger.logDebugMessage("Generate block failed: " + e.getMessage());
            TransactionProcessorImpl.getInstance().processWaitingTransactions();
            TransactionImpl transaction = e.getTransaction();
            Logger.logDebugMessage("Removing invalid transaction: " + transaction.getStringId());
            TransactionProcessorImpl.getInstance().removeUnconfirmedTransaction(transaction);
            throw e;
        } catch (BlockNotAcceptedException e) {
            Logger.logDebugMessage("Generate block failed: " + e.getMessage());
            throw e;
        }
    }

    private boolean hasAllReferencedTransactions(TransactionImpl transaction, int timestamp, int count) {
        if (transaction.referencedTransactionFullHash() == null) {
            return timestamp - transaction.getTimestamp() < Constants.MAX_REFERENCED_TRANSACTION_TIMESPAN && count < 10;
        }
        TransactionImpl referencedTransaction = TransactionDb.findTransactionByFullHash(transaction.referencedTransactionFullHash());
        return referencedTransaction != null
                && referencedTransaction.getHeight() < transaction.getHeight()
                && hasAllReferencedTransactions(referencedTransaction, timestamp, count + 1);
    }

    void scheduleScan(int height, boolean validate) {
        try (Connection con = Db.db.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE scan SET rescan = TRUE, height = ?, validate = ?")) {
            pstmt.setInt(1, height);
            pstmt.setBoolean(2, validate);
            pstmt.executeUpdate();
            Logger.logDebugMessage("Scheduled scan starting from height " + height + (validate ? ", with validation" : ""));
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    @Override
    public void scan(int height, boolean validate) {
        scan(height, validate, false);
    }

    @Override
    public void fullScanWithShutdown() {
        scan(0, true, true);
    }

    private void scan(int height, boolean validate, boolean shutdown) {
        synchronized (blockchain) {
            if (!Db.db.isInTransaction()) {
                try {
                    Db.db.beginTransaction();
                    if (validate) {
                        blockListeners.addListener(checksumListener, Event.BLOCK_SCANNED);
                    }
                    scan(height, validate, shutdown);
                    Db.db.commitTransaction();
                } catch (Exception e) {
                    Db.db.rollbackTransaction();
                    throw e;
                } finally {
                    Db.db.endTransaction();
                    blockListeners.removeListener(checksumListener, Event.BLOCK_SCANNED);
                }
                return;
            }
            scheduleScan(height, validate);
            if (height > 0 && height < getMinRollbackHeight()) {
                Logger.logMessage("Rollback to height less than " + getMinRollbackHeight() + " not supported, will do a full scan");
                height = 0;
            }
            if (height < 0) {
                height = 0;
            }
            Logger.logMessage("Scanning blockchain starting from height " + height + "...");
            if (validate) {
                Logger.logDebugMessage("Also verifying signatures and validating transactions...");
            }
            try (Connection con = Db.db.getConnection();
                 PreparedStatement pstmtSelect = con.prepareStatement("SELECT * FROM block WHERE height >= ? ORDER BY db_id ASC");
                 PreparedStatement pstmtDone = con.prepareStatement("UPDATE scan SET rescan = FALSE, height = 0, validate = FALSE")) {
                isScanning = true;
                if (height > blockchain.getHeight() + 1) {
                    Logger.logMessage("Rollback height " + (height - 1) + " exceeds current blockchain height of " + blockchain.getHeight() + ", no scan needed");
                    pstmtDone.executeUpdate();
                    Db.db.commitTransaction();
                    return;
                }
                if (height == 0) {
                    Logger.logDebugMessage("Dropping all full text search indexes");
                    FullTextLucene.dropAll(con);
                }
                for (DerivedDbTable table : derivedTables) {
                    if (height == 0) {
                        table.truncate();
                    } else {
                        table.rollback(height - 1);
                    }
                }
                Db.db.commitTransaction();
                Logger.logDebugMessage("Rolled back derived tables");
                BlockImpl currentBlock = BlockDb.findBlockAtHeight(height);
                blockListeners.notify(currentBlock, Event.RESCAN_BEGIN);
                long currentBlockId = currentBlock.getId();
                if (height == 0) {
                    blockchain.setLastBlock(currentBlock); // special case to avoid no last block
                    Account.addOrGetAccount(Genesis.CREATOR_ID).apply(Genesis.CREATOR_PUBLIC_KEY);
                } else {
                    blockchain.setLastBlock(BlockDb.findBlockAtHeight(height - 1));
                }
                if (shutdown) {
                    Logger.logMessage("Scan will be performed at next start");
                    new Thread(() -> {
                        System.exit(0);
                    }).start();
                    return;
                }
                pstmtSelect.setInt(1, height);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    while (rs.next()) {
                        try {
                            currentBlock = BlockDb.loadBlock(con, rs, true);
                            currentBlock.loadTransactions();
                            if (currentBlock.getId() != currentBlockId || currentBlock.getHeight() > blockchain.getHeight() + 1) {
                                throw new NxtException.NotValidException("Database blocks in the wrong order!");
                            }
                            Map<TransactionType, Map<String, Boolean>> duplicates = new HashMap<>();
                            List<TransactionImpl> validPhasedTransactions = new ArrayList<>();
                            List<TransactionImpl> invalidPhasedTransactions = new ArrayList<>();
                            validatePhasedTransactions(blockchain.getHeight(), validPhasedTransactions, invalidPhasedTransactions, duplicates);
                            if (validate && currentBlockId != Genesis.GENESIS_BLOCK_ID) {
                                int curTime = Nxt.getEpochTime();
                                validate(currentBlock, blockchain.getLastBlock(), curTime);
                                byte[] blockBytes = currentBlock.bytes();
                                if (ASSET_FREEZE_57M_THEFT_BLOCK.equals(currentBlock.getId()) && currentBlock.getHeight() == ASSET_FREEZE_57M_THEFT_HEIGHT) {
                                    throw new NxtException.NotValidException("Asset freeze after 57M theft");
                                }
                                JSONObject blockJSON = (JSONObject) JSONValue.parse(currentBlock.getJSONObject().toJSONString());
                                if (!Arrays.equals(blockBytes, BlockImpl.parseBlock(blockJSON).bytes())) {
                                    throw new NxtException.NotValidException("Block JSON cannot be parsed back to the same block");
                                }
                                validateTransactions(currentBlock, blockchain.getLastBlock(), curTime, duplicates);
                                for (TransactionImpl transaction : currentBlock.getTransactions()) {
                                    byte[] transactionBytes = transaction.bytes();
                                    if (currentBlock.getHeight() > Constants.NQT_BLOCK
                                            && !Arrays.equals(transactionBytes, TransactionImpl.newTransactionBuilder(transactionBytes).build().bytes())) {
                                        throw new NxtException.NotValidException("Transaction bytes cannot be parsed back to the same transaction: "
                                                + transaction.getJSONObject().toJSONString());
                                    }
                                    JSONObject transactionJSON = (JSONObject) JSONValue.parse(transaction.getJSONObject().toJSONString());
                                    if (!Arrays.equals(transactionBytes, TransactionImpl.newTransactionBuilder(transactionJSON).build().bytes())) {
                                        throw new NxtException.NotValidException("Transaction JSON cannot be parsed back to the same transaction: "
                                                + transaction.getJSONObject().toJSONString());
                                    }
                                }
                            }
                            blockListeners.notify(currentBlock, Event.BEFORE_BLOCK_ACCEPT);
                            blockchain.setLastBlock(currentBlock);
                            accept(currentBlock, validPhasedTransactions, invalidPhasedTransactions);
                            currentBlockId = currentBlock.getNextBlockId();
                            Db.db.commitTransaction();
                        } catch (NxtException | RuntimeException e) {
                            Db.db.rollbackTransaction();
                            Logger.logDebugMessage(e.toString(), e);
                            Logger.logDebugMessage("Applying block " + Long.toUnsignedString(currentBlockId) + " at height "
                                    + (currentBlock == null ? 0 : currentBlock.getHeight()) + " failed, deleting from database");
                            if (currentBlock != null) {
                                currentBlock.loadTransactions();
                                TransactionProcessorImpl.getInstance().processLater(currentBlock.getTransactions());
                            }
                            while (rs.next()) {
                                try {
                                    currentBlock = BlockDb.loadBlock(con, rs, true);
                                    currentBlock.loadTransactions();
                                    TransactionProcessorImpl.getInstance().processLater(currentBlock.getTransactions());
                                } catch (RuntimeException e2) {
                                    Logger.logErrorMessage(e2.toString(), e);
                                    break;
                                }
                            }
                            BlockDb.deleteBlocksFrom(currentBlockId);
                            BlockImpl lastBlock = BlockDb.findLastBlock();
                            blockchain.setLastBlock(lastBlock);
                            popOffTo(lastBlock);
                            break;
                        }
                        blockListeners.notify(currentBlock, Event.BLOCK_SCANNED);
                    }
                }
                if (height == 0) {
                    for (DerivedDbTable table : derivedTables) {
                        table.createSearchIndex(con);
                    }
                }
                pstmtDone.executeUpdate();
                Db.db.commitTransaction();
                blockListeners.notify(currentBlock, Event.RESCAN_END);
                Logger.logMessage("...done at height " + blockchain.getHeight());
                if (height == 0 && validate) {
                    Logger.logMessage("SUCCESSFULLY PERFORMED FULL RESCAN WITH VALIDATION");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            } finally {
                isScanning = false;
            }
        } // synchronized
    }
}
