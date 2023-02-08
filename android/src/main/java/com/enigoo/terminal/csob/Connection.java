package com.enigoo.terminal.csob;

import android.os.AsyncTask;

import java.io.IOException;

public class Connection extends AsyncTask<Void, Void, Boolean> {

    private final String mServerAddress;
    private final int mServerPort;

    public Payment payment;

    private final byte[] message;

    public Connection(String serverAddress, int serverPort, Payment payment, byte[] message) {
        mServerAddress = serverAddress;
        mServerPort = serverPort;
        this.payment = payment;
        this.message = message;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            ProcessMessage processMessage = new ProcessMessage(mServerAddress,mServerPort,payment,message);
            Thread t = new Thread(processMessage);
            t.start();

            return true;

        } catch (IOException  e) {
            e.printStackTrace();
        }

        return false;
    }
}
