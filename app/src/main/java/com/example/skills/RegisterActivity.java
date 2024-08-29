package com.example.skills;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends BaseActivity {

    private EditText emailEditText;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private Spinner schoolSpinner;
    private CheckBox showPasswordCheckBox;
    private TextView just_LOGIN;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String selectedSchool;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        emailEditText = findViewById(R.id.email);
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        just_LOGIN = findViewById(R.id.just_login);
        schoolSpinner = findViewById(R.id.school_spinner);
        showPasswordCheckBox = findViewById(R.id.show_password);
        registerButton = findViewById(R.id.register_btn);

        just_LOGIN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigating to MainActivity
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.school_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        schoolSpinner.setAdapter(adapter);

        // Set up the spinner
        schoolSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) { // Skipping the "Choose school" option
                    selectedSchool = (String) parent.getItemAtPosition(position);
                } else {
                    selectedSchool = null;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSchool = null;
            }
        });

        showPasswordCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });

        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        // Check network connectivity
        if (!isConnected()) {
            Toast.makeText(RegisterActivity.this, "Please connect to the internet.", Toast.LENGTH_LONG).show();
            return;
        }
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || username.isEmpty() || selectedSchool == null) {
            Toast.makeText(RegisterActivity.this, "Please fill all fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isValidUsername(username)) {
            Toast.makeText(RegisterActivity.this, "Please use your Reg.NO e.g., EDS-M-0021-2018", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(email)) {
            Toast.makeText(RegisterActivity.this, "Please use your student email.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(password)) {
            Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters, include uppercase, lowercase, number, and special character", Toast.LENGTH_SHORT).show();
            return;
        }
        // Show the ProgressDialog
        progressDialog = new ProgressDialog(RegisterActivity.this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(RegisterActivity.this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)  // Store the username in displayName
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileUpdateTask -> {
                                        if (profileUpdateTask.isSuccessful()) {
                                            saveSchoolToFirestore(user, username);
                                        } else {
                                            Toast.makeText(RegisterActivity.this, "Profile update failed", Toast.LENGTH_SHORT).show();
                                            progressDialog.dismiss();
                                        }
                                    });
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration failed. " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration failed. " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
    }

    private void saveSchoolToFirestore(FirebaseUser user, String username) {
        DocumentReference userRef = db.collection("users").document(username);
        UserProfile userProfile = new UserProfile(user.getEmail(), selectedSchool, username);

        // Log data to debug
        Log.d("FirestoreDebug", "Saving user profile: " + userProfile.getEmail() + ", " + userProfile.getSchool() + ", " + userProfile.getUsername());

        userRef.set(userProfile)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        sendVerificationEmail(user);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Failed to save school. " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    progressDialog.dismiss();
                });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Verification email sent", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Failed to send verification email. " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isValidPassword(String password) {
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$";
        Pattern pattern = Pattern.compile(passwordPattern);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@students\\.kabianga\\.ac\\.ke$";
        Pattern pattern = Pattern.compile(emailPattern);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private boolean isValidUsername(String username) {
        String usernamePattern = "^[A-Z]{2,3}-M-[0-9]{4}-(201[3-9]|202[0-9]|2030)$";
        Pattern pattern = Pattern.compile(usernamePattern);
        Matcher matcher = pattern.matcher(username);
        return matcher.matches();
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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

    private static class UserProfile {
        private String email;
        private String school;
        private String username;
        private boolean hasVoted;

        public UserProfile(String email, String school, String username) {
            this.email = email;
            this.school = school;
            this.username = username;
            this.hasVoted = false; // Initialize hasVoted to false during registration
        }

        public String getEmail() {
            return email;
        }

        public String getSchool() {
            return school;
        }

        public String getUsername() {
            return username;
        }

        public boolean getHasVoted() {
            return hasVoted;
        }
    }
}
