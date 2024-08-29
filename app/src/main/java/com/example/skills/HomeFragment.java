package com.example.skills;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HomeFragment extends Fragment implements CardAdapter.OnButtonClickListener {
    private static final String TAG = "HomeFragment";
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ProgressDialog progressDialog;

    private RecyclerView recyclerView;
    private CardAdapter cardAdapter;
    private List<CardItem> cardItemList;

    private TextView countdownText;
    private CountDownTimer countDownTimer;
    private long countdownTime; // Time in milliseconds until voting ends
    private boolean votingEnded = false; // Flag to indicate voting has ended

    private MaterialButton delresultBtn;
    private MaterialButton seniorresultBtn;

    private String userSchool;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        countdownText = view.findViewById(R.id.countdown_text);
        delresultBtn = view.findViewById(R.id.del_results_btn);
        seniorresultBtn = view.findViewById(R.id.senior_results_btn);

        // Fetch user school
        fetchUserSchool();

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        cardItemList = new ArrayList<>();
        cardItemList.add(new CardItem(R.drawable.instruct_img, "Instructions", "Check Instruction"));
        cardItemList.add(new CardItem(R.drawable.vote_img, "Delegate", "Vote Delegate"));
        cardItemList.add(new CardItem(R.drawable.vote_img, "Senior", "Vote Executives"));

        cardAdapter = new CardAdapter(cardItemList, this);
        recyclerView.setAdapter(cardAdapter);

        startCountdown();

        // Delegate results button navigation
        delresultBtn.setOnClickListener(v -> {
            if (isConnected()) {
                ResultsFragment resultsFragment = new ResultsFragment();
                Bundle args = new Bundle();
                args.putString("userSchool", userSchool);
                resultsFragment.setArguments(args);
                navigateToFragment(resultsFragment);
            } else {
                Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_SHORT).show();
            }
        });

        // Senior results button navigation
        seniorresultBtn.setOnClickListener(v -> {
            if (isConnected()) {
                navigateToFragment(new ResultseniorFragment());
            } else {
                Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopCountdown();
    }

    @Override
    public void onButtonClick(int position) {
        if (votingEnded) {
            Toast.makeText(getContext(), "Voting has ended.", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (position) {
            case 0:
                // Instructions button clicked
                if (isConnected()) {
                    navigateToFragment(new InstructionsFragment());
                } else {
                    Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_SHORT).show();
                }
                break;
            case 1:
                // Delegate button clicked
                if (isConnected()){
                    navigateToDelegateFragment();
                } else {
                    Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2:
                // Senior button clicked
                if (isConnected()){
                    navigateToFragment(new SeniorFragment());
                } else {
                    Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void fetchUserSchool() {
        if (!isConnected()) {
            Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_LONG).show();
            return;
        }
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    userSchool = documentSnapshot.getString("school");
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user document", e);
                Toast.makeText(getContext(), "Failed to fetch user school", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void startCountdown() {
        db.collection("Controlvote").document("VotingStatus").get()
                .addOnCompleteListener(task -> {
                    // hideProgressDialog();


                    if (task.isSuccessful()){
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()){
                            long votingEndTime = 0;

                            if (document.contains("votingEndTime")) {
                                Object votingEndTimeObj = document.get("votingEndTime");
                                if (votingEndTimeObj instanceof com.google.firebase.Timestamp) {
                                    votingEndTime = ((com.google.firebase.Timestamp) votingEndTimeObj).toDate().getTime();
                                }
                            }

                            long currentTime = System.currentTimeMillis();

                            countdownTime = votingEndTime - currentTime;

                            countDownTimer = new CountDownTimer(countdownTime, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {
                                    long days = TimeUnit.MILLISECONDS.toDays(millisUntilFinished);
                                    millisUntilFinished -= TimeUnit.DAYS.toMillis(days);

                                    long hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished);
                                    millisUntilFinished -= TimeUnit.HOURS.toMillis(hours);

                                    long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                                    millisUntilFinished -= TimeUnit.MINUTES.toMillis(minutes);

                                    long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished);

                                    String countdown = String.format("%02d d : %02d h : %02d m : %02d s", days, hours, minutes, seconds);
                                    countdownText.setText(countdown);
                                }

                                @Override
                                public void onFinish() {
                                    votingEnded = true; // Voting has ended
                                    countdownText.setText("Voting Has Ended.");
                                }
                            }.start();
                        }else {
                            Log.e("HomeFragment", "No such document");
                            Toast.makeText(getContext(), "Error: Voting status document does not exist.", Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        Log.e("HomeFragment", "get failed with ", task.getException());
                        Toast.makeText(getContext(), "Error getting voting status.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Log.e("HomeFragment", "Error getting document", e);
                    Toast.makeText(getContext(), "Error getting voting status.", Toast.LENGTH_SHORT).show();
                });

    }

    private void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void navigateToDelegateFragment() {
        DelegateFragment delegateFragment = new DelegateFragment();
        Bundle args = new Bundle();
        args.putString("userSchool", userSchool);
        delegateFragment.setArguments(args);
        navigateToFragment(delegateFragment);
    }

    private void navigateToFragment(Fragment fragment) {
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot navigate to fragment");
            return;
        }

        if (!isConnected()) {
            Toast.makeText(getContext(), "Please connect to the internet", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
            Log.d(TAG, "Navigated to fragment: " + fragment.getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to fragment", e);
            Toast.makeText(getContext(), "Error navigating to fragment: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            try {
                // Performing a ping to the server
                InetAddress ipAddr = InetAddress.getByName("8.8.8.8");
                return !ipAddr.equals("");
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
