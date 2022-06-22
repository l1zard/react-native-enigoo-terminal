package com.enigoo.terminal.fiskalpro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class FPPayment {

  private static String prefix = "Enigoo-";

  private byte[] prevodnik(String message) throws IOException {

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    buffer.write(new byte[]{(byte) 0x01});
    buffer.write(new byte[]{(byte) 0x01});

    buffer.write(new byte[]{(byte) (message.length() / 256)});
    buffer.write(new byte[]{(byte) (message.length() % 256)});

    buffer.write(message.getBytes());
    buffer.write(new byte[]{this.lrc(message)});
    return buffer.toByteArray();
  }

  private byte lrc(String message) {
    byte lrc = 0x00;
    byte[] bytes = message.getBytes();

    for (int i = 0; i < message.length(); i++) {
      lrc = (byte) (lrc ^ bytes[i]);
    }

    return lrc;
  }


  public byte[] dateTest() throws IOException {

    return this.prevodnik("FRRTC");
  }

  public byte[] createOrderId(String orderId) throws IOException {

    return this.prevodnik("FTUID" + prefix + orderId);
  }

  public byte[] createOrder(String price) throws IOException {

    return this.prevodnik("FTCARDSTART" + price.replace(",", "."));
  }

  public byte[] refund(String price) throws IOException {

    return this.prevodnik("FTCARDCANCELLAST-" + price.replace(",", "."));
  }


}
