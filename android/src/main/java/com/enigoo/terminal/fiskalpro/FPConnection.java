package com.enigoo.terminal.fiskalpro;

import android.os.AsyncTask;

import com.enigoo.terminal.EnigooTerminalModule;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONException;

import java.io.IOException;
import java.net.Socket;

public class FPConnection extends AsyncTask<Void, Void, Socket> {

  private final String mServerAddress;
  private final int mServerPort;
  private final String mTtype;

  public FPConnection(String serverAddress, int serverPort, String type) {
    mServerAddress = serverAddress;
    mServerPort = serverPort;
    mTtype = type;
  }

  @Override
  protected Socket doInBackground(Void... voids) {
    try {
      Socket socket = new Socket(mServerAddress, mServerPort);
      new Thread(new GetThread(socket, mTtype)).start();

      return socket;

    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  static class GetThread implements Runnable {

    Socket socket;
    String type;


    public GetThread(Socket socket, String type) {
      this.socket = socket;
      this.type = type;
    }

    public byte bytesToMessage(byte[] bytes) {
      return bytes[3];
    }

    @Override
    public void run() {


      boolean isDone = false;
      FPResponse res = null;
      try {
        while (!isDone) {
          // byte[] data = new byte[socket.getInputStream().read()];
          // int count = mSocketContainer.getSocketInputStream().read(data);
          byte[] data = new byte[1024];
          int count = socket.getInputStream().read(data);
          if (count == 4) {
            FPMessage message = new FPMessage(bytesToMessage(data));
            FPResponse response = message.process();
            if (response.isDone()) {
              res = response;
            }

            isDone = response.isDone();
          }
        }

        if (res != null) {
          WritableMap params = Arguments.createMap();
          params.putString("type", "this.type");
          EnigooTerminalModule.emit(params);
        }

        socket.close();

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
