package com.enigoo.terminal.csob.enums;

public enum ProtocolTypes {
    ACTIVITY_INFO_MESSAGE("B0"),
    TRANSACTION_REQUEST("B1"),
    TRANSACTION_RESPONSE("B2"),
    TICKET_REQUEST("B3"),
    TICKET_RESPONSE("B4");
    private final String code;

    ProtocolTypes(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
