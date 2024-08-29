package com.example.skills;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.net.InetAddress;

public class ForgetPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button sendResetLinkButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        mAuth = FirebaseAuth.getInstance();
        emailEditText = findViewById(R.id.email_edit_text);
        sendResetLinkButton = findViewById(R.id.send_reset_link_button);

        sendResetLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                if (!isValidEmail(email)) {
                    Toast.makeText(ForgetPasswordActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (email.isEmpty()) {
                    Toast.makeText(ForgetPasswordActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                    return;
                }else {
                    forgetUserpass(email);
                }

                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(ForgetPasswordActivity.this, "Reset link sent to your email", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(ForgetPasswordActivity.this, "Failed to send reset email", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }

            private void forgetUserpass(String email) {
                if (!isConnected()) {
                    Toast.makeText(ForgetPasswordActivity.this, "Please connect to the internet", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        });
    }
    private boolean isValidEmail(String email) {
        String emailPattern = "^[a-zA-Z0-9._%+-]+@students\\.kabianga\\.ac\\.ke$";
        return email.matches(emailPattern);
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
}