package com.ori.proteinapplication;


import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.time.LocalDate;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;


/**
 * MainDashboardActivity — מקבל תמונה, שולח ל-GeminiManager,
 * מציג דיאלוג עריכה של רכיבים עם משקל, מחשב חלבון/קלוריות,
 * ושומר לפיירסטור/Realtime DB.
 */
public class MainDashboardActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "MainDashboardActivity";

    // UI
    private ImageView imgMealPreview;
    private TextView tvProteinProgress, tvCaloriesProgress;
    private CircularProgressIndicator progressProteinCircular, progressCaloriesCircular;
    private ProgressBar progressAi;
    private Button btnUploadMeal;

    // Firebase
    private FirebaseFirestore firestoreDb;
    private DatabaseReference realtimeDbRef;
    private String uid;

    // user goals / current
    private int proteinGoal = 0, caloriesGoal = 0;
    private int currentProtein = 0, currentCalories = 0;

    // image + bitmap
    private Uri imageUri;
    private Bitmap imageBitmap;
    private List<Meal> allMeals = new ArrayList<>(); // כל הארוחות
    private List<Meal> filteredMeals = new ArrayList<>(); // רק לפי יום
    private Button btnCaptureMeal;
    private static final int CAMERA_REQUEST = 2; // קוד מזהה למצלמה
    private static final int CAMERA_PERMISSION_CODE = 100;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private TextView tvStreak;

    // מאגר ערכי תזונה נפוצים (per 100g) — אפשר להרחיב
    private final Map<String, float[]> nutrientPer100gMap = new HashMap<>();

    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dashboard);

        tvStreak = findViewById(R.id.tvStreak);
        imgMealPreview = findViewById(R.id.imgMeal);
        tvProteinProgress = findViewById(R.id.tvProteinProgress);
        tvCaloriesProgress = findViewById(R.id.tvCaloriesProgress);
        progressProteinCircular = findViewById(R.id.progressProteinCircular);
        progressCaloriesCircular = findViewById(R.id.progressCaloriesCircular);
        progressAi = findViewById(R.id.progressAi);
        btnUploadMeal = findViewById(R.id.btnUploadMeal);
        btnCaptureMeal = findViewById(R.id.btnCaptureMeal); // <--- השורה שהייתה חסרה!
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 2. firebase
        uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid != null) {
            firestoreDb = FirebaseFirestore.getInstance();
            realtimeDbRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            loadUserGoals();
        }

        // 3. Listeners

        // כפתור גלריה
        btnUploadMeal.setOnClickListener(v -> openFileChooser());
// כפתור מצלמה (עם בדיקת הרשאות)
        if (btnCaptureMeal != null) {
            btnCaptureMeal.setOnClickListener(v -> {
                if (checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // זה יפעיל את ה-cameraLauncher.launch(intent) החדש
                    openCamera();
                } else {
                    requestPermissions(new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                }
            });
        }
        // בתוך ה-onCreate שלך
        uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (uid != null) {
            firestoreDb = FirebaseFirestore.getInstance();
            realtimeDbRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

            // --- כאן תקרא לפונקציה החדשה ---
            checkAndResetDaily();

            loadUserGoals();
        }
// הגדרת חזרה מהמצלמה
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageBitmap = (Bitmap) result.getData().getExtras().get("data");
                        handleImageSuccess();
                    }
                }
        );

        // הגדרת חזרה מהגלריה
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        try {
                            imageBitmap = uriToBitmap(imageUri);
                            imageBitmap = resizeBitmapIfNeeded(imageBitmap, 1024);
                            handleImageSuccess();
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
        // 4. Helpers
        scheduleDailyReset();
        BottomNavigationHelper.setupBottomNavigation(
                this,
                bottomNavigationView,
                R.id.nav_main
        );
        checkAndUpdateStreak();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void openCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    // פונקציית עזר כדי לא לשכפל קוד
    private void handleImageSuccess() {
        if (imageBitmap != null) {
            imgMealPreview.setImageBitmap(imageBitmap);
            analyzeMealWithAI(imageBitmap);
        }
    }

    private void scheduleDailyReset() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, DailyResetReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // הגדרת זמן לחצות הלילה הקרוב
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // אם חצות של היום כבר עבר (כלומר עכשיו כבר בוקר/צהריים), נקבע למחר ב-00:00
        if (cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            // ביטול של שעונים קודמים אם היו (ליתר ביטחון)
            alarmManager.cancel(pendingIntent);

            // קביעת השעון המעורר היומי
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    cal.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }

        // הערה חשובה: הסרתי את ה-if שבודק "alarm_set" כדי שהשינוי יתפוס מיד
        Log.d(TAG, "השעון הוגדר מחדש לחצות הלילה.");
    }
    private void checkAndResetDaily() {
        // SharedPreferences זה זיכרון קטן בתוך הטלפון
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String lastResetDate = prefs.getString("lastResetDate", "");

        // השגת התאריך של היום
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // אם התאריך השמור לא שווה לתאריך של היום - סימן שעבר לילה!
        if (!lastResetDate.equals(todayDate)) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("currentProtein", 0);
            updates.put("currentCalories", 0);

            realtimeDbRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                // שומרים שהיום כבר איפסנו, כדי שלא יאפס שוב בכל פעם שנכנסים
                prefs.edit().putString("lastResetDate", todayDate).apply();
                Log.d(TAG, "הנתונים אופסו בהצלחה ליום חדש");
            });
        }
    }
    private void loadUserGoals() {
        if (uid == null) return;

        realtimeDbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // המרה בטוחה של כל הערכים - זה מונע קריסות אם המספר נשמר כעשרוני
                    proteinGoal = getSafeInt(snapshot.child("dailyProtein"));
                    caloriesGoal = getSafeInt(snapshot.child("dailyCalories"));
                    currentProtein = getSafeInt(snapshot.child("currentProtein"));
                    currentCalories = getSafeInt(snapshot.child("currentCalories"));
                }
                // עדכון התצוגה של העיגולים והטקסט
                updateDashboard();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadUserGoals cancelled", error.toException());
            }
        });
    }

    // פונקציית עזר קטנה כדי להפוך את הקוד למעלה לנקי ובטוח יותר
    private int getSafeInt(DataSnapshot snapshot) {
        Object value = snapshot.getValue();
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0; // ערך ברירת מחדל אם אין נתונים
    }

    private void updateDashboard() {
        // --- חגיגת יעד חלבון ---
        if (proteinGoal > 0 && currentProtein >= proteinGoal) {
            tvProteinProgress.setText("🏆 יעד הושג! " + currentProtein + " / " + proteinGoal);
            tvProteinProgress.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // ירוק
        } else {
            tvProteinProgress.setText(currentProtein + " / " + proteinGoal + " גרם חלבון");
            tvProteinProgress.setTextColor(android.graphics.Color.WHITE); // או הצבע הרגיל שלך
        }
        tvCaloriesProgress.setText(currentCalories + " / " + caloriesGoal + " קלוריות");
        progressProteinCircular.setProgress(proteinGoal > 0 ? (int)((currentProtein/(float)proteinGoal)*100) : 0);
        progressCaloriesCircular.setProgress(caloriesGoal > 0 ? (int)((currentCalories/(float)caloriesGoal)*100) : 0);
    }



    // Convert uri -> Bitmap
    private Bitmap uriToBitmap(Uri uri) throws IOException {
        ContentResolver resolver = getContentResolver();
        InputStream input = resolver.openInputStream(uri);
        Bitmap bmp = BitmapFactory.decodeStream(input);
        if (input != null) input.close();
        return bmp;
    }

    // Resize helper (to avoid timeouts)
    private Bitmap resizeBitmapIfNeeded(Bitmap bmp, int maxDim) {
        if (bmp == null) return null;
        int w = bmp.getWidth(), h = bmp.getHeight();
        int max = Math.max(w, h);
        if (max <= maxDim) return bmp;
        float ratio = maxDim / (float) max;
        return Bitmap.createScaledBitmap(bmp, Math.round(w*ratio), Math.round(h*ratio), true);
    }



    private void analyzeMealWithAI(Bitmap bitmap) {
        progressAi.setVisibility(View.VISIBLE);

        String prompt = "נתח את התמונה והחזר אך ורק JSON תקין, ללא טקסט נוסף או הסברים.\n" +
                "JSON חייב להיות במבנה הבא:\n" +
                "{\n" +
                "  \"components\": [\n" +
                "    {\n" +
                "      \"name\": \"string\",\n" +
                "      \"weight\": number,\n" +
                "      \"protein\": number,\n" +
                "      \"calories\": number\n" +
                "    }\n" +
                "  ],\n" +
                "  \"totalProtein\": number,\n" +
                "  \"totalCalories\": number\n" +
                "}\n" +
                "הערות:\n" +
                "- weight = משקל מוערך של כל רכיב בתמונה (בגרם)\n" +
                "- protein = חלבון כולל של הרכיב לפי המשקל\n" +
                "- calories = קלוריות כוללות של הרכיב לפי המשקל\n" +
                "- totalProtein = סכום החלבון מכל הרכיבים\n" +
                "- totalCalories = סכום הקלוריות מכל הרכיבים\n" +
                "אל תשתמש בערכים ל-100 גרם, אל תוסיף טקסט או הסברים נוספים.";

        GeminiManager.getInstance().sendMessageWithPhoto(prompt, bitmap, new GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "AI response: " + response);
                runOnUiThread(() -> {
                    progressAi.setVisibility(View.GONE);
                    try {
                        String cleanJson = response
                                .replace("```json", "")
                                .replace("```", "")
                                .trim();

                        List<MealComponent> comps = parseMealJson(cleanJson);
                        if (comps.isEmpty()) {
                            Toast.makeText(MainDashboardActivity.this,
                                    "AI לא זיהה רכיבים — צלם מחדש או כתוב ידנית.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        showComponentsDialog(comps);
                    } catch (Exception e) {
                        Log.e(TAG, "parse error", e);
                        Toast.makeText(MainDashboardActivity.this, "שגיאה בניתוח הנתונים", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "AI error: " + error);
                runOnUiThread(() -> {
                    progressAi.setVisibility(View.GONE);
                    Toast.makeText(MainDashboardActivity.this, "שגיאת AI: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    private void showComponentsDialog(List<MealComponent> components) {
        if (components == null || components.isEmpty()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ערוך רכיבים ומשקל (גרמים)");

        ScrollView scroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24,24,24,24);
        scroll.addView(container);

        final TextView tvTotals = new TextView(this);
        tvTotals.setTextSize(16f);
        tvTotals.setPadding(8,16,8,8);

        final List<TextView> perCompTv = new ArrayList<>();

        // נשמור את הערכים המקוריים לכל רכיב
        final List<Float> originalWeights = new ArrayList<>();
        final List<Float> originalProteins = new ArrayList<>();
        final List<Float> originalCalories = new ArrayList<>();
        for (MealComponent comp : components) {
            originalWeights.add(comp.getWeight());
            originalProteins.add(comp.getProtein());
            originalCalories.add(comp.getCalories());
        }

        for (int i = 0; i < components.size(); i++) {
            MealComponent comp = components.get(i);

            TextView tvName = new TextView(this);
            tvName.setText(comp.getName());
            tvName.setTextSize(15f);
            container.addView(tvName);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 10, 0, 20);

            EditText etWeight = new EditText(this);
            etWeight.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            etWeight.setText(String.valueOf((int) comp.getWeight()));
            etWeight.setEms(5);
            row.addView(etWeight);

            TextView tvInfo = new TextView(this);
            tvInfo.setPadding(24,0,0,0);
            tvInfo.setTextSize(14f);
            row.addView(tvInfo);

            tvInfo.setText(String.format(Locale.getDefault(),
                    "חלבון: %.1fg | קלוריות: %.0f kcal",
                    comp.getProtein(), comp.getCalories()));
            perCompTv.add(tvInfo);

            final int idx = i;

            etWeight.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    float newWeight = components.get(idx).getWeight();
                    try {
                        String txt = s.toString().trim();
                        if (!txt.isEmpty()) newWeight = Float.parseFloat(txt);
                    } catch (Exception ignored) {}

                    // עדכון משקל
                    components.get(idx).setWeight(newWeight);

                    // חישוב חלבון וקלוריות בהתאם לפרופורציה
                    float origW = originalWeights.get(idx);
                    float origP = originalProteins.get(idx);
                    float origC = originalCalories.get(idx);

                    components.get(idx).setProtein(origP / origW * newWeight);
                    components.get(idx).setCalories(origC / origW * newWeight);

                    // עדכון התצוגה של הרכיב
                    MealComponent c = components.get(idx);
                    perCompTv.get(idx).setText(String.format(Locale.getDefault(),
                            "חלבון: %.1fg | קלוריות: %.0f kcal",
                            c.getProtein(), c.getCalories()));

                    // עדכון טוטל
                    double totalP = 0, totalC = 0;
                    for (MealComponent cc : components) {
                        totalP += cc.getProtein();
                        totalC += cc.getCalories();
                    }
                    tvTotals.setText(String.format(Locale.getDefault(),
                            "סה״כ: חלבון %.1f גרם | קלוריות %.0f kcal",
                            totalP, totalC));
                }
            });

            container.addView(row);
        }

        // טוטל התחלי
        double initP = 0, initC = 0;
        for (MealComponent c : components) {
            initP += c.getProtein();
            initC += c.getCalories();
        }
        tvTotals.setText(String.format(Locale.getDefault(),
                "סה״כ: חלבון %.1f גרם | קלוריות %.0f kcal",
                initP, initC));
        container.addView(tvTotals);

        builder.setView(scroll);

        builder.setPositiveButton("שמור והתעדכן ביומי", (dialog, which) -> {
            double totalP = 0, totalC = 0;
            for (MealComponent c : components) {
                totalP += c.getProtein();
                totalC += c.getCalories();
            }
            saveMealAndUpdateDaily((int)Math.round(totalP), (int)Math.round(totalC),components);
        });

        builder.setNegativeButton("בטל", null);
        builder.show();
    }


    // שמירה ל-Firestore ועדכון ה-realtime של currentProtein/currentCalories
    private void saveMealAndUpdateDaily(int protein, int calories,List<MealComponent> components) {
        if (uid == null || firestoreDb == null) {
            Toast.makeText(this, "משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            return;
        }
        if (imageBitmap == null && imageUri == null) {
            Toast.makeText(this, "אין תמונה לשמור", Toast.LENGTH_SHORT).show();
            return;
        }

        // encode image to base64 (keeps it in Firestore doc) — אפשר לשמור ב־Storage במקום
        String base64 = null;
        try {
            Bitmap bmp = imageBitmap;
            if (bmp == null && imageUri != null) bmp = uriToBitmap(imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) {
            Log.w(TAG, "image encode failed", e);
        }

        Map<String, Object> meal = new HashMap<>();
        if (base64 != null) meal.put("image", base64);
        else if (imageUri != null) meal.put("imageUri", imageUri.toString());
        String today = LocalDate.now().toString(); // yyyy-MM-dd
        meal.put("date", today);
        meal.put("protein", protein);
        meal.put("calories", calories);
        meal.put("timestamp", System.currentTimeMillis());
// components -> Map<String, Double>
        Map<String, Double> componentsMap = new HashMap<>();
        for (MealComponent c : components) {
            componentsMap.put(c.getName(), (double) c.getWeight());
        }

        meal.put("components", componentsMap);

        firestoreDb.collection("Users").document(uid).collection("Meals")
                .add(meal)
                .addOnSuccessListener(docRef -> {
                    // update realtime counters
                    int p = protein;
                    int c = calories;
                    currentProtein += p;
                    currentCalories += c;
                    Map<String, Object> update = new HashMap<>();
                    update.put("currentProtein", currentProtein);
                    update.put("currentCalories", currentCalories);
                    realtimeDbRef.updateChildren(update)
                            .addOnSuccessListener(unused -> {
                                updateDashboard();
                                checkAndUpdateStreak(); // <--- הוספנו את זה כאן!
                            })
                            .addOnFailureListener(ex -> Log.w(TAG, "realtime update fail", ex));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "save meal failed", e);
                    Toast.makeText(this, "שגיאה בשמירת הארוחה", Toast.LENGTH_SHORT).show();
                });
    }

    // model for component with nutrient rates (per gram)



    private List<MealComponent> parseMealJson(String json) throws Exception {
        List<MealComponent> list = new ArrayList<>();

        JSONObject obj = new JSONObject(json);
        JSONArray arr = obj.getJSONArray("components");

        for (int i = 0; i < arr.length(); i++) {
            JSONObject c = arr.getJSONObject(i);

            String name = c.optString("name", "");
            float weight = (float) c.optDouble("weight", 0);
            float protein = (float) c.optDouble("protein", 0);
            float calories = (float) c.optDouble("calories", 0);

            list.add(new MealComponent(name, weight, protein, calories));
        }

        return list;
    }
    // 1. פונקציה שמציגה את החלונית להוספה ידנית

    // 2. פונקציה לחישוב הרצף
    private void checkAndUpdateStreak() {
        if (uid == null) return;
        DocumentReference userRef = FirebaseFirestore.getInstance().collection("Users").document(uid);

        userRef.get().addOnSuccessListener(doc -> {
            long currentStreak = doc.contains("currentStreak") ? doc.getLong("currentStreak") : 0;
            String lastLogDate = doc.contains("lastLogDate") ? doc.getString("lastLogDate") : "";

            // שימוש בתאריכים פשוטים במקום חישובי מילישניות מסובכים
            String today = LocalDate.now().toString();
            String yesterday = LocalDate.now().minusDays(1).toString();

            if (!lastLogDate.equals(today)) {
                if (lastLogDate.equals(yesterday)) {
                    currentStreak++; // הזין אתמול, הרצף גדל!
                } else {
                    currentStreak = 1; // פספס יום או פעם ראשונה, מתחילים מ-1
                }

                // שומרים בשרת שמעודכן להיום
                Map<String, Object> updates = new HashMap<>();
                updates.put("currentStreak", currentStreak);
                updates.put("lastLogDate", today);
                userRef.update(updates);
            }

            // עדכון המסך בסוף התהליך
            if (tvStreak != null) {
                tvStreak.setText("🔥 " + currentStreak + " ימים");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "שגיאה בטעינת Streak", e));
    }
    }