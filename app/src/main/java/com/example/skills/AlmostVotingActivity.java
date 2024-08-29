package com.example.skills;

import static com.airbnb.lottie.L.TAG;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.util.concurrent.TimeUnit;

public class AlmostVotingActivity extends AppCompatActivity {
    private TextView countdownText;
    private MaterialButton HomeBtn;
    private MaterialButton ExitBtn;
    private boolean isCountdownFinished = false;
    private long countdownTime; // Time in milliseconds until voting starts

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_almost_voting);

        countdownText = findViewById(R.id.countdown_text);
        HomeBtn = findViewById(R.id.homeBtn);
        ExitBtn = findViewById(R.id.exitBtn);

        ExitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitApp();
            }
        });

        HomeBtn.setOnClickListener(v -> {
            if (isCountdownFinished) {
                Intent intent = new Intent(AlmostVotingActivity.this, DashboardActivity.class);
                intent.putExtra("fromAlmostVoting", true); // Indicate that the intent is from AlmostVotingActivity
                startActivity(intent);
            } else {
                Toast.makeText(AlmostVotingActivity.this, "Not yet time, Please wait or try again later.", Toast.LENGTH_SHORT).show();
            }
        });

        // Get countdownTime from intent or Firebase
        countdownTime = getIntent().getLongExtra("countdownTime", 0);

        new CountDownTimer(countdownTime, 1000) {
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
                isCountdownFinished = true;
                HomeBtn.setEnabled(true);
                HomeBtn.setOnClickListener(v -> {
                    Intent intent = new Intent(AlmostVotingActivity.this, DashboardActivity.class);
                    intent.putExtra("fromAlmostVoting", true); // Indicate that the intent is from AlmostVotingActivity
                    startActivity(intent);
                });
            }
        }.start();
    }

    public void exitApp(){
        Toast.makeText(AlmostVotingActivity.this, "Exiting the app.", Toast.LENGTH_SHORT).show();
        finishAffinity();
    }
}
