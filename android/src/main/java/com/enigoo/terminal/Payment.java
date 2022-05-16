package com.enigoo.terminal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Payment {

    String deviceId = "PAXX2963";


    private String calculateLength(String[] message) {
        int count = message.length + 1;
        for (int i = 0; i < message.length; i++) {
            count += message[0].length();
        }

        String string = Integer.toHexString(count);

        if (string.length() < 4) {
            int length = 4 - string.length();
            for (int i = 0; i < length; i++) {
                string = "0" + string;
            }
        }


        return string;
    }

    private byte[] prevodnik(String header, String[] message) throws IOException {

        byte[] data = header.getBytes();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(new byte[]{(byte) 0x02});
        buffer.write(data);

        for (String item : message) {
            buffer.write(new byte[]{(byte) 0x1c});
            buffer.write(item.getBytes());
        }

        buffer.write(new byte[]{(byte) 0x03});
        return buffer.toByteArray();
    }


    public byte[] createPayment(double price) throws IOException {

        String[] messages = {"T00", "B" + String.format("%.2f", price).replace(".", "") + "", "E203", "D1"};
        byte[] bytes = this.prevodnik("B101" + deviceId + this.getDate() + "0001" + this.calculateLength(messages) + "A5A5", messages);
        return bytes;
    }


    private String getDate() {
        String pattern = "YYMMddHHmmss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(new Date());
    }
}
