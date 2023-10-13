package nxt.peer;

import nxt.AppVersion;
import nxt.Nxt;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONObject;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PeerLastBlockInfo {

    private static PeerLastBlockInfo instance;

    public static PeerLastBlockInfo get() {
        if (instance == null) {
            synchronized (PeerLastBlockInfo.class) {
                if (instance == null) {
                    instance = new PeerLastBlockInfo();
                }
            }
        }
        return instance;
    }

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private ConcurrentMap<String, JSONObject> peerInfos;
    private long updateTime;

    public PeerLastBlockInfo() {
        this.peerInfos = new ConcurrentHashMap<>();
    }

    public ConcurrentMap<String, JSONObject> getPeerInfos() {
        return peerInfos;
    }

    public Runnable getUpdater(int debounceGap) {
        return new Runnable() {
            @Override
            public void run() {
                if ((System.currentTimeMillis() - updateTime) / 1000 < debounceGap) return;
                requestInfos();
            }
        };
    }

    public Integer requestInfos(int delayMillis) {
        if (delayMillis > 0) {
            executorService.schedule(() -> requestInfos(), delayMillis, TimeUnit.MILLISECONDS);
        } else {
            this.requestInfos();
        }
        return null;
    }

    public int requestInfos() {
        AtomicInteger updatedCount = new AtomicInteger();
        try {
            try {
                Peers.peers.values().parallelStream().unordered()
                        .filter(peer -> peer.getState() != Peer.State.NON_CONNECTED)
                        .forEach(peer -> updatedCount.set(updatedCount.get() + request(peer)));
                updateTime = System.currentTimeMillis();
            } catch (Exception e) {
                Logger.logErrorMessage("", e);
            }
        } catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t);
        }
        return updatedCount.get();
    }

    private int request(Peer peer) {
        AppVersion peerVersion = new AppVersion(peer.getVersion());
        AppVersion thisVersion = new AppVersion(Nxt.VERSION);
        if (thisVersion.compareTo(peerVersion) > 0) return 0;

        JSONObject request = new JSONObject();
        request.put("requestType", "getLastBlockInfo");
        JSONObject response = peer.send(JSON.prepareRequest(request));
        if (response != null) {
            Object error = response.get("error");
            if (error == null) {
                response.put("requestTimeMillis", System.currentTimeMillis());
                peerInfos.put(peer.getHost(), response);
                return 1;
            } else {
                Logger.logErrorMessage(error.toString());
            }
        }
        return 0;
    }

}