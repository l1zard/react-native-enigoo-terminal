package com.enigoo.terminal.fiskalpro;

public class FPMessage {

    private byte _byte;

    public FPMessage(byte message) {
        this._byte = message;
    }

    public FPResponse process() {
        return new FPResponse(this._byte);
    }



}
