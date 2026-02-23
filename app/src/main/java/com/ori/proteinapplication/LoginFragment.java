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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

public class LoginFragment extends Fragment {

    private EditText etEmailLogin, etPasswordLogin;
    private Button btnLogin;
    private TextView tvRegisterLink;

    public LoginFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        etEmailLogin = view.findViewById(R.id.etEmailLogin);
        etPasswordLogin = view.findViewById(R.id.etPasswordLogin);
        btnLogin = view.findViewById(R.id.btnLogin);
        tvRegisterLink = view.findViewById(R.id.tvRegisterLink);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginFragment.this.loginUser();
            }
        });

        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AuthActivity) LoginFragment.this.getActivity()).showRegisterFragment();
            }
        });

        return view;
    }

    private void loginUser() {
        String email = etEmailLogin.getText().toString().trim();
        String password = etPasswordLogin.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(getActivity(), "אנא מלא את כל הפרטים", Toast.LENGTH_SHORT).show();
            return;
        }

        FBRef.mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginFragment.this.getActivity(), "התחברות הצליחה!", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(LoginFragment.this.getActivity(), MainDashboardActivity.class);
                            LoginFragment.this.startActivity(intent);
                            LoginFragment.this.getActivity().finish();

                        } else {
                            String error = "שגיאה לא ידועה";
                            if (task.getException() != null)
                                error = task.getException().getMessage();
                            Toast.makeText(LoginFragment.this.getActivity(), error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}

