package com.enigoo.terminal.csob;

import com.enigoo.terminal.EnigooTerminalModule;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketConnection {

    private static Socket socket;

    private static boolean isOpen = false;

    private static String deviceId = "";

    public static String getDeviceId(){
        return deviceId;
    }

    public static boolean getIsOpen(){
        return isOpen;
    }

    public static void init(String ipAddress, int port, String devId) {
        if (isOpen) {
            close();
        }
        try {
            socket = new Socket(ipAddress, port);
            isOpen = true;
            deviceId = devId;
            WritableMap map = Arguments.createMap();
            map.putString("INIT_CONNECTION", "OK");
            EnigooTerminalModule.emit(map);
        } catch (IOException e) {
            WritableMap map = Arguments.createMap();
            map.putString("INIT_CONNECTION", "ERROR");
            EnigooTerminalModule.emit(map);
            isOpen = false;
        }
    }

    public static boolean send(byte[] message) throws IOException {
        if (isOpen) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.write(message);
            out.flush();
            return true;
        } else {
            throw new IOException();
        }
    }

    public static byte[] read(int timeInSeconds) throws IOException {
        if (isOpen) {
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
                deviceId = "";
            } catch (IOException e) {

            }
        }
    }

    private static byte[] bytesToMessage(byte[] bytes) {
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
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
}