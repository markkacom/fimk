package nxt.peer;

import nxt.util.Logger;

public class PeerUnblacklistingThread implements Runnable {

    @Override
    public void run() {
        try {
            try {

                long curTime = System.currentTimeMillis();
                for (PeerImpl peer : Peers.peers.values()) {
                    peer.updateBlacklistedStatus(curTime);
                }

            } catch (Exception e) {
                Logger.logDebugMessage("Error un-blacklisting peer", e);
            }
        } catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }
    }

}