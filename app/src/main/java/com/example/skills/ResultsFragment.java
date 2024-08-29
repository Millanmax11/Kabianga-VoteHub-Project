package com.example.skills;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ResultsFragment extends Fragment {
    private String username = "";
    private FirebaseAuth auth;
    private HorizontalBarChart chart;
    private ViewGroup resultsContainer;
    private ProgressDialog progressDialog;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_results, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }

        toolbar.setNavigationOnClickListener(v -> {
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
        });
        auth = FirebaseAuth.getInstance();
        chart = view.findViewById(R.id.chart);
        resultsContainer = view.findViewById(R.id.results_container);
        db = FirebaseFirestore.getInstance();

        if (!isConnected()) {
            Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_LONG).show();
            return view;
        }

        // Get the username from FirebaseAuth or Bundle
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            username = user.getDisplayName(); // Assuming username is stored in displayName
        } else {
            // Get username from Bundle if passed as an argument
            Bundle args = getArguments();
            if (args != null) {
                username = args.getString("username");
            }
        }
        // Retrieve userSchool from firestore
        fetchUserSchoolAndFetchResults();
        return view;
    }
    private void fetchUserSchoolAndFetchResults() {
        // Check network connectivity
        if (!isConnected()) {
            Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_SHORT).show();
            return;
        }
        if (username == null) {
            Toast.makeText(getContext(), "Username not found. Please log in.", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Loading results...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        db.collection("users").document(username).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String userSchool = documentSnapshot.getString("school");
                if (userSchool != null) {
                    fetchResults(userSchool);
                } else {
                    Toast.makeText(getContext(), "School information not found. Please log in.", Toast.LENGTH_SHORT).show();
                    hideProgressDialog();
                }
            } else {
                Toast.makeText(getContext(), "User document not found.", Toast.LENGTH_SHORT).show();
                hideProgressDialog();
            }
        }).addOnFailureListener(e -> {
            Log.e("ResultsFragment", "Error getting user document", e);
            Toast.makeText(getContext(), "Error getting user document.", Toast.LENGTH_SHORT).show();
            hideProgressDialog();
        });
    }

    private void fetchResults(String userSchool) {
        // Check network connectivity
        if (!isConnected()) {
            Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_SHORT).show();
            hideProgressDialog();
            return;
        }
        if (username == null) {
            Toast.makeText(getContext(), "Username not found. Please log in.", Toast.LENGTH_SHORT).show();
            hideProgressDialog();
            return;
        }

        db.collection("Votes").document(userSchool).collection("Delegates").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<BarEntry> barEntries = new ArrayList<>();
                List<String> delegateNames = new ArrayList<>();
                int index = 0;

                for (QueryDocumentSnapshot delegateDoc : task.getResult()) {
                    String delegateName = delegateDoc.getId();
                    String delegateYear = delegateDoc.getString("year");
                    Long voteCount = delegateDoc.getLong("voteCount");
                    String imageUrl = delegateDoc.getString("image");

                    User delegate = new User(delegateName, delegateYear, imageUrl); // Create User object with image URL

                    barEntries.add(new BarEntry(index, voteCount != null ? voteCount : 0));
                    delegateNames.add(delegateName);
                    addResultToContainer(delegate, (int) (voteCount != null ? voteCount : 0)); // Pass User object

                    index++;
                }

                // Check if any data is available
                if (!barEntries.isEmpty()) {
                    BarDataSet barDataSet = new BarDataSet(barEntries, "Votes");

                    // Customize colors for each bar
                    List<Integer> colors = new ArrayList<>();
                    colors.add(Color.parseColor("#80279c"));
                    colors.add(Color.parseColor("#66b50b"));
                    colors.add(Color.parseColor("#10115c"));
                    colors.add(Color.parseColor("#5ca5ad"));
                    colors.add(Color.parseColor("#8f1e31"));
                    colors.add(Color.parseColor("#72ad5c"));
                    colors.add(Color.parseColor("#877a87"));
                    colors.add(Color.parseColor("#ad6f5c"));

                    barDataSet.setColors(colors); // Set custom colors for bars

                    BarData barData = new BarData(barDataSet);
                    chart.setData(barData);

                    // Set delegate names as labels for the x-axis
                    XAxis xAxis = chart.getXAxis();
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(delegateNames));
                    xAxis.setTextColor(Color.WHITE); // Set text color to white
                    xAxis.setTextSize(13f);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // Position labels outside the chart
                    xAxis.setGranularity(1f);
                    xAxis.setLabelRotationAngle(-40f);
                    xAxis.setGranularityEnabled(true);

                    YAxis yAxisLeft = chart.getAxisLeft();
                    yAxisLeft.setGranularity(1f);
                    yAxisLeft.setGranularityEnabled(true);

                    // Adjust the chart view offset to make sure the labels are fully visible
                    chart.setExtraOffsets(90, 10, 20, 60); // Increase the left offset to accommodate the full delegate names

                    chart.getAxisRight().setEnabled(false);

                    // Remove grid lines
                    chart.getAxisLeft().setDrawGridLines(false);
                    chart.getXAxis().setDrawGridLines(false);

                    chart.invalidate(); // refresh
                } else {
                    Toast.makeText(getContext(), "No data available.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Failed to fetch results. Please try again.", Toast.LENGTH_SHORT).show();
            }
            hideProgressDialog(); // Dismiss progress dialog after fetching results
        }).addOnFailureListener(e -> {
            Log.e("ResultsFragment", "Error fetching results", e);
            Toast.makeText(getContext(), "Error fetching results", Toast.LENGTH_SHORT).show();
            hideProgressDialog();
        });
    }

    private void addResultToContainer(User delegate, int voteCount) {
        // Inflate the card view layout
        View cardViewLayout = LayoutInflater.from(getContext()).inflate(R.layout.cart_view_result, null);

        // Find the TextViews and ImageView in the inflated layout
        ImageView imageView = cardViewLayout.findViewById(R.id.imageView);
        TextView delegateNameTextView = cardViewLayout.findViewById(R.id.delegateName);
        TextView delegateYearTextView = cardViewLayout.findViewById(R.id.delegateYear);
        TextView voteCountTextView = cardViewLayout.findViewById(R.id.voteCount);

        // Load image into imageView using Glide
        Glide.with(getContext())
                .load(delegate.getImage()) // Load image URL using getImage() method from User class
                .placeholder(R.drawable.placeholder_image) // Placeholder image while loading
                .error(R.drawable.error_image) // Error image if loading fails
                .circleCrop() // Apply circular crop transformation
                .into(imageView);

        // Set delegate name, year, and vote count to the TextViews
        delegateNameTextView.setText(delegate.getName()); // Set delegate name
        delegateYearTextView.setText(delegate.getYear()); // Set delegate year
        voteCountTextView.setText(String.format("%d votes", voteCount)); // Format vote count

        // Set layout parameters for the card view
        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 16); // Add bottom margin to create gap between cards
        cardViewLayout.setLayoutParams(layoutParams);

        // Add inflated layout to the results container
        resultsContainer.addView(cardViewLayout);
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if(activeNetwork != null && activeNetwork.isConnected()){
            try {
                //performing a ping to the server
                InetAddress ipAddr = InetAddress.getByName("8.8.8.8");
                return !ipAddr.equals((""));
            }
            catch (Exception e){
                return false;
            }
        }
        else {
            return false;
        }
    }
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
