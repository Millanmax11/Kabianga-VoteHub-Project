package com.example.skills;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SeniorFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private SeniorAdapter seniorAdapter;
    private ProgressDialog progressDialog;
    private List<Object> itemList = new ArrayList<>();
    private String username;

    public SeniorFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_senior, container, false);

        // Set up toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
        });

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclingView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        seniorAdapter = new SeniorAdapter(itemList);
        recyclerView.setAdapter(seniorAdapter);
        view.findViewById(R.id.submit_del_btn).setOnClickListener(v -> submitVotes());

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
        fetchSeniors();
        return view;
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("itemList", new ArrayList<>(itemList));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore itemList from savedInstanceState
            itemList = (List<Object>) savedInstanceState.getSerializable("itemList");
            // Update RecyclerView
            seniorAdapter = new SeniorAdapter(itemList);
            recyclerView.setAdapter(seniorAdapter);
        }
    }


    private void fetchSeniors() {
        // Check network connectivity
        if (!isConnected()) {
            Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_LONG).show();
            return;
        }
        if (username == null) {
            Toast.makeText(getContext(), "Username not found. Please login", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Clear itemList to start fresh
        itemList.clear();

        db.collection("Senior").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String position = document.getString("position");
                    db.collection("Senior").document(document.getId()).collection("Student").get().addOnCompleteListener(subTask -> {
                        if (subTask.isSuccessful()) {
                            List<Senior> seniors = new ArrayList<>();
                            for (QueryDocumentSnapshot subDocument : subTask.getResult()) {
                                Senior senior = subDocument.toObject(Senior.class);
                                senior.setPosition(position);
                                seniors.add(senior);
                            }
                            updateUIWithSeniors(position, seniors);
                        } else {
                            hideProgressDialog();
                        }
                    });
                }
            } else {
                hideProgressDialog();
            }
        });
    }

    private void updateUIWithSeniors(String position, List<Senior> seniors) {
        itemList.add(position);
        itemList.addAll(seniors);
        seniorAdapter.notifyDataSetChanged();
        hideProgressDialog();
    }

    private void submitVotes() {
        // Check network connectivity
        if (!isConnected()) {
            Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_LONG).show();
            return;
        }
        if (username == null) {
            Toast.makeText(getContext(), "Username not found. Please log in.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Get selected positions map from adapter
        Map<String, Integer> selectedPositionsMap = seniorAdapter.getSelectedPositionsMap();

        // Check if all positions have been selected
        Set<String> selectedPositions = new HashSet<>(selectedPositionsMap.keySet());
        Set<String> uniquePositions = new HashSet<>();
        for (Object item : itemList) {
            if (item instanceof String) {
                uniquePositions.add((String) item);
            }
        }
        if (!selectedPositions.containsAll(uniquePositions)) {
            Toast.makeText(getContext(), "Please select a senior for each position.", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        // Retrieve user's delegate status and voting status
        DocumentReference delegateRef = db.collection("ElectedDelegate").document(username);
        delegateRef.get().addOnSuccessListener(delegateSnapshot -> {
            if (delegateSnapshot.exists()) {
                Boolean hasVoted = delegateSnapshot.getBoolean("voted");
                if (Boolean.TRUE.equals(hasVoted)) {
                    Toast.makeText(getContext(), "You've already voted.", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                } else {
                    // Proceed to submit votes
                    castVotes(selectedPositionsMap, delegateRef);
                }
            } else {
                Toast.makeText(getContext(), "You are not authorized to vote.", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        }).addOnFailureListener(e -> {
            Log.e("SeniorFragment", "Error retrieving delegate data", e);
            Toast.makeText(getContext(), "Error retrieving delegate data. Please try again.", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        });
    }

    private void castVotes(Map<String, Integer> selectedPositionsMap, DocumentReference delegateRef) {
        db.runTransaction(transaction -> {
            // Map to store the vote counts for each senior
            Map<String, Long> voteCounts = new HashMap<>();

            // Read all required documents first
            for (Map.Entry<String, Integer> entry : selectedPositionsMap.entrySet()) {
                String position = entry.getKey();
                Senior selectedSenior = (Senior) itemList.get(entry.getValue());
                DocumentReference voteRef = db.collection("SeniorVotes")
                        .document(position)
                        .collection("Student")
                        .document(selectedSenior.getUsername());

                if (transaction.get(voteRef).exists()) {
                    long currentVoteCount = transaction.get(voteRef).getLong("voteCount");
                    voteCounts.put(selectedSenior.getUsername(), currentVoteCount + 1);
                } else {
                    voteCounts.put(selectedSenior.getUsername(), 1L);
                }
            }

            // Perform all writes
            for (Map.Entry<String, Integer> entry : selectedPositionsMap.entrySet()) {
                String position = entry.getKey();
                Senior selectedSenior = (Senior) itemList.get(entry.getValue());
                DocumentReference voteRef = db.collection("SeniorVotes")
                        .document(position)
                        .collection("Student")
                        .document(selectedSenior.getUsername());

                // Set new vote count and other details
                Map<String, Object> voteData = new HashMap<>();
                voteData.put("name", selectedSenior.getName());
                voteData.put("year", selectedSenior.getYear());
                voteData.put("position", position);
                voteData.put("userName", selectedSenior.getUsername());
                voteData.put("image", selectedSenior.getImage());
                voteData.put("voteCount", voteCounts.get(selectedSenior.getUsername()));

                transaction.set(voteRef, voteData);
            }

            // Update delegate's voted status
            transaction.update(delegateRef, "voted", true);
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Votes submitted successfully!", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();

            // Navigate to results fragment
            FragmentManager fragmentManager = getParentFragmentManager();
            if (fragmentManager != null) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                ResultseniorFragment resultseniorFragment = new ResultseniorFragment();
                fragmentTransaction.replace(R.id.fragment_container, resultseniorFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        }).addOnFailureListener(e -> {
            Log.e("SeniorFragment", "Error submitting votes", e); // Detailed logging of the error
            Toast.makeText(getContext(), "Failed to submit votes. Please try again.", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        });
    }


    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            recyclerView.setVisibility(View.VISIBLE);
            progressDialog.dismiss();
        }
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
}
