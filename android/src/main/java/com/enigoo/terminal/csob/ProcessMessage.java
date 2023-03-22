package com.enigoo.terminal.csob;

import com.enigoo.terminal.EnigooTerminalModule;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProcessMessage implements Runnable {

    private final Payment payment;
    private final byte[] message;
    private final String type;

    private List<ResponseMessage> messages;
    private String orderId;

    public ProcessMessage(Payment payment, byte[] message, String type, String orderId) throws IOException {
        this.payment = payment;
        this.message = message;
        this.type = type;
        this.messages = new ArrayList<>();
        this.orderId = orderId;

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

            if (test[0] == '0' && test[1] == '3') {
                count = j;
            }
        }

        byte[] newBytes = new byte[(count + 1)];

        System.arraycopy(bytes, 0, newBytes, 0, newBytes.length);
        return newBytes;
    }

    @Override
    public void run() {
        try {
            /*
                1. Oveřit stav spojení T80 - po timeoutu 5s není spojení v pořádku, nelze provést platbu
                2. Vytvoř požadavek platby
                3. Zpracuj výsledek platby
                    OK:
                        - mám transakci, odešlu B0 jako potvrzení a vyžádám si lastTransaction pro porovnání
                          a případně zpracuji B3 (lístečky)
                    NOT OK:
                        - po timeoutu 60s nevím stále stav transakce
                        - Odešlu preventivně T81 - passivate s timeoutem 5s
                        - stále nevím stav transakce
                        - Odešlu T82 - get last trasaction - čekám 5s na odpověď
                        - Ověřím getLastTransaction s vytvořenou platbou
             */

            Response response = null;
            boolean connectionState = true;

            switch (type) {
                case "PAYMENT":
                case "REFUND":
                case "REVERSAL":
                case "HANDSHAKE":
                case "CLOSE_TOTALS":
                case "TMS_CALL":

                    //1. Ověř spojení pomocí T80
                    connectionState = verifyConnection();
                    break;
            }
            if (connectionState) {
                //2. odešli zprávu B1
                activateRequest();
                //Očekává se přijetí B0 (potvzení od terminálu, že přijal zprávu)
                //A následně se očekává B2 (potvrzení/zamítnutí platby)
                //3. Zpracuj výsledky platby/vratky
                response = waitForPayment();
                //Pokud si máš vyžádat lísteček, tak si ho vyžádej
                if (response != null && response.isWantTicket()) {
                    //DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    response.setMerchantRecipe(getRecipes("tM"));
                    response.setCustomerRecipe(getRecipes("tC"));
                }

                //emit result
                if (response != null) {
                    response.setMessages(messages);
                    EnigooTerminalModule.emit(response.toReactObject());
                } else {
                    throw new IOException();
                }
            }

        } catch (SocketTimeoutException ex) {
            SocketConnection.close();
            Response response = new Response(null, "0");
            response.setMessages(messages);
        } catch (IOException e) {
            SocketConnection.close();
            Response response = new Response(null, "0");
            response.setMessages(messages);
            EnigooTerminalModule.emit(response.toReactObject());
        }

    }

    private boolean verifyConnection() throws IOException {
        byte[] t80Req = payment.createRequest(TransactionTypes.GET_APP_INFO, 0, null);
        boolean result = false;

        SocketConnection.send(t80Req);

        ResponseMessage resMess = new ResponseMessage(new Date(), t80Req);
        messages.add(resMess);
        Logger.log(resMess, resMess.getDate(), SocketConnection.getDeviceId(), orderId);
        Response responseForT80 = waitForResponse(5);
        if (responseForT80.getResponseType().equals("000")) {
            emitStatus("CONNECTION", "SUCCESS");
            return true;
        } else {
            emitStatus("CONNECTION", "ERROR");
            responseForT80.setMessages(messages);
            EnigooTerminalModule.emit(responseForT80.toReactObject());
            return false;
        }

    }

    private void activateRequest() throws IOException {
        boolean result = SocketConnection.send(message);
        ResponseMessage resMess = new ResponseMessage(new Date(), message);
        messages.add(resMess);
        Logger.log(resMess, resMess.getDate(), SocketConnection.getDeviceId(), orderId);
        if (result) {
            emitStatus("CREATE_" + this.type, "SUCCESS");
        } else {
            throw new IOException();
        }

    }

    private Response waitForPayment() {
        try {
            //Očekávej zprávy B0 nebo B2 do max. 60s
            Response response = waitForResponse(60);

            //Potvrď přijetí zprávy B2 pomocí zprávy B0
            byte[] req = payment.createConfirmRequest();
            SocketConnection.send(req);
            ResponseMessage resMess = new ResponseMessage(new Date(), req);
            messages.add(resMess);
            Logger.log(resMess, resMess.getDate(), SocketConnection.getDeviceId(), orderId);

            if ((response.getTransactionType().equals(TransactionTypes.NORMAL_PURCHASE) || response.getTransactionType().equals(TransactionTypes.REFUND)) && response.isForcedConfirm()) {
                //Vyzadej si T82 - lastTransaction a porovnej
                byte[] reqT82 = payment.createRequest(TransactionTypes.GET_LAST_TRANS, 0, null);
                SocketConnection.send(reqT82);
                ResponseMessage resMessT82 = new ResponseMessage(new Date(), reqT82);
                messages.add(resMessT82);
                Logger.log(resMessT82, resMessT82.getDate(), SocketConnection.getDeviceId(), orderId);
                Response responseT82 = waitForResponse(5);
                //Porovnat OrderId - FID 9S
                if (!responseT82.compare(response)) {
                    //pokud se nerovná nic neprováděj dál... Vrať pokladně, že nelze provést
                    responseT82.setTransactionType(response.getTransactionType());
                    response = responseT82;
                }
            }
            return response;
        } catch (SocketTimeoutException ex) {
            //Nedostali jsme do 60 sekund žádné potvrzení B0 ani B2
            return processTimedOutResponse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Response processTimedOutResponse() {
        try {
            //Proveď passivate
            byte[] reqPassivate = payment.createRequest(TransactionTypes.PASSIVATE, 0, null);
            SocketConnection.send(reqPassivate);
            ResponseMessage resMess = new ResponseMessage(new Date(), reqPassivate);
            messages.add(resMess);
            Logger.log(resMess, resMess.getDate(), SocketConnection.getDeviceId(), orderId);

            //očekávej potvrzení B0
            waitForResponse(-1);

            //Získej data o poslední transakci
            byte[] reqGetLastTr = payment.createRequest(TransactionTypes.GET_LAST_TRANS, 0, null);
            SocketConnection.send(reqGetLastTr);
            ResponseMessage resMessLstTr = new ResponseMessage(new Date(), reqGetLastTr);
            messages.add(resMessLstTr);
            Logger.log(resMessLstTr, resMessLstTr.getDate(), SocketConnection.getDeviceId(), orderId);
            return waitForResponse(5);
        } catch (SocketTimeoutException ex) {
            emitStatus("CONNECTION", "LOST");
            return null;
        } catch (Exception exp) {
            emitStatus("UNKNOWN", exp.toString());
            return null;
        }
    }

    private List<String> getRecipes(String type) throws IOException {
        List<String> recipes = new ArrayList<>();
        byte[] req = payment.createTicketRequest(type);
        Response response;
        boolean next = true;
        while (next) {
            SocketConnection.send(req);
            ResponseMessage responseMessage = new ResponseMessage(new Date(), req);
            messages.add(responseMessage);
            Logger.log(responseMessage, responseMessage.getDate(), SocketConnection.getDeviceId(), orderId);
            response = waitForResponse(5);
            recipes.addAll(response.getRecipes());
            next = response.wantNext();
            req = payment.createTicketRequest("t-");
        }

        return recipes;
    }

    private Response waitForResponse(int timeoutInSeconds) throws IOException {
        boolean isDone = false;
        Response res = null;
        while (!isDone) {
            byte[] data = SocketConnection.read(timeoutInSeconds);
            assert data != null;
            byte[] msg = bytesToMessage(data);
            ResponseMessage resMess = new ResponseMessage(new Date(), msg);
            messages.add(resMess);
            Logger.log(resMess, resMess.getDate(), SocketConnection.getDeviceId(), orderId);
            Message message = new Message(bytesToMessage(data));
            Response response = message.process();

            if (response.isDone()) {
                res = response;
                return res;
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
