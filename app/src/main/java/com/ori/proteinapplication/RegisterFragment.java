package com.ori.proteinapplication;



import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterFragment extends Fragment {

    private EditText etEmail, etPassword;
    private Button btnRegister;
    private TextView tvLoginLink;
    private FirebaseAuth mAuth;

    public RegisterFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnRegister = view.findViewById(R.id.btnRegister);
        tvLoginLink = view.findViewById(R.id.tvLoginLink);

        mAuth = FirebaseAuth.getInstance();

        // לחיצה על כפתור הרשמה
        btnRegister.setOnClickListener(v -> registerUser());

        // קישור ל-login fragment
        tvLoginLink.setOnClickListener(v -> {
            showLoginFragment();
        });

        return view;
    }

    public void showLoginFragment() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, new LoginFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getActivity(), "אנא מלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getActivity(), "הרשמה הצליחה!", Toast.LENGTH_SHORT).show();
                        // מעבר לדף עריכת פרטים
                        Intent intent = new Intent(getActivity(), EditInfoActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    } else {
                        Toast.makeText(getActivity(), "שגיאה: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}


