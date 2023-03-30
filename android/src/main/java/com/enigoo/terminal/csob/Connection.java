package com.enigoo.terminal.csob;

import android.os.AsyncTask;

import com.enigoo.terminal.EnigooTerminalModule;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

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



            int count = 0;
            for(Thread t: Thread.getAllStackTraces().keySet()){

                if(t instanceof ConnectionThread){
                    count++;
                }
            }
            WritableMap map = Arguments.createMap();
            map.putInt("THREADS",count);
            EnigooTerminalModule.emit(map);

            ConnectionThread t = new ConnectionThread(processMessage);
            t.start();
            return true;

        } catch (IOException  e) {
            e.printStackTrace();
        }

        return false;
    }
}
