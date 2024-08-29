package com.example.skills;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ClosedVotingActivity extends AppCompatActivity {
    private Button exitBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_closed_voting);

        exitBtn = findViewById(R.id.exitBtn);
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitApp();
            }
        });
        TextView message = findViewById(R.id.closed_voting_message);
        message.setText("Voting has ended. Thank you for your participation.");
    }

    public void exitApp(){
        Toast.makeText(ClosedVotingActivity.this, "Exiting the app.", Toast.LENGTH_SHORT).show();
        finishAffinity();
    }
}
