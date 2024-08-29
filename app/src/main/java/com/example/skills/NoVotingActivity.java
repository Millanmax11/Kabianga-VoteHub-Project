package com.example.skills;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;


public class NoVotingActivity extends AppCompatActivity {
    private Button exitButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_voting);

        exitButton = findViewById(R.id.exitBtn);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitApp();
            }
        });
    }
    public void exitApp(){
        Toast.makeText(NoVotingActivity.this, "Exiting the app.", Toast.LENGTH_SHORT).show();
        finishAffinity();
    }
}
