package nxt.http;

import nxt.NxtException;

import org.json.simple.JSONStreamAware;

@SuppressWarnings("serial")
public final class ParameterException extends NxtException {

    private final JSONStreamAware errorResponse;

    public ParameterException(JSONStreamAware errorResponse) {
        this.errorResponse = errorResponse;
    }

    public JSONStreamAware getErrorResponse() {
        return errorResponse;
    }

}
