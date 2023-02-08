package com.enigoo.terminal.csob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Payment {

    private String deviceId;

    public Payment(String deviceId) {
        this.deviceId = deviceId;
    }

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

        String[] messages = {"T00", "B" + String.format("%.2f", price).replace(".", "").replace(",", "") + "", "E203", "D1"};
        byte[] bytes = this.prevodnik("B101" + this.deviceId + this.getDate() + "A000" + this.calculateLength(messages) + "A5A5", messages);
        return bytes;
    }

    public byte[] createRefund(double price) throws IOException {

        String[] messages = {"T04", "B" + String.format("%.2f", price).replace(".", "").replace(",","") + "", "E203", "D1"};
        byte[] bytes = this.prevodnik("B101" + this.deviceId + this.getDate() + "A000" + this.calculateLength(messages) + "A5A5", messages);
        return bytes;
    }

    public byte[] createTicketRequest(String t) throws IOException{
        String[] messages = {"9",t};
        byte [] data = ("B301" + this.deviceId + this.getDate() + "0002" + this.calculateLength(messages) + "A5A5").getBytes();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(new byte[]{(byte) 0x02}); //0x02 - STX, 0x1c - FID, 0x1d - SFID/GS
        buffer.write(data);
        buffer.write(new byte[]{(byte)0x1c});
        buffer.write("9".getBytes());
        buffer.write(new byte[]{(byte)0x1d});
        buffer.write(t.getBytes());
        buffer.write(new byte[]{(byte) 0x03});

        return buffer.toByteArray();
    }

    public byte[] createCloseTotals() throws IOException {
        String[] messages = {"T60"};
        byte[] bytes = this.prevodnik("B101" + this.deviceId + this.getDate() + "A0000004"  + "A5A5", messages);
        return bytes;
    }

    public byte[] createBTmsCall() throws IOException{
        String[] messages = {"T90"};
        byte[] bytes = this.prevodnik("B101"+this.deviceId+this.getDate()+"A0000004"+"A5A5",messages);
        return bytes;
    }

    public byte[] createNTmsCall() throws IOException{
        String[] messages = {"T90"};
        byte[] bytes = this.prevodnik("N101"+this.deviceId+this.getDate()+"A0000004"+"A5A5",messages);
        return bytes;
    }

    public byte[] createHandshake() throws IOException{
        String[] messages = {"T95"};
        byte[] bytes =  this.prevodnik("B101"+this.deviceId+this.getDate()+"A0000004"+"A5A5",messages);
        return bytes;
    }

    public byte[] createConfirmRequest() throws IOException {
        return this.prevodnik("B001"+this.deviceId+this.getDate()+"00000000"+"A5A5",new String[0]);
    }


    private String getDate() {
        String pattern = "YYMMddHHmmss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(new Date());
    }
}
