package com.enigoo.terminal.csob;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Response {

    private static String TERMINAL_PING = "B0";
    private static String TERMINAL_RESPONSE = "B2";
    private static String TICKET_RESPONSE = "B4";


    private static final String NORMAL_PURCHASE = "T00";
    private static final String RETURN = "T04";
    private static final String CASH_ADVANCE = "T05";
    private static final String PURCHASE_WITH_CASHBACK = "T08";
    private static final String REVERSAL = "T10";
    private static final String CLOSE_TOTALS = "T60";
    private static final String TMS_CALL = "T90";
    private static final String HANDSHAKE = "T95";

    private static final String SUCCESS = "R000";
    private static final String USER_CANCEL = "R-01";
    private static final String CARD_ERROR = "R-09";
    private static final String CARD_EXPIRED = "R-10";
    private static final String CARD_YOUNG = "R-11";
    private static final String CARD_NO_ENOUGH_MONEY = "R-12";
    private static final String TIMEOUT = "R-18";
    private static final String CARD_BLOCKED = "R-29";
    private ArrayList<String> block;
    private String messageType;
    private String transactionType = null;
    private String responseType = null;
    private String flag = null;
    private boolean isDone = false;
    private boolean wantTicket = false;

    private List<String> merchantRecipe = new ArrayList<>();

    private List<String> customerRecipe = new ArrayList<>();

    public Response(ArrayList<String> block) {
        this.block = block;
        if(block.size() > 0)
            this.messageType = (String.valueOf(block.get(0).charAt(0)) + String.valueOf(block.get(0).charAt(1)));
        switch (this.messageType){
            case "B2" :
                this.setDone(true);
                this.setResponseType(block.get(2));
                this.setTransactionType(block.get(1));
                checkFlag(block.get(0).substring(24,28));
                break;
            case "B4":
                this.setDone(true);
                this.setResponseType(block.get(2));
                break;
            default:
                this.setDone(false);
                this.setResponseType(null);

        }
    }

    public List<String> getRecipes(){
        List<String> recipes = new ArrayList<>();
        for (String bl: block) {
            if(bl.startsWith("T0") || bl.startsWith("T1") ||bl.startsWith("T2") || bl.startsWith("T3")){
                recipes.add(bl.substring(2).replaceAll("^\\s+", ""));
            }
        }
        return recipes;
    }

    private void checkFlag(String hexFlag){
        int flag = 0;
        for(int i = 0; i<hexFlag.length(); i++){
            flag+=Integer.parseInt(hexFlag.substring(i,i+1),16);
        }
        StringBuilder flagStr = new StringBuilder(Integer.toBinaryString(flag));
        while(flagStr.length()<16){
            flagStr.insert(0, "0");
        }
        this.setFlag(flagStr.toString());
        if(this.getMessageType().equals("B2") && flagStr.charAt(this.flag.length()-2)=='1'){
            this.setWantTicket(true);
        }
    }

    public boolean wantNext(){
        return block.get(block.size()-1).equals("t1");
    }

    public boolean isWantTicket() {
        return wantTicket;
    }

    public void setWantTicket(boolean wantTicket) {
        this.wantTicket = wantTicket;
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

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public List<String> getMerchantRecipe() {
        return merchantRecipe;
    }

    public void setMerchantRecipe(List<String> merchantRecipe) {
        this.merchantRecipe = merchantRecipe;
    }

    public List<String> getCustomerRecipe() {
        return customerRecipe;
    }

    public void setCustomerRecipe(List<String> customerRecipe) {
        this.customerRecipe = customerRecipe;
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
            case CLOSE_TOTALS:
                json.put("type","CLOSE_TOTALS");
                break;
            case HANDSHAKE:
                json.put("type","HANDSHAKE");
                break;
            case TMS_CALL:
                json.put("type","TMS_CALL");
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

        json.put("merchantRecipe",getMerchantRecipe());
        json.put("customerRecipe",getCustomerRecipe());


        return json.toString();
    }
}
