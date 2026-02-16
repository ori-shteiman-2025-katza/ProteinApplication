package com.ori.proteinapplication;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class EditInfoActivity extends AppCompatActivity {

    private EditText etWeight, etHeight, etAge, etWorkoutsPerWeek;
    private Spinner spGender, spIntensity;
    private Button btnSave;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private String uid;
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_info);

        // אתחול רכיבים
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        etAge = findViewById(R.id.etAge);
        etWorkoutsPerWeek = findViewById(R.id.etWorkoutsPerWeek);
        spGender = findViewById(R.id.spGender);
        spIntensity = findViewById(R.id.spIntensity);
        btnSave = findViewById(R.id.btnSave);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        uid = mAuth.getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        // 1. הגדרת הספינרים קודם כל
        setupSpinners();

        // 2. טעינת הנתונים הקיימים מהשרת כדי שהדף לא יהיה ריק
        loadUserData();

        // 3. הגדרת הניווט
        BottomNavigationHelper.setupBottomNavigation(
                this,
                bottomNavigationView,
                R.id.nav_profile
        );

        btnSave.setOnClickListener(v -> saveUserInfo());
    }

    private void loadUserData() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        // שימוש ב-Double במקום פרימיטיבי כדי למנוע קריסה אם הערך null
                        Double w = snapshot.child("weight").getValue(Double.class);
                        Double h = snapshot.child("height").getValue(Double.class);
                        Integer a = snapshot.child("age").getValue(Integer.class);
                        Integer wpw = snapshot.child("workoutsPerWeek").getValue(Integer.class);

                        if (w != null) etWeight.setText(String.valueOf(w));
                        if (h != null) etHeight.setText(String.valueOf(h));
                        if (a != null) etAge.setText(String.valueOf(a));
                        if (wpw != null) etWorkoutsPerWeek.setText(String.valueOf(wpw));

                        // טיפול בטוח בספינרים
                        String gender = snapshot.child("gender").getValue(String.class);
                        if (gender != null && spGender.getAdapter() != null) {
                            for (int i = 0; i < spGender.getAdapter().getCount(); i++) {
                                if (spGender.getAdapter().getItem(i).toString().equals(gender)) {
                                    spGender.setSelection(i);
                                    break;
                                }
                            }
                        }

                        String intensity = snapshot.child("intensity").getValue(String.class);
                        if (intensity != null && spIntensity.getAdapter() != null) {
                            for (int i = 0; i < spIntensity.getAdapter().getCount(); i++) {
                                if (spIntensity.getAdapter().getItem(i).toString().equals(intensity)) {
                                    spIntensity.setSelection(i);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("EditInfo", "Error parsing data", e);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupSpinners() {
        // מגדר
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"זכר", "נקבה"});
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(genderAdapter);

        // אינטנסיביות
        ArrayAdapter<CharSequence> intensityAdapter = ArrayAdapter.createFromResource(
                this, R.array.intensity_levels, android.R.layout.simple_spinner_item);
        intensityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spIntensity.setAdapter(intensityAdapter);
    }

    private void saveUserInfo() {
        try {
            if (etWeight.getText().toString().isEmpty() ||
                    etHeight.getText().toString().isEmpty() ||
                    etAge.getText().toString().isEmpty() ||
                    etWorkoutsPerWeek.getText().toString().isEmpty()) {
                Toast.makeText(this, "בדוק שמילאת את כל הפרטים", Toast.LENGTH_SHORT).show();
                return;
            }

            double weight = Double.parseDouble(etWeight.getText().toString());
            double height = Double.parseDouble(etHeight.getText().toString());
            int age = Integer.parseInt(etAge.getText().toString());
            int workoutsPerWeek = Integer.parseInt(etWorkoutsPerWeek.getText().toString());
            String gender = spGender.getSelectedItem().toString();
            String intensity = spIntensity.getSelectedItem().toString();

            // שליפת פקטורים מה-Strings.xml
            String[] intensityLevels = getResources().getStringArray(R.array.intensity_levels);
            String[] activityFactorsStr = getResources().getStringArray(R.array.activity_factors);
            String[] proteinFactorsStr = getResources().getStringArray(R.array.protein_factors);

            int index = 0;
            for (int i = 0; i < intensityLevels.length; i++) {
                if (intensityLevels[i].equals(intensity)) index = i;
            }

            double activityFactor = Double.parseDouble(activityFactorsStr[index]);
            double proteinFactor = Double.parseDouble(proteinFactorsStr[index]);

            // חישוב BMR
            double bmr;
            if (gender.equals("זכר")) {
                bmr = 88.36 + (13.4 * weight) + (4.8 * height) - (5.7 * age);
            } else {
                bmr = 447.6 + (9.2 * weight) + (3.1 * height) - (4.3 * age);
            }

            double dailyCalories = Math.round(bmr * activityFactor);
            double dailyProtein = Math.round(weight * proteinFactor);

            // יצירת אובייקט UserInfo (current=0)
            UserInfo user = new UserInfo(weight, height, age, gender, workoutsPerWeek, intensity,
                    dailyCalories, dailyProtein, 0, 0);

            usersRef.child(uid).setValue(user)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "הנתונים נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();
                        // חזרה לדף הראשי אחרי שמירה
                        Intent intent = new Intent(EditInfoActivity.this, MainDashboardActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );

        } catch (NumberFormatException e) {
            Toast.makeText(this, "בדוק שמילאת את כל הפרטים בצורה תקינה", Toast.LENGTH_SHORT).show();
        }
    }
}

