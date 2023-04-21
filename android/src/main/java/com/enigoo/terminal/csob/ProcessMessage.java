package com.enigoo.terminal.csob;

import com.enigoo.terminal.EnigooTerminalModule;
import com.enigoo.terminal.csob.enums.TransactionTypes;
import com.enigoo.terminal.csob.logger.Logger;
import com.enigoo.terminal.csob.socket_connection.SocketConnection;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.io.IOException;
import java.net.SocketException;
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

  private class MessagePacket {
    private int stx;
    private int etx;

    private byte[] content;

    public MessagePacket(int stx, int etx, byte[] bytes) {
      this.stx = stx;
      this.etx = etx;
      parseContent(bytes);
    }

    private void parseContent(byte[] bytes) {
      this.content = new byte[etx - stx + 1];
      System.arraycopy(bytes, stx, content, 0, content.length);
    }
  }

  private List<byte[]> bytesToMessage(byte[] bytes) {
    List<MessagePacket> etxS = new ArrayList<>();
    List<byte[]> messages = new ArrayList<>();
    char[] hexChars = new char[bytes.length * 2];
    int count = 0;
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_ARRAY[v >>> 4];
      hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];

      char[] test = new char[2];
      test[0] = hexChars[j * 2];
      test[1] = hexChars[j * 2 + 1];
      //nasel jsem zacatek
      if (new String(test).equals("02")) {
        int stx = j;
        int etx = -1;
        //najdi konec
        for (int i = j; i < bytes.length; i++) {
          int w = bytes[i] & 0xFF;
          hexChars[i * 2] = HEX_ARRAY[w >>> 4];
          hexChars[i * 2 + 1] = HEX_ARRAY[w & 0x0F];

          char[] test2 = new char[2];
          test2[0] = hexChars[i * 2];
          test2[1] = hexChars[i * 2 + 1];
          if (new String(test2).equals("03")) {
            etx = i;
            j = i;
            break;
          }
        }
        etxS.add(new MessagePacket(stx, etx, bytes));
      }


    }
    for (MessagePacket i : etxS) {
      messages.add(i.content);
    }
    return messages;
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
        if (type.equals("TMS_CALL")) {
          SocketConnection.close();
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
      EnigooTerminalModule.emit(response.toReactObject());
    } catch (SocketException exp) {
      Response res = null;
      if (((ConnectionThread) Thread.currentThread()).isPassivating()) {
        res = processPassivate(true);
      }
      if (res == null) {
        SocketConnection.close();
        res = new Response(null, "0");
      }
      res.setMessages(messages);
      EnigooTerminalModule.emit(res.toReactObject());
    } catch (IOException e) {
      SocketConnection.close();
      Response response = new Response(null, "0");
      response.setMessages(messages);
      EnigooTerminalModule.emit(response.toReactObject());
    }

  }

  private boolean verifyConnection() throws IOException {
    byte[] t80Req = payment.createRequest(TransactionTypes.GET_APP_INFO, 0, null);

    try {
      SocketConnection.send(t80Req);

    } catch (IOException ex) {
      SocketConnection.close();
      SocketConnection.send(t80Req);
    }


    ResponseMessage resMess = new ResponseMessage(new Date(), t80Req, false);
    messages.add(resMess);
    Logger.log(resMess, resMess.getDate(), SocketConnection.getDeviceId(), orderId);
    Response responseForT80 = null;

    try {
      responseForT80 = waitForResponse(5);
    } catch (IOException ex) {
      responseForT80 = waitForResponse(5);
    }

    //Potvrď přijetí zprávy B2 pomocí zprávy B0
    byte[] req = payment.createConfirmRequest();
    SocketConnection.send(req);
    ResponseMessage resMessConf = new ResponseMessage(new Date(), req, false);
    messages.add(resMessConf);
    Logger.log(resMess, resMessConf.getDate(), SocketConnection.getDeviceId(), orderId);
    switch (responseForT80.getResponseType()) {
      case "-02":
        if (SocketConnection.isReinit) {
          Response res = processPassivate(false);
          SocketConnection.isReinit = false;
          if (res != null && res.getResponseType().equals("-01")) {
            return true;
          } else {
            responseForT80.setMessages(messages);
            EnigooTerminalModule.emit(responseForT80.toReactObject());
            return false;
          }
        } else {
          responseForT80.setMessages(messages);
          EnigooTerminalModule.emit(responseForT80.toReactObject());
          return false;
        }
      case "000":
        emitStatus("CONNECTION", "SUCCESS");
        return true;
      default:
        responseForT80.setMessages(messages);
        EnigooTerminalModule.emit(responseForT80.toReactObject());
        return false;
    }
  }

  private void activateRequest() throws IOException {
    boolean result = SocketConnection.send(message);
    ResponseMessage resMess = new ResponseMessage(new Date(), message, false);
    messages.add(resMess);
    Logger.log(resMess, resMess.getDate(), SocketConnection.getDeviceId(), orderId);
    if (result) {
      emitStatus("CREATE_" + this.type, "SUCCESS");
    } else {
      throw new IOException();
    }

  }

  private Response waitForPayment() throws IOException {
    try {
      //Očekávej zprávy B0 nebo B2 do max. 60s
      int timeout = 60;
      //if(type.equals("PASSIVATE")) timeout = 3;
      Response response = waitForResponse(timeout);

      //Potvrď přijetí zprávy B2 pomocí zprávy B0
      byte[] req = payment.createConfirmRequest();
      SocketConnection.send(req);
      ResponseMessage resMess = new ResponseMessage(new Date(), req, false);
      messages.add(resMess);
      Logger.log(resMess, resMess.getDate(), SocketConnection.getDeviceId(), orderId);

      if (response.getTransactionType() != null && (response.getTransactionType().equals(TransactionTypes.NORMAL_PURCHASE) || response.getTransactionType().equals(TransactionTypes.REFUND)) && response.isForcedConfirm()) {
        //Vyzadej si T82 - lastTransaction a porovnej
        byte[] reqT82 = payment.createRequest(TransactionTypes.GET_LAST_TRANS, 0, null);
        SocketConnection.send(reqT82);
        ResponseMessage resMessT82 = new ResponseMessage(new Date(), reqT82, false);
        messages.add(resMessT82);
        Logger.log(resMessT82, resMessT82.getDate(), SocketConnection.getDeviceId(), orderId);
        Response responseT82 = waitForResponse(5);

        //TODO:Potvrzení přijetí
        byte[] confReq = payment.createConfirmRequest();
        ResponseMessage responseMessageConf = new ResponseMessage(new Date(), confReq, false);
        messages.add(responseMessageConf);
        Logger.log(responseMessageConf, responseMessageConf.getDate(), SocketConnection.getDeviceId(), orderId);
        SocketConnection.send(confReq);

        //Porovnat OrderId - FID 9S
        if (!responseT82.compare(response)) {
          //pokud se nerovná nic neprováděj dál... Vrať pokladně, že nelze provést
          responseT82.setTransactionType(response.getTransactionType());
          response = responseT82;
        }
      }
      return response;
    } catch (SocketTimeoutException ex) {
      //Nedostali jsme do 60 sekund žádné potvrzení B0 ani B2 a je to Platba nebo refundace
      if (type.equals("PAYMENT") || type.equals("REFUND")) {
        return processTimedOutResponse();
      } else {
        return null;
      }

    }
  }

  private Response processTimedOutResponse() {
    try {
      //Proveď passivate
      byte[] reqPassivate = payment.createRequest(TransactionTypes.PASSIVATE, 0, null);
      SocketConnection.send(reqPassivate);
      ResponseMessage resMess = new ResponseMessage(new Date(), reqPassivate, false);
      messages.add(resMess);
      Logger.log(resMess, resMess.getDate(), SocketConnection.getDeviceId(), orderId);

      //očekávej potvrzení B0
      waitForResponse(-1);

      //Získej data o poslední transakci
      byte[] reqGetLastTr = payment.createRequest(TransactionTypes.GET_LAST_TRANS, 0, null);
      SocketConnection.send(reqGetLastTr);
      ResponseMessage resMessLstTr = new ResponseMessage(new Date(), reqGetLastTr, false);
      messages.add(resMessLstTr);
      Logger.log(resMessLstTr, resMessLstTr.getDate(), SocketConnection.getDeviceId(), orderId);
      return waitForResponse(5);
    } catch (Exception ex) {
      return null;
    }
  }

  private List<String> getRecipes(String type) throws IOException {
    List<String> recipes = new ArrayList<>();
    byte[] req = payment.createTicketRequest(type);
    Response response;
    boolean next = true;
    while (next) {
      //vyzadani listecku
      SocketConnection.send(req);
      ResponseMessage responseMessage = new ResponseMessage(new Date(), req, false);
      messages.add(responseMessage);
      Logger.log(responseMessage, responseMessage.getDate(), SocketConnection.getDeviceId(), orderId);
      response = waitForResponse(5);
      recipes.addAll(response.getRecipes());
      //potvrzeni prijeti listecku
      byte[] confReq = payment.createConfirmRequest();
      ResponseMessage responseMessageConf = new ResponseMessage(new Date(), confReq, false);
      messages.add(responseMessageConf);
      Logger.log(responseMessageConf, responseMessageConf.getDate(), SocketConnection.getDeviceId(), orderId);
      SocketConnection.send(confReq);
      //vytvoreni requestu pro dalsi listecek
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
      if (data == null) {
        break;
      }
      List<Response> responses = new ArrayList<>();
      List<byte[]> messasges = bytesToMessage(data);
      for (byte[] msg : messasges) {
        Message message = new Message(msg);
        responses.add(message.process());
        ResponseMessage resMess = new ResponseMessage(new Date(), msg, true);
        messages.add(resMess);
        Logger.log(resMess, resMess.getDate(), SocketConnection.getDeviceId(), orderId);
      }
      for (Response r : responses) {
        if (r.isDone()) {
          res = r;
        }
        isDone = r.isDone();
      }
    }
    return res;

  }

  private Response processPassivate(boolean emit) {
    try {
      Response res = sendPassivate(emit);
      return res;
    } catch (IOException ex) {
      try {
        Response res = sendPassivate(emit);
        return res;
      } catch (IOException e) {
        if (emit) emitStatus("CREATE_PASSIVATE", "ERROR");
        return null;
      }
    }
  }

  private Response sendPassivate(boolean emit) throws IOException {
    byte[] req = payment.createRequest(TransactionTypes.PASSIVATE, 0, null);
    SocketConnection.send(req);
    if (emit) emitStatus("CREATE_PASSIVATE", "SUCCESS");
    ResponseMessage responseMessage = new ResponseMessage(new Date(), req, false);
    messages.add(responseMessage);
    Logger.log(responseMessage, responseMessage.getDate(), SocketConnection.getDeviceId(), orderId);
    Response res = waitForResponse(10);
    byte[] confReq = payment.createConfirmRequest();
    SocketConnection.send(confReq);
    ResponseMessage responseMessageConf = new ResponseMessage(new Date(), confReq, false);
    messages.add(responseMessageConf);
    Logger.log(responseMessageConf, responseMessageConf.getDate(), SocketConnection.getDeviceId(), orderId);
    return res;
  }

  private void emitStatus(String type, String status) {
    WritableMap params = Arguments.createMap();
    params.putString("type", type);
    params.putString("status", status);
    EnigooTerminalModule.emit(params);
  }
}
