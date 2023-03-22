package com.enigoo.terminal.csob;

import android.content.Context;

import com.enigoo.terminal.EnigooTerminalModule;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Logger {

    /**
     * Method to save log to the day
     *
     * @param messages
     * @param date
     */
    public static void log(List<ResponseMessage> messages, Date date, String deviceId, String orderId) {
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        String fileName = dateFormat.format(date) + ".txt";
        FileOutputStream fos = getOutputStream(fileName);
        if (fos == null) {
            WritableMap map = Arguments.createMap();
            map.putString("ERROR", "log_not_exist");
            EnigooTerminalModule.emit(map);
        } else {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            try {
                for (ResponseMessage message : messages) {
                    bw.write(deviceId+";" + orderId + ";" +
                            parseDate(message.getDate()) + ";" +
                            parseMessage(message.getMessage()) + ";");
                    bw.newLine();
                }
                bw.flush();
                bw.close();
            } catch (IOException e) {

            }
        }
    }

    public static void log(ResponseMessage message, Date date, String deviceId, String orderId) {
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        String fileName = dateFormat.format(date) + ".txt";
        FileOutputStream fos = getOutputStream(fileName);
        if (fos == null) {
            WritableMap map = Arguments.createMap();
            map.putString("ERROR", "log_not_exist");
            EnigooTerminalModule.emit(map);
        } else {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            try {
                bw.write(deviceId + ";" + orderId + ";" +
                        parseDate(message.getDate()) + ";" +
                        parseMessage(message.getMessage()) + ";");
                bw.newLine();
                bw.flush();
                bw.close();
            } catch (IOException e) {

            }
        }
    }

    /**
     * @param date Date in format "YYYY-MM-DD"
     * @return list of logs from the day
     */
    public static List<String> getLogs(String date,String orderId) {
        List<String> logs = new ArrayList<>();
        FileInputStream fis = getInputStream(date + ".txt");
        if (fis == null) return logs;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            while (br.ready()) {
                String line = br.readLine();
                if(line.split(";")[1].equals(orderId)){
                    logs.add(line);
                }
            }
        } catch (IOException e) {
            return logs;
        }

        return logs;
    }

    public static boolean deleteLogs(String date) {
        File file = getFile(date + ".txt");
        if (file != null) return file.delete();
        return false;
    }

    private static FileInputStream getInputStream(String name) {
        try {
            File file = getFile(name);
            if (file == null) {
                file = createFile(name);
            }
            FileInputStream fis = new FileInputStream(file);
            return fis;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private static FileOutputStream getOutputStream(String name) {
        try {
            File file = getFile(name);
            if (file == null) {
                file = createFile(name);
            }
            return new FileOutputStream(file, true);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private static File getFile(String name) {
        File dir = EnigooTerminalModule.reactApplicationContext.getDir("logs", Context.MODE_PRIVATE);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir.getAbsolutePath() + "/" + name);
        if (file.exists()) return file;
        else return null;
    }


    private static File createFile(String name) {
        File dir = EnigooTerminalModule.reactApplicationContext.getDir("logs", Context.MODE_PRIVATE);
        File file = new File(dir.getAbsolutePath() + "/" + name);
        if (!file.exists()) {
            try {
                file.createNewFile();
                return file;
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    private static String parseDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()).format(date);
    }

    private static String parseMessage(byte[] messageByte) {
        char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[messageByte.length * 2];
        for (int j = 0; j < messageByte.length; j++) {
            int v = messageByte[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        String hexCharsInString = new String(hexChars);
        hexCharsInString = hexCharsInString.replaceAll("(.{" + 2 + "})", "$1 ").trim();
        return hexCharsInString;

    }

    public static List<String> getLastTransactionData(){
        String date = parseDate(new Date());
        List<String> logs = getLogs(date,null);
        Collections.reverse(logs);
        for (String l:logs) {
            String [] logMess = l.split(";");
            List<String> messages = split(logMess[logMess.length-1].replaceAll(" ","").getBytes());
            for (String m: messages) {
                if(m.startsWith("T")) return messages;
            }
        }

        return new ArrayList<>();
    }
    static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();


    private static List<String> split(byte[] bytes) {
        List<String> messages = new ArrayList<>();

        char[] hexChars = new char[bytes.length * 2];
        int count = 0;
        boolean isHeader = false;
        int lastPosition = 0;

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];

            char[] test = new char[2];
            test[0] = hexChars[j * 2];
            test[1] = hexChars[j * 2 + 1];
            if((test[0] == '1' && test[1]=='C') || (test[0]=='0' && test[1]=='3') || (test[0]=='1' && test[1]=='D')){
                messages.add(createMessage(bytes,j - lastPosition, lastPosition));
                lastPosition = j;
            }

        }
        return messages;
    }

    private static String createMessage(byte[]bytes,int length, int starter) {
        byte[] newBytes = new byte[(length - 1)];

        for (int j = 0; j < newBytes.length; j++) {
            newBytes[j] = bytes[j + 1 + starter];
        }

        try {
            return new String(newBytes, "ISO-8859-2");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
