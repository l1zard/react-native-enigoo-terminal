package com.enigoo.terminal;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;


import com.enigoo.terminal.csob.Connection;
import com.enigoo.terminal.csob.Payment;
import com.enigoo.terminal.csob.SendMessage;
import com.enigoo.terminal.fiskalpro.FPConnection;
import com.enigoo.terminal.fiskalpro.FPPayment;
import com.enigoo.terminal.fiskalsk.FiskalPro;
import com.enigoo.terminal.fiskalsk.UsbService;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
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
      new SendMessage(s, new FPPayment().createOrder(price)).execute();
      emit(new JSONObject().put("type", "CREATE_PAYMENT").put("status", "SUCCESS").toString());
    }
  }

  @ReactMethod
  public void createFiscalProRefund(String price, String orderId, String ipAddress, int port) throws IOException, JSONException, ExecutionException, InterruptedException {
    Socket s = new FPConnection(ipAddress, port, "RETURN").execute().get();
    if (s == null) {
      emit(new JSONObject().put("type", "CONNECTION").put("status", "FAILED").toString());
    } else {
      emit(new JSONObject().put("type", "CONNECTION").put("status", "SUCCESS").toString());
      new SendMessage(s, new FPPayment().createOrderId(orderId)).execute();
      new SendMessage(s, new FPPayment().refund(price)).execute();
      emit(new JSONObject().put("type", "CREATE_REFUND").put("status", "SUCCESS").toString());
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
