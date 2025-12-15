package com.ori.proteinapplication;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class LoginFragment extends Fragment {

    private EditText etEmailLogin, etPasswordLogin;
    private Button btnLogin;
    private TextView tvRegisterLink;
    private FirebaseAuth mAuth;

    public LoginFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        etEmailLogin = view.findViewById(R.id.etEmailLogin);
        etPasswordLogin = view.findViewById(R.id.etPasswordLogin);
        btnLogin = view.findViewById(R.id.btnLogin);
        tvRegisterLink = view.findViewById(R.id.tvRegisterLink);

        mAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> loginUser());
        tvRegisterLink.setOnClickListener(v -> ((AuthActivity)getActivity()).showRegisterFragment());

        return view;
    }

    private void loginUser() {
        String email = etEmailLogin.getText().toString().trim();
        String password = etPasswordLogin.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(getActivity(), "אנא מלא את כל הפרטים", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        Toast.makeText(getActivity(), "התחברות הצליחה!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getActivity(), MainDashboardActivity.class);
                        startActivity(intent);
                        getActivity().finish();

                    } else {
                        Toast.makeText(getActivity(), "שגיאה: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d("RINAT",task.getException().getMessage());
                    }
                });
    }
}

