package com.enigoo.terminal.csob;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.io.UnsupportedEncodingException;
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

    private static final String OPERATION_DISCARD = "R-21";
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

    private List<byte[]> messages;

    public Response(ArrayList<String> block) {
        this.block = block;
        if (block.size() > 0) {
            this.messageType = (String.valueOf(block.get(0).charAt(0)) + String.valueOf(block.get(0).charAt(1)));
        } else {
            this.messageType = "";
        }
        switch (this.messageType) {
            case "B2":
                this.setDone(true);
                this.setResponseType(block.get(2));
                this.setTransactionType(block.get(1));
                checkFlag(block.get(0).substring(24, 28));
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

    public Response(String transactionType, String responseType) {
        this.transactionType = transactionType;
        this.responseType = responseType;
        this.isDone = false;
        this.wantTicket = false;
        this.messageType = null;
    }

    public List<String> getRecipes() {
        List<String> recipes = new ArrayList<>();
        for (String bl : block) {
            if (bl.startsWith("T0") || bl.startsWith("T1") || bl.startsWith("T2") || bl.startsWith("T3")) {
                recipes.add(bl.substring(2).replaceAll("^\\s+", ""));
            }
        }
        return recipes;
    }

    private void checkFlag(String hexFlag) {
        this.setFlag(Integer.toBinaryString(Integer.parseInt(hexFlag, 16)));
        if (this.getMessageType().equals("B2") && flag.charAt(this.flag.length() - 2) == '1') {
            this.setWantTicket(true);
        }
    }

    public boolean wantNext() {
        return block.get(block.size() - 1).equals("t1");
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

    public void setMessages(List<byte[]> messages){
      this.messages = messages;
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

    public WritableMap toReactObject() {
        WritableMap params = Arguments.createMap();
        switch (this.transactionType) {
            case NORMAL_PURCHASE:
                params.putString("type", "PURCHASE");
                break;
            case RETURN:
                params.putString("type", "RETURN");
                break;
            case PURCHASE_WITH_CASHBACK:
                params.putString("type", "PURCHASE_WITH_CASHBACK");
                break;
            case REVERSAL:
                params.putString("type", "REVERSAL");
                break;
            case CLOSE_TOTALS:
                params.putString("type", "CLOSE_TOTALS");
                break;
            case HANDSHAKE:
                params.putString("type", "HANDSHAKE");
                break;
            case TMS_CALL:
                params.putString("type", "TMS_CALL");
                break;
            case "0":
                params.putString("type", "CONNECTION");
                break;
            default:
                params.putString("type", "UNKNOWN");
                break;
        }
        switch (this.responseType) {
            case SUCCESS:
                params.putString("status", "SUCCESS");
                break;
            case USER_CANCEL:
                params.putString("status", "CANCEL");
                break;
            case CARD_ERROR:
                params.putString("status", "CARD_ERROR");
                break;
            case CARD_EXPIRED:
                params.putString("status", "CARD_EXPIRED");
                break;
            case CARD_YOUNG:
                params.putString("status", "CARD_YOUNG");
                break;
            case CARD_NO_ENOUGH_MONEY:
                params.putString("status", "CARD_NO_ENOUGH_MONEY");
                break;
            case TIMEOUT:
                params.putString("status", "TIMEOUT");
                break;
            case CARD_BLOCKED:
                params.putString("status", "CARD_BLOCKED");
                break;
            case "0":
                params.putString("status", "LOST");
                break;
            default:
                params.putString("status", "DEFAULT_ERROR");
                break;
        }
        WritableArray arrayMerch = Arguments.createArray();
        for (String row : getMerchantRecipe()) {
            arrayMerch.pushString(row);
        }
        params.putArray("merchantRecipe", arrayMerch);
        WritableArray arrayCust = Arguments.createArray();
        for (String row : getCustomerRecipe()) {
            arrayCust.pushString(row);
        }
        params.putArray("customerRecipe", arrayCust);

        params.putString("responseCode",responseType);
        try {
          WritableArray arrayMess = parseMessages();
          params.putArray("messages", arrayMess);
        }catch (UnsupportedEncodingException ex){

        }


        return params;
    }

    private WritableArray parseMessages() throws UnsupportedEncodingException {
      char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
      WritableArray messagesArray = Arguments.createArray();
      for (byte[] mess: this.messages) {
        int dateStart = 13;
        int dateEnd = 25;
        String date = new String(mess,"ISO-8859-2").substring(dateStart,dateEnd);

        int year = Integer.parseInt(date.substring(0,2));
        int month = Integer.parseInt(date.substring(2,4));
        int day = Integer.parseInt(date.substring(4,6));
        int hour = Integer.parseInt(date.substring(6,8));
        int minutes = Integer.parseInt(date.substring(8,10));
        int seconds = Integer.parseInt(date.substring(10,12));

        char[] hexChars = new char[mess.length * 2];
        for (int j = 0; j < mess.length; j++) {
          int v = mess[j] & 0xFF;
          hexChars[j * 2] = HEX_ARRAY[v >>> 4];
          hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        String hexCharsInString = new String(hexChars);
        hexCharsInString = hexCharsInString.replaceAll("(.{" + 2 + "})", "$1 ").trim();

        WritableMap obj = Arguments.createMap();
        obj.putString("date",year+"-"+month+"-"+day+"-"+hour+"-"+minutes+"-"+seconds);
        obj.putString("data",hexCharsInString);
        messagesArray.pushMap(obj);
      }

      return messagesArray;
    }
}
