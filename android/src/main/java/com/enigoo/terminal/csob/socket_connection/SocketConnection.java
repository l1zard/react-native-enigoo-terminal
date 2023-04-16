package com.enigoo.terminal.csob.socket_connection;


import com.enigoo.terminal.EnigooTerminalModule;
import com.enigoo.terminal.csob.Message;
import com.enigoo.terminal.csob.Payment;
import com.enigoo.terminal.csob.ProcessMessage;
import com.enigoo.terminal.csob.Response;
import com.enigoo.terminal.csob.ResponseMessage;
import com.enigoo.terminal.csob.enums.TransactionTypes;
import com.enigoo.terminal.csob.logger.Logger;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SocketConnection {

  public static Socket socket;

  private static boolean isOpen = false;

  private static String deviceId = "";
  private static String ipAddr = "";
  private static int por = 0;

  public static boolean isReinit = false;

  public static String getDeviceId() {
    return deviceId;
  }

  public static void interrupt() {
    try {
      socket.close();
      isOpen = false;
    } catch (IOException e) {
    }

  }

  public static void open() throws IOException {
    close();
    socket = new Socket();
    SocketAddress address = new InetSocketAddress(ipAddr, por);
    socket.connect(address, 3000);
    isOpen = true;
    isReinit = true;
  }

  public static void init(String ipAddress, int port, String devId) {
    try {
      if (socket == null || !isOpen || !ipAddr.equals(ipAddress) || port != por || !deviceId.equals(devId)) {
        deviceId = devId;
        por = port;
        ipAddr = ipAddress;
        open();
        isReinit = false;
      }
      if (verifyConnection()) {
        WritableMap map = Arguments.createMap();
        map.putString("type", "INIT_CONNECTION");
        map.putString("status", "SUCCESS");
        EnigooTerminalModule.emit(map);
      } else {
        throw new IOException();
      }
    } catch (IOException e) {
      WritableMap map = Arguments.createMap();
      map.putString("type", "INIT_CONNECTION");
      map.putString("status", "ERROR");
      EnigooTerminalModule.emit(map);
      isOpen = false;
    }
  }

  private static boolean verifyConnection() {
    Payment pay = new Payment(deviceId);
    try {
      byte[] req = pay.createRequest(TransactionTypes.GET_APP_INFO, 0, null);
      send(req);
      Logger.log(new ResponseMessage(new Date(), req, false), new Date(), deviceId, "GET_INFO");
      byte[] res = read(5);
      List<byte[]> bytesMessages = bytesToMessage(res);
      if (bytesMessages.size() == 0) return false;
      List<Response> responses = new ArrayList<>();
      for (byte[] b : bytesMessages) {
        Logger.log(new ResponseMessage(new Date(), b, true), new Date(), deviceId, "GET_INFO");

        Message message = new Message(b);
        responses.add(message.process());
      }

      for (Response resp : responses) {
        if (resp != null && resp.getResponseType() != null && !resp.getResponseType().equals("000")) {
          byte[] reqPass = pay.createRequest(TransactionTypes.PASSIVATE, 0, null);
          send(reqPass);
          Logger.log(new ResponseMessage(new Date(), reqPass, false), new Date(), deviceId, "PASSIVATE");
          byte[] resPass = read(5);
          Logger.log(new ResponseMessage(new Date(), resPass, true), new Date(), deviceId, "PASSIVATE");
          byte[] reqConf = pay.createConfirmRequest();
          send(reqConf);
          Logger.log(new ResponseMessage(new Date(), reqConf, false), new Date(), deviceId, "PASSIVATE");
        }
      }


      return true;
    } catch (SocketException ex) {
      return false;
    } catch (IOException ex) {
      return false;
    }
  }

  private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

  private static class MessagePacket {
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

  private static List<byte[]> bytesToMessage(byte[] bytes) {
    List<MessagePacket> etxS = new ArrayList<>();
    List<byte[]> messages = new ArrayList<>();
    if (bytes == null) return messages;
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

  public static boolean send(byte[] message) throws IOException {
    if (!isOpen || socket == null || socket.isClosed() || !socket.isConnected()) {
      try {
        open();
      } catch (IOException ex) {
        isOpen = false;
      }
    }
    if (socket != null && isOpen && !socket.isClosed()) {
      DataOutputStream out = new DataOutputStream(socket.getOutputStream());
      out.write(message);
      out.flush();
      return true;
    } else {
      throw new IOException();
    }
  }

  public static byte[] read(int timeInSeconds) throws IOException {
    if (!isOpen || socket == null || socket.isClosed() || !socket.isConnected()) {
      try {
        open();
      } catch (IOException ex) {
        isOpen = false;
      }
    }
    if (socket != null && isOpen && !socket.isClosed()) {
      if (timeInSeconds > 0) {
        socket.setSoTimeout(timeInSeconds * 1000);
      }
      byte[] bytes = new byte[1024];
      int count = socket.getInputStream().read(bytes);
      return bytes;
    } else {
      return null;
    }
  }

  public static void close() {
    if (socket != null) {
      try {
        socket.close();
        isOpen = false;
      } catch (IOException e) {

      }
    }
  }
}
