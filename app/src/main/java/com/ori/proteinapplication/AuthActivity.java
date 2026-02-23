package com.ori.proteinapplication;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseUser;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        FirebaseUser user = FBRef.mAuth.getCurrentUser();

        // נטען Fragment רק ביצירה הראשונה של ה-Activity
        // (כדי למנוע טעינה כפולה במקרה של סיבוב מסך / שחזור מצב)
        if (savedInstanceState == null) {


            if (user != null) {
                // יש משתמש מחובר
                goToMainDashboard();

            } else {
                // אין משתמש מחובר
                showRegisterFragment();
            }
        }
    }

    private void goToMainDashboard() {
        Intent intent = new Intent(this, MainDashboardActivity.class);
        startActivity(intent);
        finish();
    }

    public void showRegisterFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new RegisterFragment())
                .commit();
    }
}
