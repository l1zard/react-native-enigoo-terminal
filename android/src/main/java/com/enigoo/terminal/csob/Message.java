package com.enigoo.terminal.csob;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class Message {

    final String RESPONSE = "B2";

    private byte[] bytes;
    final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private ArrayList<String> messages = new ArrayList<>();

    public Message(byte[] message) {
        this.bytes = message;
        split();
    }

    public Response process() {
        return new Response(this.messages);
    }

    private void split() {

        char[] hexChars = new char[this.bytes.length * 2];
        int count = 0;
        boolean isHeader = false;
        int lastPosition = 0;

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];

            char[] test = new char[2];
            test[0] = hexChars[j * 2];
            test[1] = hexChars[j * 2 + 1];
            if((test[0] == '1' && test[1]=='C') || (test[0]=='0' && test[1]=='3') || (test[0]=='1' && test[1]=='D')){
                messages.add(createMessage(j - lastPosition, lastPosition));
                lastPosition = j;
            }

        }
    }


    private String createMessage(int length, int starter) {
        byte[] newBytes = new byte[(length - 1)];

        for (int j = 0; j < newBytes.length; j++) {
            newBytes[j] = this.bytes[j + 1 + starter];
        }

        try {
            return new String(newBytes, "ISO-8859-2");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
