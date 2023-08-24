package com.example.fypv2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.widget.Switch;

public class RegisterActivity extends AppCompatActivity {

    TextInputEditText etRegEmail;
    TextInputEditText etRegPassword;
    TextInputEditText etRegFirstName;
    TextInputEditText etRegSurname;
    TextView tvLoginHere;
    Button btnRegister;
    TextView userTypeTextView;

    Switch userTypeSwitch;

    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etRegEmail = findViewById(R.id.etRegEmail);
        etRegPassword = findViewById(R.id.etRegPass);
        etRegFirstName = findViewById(R.id.etRegFirstName);
        etRegSurname = findViewById(R.id.etRegSurname);
        // tvLoginHere = findViewById(R.id.tvLoginHere);
        btnRegister = findViewById(R.id.btnRegister);

        userTypeSwitch = findViewById(R.id.userTypeSwitch);

        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://fypv2-23d59-default-rtdb.europe-west1.firebasedatabase.app");
        mDatabase = database.getReference();

        btnRegister.setOnClickListener(view -> {
            createUser();
        });

        userTypeTextView = findViewById(R.id.userTypeTextView);
        userTypeSwitch = findViewById(R.id.userTypeSwitch);

        userTypeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                userTypeTextView.setText("Safety Organisation User");
            } else {
                userTypeTextView.setText("Fighter User");
            }
        });


    }

    private void createUser() {
        String email = etRegEmail.getText().toString();
        String password = etRegPassword.getText().toString();
        String firstName = etRegFirstName.getText().toString();
        String surname = etRegSurname.getText().toString();

        String EMAIL_PATTERN = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);
        Matcher emailMatcher = emailPattern.matcher(email);

        String PASSWORD_PATTERN = "^(?=.*[!@#$%^&*(),.?\":{}|<>])(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        Pattern passwordPattern = Pattern.compile(PASSWORD_PATTERN);
        Matcher passwordMatcher = passwordPattern.matcher(password);

        if (TextUtils.isEmpty(email)) {
            etRegEmail.setError("Email cannot be empty");
            etRegEmail.requestFocus();
        } else if (!emailMatcher.matches()) {
            etRegEmail.setError("Please enter a valid email. E.g., example@gmail.com. Must contain @");
            etRegEmail.requestFocus();
        } else if (TextUtils.isEmpty(password)) {
            etRegPassword.setError("Password cannot be empty");
            etRegPassword.requestFocus();
        } else if (!passwordMatcher.matches()) {
            etRegPassword.setError("Password must have 8+ characters, at least 1 digit, and 2 special characters");
            etRegPassword.requestFocus();
        } else {
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();
                        String userId = mAuth.getCurrentUser().getUid();
                        String userType = userTypeSwitch.isChecked() ? "SafetyOrganisationUser" : "FighterUser";
                        DatabaseReference currentUserDbRef = mDatabase.child(userType).child(userId);


                        // Creating a HashMap to store user data
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("firstName", firstName);
                        userData.put("surname", surname);
                        userData.put("email", email);

                        Log.d("RegisterActivity", "Attempting to save user data");

                        // Saving user data to Realtime Database
                        currentUserDbRef.setValue(userData).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d("RegisterActivity", "User data saved successfully");
                                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));

                                } else {
                                    Log.e("RegisterActivity", "Error saving user data: " + task.getException().getMessage());
                                    Toast.makeText(RegisterActivity.this, "Error saving user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Log.e("RegisterActivity", "Registration Error: " + task.getException().getMessage());
                        Toast.makeText(RegisterActivity.this, "Registration Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }
    }
}

