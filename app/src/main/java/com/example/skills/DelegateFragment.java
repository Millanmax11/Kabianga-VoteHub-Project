package com.example.skills;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DelegateFragment extends Fragment {
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private FirebaseAuth auth;
    private String userSchool;
    private TextView textViewSchool;
    private ProgressDialog progressDialog;
    private String username;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delegate, container, false);

        // Check network connectivity
        if (!isConnected()) {
            Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_LONG).show();
            return view;
        }


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

        // Initialize Firebase Firestore and Auth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize TextView
        textViewSchool = view.findViewById(R.id.textViewSchool);



        // Initialize User list and adapter
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList);
        recyclerView.setAdapter(userAdapter);

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
        if (username != null) {
            // Fetch user school and delegates
            fetchUserSchoolAndDelegates();
        } else {
            Toast.makeText(getContext(), "Username not found. Please login", Toast.LENGTH_SHORT).show();
        }

        // Set the submit button event listener
        view.findViewById(R.id.submit_del_btn).setOnClickListener(v -> submitVote());

        return view;
    }

    private void fetchUserSchoolAndDelegates() {
        // Show progress dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Fetch user school
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(username).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    userSchool = documentSnapshot.getString("school");
                    textViewSchool.setText(userSchool);
                    fetchDelegates(userSchool);
                } else {
                    Toast.makeText(getContext(), "User document not found.", Toast.LENGTH_SHORT).show();
                    hideProgressDialog();
                }
            }).addOnFailureListener(e -> {
                Log.e("DelegateFragment", "Error getting user document", e);
                Toast.makeText(getContext(), "Error getting user document.", Toast.LENGTH_SHORT).show();
                hideProgressDialog();
            });
        } else {
            hideProgressDialog();
        }
    }

    private void fetchDelegates(String school) {
        db.collection("Delegate").whereEqualTo("School", school).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                userList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    document.getReference().collection("Student").get().addOnSuccessListener(studentSnapshots -> {
                        for (QueryDocumentSnapshot studentDocument : studentSnapshots) {
                            User delegate = studentDocument.toObject(User.class);
                            delegate.setUsername(studentDocument.getString("username")); // Set delegate's username
                            userList.add(delegate);
                        }
                        userAdapter.notifyDataSetChanged();
                        hideProgressDialog();
                    }).addOnFailureListener(e -> {
                        Log.e("DelegateFragment", "Error getting student documents", e);
                        hideProgressDialog();
                    });
                }
            } else {
                Log.w("DelegateFragment", "Error getting delegate documents.", task.getException());
                hideProgressDialog();
            }
        }).addOnFailureListener(e -> {
            Log.e("DelegateFragment", "Error getting delegate documents", e);
            hideProgressDialog();
        });
    }

    private void submitVote() {
        // Show progress dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        int selectedPosition = userAdapter.getSelectedPosition();
        if (selectedPosition == -1) {
            Toast.makeText(getContext(), "Please select a delegate to vote for", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss(); // Dismiss the progress dialog if no delegate is selected
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // Check if the user has already voted
            db.collection("users").document(username).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Boolean hasVoted = documentSnapshot.getBoolean("hasVoted");
                    if (Boolean.TRUE.equals(hasVoted)) {
                        Toast.makeText(getContext(), "You've already voted.", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss(); // Dismiss the progress dialog if user has already voted
                    } else {
                        // Proceed with voting
                        castVote(user, selectedPosition);
                    }
                } else {
                    Toast.makeText(getContext(), "User data not found.", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }).addOnFailureListener(e -> {
                Log.e("DelegateFragment", "Error checking vote status", e);
                Toast.makeText(getContext(), "Error checking vote status. Please try again.", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            });
        } else {
            progressDialog.dismiss();
        }
    }

    private void castVote(FirebaseUser user, int selectedPosition) {
        User selectedDelegate = userList.get(selectedPosition);
        String delegateName = selectedDelegate.getName();
        String delegateYear = selectedDelegate.getYear();
        String delegateImage = selectedDelegate.getImage(); // Get the image URL
        String delegateUsername = selectedDelegate.getUsername(); // Get the username

        // Update vote count in Firebase Firestore
        DocumentReference voteRef = db.collection("Votes")
                .document(userSchool)
                .collection("Delegates")
                .document(delegateName);

        db.runTransaction(transaction -> {
            DocumentReference delegateDocRef = voteRef;
            Long newVoteCount;
            if (transaction.get(delegateDocRef).exists()) {
                long currentVoteCount = transaction.get(delegateDocRef).getLong("voteCount");
                newVoteCount = currentVoteCount + 1;
            } else {
                newVoteCount = 1L;
            }

            // Set new vote count and image URL
            Map<String, Object> voteData = new HashMap<>();
            voteData.put("school", userSchool);
            voteData.put("userName", delegateUsername);
            voteData.put("delegateName", delegateName);
            voteData.put("year", delegateYear);
            voteData.put("voteCount", newVoteCount);
            voteData.put("image", delegateImage); // Include the image URL

            transaction.set(delegateDocRef, voteData);

            // Update hasVoted flag
            transaction.update(db.collection("users").document(username), "hasVoted", true);

            return newVoteCount;
        }).addOnSuccessListener(newVoteCount -> {
            Toast.makeText(getContext(), "Vote submitted successfully!", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss(); // Dismiss the progress dialog after successful vote submission

            // Navigate to ResultFragment
            FragmentManager fragmentManager = getParentFragmentManager();
            if (fragmentManager != null) {
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                ResultsFragment resultsFragment = new ResultsFragment(); // Create a new instance
                // Create a bundle to pass arguments
                Bundle args = new Bundle();
                args.putString("userSchool", userSchool); // Pass the userSchool value
                resultsFragment.setArguments(args); // Set the arguments for the instance
                fragmentTransaction.replace(R.id.fragment_container, resultsFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        }).addOnFailureListener(e -> {
            Log.e("DelegateFragment", "Error submitting vote", e);
            Toast.makeText(getContext(), "Failed to submit vote. Please try again.", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss(); // Dismiss the progress dialog if vote submission fails
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
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
