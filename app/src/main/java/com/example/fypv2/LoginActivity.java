package com.example.fypv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText etLoginEmail;
    TextInputEditText etLoginPassword;
    TextView tvRegisterHere;
    Button btnLogin;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etLoginEmail = findViewById(R.id.etLoginEmail);
        etLoginPassword = findViewById(R.id.etLoginPass);
        tvRegisterHere = findViewById(R.id.tvRegisterHere);
        btnLogin = findViewById(R.id.btnLogin);

        mAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(view -> {
            loginUser();
        });
        tvRegisterHere.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        String email = etLoginEmail.getText().toString();
        String password = etLoginPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            etLoginEmail.setError("Email cannot be empty");
            etLoginEmail.requestFocus();
        } else if (TextUtils.isEmpty(password)) {
            etLoginPassword.setError("Password cannot be empty");
            etLoginPassword.requestFocus();
        } else {
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        String currentUserId = mAuth.getCurrentUser().getUid();
                        DatabaseReference rootRef = FirebaseDatabase.getInstance("https://fypv2-23d59-default-rtdb.europe-west1.firebasedatabase.app").getReference();

                        // Add a flag to check if a valid user type has been found
                        final boolean[] validUserTypeFound = {false};

                        // Add a counter to keep track of onDataChange() calls
                        final int[] onDataChangeCounter = {0};

                        ValueEventListener userTypeListener = new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                onDataChangeCounter[0]++;
                                if (snapshot.exists()) {
                                    // This block of code will be executed if the user type is found
                                    String userType = snapshot.getRef().getParent().getKey();
                                    if (userType.equals("FighterUser")) {
                                        validUserTypeFound[0] = true;
                                        Toast.makeText(LoginActivity.this, "Fighter user logged in successfully", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    } else if (userType.equals("SafetyOrganisationUser")) {
                                        validUserTypeFound[0] = true;
                                        Toast.makeText(LoginActivity.this, "Safety organisation user logged in successfully", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(LoginActivity.this, SafetyOrganisationMainActivity.class));
                                        finish();
                                    }
                                }

                                // Only show "User type not found" message after both onDataChange() calls have been executed
                                if (!validUserTypeFound[0] && onDataChangeCounter[0] == 2) {
                                    mAuth.signOut();
                                    Toast.makeText(LoginActivity.this, "User type not found", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                mAuth.signOut();
                                Toast.makeText(LoginActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        };

                        rootRef.child("FighterUser").child(currentUserId).addListenerForSingleValueEvent(userTypeListener);
                        rootRef.child("SafetyOrganisationUser").child(currentUserId).addListenerForSingleValueEvent(userTypeListener);






                    } else {
                        Toast.makeText(LoginActivity.this, "Log in Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}