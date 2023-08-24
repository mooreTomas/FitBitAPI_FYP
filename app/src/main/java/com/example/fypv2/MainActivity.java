package com.example.fypv2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import android.util.Log;


// imports to make requests
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

// for requests
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;

import okhttp3.OkHttpClient;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "23QZDN";
    private static final String REDIRECT_URI = "com.example.fypv2://oauth2redirect";
    private static final String AUTHORIZATION_ENDPOINT = "https://www.fitbit.com/oauth2/authorize";
    private static final String TOKEN_ENDPOINT = "https://api.fitbit.com/oauth2/token";
    private static final int AUTH_REQUEST_CODE = 1001;











    private JSONObject userProfile;


    private AuthorizationService authService;
    private OkHttpClient httpClient;

    Button btnLogOut;
    Button btnAuthenticate;
    FirebaseAuth mAuth;


    Button btnProfile;
    Button btnInitiateWeightCut;

    FirebaseDatabase database = FirebaseDatabase.getInstance("https://fypv2-23d59-default-rtdb.europe-west1.firebasedatabase.app");
    DatabaseReference customerValuesRef = database.getReference("CustomerValues");

    String firstName;
    String lastName;




    private void fetchUserDetails(String userId, Consumer<Void> onUserDetailsFetched) {
        DatabaseReference userRef = database.getReference().child("FighterUser").child(userId);
        Log.d("fetching", "Fetching user details for userId: " + userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    firstName = dataSnapshot.child("firstName").getValue(String.class);
                    lastName = dataSnapshot.child("surname").getValue(String.class);
                    Log.d("fetching", "First name: " + firstName + ", Last name: " + lastName);
                    try {
                        onUserDetailsFetched.accept(null);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Error: User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }










    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authService = new AuthorizationService(this);
        httpClient = new OkHttpClient();
        Log.d("MainActivity", "onCreate: authService and httpClient initialized");


        btnLogOut = findViewById(R.id.btnLogout);
        btnAuthenticate = findViewById(R.id.btnAuthenticate);
        mAuth = FirebaseAuth.getInstance();


        btnProfile = findViewById(R.id.btnProfile);

        btnLogOut.setOnClickListener(view -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        });

        btnAuthenticate.setOnClickListener(view -> {
            authenticate();
        });

        btnInitiateWeightCut = findViewById(R.id.btnInitiateWeightCut);


        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if user is authenticated
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser == null) {
                    // User is not authenticated, redirect to LoginActivity
                     startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    return;
                }

                // User is authenticated, start ProfileActivity
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            }
        });



        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                if (currentUser == null) {
                    // User is not authenticated, redirect to LoginActivity
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        });
    }



    private void authenticate() {


        AuthorizationServiceConfiguration serviceConfiguration =
                new AuthorizationServiceConfiguration(
                        Uri.parse(AUTHORIZATION_ENDPOINT),
                        Uri.parse(TOKEN_ENDPOINT)
                );

        AuthorizationRequest.Builder authRequestBuilder =
                new AuthorizationRequest.Builder(
                        serviceConfiguration,
                        CLIENT_ID,
                        ResponseTypeValues.CODE,
                        Uri.parse(REDIRECT_URI)
                );

        authRequestBuilder.setScopes("activity", "heartrate", "sleep", "profile", "weight", "nutrition");




        AuthorizationRequest authRequest = authRequestBuilder.build();

        Intent authIntent = authService.getAuthorizationRequestIntent(authRequest);
        startActivityForResult(authIntent, AUTH_REQUEST_CODE); // use AUTH_REQUEST_CODE instead of 1
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTH_REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationResponse.fromIntent(data);
            AuthorizationException exception = AuthorizationException.fromIntent(data);

            if (response != null) {
                Log.d("MainActivity", "onActivityResult: AUTH_REQUEST_CODE received");
                // Handle successful authentication
                Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show();

                // Perform token exchange
                performTokenExchange(response);
                Log.d("MainActivity", "performTokenExchange: TokenRequest created");
            } else if (exception != null) {
                // Handle authentication error
                Toast.makeText(this, "Authentication error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            } else {
                // Handle authentication cancellation
                Toast.makeText(this, "Authentication cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // performs token exchange
    private void performTokenExchange(AuthorizationResponse authorizationResponse) {
        TokenRequest tokenRequest = authorizationResponse.createTokenExchangeRequest();
        authService.performTokenRequest(tokenRequest, (tokenResponse, exception) -> {
            if (tokenResponse != null) {
                // Save the AuthState object
                AuthState authState = new AuthState(authorizationResponse, tokenResponse, exception);

                btnInitiateWeightCut.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("MainActivity", "Initiate Weight Cut button clicked"); // Add this log statement
                        Toast.makeText(MainActivity.this, "Initiate Weight Cut button clicked", Toast.LENGTH_SHORT).show(); // Add this toast message
                        startWeightCutActivity(authState);
                    }
                });







                // TODO: Save authState to SharedPreferences or other persistent storage

                // You can now make requests to the Fitbit API using the access token
                userProfile = new JSONObject();
                makeRequest(authState);
            } else if (exception != null) {
                // Handle token exchange error
                Toast.makeText(MainActivity.this, "Token exchange error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            } else {
                // Handle token exchange cancellation
                Toast.makeText(MainActivity.this, "Token exchange cancelled", Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void makeRequest(AuthState authState) {
        String accessToken = authState.getAccessToken();
        String weightUrl = "https://api.fitbit.com/1/user/-/body/log/weight/date/today.json";
        String fatUrl = "https://api.fitbit.com/1/user/-/body/fat/date/today/1d.json";

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not authenticated, redirect to LoginActivity or handle the case as you wish
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            return;
        }
        String customerEmail = currentUser.getEmail();

        Map<String, Object> mergedData = new HashMap<>();

        Consumer<Map<String, Object>> onApiDataReceived = data -> {
            mergedData.putAll(data);
            if (mergedData.containsKey("body-fat") && mergedData.containsKey("weight")) {
                DatabaseReference dbRef = database.getReference("CustomerValues");
                dbRef.push().setValue(mergedData);
            }
        };

        makeApiRequest(fatUrl, accessToken, "body-fat", "dateTime", customerEmail, onApiDataReceived);
        makeApiRequest(weightUrl, accessToken, "weight", "date", customerEmail, onApiDataReceived);
    }


    private void makeApiRequest(String url, String accessToken, String dataType, String dateKey, String customerEmail, Consumer<Map<String, Object>> onSuccess) {
        // Update Firebase database with customer data
        FirebaseDatabase firebaseDatabase = database;
        DatabaseReference dbRef = firebaseDatabase.getReference("CustomerValues");

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Request failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(MainActivity.this, dataType + " data is not available", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (JSONException e) {
                        Log.e("MainActivity", "JSON Error: " + e.getMessage()); // Log the JSON error message
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "JSON error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    String responseBody = response.body().string();
                    Log.e("MainActivity", "Request failed with code " + response.code() + ", response body: " + responseBody);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Request failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }





    public interface Consumer<T> {
        void accept(T t) throws JSONException;
    }






    private void startWeightCutActivity(AuthState authState) {
        Intent intent = new Intent(MainActivity.this, WeightCutActivity.class);
        intent.putExtra("authState", authState.jsonSerializeString());
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User is not authenticated, redirect to LoginActivity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            return;
        }
        String userId = currentUser.getUid();

        // Define the callback function to be executed once the user details are fetched
        Consumer<Void> onUserDetailsFetched = (unused) -> {
            intent.putExtra("firstName", firstName);
            intent.putExtra("surname", lastName);
            Log.d("MainActivity", "firstName: " + firstName + ", lastName: " + lastName);

            startActivity(intent);
        };

        // Fetch user details and pass the callback function
        fetchUserDetails(userId, onUserDetailsFetched);
    }











}