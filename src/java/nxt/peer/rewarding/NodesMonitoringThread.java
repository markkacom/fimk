package nxt.peer.rewarding;

import nxt.peer.Peer;
import nxt.peer.Peers;
import nxt.reward.AccountNode;
import nxt.util.Logger;
import org.json.simple.JSONObject;

import java.util.List;

public class NodesMonitoringThread implements Runnable {

    public static boolean roundSuccess;


    /**
     * Run node scores calculation round.
     * Scores define the list of reward candidates (rewardable node accounts).
     * Nodes with scores are saved in the persistence storage.
     * The rewarding module use scores to select candidate accounts for rewarding.
     */
    @Override
    public void run() {
        if (AccountNode.REGISTRATION_ACCOUNT_ID == 0) return;
        try {
            List<AccountNode> accountNodes = AccountNode.getActualAccountNodes();
            int correctAnswerCount = 0;
            for (AccountNode accountNode : accountNodes) {
                Peer peer = Peers.getPeer(accountNode.getAddress());
                if (peer == null) {
                    // todo create the peer
                    if (peer == null) {
                        if (accountNode.getScore() > 0) {
                            accountNode.setRoundScore(Math.max(accountNode.getScore() - 1, AccountNode.MIN_SCORE));
                        }
                    }
                    continue;
                }
                accountNode.updateRequestTimestamp();

                JSONObject response = peer.send(Peers.myPeerInfoRequest);

                if (response == null) {
                    accountNode.setRoundScore(Math.max(accountNode.getScore() - 2, AccountNode.MIN_SCORE));
                    continue;
                }
                String peerAccountNode = (String) response.get("nodeToken");

                if (accountNode.getToken().equals(peerAccountNode)) {
                    if (accountNode.getScore() < -4) {
                        accountNode.setRoundScore(-3);
                    } else {
                        accountNode.setRoundScore(Math.min(accountNode.getScore() + 1, 4));
                    }
                    correctAnswerCount++;
                } else {
                    accountNode.setRoundScore(Math.max(accountNode.getScore() - 3, AccountNode.MIN_SCORE));
                }
            }

            // apply scores if was at least 1 correct answer,
            // otherwise perhaps the network problem on this node so the round should be ignored (not applied)
            roundSuccess = correctAnswerCount > 0;
            if (roundSuccess) {
                for (AccountNode nodeToken : accountNodes) {
                    nodeToken.updateScore();
                }
                // todo if (nodeToken.isChanged())
                AccountNode.update(accountNodes);
            }
        } catch (Exception e) {
            Logger.logErrorMessage("", e);
        }
    }

    // not completed function. It is needed in case the message from node's account is encrypted
    private void resolveToken(AccountNode accountNode) {
        /*int i = accountNode.getToken().indexOf("notresolved");
        if (i > -1) {
            String encrypted = accountNode.getToken().substring(i + "notresolved".length());
            i = encrypted.indexOf(" ");
            String part1 = encrypted.substring(0, i).trim();
            String part2 = encrypted.substring(i).trim();
            byte[] data = Convert.parseHexString(part1);
            byte[] nonce = Convert.parseHexString(part2);
            EncryptedData encryptedData = new EncryptedData(data, nonce);
            byte[] decrypted = encryptedData.decrypt();
            String tokenAndAddress = Convert.toString(decrypted);
        }*/

    }

}
