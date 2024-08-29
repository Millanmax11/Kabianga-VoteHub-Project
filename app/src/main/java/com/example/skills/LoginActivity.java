package com.example.skills;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.net.InetAddress;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText emailEditText, passwordEditText;
    private MaterialButton loginbtn;
    private TextView just_REGISTER;
    private TextView just_FORGET;
    private ProgressDialog progressDialog;
    private long countdownTime;
    private CheckBox showPasswordCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        loginbtn = findViewById(R.id.loginbtn);
        just_REGISTER = findViewById(R.id.just_register);
        just_FORGET = findViewById(R.id.forgetpass);
        showPasswordCheckbox = findViewById(R.id.show_password);

        showPasswordCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // show password
                passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                // hide password
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            // move the cursor to the end of input
            passwordEditText.setSelection(passwordEditText.length());
        });

        loginbtn.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        just_REGISTER.setOnClickListener(v -> {
            // navigating to RegisterActivity
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });

        just_FORGET.setOnClickListener(v -> {
            // navigating to ForgetPasswordActivity
            Intent intent = new Intent(LoginActivity.this, ForgetPasswordActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void loginUser(String email, String password) {
        // Check network connectivity
        if (!isConnected()) {
            Toast.makeText(LoginActivity.this, "Please connect to the internet.", Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    hideProgressDialog(); // Hide progress dialog after Firebase operation completes

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                            checkVotingStatus();
                        } else {
                            Toast.makeText(LoginActivity.this, "Please verify your email before logging in.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut(); // sign out the user
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed: Credentials are incorrect, or not registered...", Toast.LENGTH_SHORT).show();
                        Toast.makeText(LoginActivity.this, "...or no Active Data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkVotingStatus() {
        db.collection("Controlvote").document("VotingStatus").get()
                .addOnCompleteListener(task -> {
                    hideProgressDialog(); // Hide progress dialog after Firestore operation completes

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            boolean isVotingOpen = Boolean.TRUE.equals(document.getBoolean("isVotingOpen"));
                            boolean isVotingClosed = Boolean.TRUE.equals(document.getBoolean("isVotingClosed"));

                            long votingStartTime = 0;
                            long votingEndTime = 0;

                            // Get and cast votingStartTime and votingEndTime from Timestamp to long
                            if (document.contains("votingStartTime")) {
                                Object votingStartTimeObj = document.get("votingStartTime");
                                if (votingStartTimeObj instanceof com.google.firebase.Timestamp) {
                                    votingStartTime = ((com.google.firebase.Timestamp) votingStartTimeObj).toDate().getTime();
                                }
                            }

                            if (document.contains("votingEndTime")) {
                                Object votingEndTimeObj = document.get("votingEndTime");
                                if (votingEndTimeObj instanceof com.google.firebase.Timestamp) {
                                    votingEndTime = ((com.google.firebase.Timestamp) votingEndTimeObj).toDate().getTime();
                                }
                            }

                            long currentTime = System.currentTimeMillis();

                            if (isVotingClosed || currentTime > votingEndTime) {
                                startActivity(new Intent(this, ClosedVotingActivity.class));
                            } else if (!isVotingOpen && currentTime < votingStartTime) {
                                Intent intent = new Intent(this, AlmostVotingActivity.class);
                                intent.putExtra("countdownTime", votingStartTime - currentTime);
                                startActivity(intent);
                            } else if (!isVotingOpen && currentTime >= votingStartTime) {
                                startActivity(new Intent(this, NoVotingActivity.class));
                            } else {
                                // navigate to homefragment
                                Intent intent = new Intent(this, DashboardActivity.class);
                                startActivity(intent);
                            }
                        } else {
                            Log.e("LoginActivity", "No such document");
                            Toast.makeText(LoginActivity.this, "Error: Voting status document does not exist.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("LoginActivity", "get failed with ", task.getException());
                        Toast.makeText(LoginActivity.this, "Error getting voting status.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Log.e("LoginActivity", "Error getting document", e);
                    Toast.makeText(LoginActivity.this, "Error getting voting status.", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToHomeFragment() {
        // Example of setting countdownTime, adjust as needed
        countdownTime = System.currentTimeMillis() + 10000; // 10 seconds for example

        HomeFragment homeFragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putLong("countdownTime", countdownTime);
        homeFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, homeFragment)
                .commit();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.isConnected()) {
            return true;
        } else {
            return false;
        }
    }
}
