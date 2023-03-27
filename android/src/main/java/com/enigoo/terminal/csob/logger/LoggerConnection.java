package com.enigoo.terminal.csob.logger;

import android.os.AsyncTask;

public class LoggerConnection extends AsyncTask<Void,Void,Boolean> {

    private final String type;
    private final String date;
    private final String orderId;

    public LoggerConnection(String type, String date, String orderId) {
        this.type = type;
        this.date = date;
        this.orderId = orderId;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        ProcessLog processLog = new ProcessLog(type,date,orderId);
        Thread t = new Thread(processLog);
        t.start();
        return true;
    }
}
