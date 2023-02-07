package com.enigoo.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.enigoo.terminal.csob.Connection;
import com.enigoo.terminal.csob.Payment;
import com.enigoo.terminal.csob.SendMessage;
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


    EnigooTerminalModule(@Nullable ReactApplicationContext reactContext) {
        super(reactContext);
        reactApplicationContext = reactContext;
    }

    @Override
    @NonNull
    public String getName(){
        return NAME;
    }

    @ReactMethod
    public static void emit(String data){
        emitDeviceEvent("TERMINAL_EVENTS",data);
    }

    private static void emitDeviceEvent(String eventName, String eventData){
        reactApplicationContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName,eventData);
    }

    @ReactMethod
    public void createCsobPayment(String price, String ipAddress, int port, String deviceId) throws JSONException, IOException, ExecutionException, InterruptedException {
        Payment pay = new Payment(deviceId);
        Socket s = new Connection(ipAddress, port,pay).execute().get();
        if (s == null) {
            emit(new JSONObject().put("type", "CONNECTION").put("status", "FAILED").toString());
        } else {
            emit(new JSONObject().put("type", "CONNECTION").put("status", "SUCCESS").toString());
            new SendMessage(s, pay.createPayment(Double.parseDouble(price))).execute();
            emit(new JSONObject().put("type", "CREATE_PAYMENT").put("status", "SUCCESS").toString());
        }
    }

    @ReactMethod
    public void createCsobRefund(String price, String ipAddress, int port, String deviceId) throws IOException, JSONException, ExecutionException, InterruptedException {
        Payment pay = new Payment(deviceId);
        Socket s = new Connection(ipAddress, port,pay).execute().get();
        if (s == null) {
            emit(new JSONObject().put("type", "CONNECTION").put("status", "FAILED").toString());
        } else {
            emit(new JSONObject().put("type", "CONNECTION").put("status", "SUCCESS").toString());
            new SendMessage(s, pay.createRefund(Double.parseDouble(price))).execute();
            emit(new JSONObject().put("type", "CREATE_REFUND").put("status", "SUCCESS").toString());
        }
    }

    @ReactMethod
    public void createCsobCloseTotals(String ipAddress, int port, String deviceId) throws ExecutionException, InterruptedException, JSONException, IOException {
        Payment pay = new Payment(deviceId);
        Socket s = new Connection(ipAddress,port,pay).execute().get();
        if(s == null){
            emit(new JSONObject().put("type","CONNECTION").put("status","FAILED").toString());
        }else{
            emit(new JSONObject().put("type","CONNECTION").put("status","SUCCESS").toString());
            new SendMessage(s, pay.createCloseTotals()).execute();
            emit(new JSONObject().put("type", "CLOSE_TOTALS").put("status", "SUCCESS").toString());
        }
    }

    @ReactMethod
    public void createCsobHandshake(String ipAddress, int port, String deviceId) throws ExecutionException, InterruptedException, JSONException, IOException {
        Payment pay = new Payment(deviceId);
        Socket s = new Connection(ipAddress,port,pay).execute().get();
        if(s == null){
            emit(new JSONObject().put("type","CONNECTION").put("status","FAILED").toString());
        }else{
            emit(new JSONObject().put("type","CONNECTION").put("status","SUCCESS").toString());
            new SendMessage(s, pay.createHandshake()).execute();
            emit(new JSONObject().put("type", "HANDSHAKE").put("status", "SUCCESS").toString());
        }
    }

    @ReactMethod
    public void createCsobTmsBCall(String ipAddress, int port, String deviceId) throws ExecutionException, InterruptedException, JSONException, IOException {
        Payment pay = new Payment(deviceId);
        Socket s = new Connection(ipAddress,port,pay).execute().get();
        if(s == null){
            emit(new JSONObject().put("type","CONNECTION").put("status","FAILED").toString());
        }else{
            emit(new JSONObject().put("type","CONNECTION").put("status","SUCCESS").toString());
            new SendMessage(s, pay.createBTmsCall()).execute();
            emit(new JSONObject().put("type", "TMS_CALL").put("status", "SUCCESS").toString());
        }
    }

    @ReactMethod
    public void createCsobTmsNCall(String ipAddress, int port, String deviceId) throws ExecutionException, InterruptedException, JSONException, IOException {
        Payment pay = new Payment(deviceId);
        Socket s = new Connection(ipAddress,port,pay).execute().get();
        if(s == null){
            emit(new JSONObject().put("type","CONNECTION").put("status","FAILED").toString());
        }else{
            emit(new JSONObject().put("type","CONNECTION").put("status","SUCCESS").toString());
            new SendMessage(s, pay.createNTmsCall()).execute();
            emit(new JSONObject().put("type", "TMS_CALL").put("status", "SUCCESS").toString());
        }
    }


}
