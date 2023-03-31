package com.enigoo.terminal.csob.socket_connection;


import com.enigoo.terminal.EnigooTerminalModule;
import com.enigoo.terminal.csob.Message;
import com.enigoo.terminal.csob.Payment;
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
import java.util.Date;

public class SocketConnection {

    private static Socket socket;

    private static boolean isOpen = false;

    private static String deviceId = "";
    private static String ipAddr = "";
    private static int por = 0;

    public static String getDeviceId() {
        return deviceId;
    }

    public static void interrupt(){
        try {
            socket.close();
            isOpen = false;
        } catch (IOException e) {
        }

    }
    public static void init(String ipAddress, int port, String devId) {
        try {
            if (socket==null || !isOpen || !ipAddr.equals(ipAddress) || port != por || !deviceId.equals(devId)) {
                close();
                deviceId = devId;
                por = port;
                ipAddr = ipAddress;
                socket = new Socket();
                SocketAddress address = new InetSocketAddress(ipAddr, por);
                socket.connect(address, 3000);

                isOpen = true;
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
            Logger.log(new ResponseMessage(new Date(), bytesToMessage(res), true), new Date(), deviceId, "GET_INFO");

            Message message = new Message(bytesToMessage(res));
            Response response = message.process();

            if (!response.getResponseType().equals("000")) {
                byte[] reqPass = pay.createRequest(TransactionTypes.PASSIVATE, 0, null);
                send(reqPass);
                Logger.log(new ResponseMessage(new Date(), reqPass, false), new Date(), deviceId, "PASSIVATE");
                byte[] resPass = read(5);
                Logger.log(new ResponseMessage(new Date(), resPass, true), new Date(), deviceId, "PASSIVATE");
                byte[] reqConf = pay.createConfirmRequest();
                send(reqConf);
                Logger.log(new ResponseMessage(new Date(), reqConf, false), new Date(), deviceId, "PASSIVATE");
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private static byte[] bytesToMessage(byte[] bytes) {
        if (bytes != null) {
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
        } else {
            return new byte[0];
        }
    }

    public static boolean send(byte[] message) throws IOException {
        if (!isOpen || socket==null) {
            try {
                socket = new Socket();
                SocketAddress address = new InetSocketAddress(ipAddr, por);
                socket.connect(address, 3000);
                isOpen = true;
            } catch (IOException e) {
                isOpen = false;
            }
        }
        if (isOpen && socket.isConnected()) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.write(message);
            out.flush();
            return true;
        } else {
            throw new IOException();
        }
    }

    public static byte[] read(int timeInSeconds) throws IOException {
        if (!isOpen || socket==null) {
            try {
                socket = new Socket();
                SocketAddress address = new InetSocketAddress(ipAddr, por);
                socket.connect(address, 3000);
                isOpen = true;
            } catch (IOException e) {
                isOpen = false;
            }
        }
        if (isOpen && socket.isConnected()) {
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
        if (isOpen) {
            try {
                socket.close();
                isOpen = false;
            } catch (IOException e) {

            }
        }
    }
}
