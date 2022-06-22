package com.enigoo.terminal;

import androidx.annotation.NonNull;

import com.enigoo.terminal.csob.Connection;
import com.enigoo.terminal.csob.Payment;
import com.enigoo.terminal.csob.SendMessage;
import com.enigoo.terminal.fiskalpro.FPConnection;
import com.enigoo.terminal.fiskalpro.FPPayment;
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
    if (s == null) {
      emit(new JSONObject().put("type", "CONNECTION").put("status", "FAILED").toString());
    } else {
      emit(new JSONObject().put("type", "CONNECTION").put("status", "SUCCESS").toString());
      new SendMessage(s, new Payment(deviceId).createPayment(Double.parseDouble(price))).execute();
      emit(new JSONObject().put("type", "CREATE_PAYMENT").put("status", "SUCCESS").toString());
    }
  }

  @ReactMethod
  public void createCsobRefund(String price, String ipAddress, int port, String deviceId) throws IOException, JSONException, ExecutionException, InterruptedException {
    Socket s = new Connection(ipAddress, port).execute().get();
    if (s == null) {
      emit(new JSONObject().put("type", "CONNECTION").put("status", "FAILED").toString());
    } else {
      emit(new JSONObject().put("type", "CONNECTION").put("status", "SUCCESS").toString());
      new SendMessage(s, new Payment(deviceId).createRefund(Double.parseDouble(price))).execute();
      emit(new JSONObject().put("type", "CREATE_REFUND").put("status", "SUCCESS").toString());
    }
  }

  @ReactMethod
  public void createFiscalProPayment(String price, String orderId, String ipAddress, int port) throws IOException, JSONException, ExecutionException, InterruptedException {
    Socket s = new FPConnection(ipAddress, port, "PURCHASE").execute().get();
    if (s == null) {
      emit(new JSONObject().put("type", "CONNECTION").put("status", "FAILED").toString());
    } else {
      emit(new JSONObject().put("type", "CONNECTION").put("status", "SUCCESS").toString());
      new SendMessage(s, new FPPayment().createOrderId(orderId)).execute();
      new SendMessage(s, new FPPayment().createOrder()).execute();
      emit(new JSONObject().put("type", "CREATE_PAYMENT").put("status", "SUCCESS").toString());
    }
  }

}
