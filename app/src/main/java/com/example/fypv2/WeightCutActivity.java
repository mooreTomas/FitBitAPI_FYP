package com.example.fypv2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.text.ParseException;
import java.util.function.Consumer;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.openid.appauth.AuthState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class WeightCutActivity extends AppCompatActivity {

    Button btnUpdateWeightCut;

    FirebaseAuth mAuth;
    FirebaseDatabase mDatabase;

    AuthState mAuthState;
    OkHttpClient httpClient;

    DatabaseReference weightCutRef;
    String currentDate;

    String firstName;
    String lastName;
    String weightCutId;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_cut);

        btnUpdateWeightCut = findViewById(R.id.btnUpdateWeightCut);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://fypv2-23d59-default-rtdb.europe-west1.firebasedatabase.app/");


        // Retrieve the AuthState object from the intent
        String authStateJson = getIntent().getStringExtra("authState");
        if (authStateJson != null) {
            try {
                mAuthState = AuthState.jsonDeserialize(authStateJson);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        btnUpdateWeightCut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateWeightCut();
            }
        });

        httpClient = new OkHttpClient();

        firstName = getIntent().getStringExtra("firstName");
        lastName = getIntent().getStringExtra("surname");


    }

    private void updateWeightCut() {
        if (mAuthState != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                startActivity(new Intent(WeightCutActivity.this, LoginActivity.class));
                return;
            }

            // Call createNewWeightCut() before using weightCutRef
            createNewWeightCut(() -> {
                // Now you can safely use weightCutRef
                weightCutRef.child(currentDate).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Record for the current date already exists
                            Toast.makeText(WeightCutActivity.this, "You can only update this record once per day", Toast.LENGTH_SHORT).show();
                        } else {
                            // Call your API here to get weight and body fat values
                            makeRequest(mAuthState, weightCutRef, currentDate, firstName, lastName);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(WeightCutActivity.this, "Error checking record: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else {
            Toast.makeText(WeightCutActivity.this, "Error: AuthState is not available", Toast.LENGTH_SHORT).show();
        }
    }





    private void makeRequest(AuthState authState, DatabaseReference weightCutRef, String currentDate, String firstName, String lastName) {
        String accessToken = authState.getAccessToken();
        String weightUrl = "https://api.fitbit.com/1/user/-/body/log/weight/date/today.json";
        String fatUrl = "https://api.fitbit.com/1/user/-/body/fat/date/today/1d.json";

        DatabaseReference currentDateRef = weightCutRef.child(currentDate);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not authenticated, redirect to LoginActivity or handle the case as you wish
            startActivity(new Intent(WeightCutActivity.this, LoginActivity.class));
            return;
        }
        String customerEmail = currentUser.getEmail();

        Map<String, Object> mergedData = new HashMap<>();

        Consumer<Map<String, Object>> onApiDataReceived = data -> {
            data.put("firstName", firstName);
            data.put("lastName", lastName);
            mergedData.putAll(data);
            if (mergedData.containsKey("body-fat") && mergedData.containsKey("weight")) {
                currentDateRef.updateChildren(mergedData).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(WeightCutActivity.this, "Weight and body fat data updated", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(WeightCutActivity.this, "Error updating data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        };
        Log.d("WeightCutActivity", "firstName: " + firstName + ", lastName: " + lastName);

        makeApiRequest(fatUrl, accessToken, "body-fat", "dateTime", customerEmail, currentDateRef, onApiDataReceived);
        makeApiRequest(weightUrl, accessToken, "weight", "date", customerEmail, currentDateRef, onApiDataReceived);
    }

    private void makeApiRequest(String url, String accessToken, String dataType, String dateKey, String customerEmail, DatabaseReference currentDateRef, Consumer<Map<String, Object>> onSuccess) {
        // Update Firebase database with customer data
        FirebaseDatabase firebaseDatabase = mDatabase;
        DatabaseReference dbRef = firebaseDatabase.getReference("FighterUserWeightCut");

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(WeightCutActivity.this, "Request failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("MainActivity", "dataType: " + dataType + ", JSON Response: " + responseBody);

                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray dataArray = jsonResponse.getJSONArray(dataType);
                        if (dataArray.length() > 0) {
                            JSONObject latestData = dataArray.getJSONObject(dataArray.length() - 1);

                            double dataValue;
                            if (dataType.equals("body-fat")) {
                                dataValue = latestData.getDouble("value");
                            } else {
                                dataValue = latestData.getDouble(dataType);
                            }

                            // Update Firebase database with fighter data
                            String currentDate = latestData.getString(dateKey);
                            Map<String, Object> data = new HashMap<>();
                            data.put("email", customerEmail);
                            data.put("date", currentDate);
                            data.put(dataType, dataValue);

                            onSuccess.accept(data);
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(WeightCutActivity.this, dataType + " data is not available", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("MainActivity", "JSON Error: " + e.getMessage()); // Log the JSON error message
                        runOnUiThread(() -> {
                            Toast.makeText(WeightCutActivity.this, "JSON error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    String responseBody = response.body().string();
                    Log.e("MainActivity", "Request failed with code " + response.code() + ", response body: " + responseBody);
                    runOnUiThread(() -> {
                        Toast.makeText(WeightCutActivity.this, "Request failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    Map<String, Object> mergedData = new HashMap<>();

    private void createNewWeightCut(Runnable callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(WeightCutActivity.this, LoginActivity.class));
            return;
        }

        String userId = currentUser.getUid();

        if (userId == null) {
            Toast.makeText(WeightCutActivity.this, "Error: User ID is not available", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference weightCutCounterRef = mDatabase.getReference().child("FighterUserWeightCutCounter").child(userId);

        weightCutCounterRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final int[] weightCutCounter = {1};
                if (dataSnapshot.exists()) {
                    weightCutCounter[0] = dataSnapshot.getValue(Integer.class);
                }

                final String[] weightCutId = {String.format("%s_%s_Weight_Cut_%d", firstName, lastName, weightCutCounter[0]).replaceAll(" ", "_")};

                // Check if a new weight cut should be started
                DatabaseReference lastWeightCutRef = mDatabase.getReference().child("FighterUserWeightCut").child(userId).child(weightCutId[0]);
                lastWeightCutRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean shouldStartNewWeightCut = false;

                        if (dataSnapshot.exists()) {
                            for (DataSnapshot data : dataSnapshot.getChildren()) {
                                String lastDate = data.getKey();
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                try {
                                    Date lastDateObj = sdf.parse(lastDate);
                                    long daysDifference = (System.currentTimeMillis() - lastDateObj.getTime()) / (1000 * 60 * 60 * 24);

                                    if (daysDifference > 7) {
                                        weightCutCounter[0]++; // Increment the counter
                                        shouldStartNewWeightCut = true;
                                        weightCutCounterRef.setValue(weightCutCounter[0]);
                                        weightCutId[0] = String.format("%s_%s_Weight_Cut_%d", firstName, lastName, weightCutCounter[0]).replaceAll(" ", "_");
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            shouldStartNewWeightCut = true;
                            weightCutCounterRef.setValue(weightCutCounter[0]);
                        }

                        if (shouldStartNewWeightCut) {
                            // Start the new weight cut process and initialize weightCutRef
                            startWeightCutProcess(userId, weightCutId[0]);
                        } else {
                            // Continue with the current weight cut
                            weightCutRef = mDatabase.getReference().child("FighterUserWeightCut").child(userId).child(weightCutId[0]);
                        }

                        // Get the current date in the format yyyy-MM-dd
                        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());


                        // Execute the callback
                        if (callback != null) {
                            callback.run();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(WeightCutActivity.this, "Error checking last weight cut date: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(WeightCutActivity.this, "Error retrieving weight cut counter: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Start the new weight cut process
    private void startWeightCutProcess(String userId, String weightCutId) {
        this.weightCutId = weightCutId;
        weightCutRef = mDatabase.getReference().child("FighterUserWeightCut").child(userId).child(weightCutId);


    }



}




