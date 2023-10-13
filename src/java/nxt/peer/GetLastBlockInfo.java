package nxt.peer;

import nxt.Block;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetLastBlockInfo extends PeerServlet.PeerRequestHandler {

    static final GetLastBlockInfo instance = new GetLastBlockInfo();

    private GetLastBlockInfo() {}

    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {
        Block block = Nxt.getBlockchain().getLastBlock();

        JSONObject json = new JSONObject();
        json.put("id", Long.toUnsignedString(block.getId()));
        json.put("version", block.getVersion());
        json.put("timestamp", block.getTimestamp());
        json.put("height", block.getHeight());
        json.put("previousBlock", Long.toUnsignedString(block.getPreviousBlockId()));
        json.put("generatorPublicKey", Convert.toHexString(block.getGeneratorPublicKey()));
        json.put("generatorId", Long.toUnsignedString(block.getGeneratorId()));

        return json;
    }

}
