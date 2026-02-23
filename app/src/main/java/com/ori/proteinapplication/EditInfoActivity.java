package com.ori.proteinapplication;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class EditInfoActivity extends AppCompatActivity {

    private EditText etWeight, etHeight, etAge, etWorkoutsPerWeek;
    private Spinner spGender, spIntensity;
    private Button btnSave;
    private DatabaseReference userRef;
    private String uid;
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_info);

        if (FBRef.mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        // אתחול רכיבים
        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        etAge = findViewById(R.id.etAge);
        etWorkoutsPerWeek = findViewById(R.id.etWorkoutsPerWeek);
        spGender = findViewById(R.id.spGender);
        spIntensity = findViewById(R.id.spIntensity);
        btnSave = findViewById(R.id.btnSave);
        bottomNavigationView = findViewById(R.id.bottom_navigation);



        uid = FBRef.mAuth.getCurrentUser().getUid();
        userRef = FBRef.refUsers.child(uid);

        // הגדרת הספינרים
        setupSpinners();

        // טעינת הנתונים הקיימים
        loadUserData();

        // הגדרת הניווט
        BottomNavigationHelper.setupBottomNavigation(
                this,
                bottomNavigationView,
                R.id.nav_profile
        );

        // מאזינים
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInfo();
            }
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


    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null)
            return;

        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        int position = adapter.getPosition(value);
        if (position >= 0)
            spinner.setSelection(position);
    }


    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // שימוש ב-Double במקום פרימיטיבי כדי למנוע קריסה אם הערך null
                    Double w = snapshot.child("weight").getValue(Double.class);
                    Double h = snapshot.child("height").getValue(Double.class);
                    Integer a = snapshot.child("age").getValue(Integer.class);
                    Integer wpw = snapshot.child("workoutsPerWeek").getValue(Integer.class);

                    if (w != null) etWeight.setText(String.valueOf(w));
                    if (h != null) etHeight.setText(String.valueOf(h));
                    if (a != null) etAge.setText(String.valueOf(a));
                    if (wpw != null) etWorkoutsPerWeek.setText(String.valueOf(wpw));

                    setSpinnerSelection(spGender,
                            snapshot.child("gender").getValue(String.class));

                    setSpinnerSelection(spIntensity,
                            snapshot.child("intensity").getValue(String.class));

                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
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
                if (intensityLevels[i].equals(intensity))
                    index = i;
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

            userRef.setValue(user)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(EditInfoActivity.this, "הנתונים נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();

                            // חזרה לדף הראשי אחרי שמירה
                            Intent intent = new Intent(EditInfoActivity.this, MainDashboardActivity.class);
                            EditInfoActivity.this.startActivity(intent);
                            EditInfoActivity.this.finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                                              @Override
                                              public void onFailure(@NonNull Exception e) {
                                                  Toast.makeText(EditInfoActivity.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                              }
                                          }
                    );

        } catch (NumberFormatException e) {
            Toast.makeText(this, "בדוק שמילאת את כל הפרטים בצורה תקינה", Toast.LENGTH_SHORT).show();
        }
    }
}

