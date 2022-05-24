package com.enigoo.terminal;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Response {

    private static String TERMINAL_PING = "B0";
    private static String TERMINAL_RESPONSE = "B2";


    private static final String NORMAL_PURCHASE = "T00";
    private static final String RETURN = "T04";
    private static final String CASH_ADVANCE = "T05";
    private static final String PURCHASE_WITH_CASHBACK = "T08";
    private static final String REVERSAL = "T10";


    private static final String SUCCESS = "R000";
    private static final String USER_CANCEL = "R-01";
    private static final String CARD_ERROR = "R-09";
    private static final String CARD_EXPIRED = "R-10";
    private static final String CARD_YOUNG = "R-11";
    private static final String CARD_NO_ENOUGH_MONEY = "R-12";
    private static final String TIMEOUT = "R-18";
    private static final String CARD_BLOCKED = "R-29";

    private String messageType;
    private String transactionType = null;
    private String responseType = null;
    private boolean isDone = false;

    public Response(ArrayList<String> block) {
      if(block.size() > 0)
        this.messageType = (String.valueOf(block.get(0).charAt(0)) + String.valueOf(block.get(0).charAt(1)));
        if (this.messageType.equals(TERMINAL_RESPONSE)) {
            this.setTransactionType(block.get(1));
            this.setResponseType(block.get(2));
            this.setDone(true);

        }
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public void Logger() {
        Log.i("TP", this.getTransactionType());
        Log.i("RT", this.getResponseType());
    }

    public String toJsonString() throws JSONException {
        JSONObject json = new JSONObject();

        switch (this.transactionType) {
            case NORMAL_PURCHASE:
                json.put("type", "PURCHASE");
                break;
            case RETURN:
                json.put("type", "RETURN");
                break;
            case PURCHASE_WITH_CASHBACK:
                json.put("type", "PURCHASE_WITH_CASHBACK");
                break;
            case REVERSAL:
                json.put("type", "REVERSAL");
                break;
        }

        switch (this.responseType) {
            case SUCCESS:
                json.put("status", "SUCCESS");
                break;
            case USER_CANCEL:
                json.put("status", "CANCEL");
                break;
            case CARD_ERROR:
                json.put("status", "CARD_ERROR");
                break;
            case CARD_EXPIRED:
                json.put("status", "CARD_EXPIRED");
                break;
            case CARD_YOUNG:
                json.put("status", "CARD_YOUNG");
                break;
            case CARD_NO_ENOUGH_MONEY:
                json.put("status", "CARD_NO_ENOUGH_MONEY");
                break;
            case TIMEOUT:
                json.put("status", "TIMEOUT");
                break;
            case CARD_BLOCKED:
                json.put("status", "CARD_BLOCKED");
                break;
            default:
                json.put("status", "DEFAULT_ERROR");
                break;
        }


        return json.toString();
    }
}
