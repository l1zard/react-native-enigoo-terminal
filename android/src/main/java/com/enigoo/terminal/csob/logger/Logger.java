package com.enigoo.terminal.csob.logger;

import android.content.Context;

import com.enigoo.terminal.EnigooTerminalModule;
import com.enigoo.terminal.csob.ResponseMessage;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Logger {

    /**
     * Method to save log to the day
     *
     * @param message
     * @param date
     * @param deviceId
     * @param orderId
     */
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
                //deviceId;orderId;in;date;message
                bw.write(deviceId + ";" + orderId + ";" + (message.isIn() ? "1" : "0") + ";" +
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
    public static List<String> getLogs(String date, String orderId) {
        List<String> logs = new ArrayList<>();
        FileInputStream fis = getInputStream(date + ".txt");
        if (fis == null) return logs;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            while (br.ready()) {
                String line = br.readLine();
                if (orderId == null || orderId.equals("")) {
                    logs.add(line);
                } else if (line.split(";")[1].equals(orderId)) {
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

}
