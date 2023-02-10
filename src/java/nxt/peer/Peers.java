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

package nxt.peer;

import nxt.*;
import nxt.gossip.Gossip;
import nxt.http.API;
import nxt.peer.rewarding.NodesMonitoringThread;
import nxt.util.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.DoSFilter;
import org.eclipse.jetty.servlets.GzipFilter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public final class Peers {

    public enum Event {
        BLACKLIST, UNBLACKLIST, DEACTIVATE, REMOVE,
        DOWNLOADED_VOLUME, UPLOADED_VOLUME, WEIGHT,
        ADDED_ACTIVE_PEER, CHANGED_ACTIVE_PEER,
        NEW_PEER, ADD_INBOUND, REMOVE_INBOUND
    }

    static final int LOGGING_MASK_EXCEPTIONS = 1;
    static final int LOGGING_MASK_NON200_RESPONSES = 2;
    static final int LOGGING_MASK_200_RESPONSES = 4;
    static volatile int communicationLoggingMask;

    static final List<String> wellKnownPeers;
    static final Set<String> knownBlacklistedPeers;
    static final Set<String> knownWhitelistedPeers;

    static final int connectTimeout;
    static final int readTimeout;
    static final int blacklistingPeriod;
    static final boolean getMorePeers;
    static final boolean nodesRewardingEnabled;
    static final int MAX_REQUEST_SIZE = 1024 * 1024;
    static final int MAX_RESPONSE_SIZE = 1024 * 1024;
    static final boolean useWebSockets;
    static final int webSocketIdleTimeout;
    static final boolean useProxy = System.getProperty("socksProxyHost") != null || System.getProperty("http.proxyHost") != null;

    private static final int DEFAULT_PEER_PORT = 7884;
    private static final int TESTNET_PEER_PORT = 6884;
    private static final String myPlatform;
    public static final String myAddress;
    public static String inetAddress;
    private static final int myPeerServerPort;

    private static int defaultPeerPort;
    private static final String myHallmark;
    private static final boolean shareMyAddress;
    private static final int maxNumberOfInboundConnections;
    private static final int maxNumberOfOutboundConnections;
    static final int maxNumberOfConnectedPublicPeers;
    private static final int maxNumberOfKnownPeers;
    static final int minNumberOfKnownPeers;
    static final boolean enableHallmarkProtection;
    private static final int pushThreshold;
    private static final int pullThreshold;
    private static final int sendToPeersLimit;
    private static final boolean usePeersDb;
    static final boolean savePeers;
    static final boolean ignorePeerAnnouncedAddress;
    static final boolean cjdnsOnly;
    public static final boolean gossipEnabled;
    static final int MAX_VERSION_LENGTH = 10;
    static final int MAX_APPLICATION_LENGTH = 20;
    static final int MAX_PLATFORM_LENGTH = 30;
    static final int MAX_ANNOUNCED_ADDRESS_LENGTH = 100;


    public static final JSONStreamAware myPeerInfoRequest;
    static final JSONStreamAware myPeerInfoResponse;

    private static final Listeners<Peer,Event> listeners = new Listeners<>();

    static final ConcurrentMap<String, PeerImpl> peers = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> selfAnnouncedAddresses = new ConcurrentHashMap<>();

    static final Collection<PeerImpl> allPeers = Collections.unmodifiableCollection(peers.values());

    static final ExecutorService peersService = Executors.newCachedThreadPool();
    private static final ExecutorService sendingService = Executors.newFixedThreadPool(10);

    static final SendToPeersRequestQueue sendToPeersRequestQueue = new SendToPeersRequestQueue();

    static {

        myPlatform = Nxt.getStringProperty("fimk.myPlatform");
        if (myPlatform.length() > MAX_PLATFORM_LENGTH) {
            throw new RuntimeException("fimk.myPlatform length exceeds " + MAX_PLATFORM_LENGTH);
        }
        String nodeToken = Nxt.getStringProperty("fimk.popReward.nodeToken");
        myAddress = Convert.emptyToNull(Nxt.getStringProperty("fimk.myAddress", "").trim());
        if (myAddress != null && myAddress.endsWith(":" + TESTNET_PEER_PORT) && !Constants.isTestnet) {
            throw new RuntimeException("Port " + TESTNET_PEER_PORT + " should only be used for testnet!!!");
        }
        myPeerServerPort = Nxt.getIntProperty("fimk.peerServerPort");
        if (myPeerServerPort == TESTNET_PEER_PORT && !Constants.isTestnet) {
            throw new RuntimeException("Port " + TESTNET_PEER_PORT + " should only be used for testnet!!!");
        }
        shareMyAddress = Nxt.getBooleanProperty("fimk.shareMyAddress") && ! Constants.isOffline;
        myHallmark = Nxt.getStringProperty("fimk.myHallmark");
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            try {
                Hallmark hallmark = Hallmark.parseHallmark(Peers.myHallmark);
                if (!hallmark.isValid()) {
                    throw new RuntimeException();
                }
                if (myAddress != null) {
                    URI uri = new URI("http://" + myAddress);
                    String host = uri.getHost();
                    if (!hallmark.getHost().equals(host)) {
                        throw new RuntimeException("Invalid hallmark host");
                    }
                    int myPort = uri.getPort() == -1 ? Peers.getDefaultPeerPort() : uri.getPort();
                    if (myPort != hallmark.getPort()) {
                        throw new RuntimeException("Invalid hallmark port");
                    }
                }
            } catch (RuntimeException | URISyntaxException e) {
                Logger.logMessage("Your hallmark is invalid: " + Peers.myHallmark + " for your address: " + myAddress);
                throw new RuntimeException(e.toString(), e);
            }
        }

        JSONObject json = new JSONObject();
        if (myAddress != null) {
            try {
                URI uri = new URI("http://" + myAddress);
                String host = uri.getHost();
                int port = uri.getPort();
                String announcedAddress;
                if (Constants.isTestnet) {
                    announcedAddress = host;
                } else {
                    announcedAddress = port >= 0
                            ? myAddress
                            : host + (myPeerServerPort != DEFAULT_PEER_PORT ? ":" + myPeerServerPort : "");
                }
                if (announcedAddress == null || announcedAddress.length() > MAX_ANNOUNCED_ADDRESS_LENGTH) {
                    throw new RuntimeException("Invalid announced address length: " + announcedAddress);
                }
                json.put("announcedAddress", announcedAddress);

                Peers.inetAddress = InetAddress.getByName(host).toString();
            } catch (URISyntaxException | UnknownHostException e) {
                Logger.logMessage("Your announce address is invalid: " + myAddress);
                throw new RuntimeException(e.toString(), e);
            }
        }
        if (Peers.myHallmark != null && Peers.myHallmark.length() > 0) {
            json.put("hallmark", Peers.myHallmark);
        }
        json.put("application", Nxt.APPLICATION);
        json.put("version", Nxt.VERSION);
        json.put("platform", Peers.myPlatform);
        json.put("shareAddress", Peers.shareMyAddress);
        json.put("nodeToken", nodeToken);

        gossipEnabled = Nxt.getBooleanProperty("fimk.gossipEnabled");
        if (gossipEnabled) {
            json.put("gossip", true);
        }

        Logger.logDebugMessage("My peer info:\n" + json.toJSONString());
        myPeerInfoResponse = JSON.prepare(json);
        json.put("requestType", "getInfo");
        myPeerInfoRequest = JSON.prepareRequest(json);

        final List<String> defaultPeers = Constants.isTestnet ? Nxt.getStringListProperty("fimk.defaultTestnetPeers")
                : Nxt.getStringListProperty("fimk.defaultPeers");
        wellKnownPeers = Collections.unmodifiableList(Constants.isTestnet ? Nxt.getStringListProperty("fimk.testnetPeers")
                : Nxt.getStringListProperty("fimk.wellKnownPeers"));

        List<String> knownBlacklistedPeersList = Nxt.getStringListProperty("fimk.knownBlacklistedPeers");
        if (knownBlacklistedPeersList.isEmpty()) {
            knownBlacklistedPeers = Collections.emptySet();
        } else {
            knownBlacklistedPeers = Collections.unmodifiableSet(new HashSet<>(knownBlacklistedPeersList));
        }

        maxNumberOfInboundConnections = Nxt.getIntProperty("fimk.maxNumberOfInboundConnections");
        maxNumberOfOutboundConnections = Nxt.getIntProperty("fimk.maxNumberOfOutboundConnections");
        maxNumberOfConnectedPublicPeers = Math.min(Nxt.getIntProperty("fimk.maxNumberOfConnectedPublicPeers"),
                maxNumberOfOutboundConnections);

        List<String> knownWhitelistedPeersList = Nxt.getStringListProperty("fimk.knownWhitelistedPeers");
        if (knownWhitelistedPeersList.isEmpty()) {
            knownWhitelistedPeers = Collections.emptySet();
        } else {
            knownWhitelistedPeers = Collections.unmodifiableSet(new HashSet<>(knownWhitelistedPeersList));
        }        

        maxNumberOfKnownPeers = Nxt.getIntProperty("fimk.maxNumberOfKnownPeers");
        minNumberOfKnownPeers = Nxt.getIntProperty("fimk.minNumberOfKnownPeers");
        connectTimeout = Nxt.getIntProperty("fimk.connectTimeout");
        readTimeout = Nxt.getIntProperty("fimk.readTimeout");
        enableHallmarkProtection = Nxt.getBooleanProperty("fimk.enableHallmarkProtection");
        pushThreshold = Nxt.getIntProperty("fimk.pushThreshold");
        pullThreshold = Nxt.getIntProperty("fimk.pullThreshold");
        useWebSockets = Nxt.getBooleanProperty("fimk.useWebSockets");
        webSocketIdleTimeout = Nxt.getIntProperty("fimk.webSocketIdleTimeout");
        blacklistingPeriod = Nxt.getIntProperty("fimk.blacklistingPeriod");
        communicationLoggingMask = Nxt.getIntProperty("fimk.communicationLoggingMask");
        sendToPeersLimit = Nxt.getIntProperty("fimk.sendToPeersLimit");
        usePeersDb = Nxt.getBooleanProperty("fimk.usePeersDb") && ! Constants.isOffline;
        savePeers = usePeersDb && Nxt.getBooleanProperty("fimk.savePeers");
        getMorePeers = Nxt.getBooleanProperty("fimk.getMorePeers");
        cjdnsOnly = Nxt.getBooleanProperty("fimk.cjdnsOnly");
        ignorePeerAnnouncedAddress = Nxt.getBooleanProperty("fimk.ignorePeerAnnouncedAddress");
        if (useWebSockets && useProxy) {
            Logger.logMessage("Using a proxy, will not create outbound websockets.");
        }

        nodesRewardingEnabled = Nxt.getBooleanProperty("fimk.popReward.nodesRewardingEnabled");

        final List<Future<String>> unresolvedPeers = Collections.synchronizedList(new ArrayList<>());

        if (!Constants.isOffline) {
            ThreadPool.runBeforeStart(new Runnable() {

                private void loadPeers(Collection<String> addresses) {
                    int now = Nxt.getEpochTime();
                    for (final String address : addresses) {
                        Future<String> unresolvedAddress = peersService.submit(() -> {
                            PeerImpl peer = Peers.findOrCreatePeer(address, true);
                            if (peer != null) {
                                peer.setLastUpdated(now);
                                Peers.addPeer(peer);
                                return null;
                            }
                            return address;
                        });
                        unresolvedPeers.add(unresolvedAddress);
                    }
                }

                @Override
                public void run() {
                    loadPeers(wellKnownPeers);
                    if (usePeersDb) {
                        Logger.logDebugMessage("Loading known peers from the database...");
                        loadPeers(defaultPeers);
                        if (savePeers) {
                            List<PeerDb.Entry> dbPeers = PeerDb.loadPeers();
                            for (PeerDb.Entry dbPeer : dbPeers) {
                                Future<String> unresolvedAddress = peersService.submit(() -> {
                                    PeerImpl peer = Peers.findOrCreatePeer(dbPeer.getAddress(), true);
                                    if (peer != null) {
                                        if (peer.getLastUpdated() == 0)
                                            peer.setLastUpdated(dbPeer.getLastUpdated());
                                        Peers.addPeer(peer);
                                        return null;
                                    }
                                    return dbPeer.getAddress();
                                });
                                unresolvedPeers.add(unresolvedAddress);
                            }
                        }
                    }
                }
            }, false);
        }

        ThreadPool.runAfterStart(() -> {
            for (Future<String> unresolvedPeer : unresolvedPeers) {
                try {
                    String badAddress = unresolvedPeer.get(5, TimeUnit.SECONDS);
                    if (badAddress != null) {
                        Logger.logDebugMessage("Failed to resolve peer address: " + badAddress);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Logger.logDebugMessage("Failed to add peer", e);
                } catch (TimeoutException e) {
                }
            }
            Logger.logDebugMessage("Known peers: " + peers.size());
        });

    }

    private static class Init {

        private final static Server peerServer;

        static {
            if (Peers.shareMyAddress) {
                peerServer = new Server();
                ServerConnector connector = new ServerConnector(peerServer);
                final int port = Constants.isTestnet ? TESTNET_PEER_PORT : Peers.myPeerServerPort;
                connector.setPort(port);
                final String host = Nxt.getStringProperty("fimk.peerServerHost");
                connector.setHost(host);
                connector.setIdleTimeout(Nxt.getIntProperty("fimk.peerServerIdleTimeout"));
                connector.setReuseAddress(true);
                peerServer.addConnector(connector);

                ServletHolder peerServletHolder = new ServletHolder(new PeerServlet());
                boolean isGzipEnabled = Nxt.getBooleanProperty("fimk.enablePeerServerGZIPFilter");
                peerServletHolder.setInitParameter("isGzipEnabled", Boolean.toString(isGzipEnabled));
                ServletHandler peerHandler = new ServletHandler();
                peerHandler.addServletWithMapping(peerServletHolder, "/*");
                if (Nxt.getBooleanProperty("fimk.enablePeerServerDoSFilter")) {
                    FilterHolder dosFilterHolder = peerHandler.addFilterWithMapping(DoSFilter.class, "/*", FilterMapping.DEFAULT);
                    dosFilterHolder.setInitParameter("maxRequestsPerSec", Nxt.getStringProperty("fimk.peerServerDoSFilter.maxRequestsPerSec"));
                    dosFilterHolder.setInitParameter("delayMs", Nxt.getStringProperty("fimk.peerServerDoSFilter.delayMs"));
                    dosFilterHolder.setInitParameter("maxRequestMs", Nxt.getStringProperty("fimk.peerServerDoSFilter.maxRequestMs"));
                    dosFilterHolder.setInitParameter("trackSessions", "false");
                    dosFilterHolder.setAsyncSupported(true);
                }
                if (isGzipEnabled) {
                    FilterHolder gzipFilterHolder = peerHandler.addFilterWithMapping(GzipFilter.class, "/*", FilterMapping.DEFAULT);
                    gzipFilterHolder.setInitParameter("methods", "GET,POST");
                    gzipFilterHolder.setAsyncSupported(true);
                }

                peerServer.setHandler(peerHandler);
                peerServer.setStopAtShutdown(true);
                ThreadPool.runBeforeStart(() -> {
                    try {
                        peerServer.start();
                        Logger.logMessage("Started peer networking server at " + host + ":" + port);
                    } catch (Exception e) {
                        Logger.logErrorMessage("Failed to start peer networking server", e);
                        throw new RuntimeException(e.toString(), e);
                    }
                }, true);
            } else {
                peerServer = null;
                Logger.logMessage("shareMyAddress is disabled, will not start peer networking server");
            }
        }

        private static void init() {}

        private Init() {}

    }

    static {
        Account.addListener(account -> peers.values().parallelStream().unordered()
                .filter(peer -> peer.getHallmark() != null && peer.getHallmark().getAccountId() == account.getId())
                .forEach(peer -> Peers.listeners.notify(peer, Event.WEIGHT)), Account.Event.BALANCE);
    }

    static {
        if (! Constants.isOffline) {
            ThreadPool.scheduleThread("PeerConnecting", new PeerConnectingThread(), 20);
            ThreadPool.scheduleThread("PeerUnBlacklisting", new PeerUnblacklistingThread(), 60);
            if (Peers.getMorePeers) {
                ThreadPool.scheduleThread("GetMorePeers", new GetMorePeersThread(), 20);
            }
            if (Peers.nodesRewardingEnabled) {
                ThreadPool.scheduleThread("NodesMonitoring", new NodesMonitoringThread(), 10);
            }
            if (API.enabled()) {
                // provide via API the extra info about peers (displayed in the client app)
                // Note PeerLastBlockInfo is invoked on block pushed so no need to do this extra times by this schedule (argument debounceGap)
                ThreadPool.scheduleThread("GetLastBlockInfo", PeerLastBlockInfo.get().getUpdater(28), 29);
            }
        }
    }

    public static void init() {
        Init.init();
    }

    public static void shutdown() {
        if (Init.peerServer != null) {
            try {
                Init.peerServer.stop();
            } catch (Exception e) {
                Logger.logShutdownMessage("Failed to stop peer server", e);
            }
        }
        ThreadPool.shutdownExecutor("sendingService", sendingService, 2);
        ThreadPool.shutdownExecutor("peersService", peersService, 5);
    }

    public static boolean addListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.addListener(listener, eventType);
    }

    public static boolean removeListener(Listener<Peer> listener, Event eventType) {
        return Peers.listeners.removeListener(listener, eventType);
    }

    static void notifyListeners(Peer peer, Event eventType) {
        Peers.listeners.notify(peer, eventType);
    }

    public static int getDefaultPeerPort() {
        if (defaultPeerPort == 0) {
            defaultPeerPort = Nxt.getIntPropertyNew("peerServerPort", DEFAULT_PEER_PORT, TESTNET_PEER_PORT);
        }
        return defaultPeerPort;
    }

    public static Collection<? extends Peer> getAllPeers() {
        return allPeers;
    }

    public static List<Peer> getActivePeers() {
        return getPeers(peer -> peer.getState() != Peer.State.NON_CONNECTED);
    }

    public static List<Peer> getPeers(final Peer.State state) {
        return getPeers(peer -> peer.getState() == state);
    }

    public static List<Peer> getPeers(Filter<Peer> filter) {
        return getPeers(filter, Integer.MAX_VALUE);
    }

    public static List<Peer> getPeers(Filter<Peer> filter, int limit) {
        return peers.values().parallelStream().unordered()
                .filter(filter::ok)
                .limit(limit)
                .collect(Collectors.toList());
    }

    public static Peer getPeer(String host) {
        return peers.get(host);
    }

    public static List<Peer> getInboundPeers() {
        return getPeers(Peer::isInbound);
    }

    public static boolean hasTooManyInboundPeers() {
        return getPeers(Peer::isInbound, maxNumberOfInboundConnections).size() >= maxNumberOfInboundConnections;
    }

    public static boolean hasTooManyOutboundConnections() {
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null,
                maxNumberOfOutboundConnections).size() >= maxNumberOfOutboundConnections;
    }

    public static PeerImpl findOrCreatePeer(String announcedAddress, boolean create) {
        if (announcedAddress == null) {
            return null;
        }
        announcedAddress = announcedAddress.trim().toLowerCase();
        PeerImpl peer;
        if ((peer = peers.get(announcedAddress)) != null) {
            return peer;
        }
        String host = selfAnnouncedAddresses.get(announcedAddress);
        if (host != null && (peer = peers.get(host)) != null) {
            return peer;
        }
        try {
            URI uri = new URI("http://" + announcedAddress);
            host = uri.getHost();
            if (host == null) {
                return null;
            }
            if ((peer = peers.get(host)) != null) {
                return peer;
            }
            String host2 = selfAnnouncedAddresses.get(host);
            if (host2 != null && (peer = peers.get(host2)) != null) {
                return peer;
            }
            InetAddress inetAddress = InetAddress.getByName(host);
            return findOrCreatePeer(inetAddress, addressWithPort(announcedAddress), create);
        } catch (URISyntaxException | UnknownHostException e) {
            //Logger.logDebugMessage("Invalid peer address: " + announcedAddress + ", " + e.toString());
            return null;
        }
    }

    static PeerImpl findOrCreatePeer(String host) {
        try {
            InetAddress inetAddress = InetAddress.getByName(host);
            return findOrCreatePeer(inetAddress, null, true);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    static PeerImpl findOrCreatePeer(final InetAddress inetAddress, final String announcedAddress, final boolean create) {

        if (!Nxt.getBooleanProperty("fimk.allowLocalhostPeer")) {
            if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress()) {
                return null;
            }
        }

        String host = inetAddress.getHostAddress();
        if (Peers.cjdnsOnly && !host.substring(0,2).equals("fc")) {
            return null;
        }
        //re-add the [] to ipv6 addresses lost in getHostAddress() above
        if (host.split(":").length > 2) {
            host = "[" + host + "]";
        }

        PeerImpl peer;
        if ((peer = peers.get(host)) != null) {
            return peer;
        }
        if (!create) {
            return null;
        }

        if (Peers.myAddress != null && Peers.myAddress.equalsIgnoreCase(announcedAddress)) {
            return null;
        }
        if (announcedAddress != null && announcedAddress.length() > MAX_ANNOUNCED_ADDRESS_LENGTH) {
            return null;
        }

        peer = new PeerImpl(host, announcedAddress);

        if (!Nxt.getBooleanProperty("fimk.allowLocalhostPeer") && Constants.isTestnet && peer.getPort() != TESTNET_PEER_PORT) {
            Logger.logDebugMessage("Peer " + host + " on testnet is not using port " + TESTNET_PEER_PORT + ", ignoring");
            return null;
        }
        if (!Constants.isTestnet && peer.getPort() == TESTNET_PEER_PORT) {
            Logger.logDebugMessage("Peer " + host + " is using testnet port " + peer.getPort() + ", ignoring");
            return null;
        }
        return peer;
    }

    static void setAnnouncedAddress(PeerImpl peer, String newAnnouncedAddress) {
        Peer oldPeer = peers.get(peer.getHost());
        if (oldPeer != null) {
            String oldAnnouncedAddress = oldPeer.getAnnouncedAddress();
            if (oldAnnouncedAddress != null && !oldAnnouncedAddress.equals(newAnnouncedAddress)) {
                Logger.logDebugMessage("Removing old announced address " + oldAnnouncedAddress + " for peer " + oldPeer.getHost());
                selfAnnouncedAddresses.remove(oldAnnouncedAddress);
            }
        }
        if (newAnnouncedAddress != null) {
            String oldHost = selfAnnouncedAddresses.put(newAnnouncedAddress, peer.getHost());
            if (oldHost != null && !peer.getHost().equals(oldHost)) {
                Logger.logDebugMessage("Announced address " + newAnnouncedAddress + " now maps to peer " + peer.getHost()
                        + ", removing old peer " + oldHost);
                oldPeer = peers.remove(oldHost);
                if (oldPeer != null) {
                    Peers.notifyListeners(oldPeer, Event.REMOVE);
                }
            }
        }
        peer.setAnnouncedAddress(newAnnouncedAddress);
    }

    public static boolean addPeer(Peer peer, String newAnnouncedAddress) {
        setAnnouncedAddress((PeerImpl)peer, newAnnouncedAddress.toLowerCase());
        return addPeer(peer);
    }

    public static boolean addPeer(Peer peer) {
        if (peers.put(peer.getHost(), (PeerImpl) peer) == null) {
            listeners.notify(peer, Event.NEW_PEER);
            return true;
        }
        return false;
    }

    public static PeerImpl removePeer(Peer peer) {
        if (peer.getAnnouncedAddress() != null) {
            selfAnnouncedAddresses.remove(peer.getAnnouncedAddress());
        }
        return peers.remove(peer.getHost());
    }

    public static void connectPeer(Peer peer) {
        peer.unBlacklist();
        ((PeerImpl)peer).connect();
    }

    public static void sendToSomePeers(Block block) {
        JSONObject request = block.getJSONObject();
        request.put("requestType", "processBlock");
        sendToSomePeers(request);
    }

    private static final int sendTransactionsBatchSize = 10;

    public static void sendToSomePeers(List<? extends Transaction> transactions) {
        int nextBatchStart = 0;
        while (nextBatchStart < transactions.size()) {
            JSONObject request = new JSONObject();
            JSONArray transactionsData = new JSONArray();
            for (int i = nextBatchStart; i < nextBatchStart + sendTransactionsBatchSize && i < transactions.size(); i++) {
                transactionsData.add(transactions.get(i).getJSONObject());
            }
            request.put("requestType", "processTransactions");
            request.put("transactions", transactionsData);
            sendToSomePeers(request);
            nextBatchStart += sendTransactionsBatchSize;
        }
    }

    public static void sendToSomePeers(Gossip gossip) {
        JSONObject request = gossip.getJSONObject();
        request.put("requestType", "processGossip");
        
        long priority = 0;
        Account account = Account.getAccount(gossip.getSenderId());
        if (account != null) {
            priority = account.getGuaranteedBalanceNQT();
        }
        sendToSomePeers(request, priority);
    }

    private static void sendToSomePeers(final JSONObject request) {
        sendToSomePeers(request, Long.MAX_VALUE);
    }
    
    private static void sendToSomePeers(JSONObject request, long priority) {
        sendToPeersRequestQueue.add(request, priority);
        sendingService.submit(() -> {
            final JSONObject req = sendToPeersRequestQueue.getNext();
            if (req == null) {
                return;
            }
            final JSONStreamAware jsonRequest = JSON.prepareRequest(req);
  
            boolean isGossip = "processGossip".equals(req.get("requestType"));
            int gossipEnabledCount = 0;

            int successful = 0;
            List<Future<JSONObject>> expectedResponses = new ArrayList<>();
            for (final Peer peer : peers.values()) {

                if (Peers.enableHallmarkProtection && peer.getWeight() < Peers.pushThreshold) {
                    continue;
                }
                if (isGossip && !peer.getGossipEnabled()) {
                    continue;
                }
                if (!peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null) {
                    Future<JSONObject> futureResponse = peersService.submit(() -> peer.send(jsonRequest));
                    expectedResponses.add(futureResponse);
                    if (isGossip) {
                        gossipEnabledCount++;
                    }
                }
                if (expectedResponses.size() >= Peers.sendToPeersLimit - successful) {
                    for (Future<JSONObject> future : expectedResponses) {
                        try {
                            JSONObject response = future.get();
                            if (response != null && response.get("error") == null) {
                                successful += 1;
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (ExecutionException e) {
                            Logger.logDebugMessage("Error in sendToSomePeers", e);
                        }

                    }
                    expectedResponses.clear();
                }
                if (successful >= Peers.sendToPeersLimit) {
                    return;
                }
            }
            
            if (isGossip && gossipEnabledCount == 0) {
                StringBuilder b = new StringBuilder();
                b.append("Could not send gossip message. No gossip enabled peers found.\n");
                b.append("Connected non-blacklisted peers:\n");
                for (final Peer peer : peers.values()) {
                    if (!peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED) {
                        b.append("  "+peer.getHost()+"\n");
                    }
                }
                Logger.logDebugMessage(b.toString());
            }
        });
    }

    public static Peer getAnyPeer(final Peer.State state, final boolean applyPullThreshold) {
        return getWeightedPeer(getPublicPeers(state, applyPullThreshold));
    }

    public static List<Peer> getPublicPeers(final Peer.State state, final boolean applyPullThreshold) {
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == state && peer.getAnnouncedAddress() != null
                && (!applyPullThreshold || !Peers.enableHallmarkProtection || peer.getWeight() >= Peers.pullThreshold));
    }

    public static Peer getWeightedPeer(List<Peer> selectedPeers) {
        if (selectedPeers.isEmpty()) {
            return null;
        }
        if (! Peers.enableHallmarkProtection || ThreadLocalRandom.current().nextInt(3) == 0) {
            return selectedPeers.get(ThreadLocalRandom.current().nextInt(selectedPeers.size()));
        }
        long totalWeight = 0;
        for (Peer peer : selectedPeers) {
            long weight = peer.getWeight();
            if (weight == 0) {
                weight = 1;
            }
            totalWeight += weight;
        }
        long hit = ThreadLocalRandom.current().nextLong(totalWeight);
        for (Peer peer : selectedPeers) {
            long weight = peer.getWeight();
            if (weight == 0) {
                weight = 1;
            }
            if ((hit -= weight) < 0) {
                return peer;
            }
        }
        return null;
    }

    static String addressWithPort(String address) {
        if (address == null) {
            return null;
        }
        try {
            URI uri = new URI("http://" + address);
            String host = uri.getHost();
            int port = uri.getPort();
            return port > 0 && port != Peers.getDefaultPeerPort() ? host + ":" + port : host;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static boolean hasTooFewKnownPeers() {
        return peers.size() < Peers.minNumberOfKnownPeers;
    }

    public static boolean hasTooManyKnownPeers() {
        return peers.size() > Peers.maxNumberOfKnownPeers;
    }

    static boolean hasEnoughConnectedPublicPeers(int limit) {
        return getPeers(peer -> !peer.isBlacklisted() && peer.getState() == Peer.State.CONNECTED && peer.getAnnouncedAddress() != null
                && (! Peers.enableHallmarkProtection || peer.getWeight() > 0), limit).size() >= limit;
    }

    /**
     * Set the communication logging mask
     *
     * @param   events              Communication event list or null to reset communications logging
     * @return                      TRUE if the communication logging mask was updated
     */
    public static boolean setCommunicationLoggingMask(String[] events) {
        boolean updated = true;
        int mask = 0;
        if (events != null) {
            for (String event : events) {
                switch (event) {
                    case "EXCEPTION":
                        mask |= LOGGING_MASK_EXCEPTIONS;
                        break;
                    case "HTTP-ERROR":
                        mask |= LOGGING_MASK_NON200_RESPONSES;
                        break;
                    case "HTTP-OK":
                        mask |= LOGGING_MASK_200_RESPONSES;
                        break;
                    default:
                        updated = false;
                }
                if (!updated)
                    break;
            }
        }
        if (updated)
            communicationLoggingMask = mask;
        return updated;
    }

    private Peers() {} // never

}
