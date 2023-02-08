package com.enigoo.terminal;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;


import com.enigoo.terminal.csob.Connection;
import com.enigoo.terminal.csob.Payment;
import com.enigoo.terminal.fiskalpro.FPConnection;
import com.enigoo.terminal.fiskalpro.FPPayment;
import com.enigoo.terminal.fiskalpro.FPSendMessage;
import com.enigoo.terminal.fiskalsk.FiskalPro;
import com.enigoo.terminal.fiskalsk.UsbService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONException;

import java.io.IOException;
import java.net.Socket;
import java.util.Set;
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
  public static void emit(WritableMap data){
    emitDeviceEvent("TERMINAL_EVENTS",data);
  }

  private static void emitDeviceEvent(String eventName, WritableMap eventData){
    reactApplicationContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName,eventData);
  }

  @ReactMethod
  public void createCsobPayment(String price, String ipAddress, int port, String deviceId)  throws IOException {
    Payment pay = new Payment(deviceId);
    new Connection(ipAddress,port,pay,pay.createPayment(Double.parseDouble(price))).execute();
  }

  @ReactMethod
  public void createCsobRefund(String price, String ipAddress, int port, String deviceId) throws IOException {
    Payment pay = new Payment(deviceId);
    new Connection(ipAddress,port,pay,pay.createRefund(Double.parseDouble(price))).execute();
  }

  @ReactMethod
  public void createCsobCloseTotals(String ipAddress, int port, String deviceId) throws ExecutionException, InterruptedException, JSONException, IOException {
    Payment pay = new Payment(deviceId);
    new Connection(ipAddress,port,pay,pay.createCloseTotals()).execute();
  }

  @ReactMethod
  public void createCsobHandshake(String ipAddress, int port, String deviceId) throws ExecutionException, InterruptedException, JSONException, IOException {
    Payment pay = new Payment(deviceId);
    new Connection(ipAddress,port,pay,pay.createHandshake()).execute();
  }

  @ReactMethod
  public void createCsobTmsBCall(String ipAddress, int port, String deviceId) throws ExecutionException, InterruptedException, JSONException, IOException {
    Payment pay = new Payment(deviceId);
    new Connection(ipAddress,port,pay,pay.createBTmsCall()).execute();
  }

  @ReactMethod
  public void createCsobTmsNCall(String ipAddress, int port, String deviceId) throws ExecutionException, InterruptedException, JSONException, IOException {
    Payment pay = new Payment(deviceId);
    new Connection(ipAddress,port,pay,pay.createNTmsCall()).execute();
  }

  @ReactMethod
  public void createFiscalProPayment(String price, String orderId, String ipAddress, int port) throws IOException, JSONException, ExecutionException, InterruptedException {
    Socket s = new FPConnection(ipAddress, port, "PURCHASE").execute().get();
    if (s == null) {
      WritableMap params = Arguments.createMap();
      params.putString("type", "CONNECTION");
      params.putString("status", "FAILED");
      emit(params);
    } else {
      WritableMap params = Arguments.createMap();
      params.putString("type", "CONNECTION");
      params.putString("status", "SUCCESS");
      emit(params);
      new FPSendMessage(s, new FPPayment().createOrderId(orderId)).execute();
      new FPSendMessage(s, new FPPayment().createOrder(price)).execute();
      params = Arguments.createMap();
      params.putString("type", "CREATE_PAYMENT");
      params.putString("status", "SUCCESS");
      emit(params);
    }
  }

  @ReactMethod
  public void createFiscalProRefund(String price, String orderId, String ipAddress, int port) throws IOException, JSONException, ExecutionException, InterruptedException {
    Socket s = new FPConnection(ipAddress, port, "RETURN").execute().get();
    if (s == null) {
      WritableMap params = Arguments.createMap();
      params.putString("type", "CONNECTION");
      params.putString("status", "FAILED");
      emit(params);
    } else {
      WritableMap params = Arguments.createMap();
      params.putString("type", "CONNECTION");
      params.putString("status", "SUCCESS");
      emit(params);
      new FPSendMessage(s, new FPPayment().createOrderId(orderId)).execute();
      new FPSendMessage(s, new FPPayment().refund(price)).execute();
      params = Arguments.createMap();
      params.putString("type", "CREATE_REFUND");
      params.putString("status", "SUCCESS");
      emit(params);
    }
  }


  @ReactMethod
  public void setUsbServiceFiskalProSk() {
    startService(UsbService.class, usbConnection, null);
  }

  @ReactMethod
  public void createFiskalProSkTerminalRecord(String data) throws JSONException {
    FiskalPro.print(usbService, data);
  }


  public static UsbService usbService;

  protected final ServiceConnection usbConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName arg0, IBinder arg1) {
      usbService = ((UsbService.UsbBinder) arg1).getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      usbService = null;
    }
  };

  protected void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
    if (!UsbService.SERVICE_CONNECTED) {
      Intent startService = new Intent(reactApplicationContext.getBaseContext(), service);
      if (extras != null && !extras.isEmpty()) {
        Set<String> keys = extras.keySet();
        for (String key : keys) {
          String extra = extras.getString(key);
          startService.putExtra(key, extra);
        }
      }
      reactApplicationContext.getBaseContext().startService(startService);
    }
    Intent bindingIntent = new Intent(reactApplicationContext.getBaseContext(), service);
    reactApplicationContext.getBaseContext().bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
  }

}
