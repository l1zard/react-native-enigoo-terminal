package com.enigoo.terminal.csob;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Response {

    private static final String SUCCESS = "000";
    private static final String USER_CANCEL = "-01";
    private static final String CARD_ERROR = "-09";
    private static final String CARD_EXPIRED = "-10";
    private static final String CARD_YOUNG = "-11";
    private static final String CARD_NO_ENOUGH_MONEY = "-12";
    private static final String TIMEOUT = "-18";

    private static final String OPERATION_DISCARD = "-21";
    private static final String CARD_BLOCKED = "-29";
    private ArrayList<String> block;
    private String messageType;
    private TransactionTypes transactionType = null;
    private String responseType = null;

    private String responseMessage = null;
    private String flag = null;
    private boolean isDone = false;
    private boolean wantTicket = false;
    private boolean wantSign = false;
    private boolean forcedConfirm = false;


    private List<String> merchantRecipe = new ArrayList<>();

    private List<String> customerRecipe = new ArrayList<>();

    private List<ResponseMessage> messages;

    public Response(ArrayList<String> block) {
        this.block = block;
        if (block.size() > 0) {
            this.messageType = (block.get(0).substring(0, 2));
            parseResponseType();
            parseResponseMessage();
        } else {
            this.messageType = "";
        }
        switch (this.messageType) {
            case "B2":
                setDone(true);
                parseTransactionType();
                checkFlag(block.get(0).substring(24, 28));
                break;
            case "B4":
                setDone(true);
                break;
            default:
                this.setDone(false);
        }
    }

    private void parseTransactionType() {
        for (String b : block) {
            if (b.startsWith("T")) {
                this.setTransactionType(getTransactionType(b));
            }
        }
    }

    private TransactionTypes getTransactionType(String type) {
        for (TransactionTypes trType : TransactionTypes.values()) {
            if (trType.getCode().equals(type)) return trType;
        }
        return null;
    }

    private void parseResponseType() {
        for (String b : block) {
            if (b.startsWith("R")) {
                this.responseType = b.substring(1);
            }
        }
    }

    private void parseResponseMessage() {
        for (String b : block) {
            if (b.startsWith("g")) {
                this.responseMessage = b.substring(1);
            }
        }
    }

    public Response(TransactionTypes transactionType, String responseType) {
        this.transactionType = transactionType;
        this.responseType = responseType;
        this.isDone = false;
        this.wantTicket = false;
        this.messageType = null;
        this.messages = new ArrayList<>();
        this.merchantRecipe = new ArrayList<>();
        this.customerRecipe = new ArrayList<>();
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
        if (this.getMessageType().equals("B2") && flag.charAt(14) == '1') {
            this.setWantTicket(true);
        }
        if (this.getMessageType().equals("B2") && flag.charAt(15) == '1') {
            this.setWantSign(true);
        }if(this.getMessageType().equals("B2") && flag.charAt(0)=='1'){
            this.setForcedConfirm(true);
        }
    }

    public boolean wantNext() {
        return block.size() > 0 && block.get(block.size() - 1).equals("t1");
    }

    public boolean isWantTicket() {
        return wantTicket;
    }

    public void setWantTicket(boolean wantTicket) {
        this.wantTicket = wantTicket;
    }

    public boolean isWantSign() {
        return wantSign;
    }

    private void setWantSign(boolean wantSign) {
        this.wantSign = wantSign;
    }

    public TransactionTypes getTransactionType() {
        return transactionType;
    }

    private void setTransactionType(TransactionTypes transactionType) {
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

    private void setDone(boolean done) {
        isDone = done;
    }

    public String getMessageType() {
        return messageType;
    }

    private void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public void setMessages(List<ResponseMessage> messages) {
        this.messages = messages;
    }

    public boolean isForcedConfirm() {
        return forcedConfirm;
    }

    private void setForcedConfirm(boolean forcedConfirm) {
        this.forcedConfirm = forcedConfirm;
    }

    public void Logger() {
        Log.i("TP", this.getTransactionType().getCode());
        Log.i("RT", this.getResponseType());
    }

    private void setFlag(String flag) {
        if (flag.length() < 16) {
            StringBuilder flagBuilder = new StringBuilder(flag);
            while (flagBuilder.length() != 16) {
                flagBuilder.insert(0, "0");
            }
            flag = flagBuilder.toString();
        }
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
        params.putBoolean("sign", isWantSign());

        if(isWantSign()){
            params.putString("approvalCode",parseApprovalCode());
        }

        if (transactionType != null) {
            params.putString("type", this.transactionType.getName());
        } else {
            params.putString("type", "CONNECTION");
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

        params.putString("responseCode", responseType);

        params.putString("responseMessage", responseMessage);

        WritableArray arrayMess = parseMessages();
        params.putArray("messages", arrayMess);
        return params;
    }

    private String parseApprovalCode(){
        for (String b:block) {
            if(b.startsWith("F")) return b.substring(1);
        }
        return "";
    }

    private String parseMessage(byte[] messageByte) {
        char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[messageByte.length * 2];
        for (int j = 0; j < messageByte.length; j++) {
            int v = messageByte[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        String hexCharsInString = new String(hexChars);
        hexCharsInString = hexCharsInString.replaceAll("(.{" + 2 + "})", "$1 ").trim();
        return hexCharsInString;

    }

    private String parseDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()).format(date);
    }

    private WritableArray parseMessages() {
        WritableArray messagesArray = Arguments.createArray();
        for (ResponseMessage mess : this.messages) {


            WritableMap obj = Arguments.createMap();
            obj.putString("date", parseDate(mess.getDate()));
            obj.putString("data", parseMessage(mess.getMessage()));
            messagesArray.pushMap(obj);
        }

        return messagesArray;
    }
}
