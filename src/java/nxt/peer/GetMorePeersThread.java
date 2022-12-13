package nxt.peer;

import nxt.Db;
import nxt.Nxt;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.*;
import java.util.stream.Collectors;

public class GetMorePeersThread implements Runnable {

    private final JSONStreamAware getPeersRequest;

    {
        JSONObject request = new JSONObject();
        request.put("requestType", "getPeers");
        getPeersRequest = JSON.prepareRequest(request);
    }

    private volatile boolean updatedPeer;

    @Override
    public void run() {

        try {
            try {
                if (Peers.hasTooManyKnownPeers()) {
                    return;
                }
                Peer peer = Peers.getAnyPeer(Peer.State.CONNECTED, true);
                if (peer == null) {
                    return;
                }
                JSONObject response = peer.send(getPeersRequest, 10 * 1024 * 1024);
                if (response == null) {
                    return;
                }
                JSONArray peers = (JSONArray) response.get("peers");
                Set<String> addedAddresses = new HashSet<>();
                if (peers != null) {
                    int now = Nxt.getEpochTime();
                    for (Object announcedAddress : peers) {
                        PeerImpl newPeer = Peers.findOrCreatePeer((String) announcedAddress, true);
                        if (newPeer != null) {
                            if (now - newPeer.getLastUpdated() > 24 * 3600) {
                                newPeer.setLastUpdated(now);
                                updatedPeer = true;
                            }
                            Peers.addPeer(newPeer);
                            addedAddresses.add((String) announcedAddress);
                            if (Peers.hasTooManyKnownPeers()) {
                                break;
                            }
                        }
                    }
                    if (Peers.savePeers && updatedPeer) {
                        updateSavedPeers();
                        updatedPeer = false;
                    }
                }

                JSONArray myPeers = Peers.getAllPeers().parallelStream().unordered()
                        .filter(myPeer -> !myPeer.isBlacklisted() && myPeer.getAnnouncedAddress() != null
                                && myPeer.getState() == Peer.State.CONNECTED && myPeer.shareAddress()
                                && !addedAddresses.contains(myPeer.getAnnouncedAddress())
                                && !myPeer.getAnnouncedAddress().equals(peer.getAnnouncedAddress()))
                        .map(Peer::getAnnouncedAddress)
                        .collect(Collectors.toCollection(JSONArray::new));
                if (myPeers.size() > 0) {
                    JSONObject request = new JSONObject();
                    request.put("requestType", "addPeers");
                    request.put("peers", myPeers);
                    peer.send(JSON.prepareRequest(request), 0);
                }

            } catch (Exception e) {
                Logger.logDebugMessage("Error requesting peers from a peer", e);
            }
        } catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }

    }

    private void updateSavedPeers() {
        int now = Nxt.getEpochTime();
        //
        // Load the current database entries and map the announced address to database entry
        //
        List<PeerDb.Entry> oldPeers = PeerDb.loadPeers();
        Map<String, PeerDb.Entry> oldMap = new HashMap<>();
        oldPeers.forEach(entry -> oldMap.put(entry.getAddress(), entry));
        //
        // Create the current peer map (note that there can be duplicate peer entries with
        // the same announced address)
        //
        Map<String, PeerDb.Entry> currentPeers = new HashMap<>();
        Peers.peers.values().forEach(peer -> {
            if (peer.getAnnouncedAddress() != null && !peer.isBlacklisted() && now - peer.getLastUpdated() < 7 * 24 * 3600)
                currentPeers.put(peer.getAnnouncedAddress(), new PeerDb.Entry(peer.getAnnouncedAddress(), peer.getLastUpdated()));
        });
        //
        // Build toDelete, toUpdate and toAdd lists
        //
        List<PeerDb.Entry> toDelete = new ArrayList<>(oldPeers.size());
        oldPeers.forEach(entry -> {
            if (currentPeers.get(entry.getAddress()) == null)
                toDelete.add(entry);
        });
        List<PeerDb.Entry> toUpdate = new ArrayList<>(currentPeers.size());
        List<PeerDb.Entry> toAdd = new ArrayList<>(currentPeers.size());
        currentPeers.values().forEach(entry -> {
            PeerDb.Entry oldEntry = oldMap.get(entry.getAddress());
            if (oldEntry == null)
                toAdd.add(entry);
            else if (entry.getLastUpdated() - oldEntry.getLastUpdated() > 24 * 3600)
                toUpdate.add(entry);
        });
        //
        // Nothing to do if all of the lists are empty
        //
        if (toDelete.isEmpty() && toUpdate.isEmpty() && toAdd.isEmpty())
            return;
        //
        // Update the peer database
        //
        try {
            Db.db.beginTransaction();
            PeerDb.deletePeers(toDelete);
            PeerDb.updatePeers(toUpdate);
            PeerDb.addPeers(toAdd);
            Db.db.commitTransaction();
        } catch (Exception e) {
            Db.db.rollbackTransaction();
            throw e;
        } finally {
            Db.db.endTransaction();
        }
    }

}