package com.example.fypv2;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.PropertyName;

public class FighterUserWeightCut {
    private double bodyFat;
    private String date;
    private String email;
    private String firstName;
    private String lastName;
    private double weight;
    private String userId;

    public FighterUserWeightCut() {
    }

    public FighterUserWeightCut(double bodyFat, String date, String email, String firstName, String lastName, double weight, String userId) {
        this.bodyFat = bodyFat;
        this.date = date;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.weight = weight;
        this.userId = userId;
    }

    public FighterUserWeightCut(DataSnapshot dataSnapshot) {
        this.bodyFat = dataSnapshot.child("body-fat").getValue(Double.class);
        this.date = dataSnapshot.child("date").getValue(String.class);
        this.email = dataSnapshot.child("email").getValue(String.class);
        this.firstName = dataSnapshot.child("first-name").getValue(String.class);
        this.lastName = dataSnapshot.child("last-name").getValue(String.class);
        this.weight = dataSnapshot.child("weight").getValue(Double.class);
        this.userId = dataSnapshot.child("user-id").getValue(String.class);
    }


    @PropertyName("body-fat")
    public double getBodyFat() {
        return bodyFat;
    }

    @PropertyName("body-fat")
    public void setBodyFat(double bodyFat) {
        this.bodyFat = bodyFat;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId){
        this.userId = userId;
    }
}


