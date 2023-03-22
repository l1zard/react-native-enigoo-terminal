package com.enigoo.terminal.csob;

public enum TransactionTypes {
    NORMAL_PURCHASE("T00","NORMAL_PURCHASE"),
    PREAUTHORIZATION("T01","PREAUTHORIZATION"),
    COMPLETION_OF_PREAUTHORIZATION("T02","COMPLETION_OF_PREAUTHORIZATION"),
    REFUND("T04","REFUND"),
    CASH_ADVANCE("T05","CASH_ADVANCE"),
    BALANCE_INQUIRY("T07","BALANCE_INQUIRY"),
    PURCHASE_WITH_CASHBACK("T08","PURCHASE_WITH_CASHBACK"),
    REVERSAL("T10","REVERSAL"),
    CLOSE_TOTALS("T60","CLOSE_TOTALS"),
    SUBTOTALS("T65","SUBTOTALS"),
    HANDSHAKE("T95","HANDSHAKE"),
    GET_APP_INFO("T80","GET_APP_INFO"),
    PASSIVATE("T81","PASSIVATE"),
    GET_LAST_TRANS("T82","GET_LAST_TRANS"),
    GET_LAST_SUMS("T83","GET_LAST_SUMS"),
    GET_APP_CONFIG("T84","GET_APP_CONFIG"),
    TMS_CALL("T90","TMS_CALL");
    private final String code;
    private final String name;

    TransactionTypes(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static TransactionTypes parseFromString(String type){
        for (TransactionTypes t:TransactionTypes.values()){
            if(t.getName().toLowerCase().equals(type.toLowerCase())) return t;
        }
        return null;
    }
}
