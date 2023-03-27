package com.enigoo.terminal.csob.logger;

import com.enigoo.terminal.EnigooTerminalModule;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.List;

public class ProcessLog implements Runnable {

    private final String type;
    private final String date;
    private final String orderId;

    public ProcessLog(String type, String date, String orderId) {
        this.type = type;
        this.date = date;
        this.orderId = orderId;
    }

    @Override
    public void run() {
         switch (type){
             case "DELETE":
                 boolean result = Logger.deleteLogs(date);
                 WritableMap map = Arguments.createMap();
                 map.putString("type", "DELETE_LOG");
                 map.putString("status", result ? "SUCCESS" : "ERROR");
                 EnigooTerminalModule.emit(map);
                 break;
             case "GET":
                 List<String> logs = Logger.getLogs(date,orderId);
                 WritableMap mapLog = Arguments.createMap();
                 WritableArray array = Arguments.createArray();
                 for (String log : logs) {
                     array.pushString(log);
                 }
                 mapLog.putArray("result", array);
                 mapLog.putString("type", "GET_LOGS");
                 EnigooTerminalModule.emit(mapLog);
                 break;
         }
    }
}
