package com.enigoo.terminal.csob.socket_connection;

public class ProcessInit implements Runnable{

    private final String ipAddress;
    private final int port;
    private final String deviceId;

    public ProcessInit(String ipAddress, int port, String deviceId) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.deviceId = deviceId;
    }

    @Override
    public void run() {
        SocketConnection.init(ipAddress, port, deviceId);
    }
}
