package com.enigoo.terminal.csob;

import android.os.AsyncTask;


import com.enigoo.terminal.EnigooTerminalModule;

import org.json.JSONException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Connection extends AsyncTask<Void, Void, Socket> {

    private final String mServerAddress;
    private final int mServerPort;

    public Payment payment;
    public Connection(String serverAddress, int serverPort, Payment payment) {
        mServerAddress = serverAddress;
        mServerPort = serverPort;
        this.payment = payment;
    }

    @Override
    protected Socket doInBackground(Void... voids) {
        try {
            Socket socket = new Socket(mServerAddress, mServerPort);
            new Thread(new GetThread(socket,payment)).start();

            return socket;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    static class GetThread implements Runnable {

        Socket socket;
        Payment payment;

        public GetThread(Socket socket,Payment payment) {
            this.socket = socket;
            this.payment = payment;
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
            try {
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                Response response = waitForResponse();
                if(response.isWantTicket()){
                    List<String> merchantRecipes = new ArrayList<>();
                    byte [] req = payment.createTicketRequest("tM");
                    Response merchResponse;
                    boolean next = true;
                    while(next){
                        out.write(req);
                        out.flush();
                        merchResponse = waitForResponse();
                        merchantRecipes.addAll(merchResponse.getRecipes());
                        next = merchResponse.wantNext();
                        req = payment.createTicketRequest("t-");
                    }
                    response.setMerchantRecipe(merchantRecipes);

                    List<String> customerRecipes = new ArrayList<>();
                    req = payment.createTicketRequest("tC");
                    Response custResponse;
                    next = true;
                    while(next){
                        out.write(req);
                        out.flush();
                        custResponse = waitForResponse();
                        customerRecipes.addAll(custResponse.getRecipes());
                        next = custResponse.wantNext();
                        req = payment.createTicketRequest("t-");
                    }
                    response.setCustomerRecipe(customerRecipes);
                }

                socket.close();
                EnigooTerminalModule.emit(response.toJsonString());

            } catch (IOException | JSONException e ) {
                throw new RuntimeException(e);
            }

        }

        private Response waitForResponse(){
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
                return res;
            } catch (IOException e) {
                return res;
            }
        }
    }
}
