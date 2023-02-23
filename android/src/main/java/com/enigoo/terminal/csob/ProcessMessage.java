package com.enigoo.terminal.csob;

import com.enigoo.terminal.EnigooTerminalModule;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

public class ProcessMessage implements Runnable {

    private Socket socket;
    private final Payment payment;

    private final String serverAddress;
    private final int serverPort;

    private final byte[] message;
    private final String type;

    private List<byte[]> messages;
    public ProcessMessage(String mServerAddress, int mServerPort, Payment payment, byte[] message, String type) throws IOException {
        this.serverAddress = mServerAddress;
        this.serverPort = mServerPort;
        this.payment = payment;
        this.message = message;
        this.type = type;
        this.messages = new ArrayList<>();
        messages.add(message);
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
        this.messages.add(newBytes);
        return newBytes;
    }

    @Override
    public void run() {
        try {
            //open socket
            this.socket = new Socket(serverAddress, serverPort);
            emitStatus("CONNECTION", "SUCCESS");
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            //send message to terminal
            out.write(message);
            emitStatus("CREATE_" + this.type, "SUCCESS");
            //wait for first response timout = 25 s
            Response response = waitForResponse(25000);
            if(response == null){
                throw new SocketTimeoutException();
            }
            if (response.isWantTicket()) {
                response.setMerchantRecipe(getRecipes("tM", out));
                response.setCustomerRecipe(getRecipes("tC", out));
            } else {
                //send confirm request
              byte[]req = payment.createConfirmRequest();
                out.write(req);
                messages.add(req);
            }

            socket.close();
            //emit result
            EnigooTerminalModule.emit(response.toReactObject());

        } catch (SocketTimeoutException ex) {
            EnigooTerminalModule.emit(new Response("0", "0").toReactObject());
        } catch (IOException e) {
            emitStatus("CONNECTION", "FAILED");
        }catch (Exception e){
            emitStatus("UNKNOWN","ERROR");
        }

    }

    private List<String> getRecipes(String type, DataOutputStream out) throws IOException {
        List<String> recipes = new ArrayList<>();
        byte[] req = payment.createTicketRequest(type);
        Response response;
        boolean next = true;
        while (next) {
            messages.add(req);
            out.write(req);
            out.flush();
            response = waitForResponse(10000);
            recipes.addAll(response.getRecipes());
            next = response.wantNext();
            req = payment.createTicketRequest("t-");
        }

        return recipes;
    }

    private Response waitForResponse(int timeout) throws IOException {
        boolean isDone = false;
        Response res = null;
        socket.setSoTimeout(timeout);
        while (!isDone) {
            byte[] data = new byte[1024];
            int count = socket.getInputStream().read(data);

            Message message = new Message(bytesToMessage(data));
            Response response = message.process();


            if (response.isDone()) {
                res = response;
                res.setMessages(messages);
            }
            isDone = response.isDone();

        }
        return res;

    }

    private void emitStatus(String type, String status) {
        WritableMap params = Arguments.createMap();
        params.putString("type", type);
        params.putString("status", status);
        EnigooTerminalModule.emit(params);
    }
}
