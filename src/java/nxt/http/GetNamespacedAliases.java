package nxt.http;

import nxt.NamespacedAlias;
import nxt.db.FilteringIterator;
import nxt.util.Filter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetNamespacedAliases extends APIServlet.APIRequestHandler {

    static final GetNamespacedAliases instance = new GetNamespacedAliases();

    private GetNamespacedAliases() {
        super(new APITag[] {APITag.ALIASES}, "account", "timestamp", "filter", "firstIndex", "lastIndex");
    }

    @SuppressWarnings("unchecked")
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
      
        final long accountId = ParameterParser.getAccount(req).getId();
        final int timestamp = ParameterParser.getTimestamp(req);
        final String filter = ParameterParser.getFilter(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = Math.max(ParameterParser.getLastIndex(req), 0);
        if ((lastIndex - firstIndex) > 1000) {
            lastIndex = firstIndex + 1000; /* Guard against server spam */
        }        
        
        JSONArray aliases = new JSONArray();
        try (FilteringIterator<NamespacedAlias> aliasIterator = new FilteringIterator<>(NamespacedAlias.getAliasesByOwner(accountId, 0, -1),
                new Filter<NamespacedAlias>() {
                    @Override
                    public boolean ok(NamespacedAlias alias) {
                        if (alias.getTimestamp() >= timestamp) {
                            if (filter != null && ! alias.getAliasName().startsWith(filter)) {
                                return false;
                            }
                            return true;
                        }
                        return false;
                    }
                }, firstIndex, lastIndex)) {
            while(aliasIterator.hasNext()) {
                aliases.add(JSONData.namespacedAlias(aliasIterator.next()));
            }
        }        
        
        JSONObject response = new JSONObject();
        response.put("aliases", aliases);
        return response;        
    }

}