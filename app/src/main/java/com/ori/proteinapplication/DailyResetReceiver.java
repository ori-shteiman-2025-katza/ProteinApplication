package com.ori.proteinapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class DailyResetReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences prefs =
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);

        String today = LocalDate.now().toString(); // yyyy-MM-dd
        String lastReset = prefs.getString("last_reset_date", "");

        // אם כבר איפסנו היום – יוצאים
        if (today.equals(lastReset)) return;

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("Users")
                        .child(uid);

        Map<String, Object> reset = new HashMap<>();
        reset.put("currentProtein", 0);
        reset.put("currentCalories", 0);

        ref.updateChildren(reset)
                .addOnSuccessListener(unused -> {

                    // ✅ רק אם האיפוס הצליח

                    sendDailySummaryNotification(context);

                    prefs.edit()
                            .putString("last_reset_date", today)
                            .apply();
                })
                .addOnFailureListener(e -> {
                    // ❌ לא איפס – לא שולחים נוטיפיקציה
                    // ❌ לא מעדכנים תאריך
                    Log.e("DailyResetReceiver", "Daily reset failed", e);
                });
        //TODO if failed
    }


    private void sendDailySummaryNotification(Context context) {

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "daily_summary";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Daily Summary",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            nm.createNotificationChannel(channel);
        }

        Notification notification =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(com.google.firebase.appcheck.interop.R.drawable.common_google_signin_btn_text_dark_normal)
                        .setContentTitle("סיכום יומי")
                        .setContentText("הנתונים של היום נשמרו והאיפוס בוצע ✔️")
                        .build();

        int notificationId = (int) System.currentTimeMillis();
        nm.notify(notificationId, notification);
    }


}