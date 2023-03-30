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
import com.enigoo.terminal.csob.enums.TransactionTypes;
import com.enigoo.terminal.csob.logger.LoggerConnection;
import com.enigoo.terminal.csob.socket_connection.ConnectionInit;
import com.enigoo.terminal.csob.socket_connection.SocketConnection;
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
  public static ReactApplicationContext reactApplicationContext = null;

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
  public void initCsobConnection(String ipAddress, int port, String deviceId) {
    new ConnectionInit(ipAddress, port, deviceId).execute();
  }

  @ReactMethod
  public void createCsobGetAppInfo() throws IOException {
    Payment pay = new Payment(SocketConnection.getDeviceId());
    new Connection(pay, pay.createRequest(TransactionTypes.GET_APP_INFO, 0, null), "GET_APP_INFO", "GET_INFO").execute();
  }

  @ReactMethod
  public void createCsobHandshake() throws IOException {
    Payment pay = new Payment(SocketConnection.getDeviceId());
    new Connection(pay, pay.createRequest(TransactionTypes.HANDSHAKE, 0, null), "HANDSHAKE", "HANDSHAKE").execute();
  }

  @ReactMethod
  public void createCsobPayment(String price, String orderId) throws IOException {
    Payment pay = new Payment(SocketConnection.getDeviceId());
    new Connection(pay, pay.createRequest(TransactionTypes.NORMAL_PURCHASE, Double.parseDouble(price), orderId), "PAYMENT", orderId).execute();
  }
  @ReactMethod
  public void createCsobPassivate() throws IOException {
    Payment pay = new Payment(SocketConnection.getDeviceId());
    new Connection(pay, pay.createRequest(TransactionTypes.PASSIVATE, 0, null), "PASSIVATE", "PASSIVATE").execute();
  }

  @ReactMethod
  public void createCsobRefund(String price, String orderId) throws IOException {
    Payment pay = new Payment(SocketConnection.getDeviceId());
    new Connection(pay, pay.createRequest(TransactionTypes.REFUND, Double.parseDouble(price), orderId), "REFUND", orderId).execute();
  }

  @ReactMethod
  public void createCsobCloseTotals() throws IOException {
    Payment pay = new Payment(SocketConnection.getDeviceId());
    new Connection(pay, pay.createRequest(TransactionTypes.CLOSE_TOTALS, 0, null), "CLOSE_TOTALS", "CLOSE_TOTALS").execute();
  }

  @ReactMethod
  public void createCsobTmsBCall() throws IOException {
    Payment pay = new Payment(SocketConnection.getDeviceId());
    new Connection(pay, pay.createRequest(TransactionTypes.TMS_CALL, 0, null), "TMS_CALL", "TMS_CALL").execute();
  }

  @ReactMethod
  public void getCsobLog(String date,String orderId) {
    new LoggerConnection("GET",date,orderId).execute();
  }

  @ReactMethod
  public void getCsobLogByDate(String date) {
    new LoggerConnection("GET",date,null).execute();
  }

  @ReactMethod
  public void deleteCsobLog(String date) {
    new LoggerConnection("DELETE",date,null).execute();
  }

  @ReactMethod
  public void createCsobReversal(String approvalCode) throws IOException {
    Payment pay = new Payment(SocketConnection.getDeviceId());
    new Connection(pay, pay.createReversalRequest(approvalCode), "REVERSAL", "REVERSAL").execute();
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
