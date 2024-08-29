package com.example.skills;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountFragment extends Fragment {
    private TextView usernameText;
    private TextView emailText;
    private TextView schoolText;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;
    private String username;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        usernameText = view.findViewById(R.id.username);
        emailText = view.findViewById(R.id.email);
        schoolText = view.findViewById(R.id.school);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
            fetchUserInfo();
        } else {
            Toast.makeText(getContext(), "Username not found. Please login", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void fetchUserInfo() {
        // Show progress dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Fetch user info
        db.collection("users").document(username).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String username = document.getString("username");
                    String email = document.getString("email");
                    String school = document.getString("school");

                    usernameText.setText(username);
                    emailText.setText(email);
                    schoolText.setText(school);
                } else {
                    Toast.makeText(getContext(), "No user data found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("AccountFragment", "Error getting user documents", task.getException());
                Toast.makeText(getContext(), "Error fetching user data", Toast.LENGTH_SHORT).show();
            }
            hideProgressDialog();
        }).addOnFailureListener(e -> {
            Log.e("AccountFragment", "Error getting user documents", e);
            Toast.makeText(getContext(), "Error fetching user data", Toast.LENGTH_SHORT).show();
            hideProgressDialog();
        });
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
