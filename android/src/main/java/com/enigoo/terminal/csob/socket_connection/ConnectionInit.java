package com.enigoo.terminal.csob.socket_connection;

import android.os.AsyncTask;

public class ConnectionInit extends AsyncTask<Void,Void,Boolean> {
    private final String ipAddress;
    private final int port;
    private final String deviceId;

    public ConnectionInit(String ipAddress, int port, String deviceId) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.deviceId = deviceId;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        ProcessInit processInit = new ProcessInit(ipAddress,port,deviceId);
        Thread thread = new Thread(processInit);
        thread.start();
        return true;
    }
}
