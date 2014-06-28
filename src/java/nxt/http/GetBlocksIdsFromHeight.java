package nxt.http;

import static nxt.http.JSONResponses.INCORRECT_BLOCK;

import javax.servlet.http.HttpServletRequest;

import nxt.Nxt;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetBlocksIdsFromHeight extends APIServlet.APIRequestHandler {

    static final GetBlocksIdsFromHeight instance = new GetBlocksIdsFromHeight();

    private GetBlocksIdsFromHeight() {
    super("fromHeight", "toHeight");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String fromHeightValue = Convert.emptyToNull(req.getParameter("fromHeight"));
        String toHeightValue = Convert.emptyToNull(req.getParameter("toHeight"));

        int fromHeight = 0;
        int toHeight = Integer.MAX_VALUE;
        
        if (fromHeightValue != null) {
          try {
            fromHeight = Integer.parseInt(fromHeightValue);
          } catch (NumberFormatException e) {
            return INCORRECT_BLOCK;
          }
        }
        if (toHeightValue != null) {
          try {
            toHeight = Integer.parseInt(toHeightValue);
          } catch (NumberFormatException e) {
            return INCORRECT_BLOCK;
          }
        }
        toHeight = Math.min(toHeight, fromHeight+1440);
        toHeight = Math.min(toHeight, Nxt.getBlockchain().getHeight());
        
        JSONObject response = new JSONObject();
        JSONArray blockIds = new JSONArray();        

        try {
          for (int i=fromHeight; i<=toHeight; i++) {
            Long id = Nxt.getBlockchain().getBlockIdAtHeight(i);
            blockIds.add(Convert.toUnsignedLong(id));            
          }
        } catch (RuntimeException e) {
          return INCORRECT_BLOCK;
        }

        response.put("fromHeight", fromHeight);
        response.put("blockIds", blockIds);
        return response;
    }

}