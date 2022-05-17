package com.enigoo.terminal;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

@ReactModule(name = EnigooTerminalModule.NAME)
public class EnigooTerminalModule extends ReactContextBaseJavaModule {
  public static final String NAME = "EnigooTerminal";
  private static ReactApplicationContext reactApplicationContext = null;

  public EnigooTerminalModule(ReactApplicationContext reactContext) {
    super(reactContext);
    reactApplicationContext = reactContext;
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public static void emit(String data) {
    emitDeviceEvent("TERMINAL_EVENTS", data);
  }

  private static void emitDeviceEvent(String eventName, String eventData) {
    reactApplicationContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, eventData);
  }

  @ReactMethod
  public void createCsobPayment(String price, String ipAddress, int port, String deviceId) throws IOException, JSONException, ExecutionException, InterruptedException {
    Socket s = new Connection(ipAddress, port).execute().get();
    new SendMessage(s, new Payment(deviceId).createPayment(Double.parseDouble(price))).execute();

    emit(new JSONObject().put("type", "CREATE_PAYMENT").put("status", "SUCCESS").toString());
  }

  @ReactMethod
  public void createCsobRefund(String price, String ipAddress, int port, String deviceId) throws IOException, JSONException, ExecutionException, InterruptedException {
    Socket s = new Connection(ipAddress, port).execute().get();
    new SendMessage(s, new Payment(deviceId).createRefund(Double.parseDouble(price))).execute();

    emit(new JSONObject().put("type", "CREATE_REFUND").put("status", "SUCCESS").toString());
  }


}
