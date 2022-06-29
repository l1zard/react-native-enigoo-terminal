package com.enigoo.terminal.fiskalsk;

import com.enigoo.terminal.EnigooTerminalModule;

import org.json.JSONException;
import org.json.JSONObject;

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

  public static void print(UsbService usbService, String data) {
    if (usbService != null) { // if UsbService was correctly binded, Send data
      try {
        for (String com : generateDataSentence(data)) {
          usbService.write(createCommand(com));
        }

        EnigooTerminalModule.emit(new JSONObject().put("type", "FISKAL_PAYMENT").put("status", "SUCCESS").toString());
      } catch (IOException | JSONException e) {
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


  private static List<String> generateDataSentence(String data) throws JSONException {
    List<String> sentence = new ArrayList<>();
    JSONObject dataObject = new JSONObject(data);


    //Unikátní identifikátor dokladu
    sentence.add("FTUIDTX" + "TX" + UUID.randomUUID().toString());
    //Otevření transakce
    sentence.add("FTOPEN0");
    //Produkty
    //count, display name, price, total price, vat

    Double totalPrice = 0.00;

    for (int i = 0; i < dataObject.getJSONArray("items").length(); i++) {
      JSONObject obj = dataObject.getJSONArray("items").getJSONObject(i);

      Double price = obj.getDouble("singlePrice") * obj.getInt("count");
      totalPrice = totalPrice + price;

      sentence.add("FITEMA" + String.format("%.2f", price).replace(",", "."));
      sentence.add("FITEMP" + String.format("%.2f", obj.getDouble("singlePrice")).replace(",", "."));
      sentence.add("FITEMT" + obj.getString("name"));
      sentence.add("FITEMQ" + obj.get("count").toString());
      sentence.add("FITEMV" + obj.get("vat").toString());
    }


    //Total price
    sentence.add("FTOTA" + String.format("%.2f", totalPrice).replace(",", "."));
    //Payment type
    sentence.add("FPAYR" + (dataObject.get("paymentType").toString().equals("1") ? "Hotovost" : "Kartou"));
    sentence.add("FPAYI" + dataObject.get("paymentType").toString());
    //Total price2
    sentence.add("FPAYA" + String.format("%.2f", totalPrice).replace(",", "."));

    //Uzavření transakce
    sentence.add("FTCLOSE");
    return sentence;
  }
}
