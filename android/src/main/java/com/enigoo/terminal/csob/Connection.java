package com.enigoo.terminal.csob;

import android.os.AsyncTask;

import com.enigoo.terminal.EnigooTerminalModule;

import org.json.JSONException;

import java.io.IOException;
import java.net.Socket;

public class Connection extends AsyncTask<Void, Void, Socket> {

    private final String mServerAddress;
    private final int mServerPort;

    public Connection(String serverAddress, int serverPort) {
        mServerAddress = serverAddress;
        mServerPort = serverPort;
    }

    @Override
    protected Socket doInBackground(Void... voids) {
        try {
            Socket socket = new Socket(mServerAddress, mServerPort);
            new Thread(new GetThread(socket)).start();

            return socket;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    static class GetThread implements Runnable {

        Socket socket;

        public GetThread(Socket socket) {
            this.socket = socket;
        }

        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

        public byte[] bytesToMessage(byte[] bytes) {
            char[] hexChars = new char[bytes.length * 2];
            int count = 0;
            for (int j = 0; j < bytes.length; j++) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = HEX_ARRAY[v >>> 4];
                hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];

                char[] test = new char[2];
                test[0] = hexChars[j * 2];
                test[1] = hexChars[j * 2 + 1];

                if (new String(test).equals("03")) {
                    count = j;
                }

            }

            byte[] newBytes = new byte[(count + 1)];

            for (int j = 0; j < newBytes.length; j++) {
                newBytes[j] = bytes[j];
            }


            return newBytes;
        }

        @Override
        public void run() {


            boolean isDone = false;
            Response res = null;
            try {
                while (!isDone) {
                    // byte[] data = new byte[socket.getInputStream().read()];
                    // int count = mSocketContainer.getSocketInputStream().read(data);
                    byte[] data = new byte[1024];
                    int count = socket.getInputStream().read(data);

                    Message message = new Message(bytesToMessage(data));
                    Response response = message.process();


                    if (response.isDone()) {
                        res = response;
                    }
                    isDone = response.isDone();

                }

                if (res != null) {
                    EnigooTerminalModule.emit(res.toJsonString());
                }

                socket.close();

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }


        }
    }
}
