package com.enigoo.terminal.csob;

import java.util.Date;

public class ResponseMessage {

    private Date date;
    private byte[] message;

    private boolean in;

    public ResponseMessage(Date date, byte[] message, boolean in) {
        this.date = date;
        this.message = message;
        this.in = in;
    }

    public Date getDate() {
        return date;
    }

    public byte[] getMessage() {
        return message;
    }

    public boolean isIn() {
        return in;
    }
}
