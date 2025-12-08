package com.ori.proteinapplication;


import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
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
    private Button btnUploadMeal, btnViewMeals, btnEditInfo;

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

    // מאגר ערכי תזונה נפוצים (per 100g) — אפשר להרחיב
    private final Map<String, float[]> nutrientPer100gMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dashboard);

        // findViews
        imgMealPreview = findViewById(R.id.imgMeal);
        tvProteinProgress = findViewById(R.id.tvProteinProgress);
        tvCaloriesProgress = findViewById(R.id.tvCaloriesProgress);
        progressProteinCircular = findViewById(R.id.progressProteinCircular);
        progressCaloriesCircular = findViewById(R.id.progressCaloriesCircular);
        progressAi = findViewById(R.id.progressAi);
        btnUploadMeal = findViewById(R.id.btnUploadMeal);
        btnViewMeals = findViewById(R.id.btnViewMeals);
        btnEditInfo = findViewById(R.id.btnEditInfo);

        // firebase
        uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        firestoreDb = FirebaseFirestore.getInstance();
        realtimeDbRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        // initial nutrient DB (per 100g). Format: {name -> [proteinPer100g, caloriesPer100g]}
        nutrientPer100gMap.put("חזה עוף", new float[]{31f, 165f});
        nutrientPer100gMap.put("עוף", new float[]{27f, 165f});
        nutrientPer100gMap.put("בשר בקר", new float[]{26f, 250f});
        nutrientPer100gMap.put("ביצת עוף", new float[]{13f, 155f}); // per 100g (approx)
        nutrientPer100gMap.put("אורז", new float[]{2.7f, 130f});
        nutrientPer100gMap.put("אורז לבן", new float[]{2.6f, 130f});
        nutrientPer100gMap.put("ברוקולי", new float[]{2.8f, 34f});
        nutrientPer100gMap.put("גבינה צהובה", new float[]{25f, 350f});
        nutrientPer100gMap.put("חומוס", new float[]{19f, 364f});
        nutrientPer100gMap.put("בטטה", new float[]{1.6f, 86f});
        // הוסף עוד לפי הצורך...

        loadUserGoals();

        btnUploadMeal.setOnClickListener(v -> openFileChooser());
        btnViewMeals.setOnClickListener(v -> startActivity(new Intent(this, MealHistoryActivity.class)));
        btnEditInfo.setOnClickListener(v -> startActivity(new Intent(this, editInfo.class)));
    }

    private void loadUserGoals() {
        if (uid == null) return;
        realtimeDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    proteinGoal = snapshot.child("dailyProtein").getValue(Integer.class) != null ?
                            snapshot.child("dailyProtein").getValue(Integer.class) : 0;
                    caloriesGoal = snapshot.child("dailyCalories").getValue(Integer.class) != null ?
                            snapshot.child("dailyCalories").getValue(Integer.class) : 0;
                    currentProtein = snapshot.child("currentProtein").getValue(Integer.class) != null ?
                            snapshot.child("currentProtein").getValue(Integer.class) : 0;
                    currentCalories = snapshot.child("currentCalories").getValue(Integer.class) != null ?
                            snapshot.child("currentCalories").getValue(Integer.class) : 0;
                }
                updateDashboard();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { Log.w(TAG,"loadUserGoals cancelled", error.toException()); }
        });
    }

    private void updateDashboard() {
        tvProteinProgress.setText(currentProtein + " / " + proteinGoal + " גרם חלבון");
        tvCaloriesProgress.setText(currentCalories + " / " + caloriesGoal + " קלוריות");
        progressProteinCircular.setProgress(proteinGoal > 0 ? (int)((currentProtein/(float)proteinGoal)*100) : 0);
        progressCaloriesCircular.setProgress(caloriesGoal > 0 ? (int)((currentCalories/(float)caloriesGoal)*100) : 0);
    }

    // Open gallery
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
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

    // Receive selected image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            if (imageUri == null) {
                Toast.makeText(this, "לא נמצאה תמונה", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                imageBitmap = uriToBitmap(imageUri);
                imageBitmap = resizeBitmapIfNeeded(imageBitmap, 1024); // shrink to avoid timeout
            } catch (IOException e) {
                Log.e(TAG, "uriToBitmap failed", e);
                Toast.makeText(this, "שגיאה בקריאת תמונה", Toast.LENGTH_SHORT).show();
                return;
            }

            // תצוגה מקדימה
            imgMealPreview.setImageBitmap(imageBitmap);

            // שלח ל-AI
            progressAi.setVisibility(View.VISIBLE);

            // Prompt — בקש JSON ברור
            //String prompt = "נתח את התמונה והחזר אך ורק JSON במבנה הבא: {\"רכיבים\":[{\"שם\":\"טקסט\",\"גרם\":מספר,\"חלבון\":מספר,\"קלוריות\":מספר}], \"חלבון כולל\":מספר, \"קלוריות כולל\":מספר}. אל תוסיף שום טקסט חוץ מה-JSON. לכל רכיב תן הערכת גרם, חלבון וקלוריות לפי מה שאתה רואה בתמונה.";
            String prompt = "נתח את התמונה והחזר אך ורק JSON תקין, ללא טקסט נוסף או הסברים.\n" +
                    "JSON חייב להיות במבנה הבא:\n" +
                    "{\n" +
                    "  \"ingredients\": [\n" +
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



            // קריאה ל-GeminiManager
            GeminiManager.getInstance().sendMessageWithPhoto(prompt, imageBitmap, new GeminiCallback() {
                @Override
                public void onSuccess(String response) {
                    Log.d(TAG, "AI response: " + response);
                    runOnUiThread(() -> {
                        progressAi.setVisibility(View.GONE);
                        try {

                            List<MealComponent> comps = parseMealJson(response);
                            if (comps.isEmpty()) {
                                Toast.makeText(MainDashboardActivity.this,
                                        "AI לא זיהה רכיבים — צלם מחדש או כתוב ידנית.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            showComponentsDialog(comps);
                        } catch (Exception e) {
                            Log.e(TAG, "parse error", e);
                            Toast.makeText(MainDashboardActivity.this, "שגיאה בפרסינג ה-AI", Toast.LENGTH_LONG).show();
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
    }

    /**
     * דיאלוג עריכה: מציג כל רכיב, משקל (EditText), חלבון וקלוריות לכל רכיב,
     * מחשב טוטאל בזמן אמת כאשר המשתמש משנה משקל.
     */
    private void showComponentsDialog(List<MealComponent> components) {
        if (components == null || components.isEmpty()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ערוך רכיבים ומשקל (גרמים)");

        ScrollView scroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24,24,24,24);
        scroll.addView(container);

        // totals
        final TextView tvTotals = new TextView(this);
        tvTotals.setTextSize(16f);
        tvTotals.setPadding(8,16,8,8);

        final List<TextView> perCompTv = new ArrayList<>();

        // build rows
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

            // initial values
            tvInfo.setText(String.format(Locale.getDefault(),
                    "חלבון: %.1fg | קלוריות: %.0f kcal",
                    comp.getProtein(), comp.getCalories()));

            perCompTv.add(tvInfo);

            final int idx = i;

            etWeight.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {

                    float newW = comp.getWeight();
                    try {
                        String txt = s.toString().trim();
                        if (!txt.isEmpty()) newW = Float.parseFloat(txt);
                    } catch (Exception ignored) {}

                    // update the component weight
                    components.get(idx).setWeight(newW);

                    // refresh per-component text
                    MealComponent c = components.get(idx);
                    perCompTv.get(idx).setText(String.format(Locale.getDefault(),
                            "חלבון: %.1fg | קלוריות: %.0f kcal",
                            c.getProtein(), c.getCalories()));

                    // refresh totals
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

        // initial totals
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

            saveMealAndUpdateDaily(
                    (int) Math.round(totalP),
                    (int) Math.round(totalC)
            );
        });

        builder.setNegativeButton("בטל", null);
        builder.show();
    }

    // שמירה ל-Firestore ועדכון ה-realtime של currentProtein/currentCalories
    private void saveMealAndUpdateDaily(int protein, int calories) {
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
        meal.put("protein", protein);
        meal.put("calories", calories);
        meal.put("timestamp", System.currentTimeMillis());

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
                            .addOnSuccessListener(unused -> updateDashboard())
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

}
