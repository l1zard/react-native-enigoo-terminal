package com.enigoo.terminal.csob;

import android.os.AsyncTask;

import java.io.IOException;

public class Connection extends AsyncTask<Void, Void, Boolean> {


    public Payment payment;

    private final byte[] message;
    private String type;
    private String orderId;

    public Connection(Payment payment, byte[] message,String type,String orderId) {
        this.payment = payment;
        this.message = message;
        this.type = type;
        this.orderId = orderId;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            ProcessMessage processMessage = new ProcessMessage(payment,message,type,orderId);
            Thread t = new Thread(processMessage);
            t.start();

            return true;

        } catch (IOException  e) {
            e.printStackTrace();
        }

        return false;
    }
}
