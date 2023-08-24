package com.example.fypv2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Intent intent = getIntent();
        double weightValue = intent.getDoubleExtra("weight", 0.0);
        double fatValue = intent.getDoubleExtra("fat", 0.0);

        TextView textViewWeight = findViewById(R.id.textView);
        textViewWeight.setText("Latest Weight: " + weightValue + " kg");

        TextView textViewFat = findViewById(R.id.textViewFat);
        textViewFat.setText("Latest Body Fat: " + fatValue + " %");
    }
}


