package com.enigoo.terminal.fiskalpro;

import org.json.JSONException;
import org.json.JSONObject;

public class FPResponse {


    private static final int SUCCESS = 17;
    private static final int DENIED = 18;
    private static int OK = 6;

    private boolean isDone = false;
    private byte status;

    public FPResponse(byte _byte) {
        this.status = _byte;
        if (_byte == SUCCESS || _byte == DENIED) {
            this.isDone = true;
        }
    }

    public boolean isDone() {
        return isDone;
    }

    public JSONObject toJsonObject() throws JSONException {
        JSONObject json = new JSONObject();

        switch ((int) this.status) {
            case SUCCESS:
                json.put("status", "SUCCESS");
                break;
            case DENIED:
                json.put("status", "CANCEL");
                break;
            default:
                json.put("status", "DEFAULT_ERROR");
                break;
        }


        return json;
    }

}
