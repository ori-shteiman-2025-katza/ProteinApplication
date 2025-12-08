package com.ori.proteinapplication;


import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class editInfo extends AppCompatActivity {

    private EditText etWeight, etHeight, etAge, etWorkoutsPerWeek;
    private Spinner spGender, spIntensity;
    private Button btnSave;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_info);

        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        etAge = findViewById(R.id.etAge);
        etWorkoutsPerWeek = findViewById(R.id.etWorkoutsPerWeek);
        spGender = findViewById(R.id.spGender);
        spIntensity = findViewById(R.id.spIntensity);
        btnSave = findViewById(R.id.btnSave);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "משתמש לא מחובר. יש להירשם קודם", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        uid = mAuth.getCurrentUser().getUid();

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"זכר", "נקבה"});
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(genderAdapter);

        ArrayAdapter<CharSequence> intensityAdapter = ArrayAdapter.createFromResource(
                this, R.array.intensity_levels, android.R.layout.simple_spinner_item);
        intensityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spIntensity.setAdapter(intensityAdapter);

        btnSave.setOnClickListener(v -> saveUserInfo());
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

            String[] intensityLevels = getResources().getStringArray(R.array.intensity_levels);
            String[] activityFactorsStr = getResources().getStringArray(R.array.activity_factors);
            String[] proteinFactorsStr = getResources().getStringArray(R.array.protein_factors);

            int index = 0;
            for (int i = 0; i < intensityLevels.length; i++)
                if (intensityLevels[i].equals(intensity)) index = i;

            double activityFactor = Double.parseDouble(activityFactorsStr[index]);
            double proteinFactor = Double.parseDouble(proteinFactorsStr[index]);

            double bmr;
            if (gender.equals("זכר")) {
                bmr = 88.36 + (13.4 * weight) + (4.8 * height) - (5.7 * age);
            } else {
                bmr = 447.6 + (9.2 * weight) + (3.1 * height) - (4.3 * age);
            }

            double dailyCalories = Math.round(bmr * activityFactor);
            double dailyProtein = Math.round(weight * proteinFactor);

            // יצירת אובייקט UserInfo
            UserInfo user = new UserInfo(weight, height, age, gender, workoutsPerWeek, intensity,
                    dailyCalories, dailyProtein, 0, 0); // currentProtein/currentCalories = 0

            usersRef.child(uid).setValue(user)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "הנתונים נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();

                        // מעבר אוטומטי ל-MainDashboardActivity
                        Intent intent = new Intent(editInfo.this, MainDashboardActivity.class);
                        startActivity(intent);
                        finish(); // סוגר את editInfo כדי שלא יחזור אליו בלחיצה על חזור
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );

        } catch (NumberFormatException e) {
            Toast.makeText(this, "בדוק שמילאת את כל הפרטים בצורה תקינה", Toast.LENGTH_SHORT).show();
        }
    }
}
