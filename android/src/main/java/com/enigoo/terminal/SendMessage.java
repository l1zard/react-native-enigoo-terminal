package com.enigoo.terminal;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SendMessage extends AsyncTask<Void, Void, Void> {

    private final byte[] data;
    private final Socket socket;

    public SendMessage(Socket mSocket, byte[] data) {
        this.data = data;
        this.socket = mSocket;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            out.write(data);
            out.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

}
