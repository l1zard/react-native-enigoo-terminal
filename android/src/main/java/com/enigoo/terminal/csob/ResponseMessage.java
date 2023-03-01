package com.enigoo.terminal.csob;

import java.util.Date;

public class ResponseMessage {

  private Date date;
  private byte[] message;

  public ResponseMessage(Date date, byte[] message) {
    this.date = date;
    this.message = message;
  }

  public Date getDate() {
    return date;
  }

  public byte[] getMessage() {
    return message;
  }
}
