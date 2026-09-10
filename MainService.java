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

// ==================== CLICK CHAT AT POSITION ====================
public boolean clickChatAtPosition(int position) {
    try {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<AccessibilityNodeInfo> lists = root.findAccessibilityNodeInfosByClassName("android.widget.ListView");
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
        List<AccessibilityNodeInfo> lists = root.findAccessibilityNodeInfosByClassName("android.widget.ListView");
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
        List<AccessibilityNodeInfo> editTexts = root.findAccessibilityNodeInfosByClassName("android.widget.EditText");
        if (!editTexts.isEmpty()) {
            AccessibilityNodeInfo editText = editTexts.get(0);
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        }
    } catch (Exception e) {}
}
