package com.enigoo.terminal.fiskalsk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class FiskalPro {

    public static byte fp_lrc(byte[] data, int len) {
        byte lrc = 0;
        for (int i = 0; i < len; i++) {
            lrc ^= data[i];
        }
        return lrc;
    }

    public static void print(UsbService usbService) {
        if (usbService != null) { // if UsbService was correctly binded, Send data
            try {
                for (String com : generateDataSentence()) {
                    usbService.write(createCommand(com));
                }
                //System.out.println(Arrays.toString(getTestBytes()));
                //System.out.println(Arrays.toString(generateTestData()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static byte[] createCommand(String comm) throws IOException {
        //String comm = "FRRTC"; //FXREPF
        byte[] data = comm.getBytes("Cp1250");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(new byte[]{0x01, 0x01});
        byte length1 = ((Integer) (data.length / 256)).byteValue();
        byte length2 = ((Integer) (data.length % 256)).byteValue();
        buffer.write(new byte[]{
                length1,
                length2
        });
        buffer.write(data);
        byte lrc = fp_lrc(data, data.length);
        buffer.write(lrc);
        return buffer.toByteArray();
    }

    private static List<String> generateDataSentence() {
        List<String> sentence = new ArrayList<>();

        //Unikátní identifikátor dokladu
        sentence.add("FTUIDTX" + "TX" + UUID.randomUUID().toString());
        //Otevření transakce
        sentence.add("FTOPEN0");
        //Produkty
        //count, display name, price, total price, vat
        sentence.add("FITEMA" + "0.00");
        sentence.add("FITEMP" + "0.00");
        sentence.add("FITEMT" + "Vstupne");
        sentence.add("FITEMQ" + "1");
        sentence.add("FITEMV" + "1");

        //Total price
        sentence.add("FTOTA" + "0.00");
        //Payment type
        sentence.add("FPAYR" + "Hotovost");
        sentence.add("FPAYI" + "1");
        //Total price2
        sentence.add("FPAYA" + "0.00");

        //Uzavření transakce
        sentence.add("FTCLOSE");
        System.out.println(Arrays.toString(sentence.toArray()));
        return sentence;
    }
}
