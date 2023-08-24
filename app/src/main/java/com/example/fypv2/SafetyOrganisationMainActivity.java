package com.example.fypv2;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SafetyOrganisationMainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<FighterUserWeightCut> weightCuts = new ArrayList<>();

    private boolean showOnlyFlagged = false;

    DatabaseReference databaseRef = FirebaseDatabase.getInstance("https://fypv2-23d59-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference()
            .child("FighterUserWeightCut");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safety_organisation_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new WeightCutAdapter(weightCuts);
        recyclerView.setAdapter(mAdapter);

        findViewById(R.id.toggleFlaggedButton).setOnClickListener(v -> {
            showOnlyFlagged = !showOnlyFlagged;
            ((WeightCutAdapter) mAdapter).filterFlaggedWeightCuts(showOnlyFlagged);
        });

        // Retrieve the list of weight cuts from the database
        // Retrieve the list of weight cuts from the database
        DatabaseReference weightCutsRef = FirebaseDatabase.getInstance("https://fypv2-23d59-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference()
                .child("FighterUserWeightCut");

        weightCutsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> flaggedWeightCuts = new ArrayList<>();
                weightCuts.clear(); // Clear the weightCuts list before populating

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    String userId = userSnapshot.getKey();

                    double prevBodyFat = 0.0;
                    double prevWeight = 0.0;

                    boolean flag = false;

                    for (DataSnapshot weightCutSnapshot : userSnapshot.getChildren()) {
                        for (DataSnapshot dateSnapshot : weightCutSnapshot.getChildren()) {
                            Double bodyFatDouble = dateSnapshot.child("body-fat").getValue(Double.class);
                            Double weightDouble = dateSnapshot.child("weight").getValue(Double.class);
                            // Create a new FighterUserWeightCut object and add it to the weightCuts list
                            FighterUserWeightCut weightCut = new FighterUserWeightCut(dateSnapshot);
                            weightCuts.add(weightCut);

                            if (bodyFatDouble != null && weightDouble != null) {
                                double bodyFat = bodyFatDouble.doubleValue();
                                double weight = weightDouble.doubleValue();

                                if (prevBodyFat != 0.0 && prevWeight != 0.0) {
                                    double bodyFatDiff = Math.abs((bodyFat - prevBodyFat) / prevBodyFat) * 100.0;
                                    double weightDiff = Math.abs(weight - prevWeight);

                                    if (bodyFatDiff >= 5.0 || weightDiff >= 10.0) {
                                        flag = true;
                                    }
                                }

                                prevBodyFat = bodyFat;
                                prevWeight = weight;
                            }
                        }
                    }




                    if (flag) {
                        flaggedWeightCuts.add(userId);
                    }
                    if (flag) {
                        Log.d("SafetyOrganisationMain", "Flagged records for " + userId);
                    } else {
                        Log.d("SafetyOrganisationMain", "No flagged records for " + userId);
                    }
                }

                if (flaggedWeightCuts.isEmpty()) {
                    System.out.println("No flagged weight cuts");
                } else {
                    System.out.println("Flagged weight cuts: " + flaggedWeightCuts);
                }

                // Pass flagged weight cuts to the adapter
                ((WeightCutAdapter) mAdapter).setFlaggedWeightCuts(flaggedWeightCuts);
            }



            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Firebase", "loadWeightCuts:onCancelled", databaseError.toException());
            }
        });


    }



        private class WeightCutAdapter extends RecyclerView.Adapter<WeightCutAdapter.WeightCutViewHolder> {

        private ArrayList<FighterUserWeightCut> weightCuts;

        // only showing flagged weight cuts
        private List<String> flaggedWeightCuts;
        private ArrayList<FighterUserWeightCut> filteredWeightCuts;

            public WeightCutAdapter(ArrayList<FighterUserWeightCut> weightCuts) {
                this.weightCuts = weightCuts;
                this.filteredWeightCuts = new ArrayList<>(weightCuts);
            }

            public void filterFlaggedWeightCuts(boolean showFlagged) {
                filteredWeightCuts.clear();
                if (showFlagged) {
                    for (FighterUserWeightCut weightCut : weightCuts) {
                        if (flaggedWeightCuts.contains(weightCut.getUserId())) {
                            filteredWeightCuts.add(weightCut);
                        }
                    }
                } else {
                    filteredWeightCuts.addAll(weightCuts);
                }
                Log.d("WeightCutAdapter", "Filtered weight cuts: " + filteredWeightCuts.toString());

                notifyDataSetChanged();
            }

            public void setFlaggedWeightCuts(List<String> flaggedWeightCuts) {
                this.flaggedWeightCuts = flaggedWeightCuts;
                filterFlaggedWeightCuts(showOnlyFlagged);
            }



            @NonNull
        @Override
        public WeightCutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.weight_cut_item_layout, parent, false);
            WeightCutViewHolder viewHolder = new WeightCutViewHolder(view);
            return viewHolder;
        }



            @Override
            public void onBindViewHolder(@NonNull WeightCutViewHolder holder, int position) {
                FighterUserWeightCut weightCut = filteredWeightCuts.get(position);
                holder.tvDate.setText((CharSequence) weightCut.getDate());
                holder.tvEmail.setText(weightCut.getEmail());
                holder.tvWeight.setText(String.valueOf(weightCut.getWeight()));
                holder.tvBodyFat.setText(String.valueOf(weightCut.getBodyFat()));
                holder.itemView.setOnClickListener(v -> {
                    // Handle click on weight cut item
                });
            }

            @Override
            public int getItemCount() {
                return filteredWeightCuts.size();
            }


            public class WeightCutViewHolder extends RecyclerView.ViewHolder {

            TextView tvDate, tvEmail, tvWeight, tvBodyFat;

            public WeightCutViewHolder(View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvEmail = itemView.findViewById(R.id.tvEmail);
                tvWeight = itemView.findViewById(R.id.tvWeight);
                tvBodyFat = itemView.findViewById(R.id.tvBodyFat);
            }
        }
    }
}
