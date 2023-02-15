package nxt.peer;

import nxt.Nxt;
import nxt.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

public class PeerConnectingThread implements Runnable {

    @Override
    public void run() {
        try {
            try {
                final int now = Nxt.getEpochTime();
                if (!Peers.hasEnoughConnectedPublicPeers(Peers.maxNumberOfConnectedPublicPeers)) {
                    List<Future> futures = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        futures.add(Peers.peersService.submit(() -> {
                            PeerImpl peer = (PeerImpl) Peers.getAnyPeer(ThreadLocalRandom.current().nextInt(2) == 0
                                    ? Peer.State.NON_CONNECTED
                                    : Peer.State.DISCONNECTED, false);
                            if (peer != null && now - peer.getLastConnectAttempt() > 600) {
                                peer.connect();
                                if (peer.getState() == Peer.State.CONNECTED &&
                                        Peers.enableHallmarkProtection && peer.getWeight() == 0 &&
                                        Peers.hasTooManyOutboundConnections()) {
                                    Logger.logDebugMessage("Too many outbound connections, deactivating peer " + peer.getHost());
                                    peer.deactivate();
                                }
                            }
                        }));
                    }
                    for (Future future : futures) {
                        future.get();
                    }
                }

                for (PeerImpl peer : Peers.peers.values()) {
                    if (peer.getState() == Peer.State.CONNECTED && now - peer.getLastUpdated() > 3600) {
                        peer.connect();
                    }
                }

                if (Peers.hasTooManyKnownPeers() && Peers.hasEnoughConnectedPublicPeers(Peers.maxNumberOfConnectedPublicPeers)) {
                    int initialSize = Peers.peers.size();
                    for (PeerImpl peer : Peers.peers.values()) {
                        if (now - peer.getLastUpdated() > 24 * 3600) {
                            peer.remove();
                        }
                        if (Peers.hasTooFewKnownPeers()) {
                            break;
                        }
                    }
                    if (Peers.hasTooManyKnownPeers()) {
                        PriorityQueue<PeerImpl> sortedPeers = new PriorityQueue<>(Peers.peers.values());
                        int skipped = 0;
                        while (skipped < Peers.minNumberOfKnownPeers) {
                            if (sortedPeers.poll() == null) {
                                break;
                            }
                            skipped += 1;
                        }
                        while (!sortedPeers.isEmpty()) {
                            sortedPeers.poll().remove();
                        }
                    }
                    Logger.logDebugMessage("Reduced peer pool size from " + initialSize + " to " + Peers.peers.size());
                }

                for (String wellKnownPeer : Peers.wellKnownPeers) {
                    PeerImpl peer = Peers.findOrCreatePeer(wellKnownPeer, true);
                    if (peer != null && now - peer.getLastUpdated() > 3600 && now - peer.getLastConnectAttempt() > 600) {
                        Peers.peersService.submit(() -> {
                            Peers.addPeer(peer);
                            Peers.connectPeer(peer);
                        });
                    }
                }

                Peers.peers.values().parallelStream().unordered()
                        .filter(peer -> peer.getLastInboundRequest() != 0 && now - peer.getLastInboundRequest() > 1800)
                        .forEach(peer -> {
                            peer.setLastInboundRequest(0);
                            Peers.notifyListeners(peer, Peers.Event.REMOVE_INBOUND);
                        });

            } catch (Exception e) {
                Logger.logDebugMessage("Error connecting to peer", e);
            }
        } catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t);
            t.printStackTrace();
            System.exit(1);
        }

    }

}