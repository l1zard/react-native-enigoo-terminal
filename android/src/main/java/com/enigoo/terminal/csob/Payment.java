package com.enigoo.terminal.csob;

import com.enigoo.terminal.EnigooTerminalModule;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Payment {

    private final int STX = 0x02; //zacatek
    private final int FID = 0x1c; //oddelovac fidu
    private final int SFID = 0x1d; //oddelovac subfidu
    private final int ETX = 0x03; //konec
    private final String CRC_CONST = "A5A5";
    private final String PROTOCOL_VERSION = "01";
    private final String FLAGS = "8000";
    private final String CZK_CODE = "E203";
    private final String deviceId;

    public Payment(String deviceId) {
        this.deviceId = deviceId;
    }

    private String calculateLength(String[] message) {
        int count = message.length + 1;
        for (String s : message) {
            if (s.startsWith("9S")) {
                count += message[0].length();
                count += message[0].length();
            }
            count += message[0].length();
        }

        String string = Integer.toHexString(count);

        if (string.length() < 4) {
            int length = 4 - string.length();
            for (int i = 0; i < length; i++) {
                string = "0" + string;
            }
        }

        return string;
    }

    private byte[] prevodnik(String header, String[] message) throws IOException {

        byte[] data = header.getBytes();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(new byte[]{(byte) STX});
        buffer.write(data);

        for (String item : message) {
            buffer.write(new byte[]{(byte) FID});
            if (item.startsWith("9S")) {
                buffer.write("9".getBytes());
                buffer.write(new byte[]{(byte) SFID});
                buffer.write(item.substring(1).getBytes());
            } else {
                buffer.write(item.getBytes());
            }
        }
        buffer.write(new byte[]{(byte) ETX});
        return buffer.toByteArray();
    }

    private String parsePrice(double price) {
        return "B" + String.format("%.2f", price).replace(".", "").replace(",", "") + "";
    }

    public byte[] createRequest(TransactionTypes type, double price, String orderId) throws IOException {
        switch (type) {
            case REFUND:
            case NORMAL_PURCHASE:
                //1. odešle se B1 s požadavkem na transakci
                //2. terminál okamžitě po příjetí odešle odpověď B0 (potvrzení přijetí požadavku)
                //3. pokladna čeká na další zprávy, které můžou být buď B0 nebo B2
                //4. Autorizace platby dokončena - terminál odesílá B2
                //5. pokladna přijme B2 z terminálu a do 5s odesílá potvrzení příjmu za pomocí B0
                //Další realizace dle nutnosti vyžádání lístečku
                String[] messages;
                if (orderId.length() < 10 && orderId.length()>0) {
                    messages = new String[4];
                    messages[0] = type.getCode();
                    messages[1] = parsePrice(price);
                    messages[2] = "S" + orderId;
                    messages[3] = CZK_CODE;
                }else if(orderId.length() >=10 && orderId.length() <20 ){
                    messages = new String[4];
                    messages[0] = type.getCode();
                    messages[1] = parsePrice(price);
                    messages[2] = "9S" + orderId;
                    messages[3] = CZK_CODE;
                } else {
                    messages = new String[3];
                    messages[0] = type.getCode();
                    messages[1] = parsePrice(price);
                    messages[2] = CZK_CODE;
                }
                return this.prevodnik(ProtocolTypes.TRANSACTION_REQUEST.getCode() + PROTOCOL_VERSION + this.deviceId + this.getDate() + FLAGS + this.calculateLength(messages) + CRC_CONST, messages);
            case GET_LAST_TRANS:
            case CLOSE_TOTALS:
            case TMS_CALL:
            case GET_APP_INFO:
            case HANDSHAKE:
            case PASSIVATE:
                String[] messages2 = {type.getCode()};
                return this.prevodnik(ProtocolTypes.TRANSACTION_REQUEST.getCode() + PROTOCOL_VERSION + this.deviceId + this.getDate() + "0000" + "0004" + CRC_CONST, messages2);
            default:
                return new byte[0];

        }

    }

    public byte[] createPassivateRequest() throws IOException {
        String[] messages2 = {TransactionTypes.PASSIVATE.getCode()};
        return this.prevodnik(ProtocolTypes.TRANSACTION_REQUEST.getCode() + PROTOCOL_VERSION + this.deviceId + this.getDate() + "8000" + "0004" + CRC_CONST, messages2);
    }

    public byte[] createTicketRequest(String t) throws IOException {
        String[] messages = {"9", t};
        byte[] data = (ProtocolTypes.TICKET_REQUEST.getCode() + PROTOCOL_VERSION + this.deviceId + this.getDate() + FLAGS + this.calculateLength(messages) + CRC_CONST).getBytes();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(new byte[]{(byte) STX}); //0x02 - STX, 0x1c - FID, 0x1d - SFID/GS
        buffer.write(data);
        buffer.write(new byte[]{(byte) FID});
        buffer.write("9".getBytes());
        buffer.write(new byte[]{(byte) SFID});
        buffer.write(t.getBytes());
        buffer.write(new byte[]{(byte) ETX});

        return buffer.toByteArray();
    }

    public byte[] createConfirmRequest() throws IOException {
        return this.prevodnik(ProtocolTypes.ACTIVITY_INFO_MESSAGE.getCode() + PROTOCOL_VERSION + this.deviceId + this.getDate() + "0000" + "0000" + CRC_CONST, new String[0]);
    }


    private String getDate() {
        String pattern = "YYMMddHHmmss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(new Date());
    }


    public byte[] createReversalRequest(String approvalCode) throws IOException {
        String[] messages = {TransactionTypes.REVERSAL.getCode(), "F" + approvalCode};
        byte[] req = this.prevodnik(ProtocolTypes.TRANSACTION_REQUEST.getCode() + PROTOCOL_VERSION + this.deviceId + this.getDate() + FLAGS + "000E" + CRC_CONST, messages);
        WritableMap map = Arguments.createMap();
        map.putString("request", new String(req));
        EnigooTerminalModule.emit(map);
        return req;
    }
}
