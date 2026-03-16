package com.example.d_vote;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.concurrent.Executor;

public class ElectionActivity extends AppCompatActivity {

    LinearLayout voteLayout;
    Button btnPartyA, btnPartyB, btnPartyC;
    DatabaseReference databaseRef;
    FirebaseUser currentUser;

    boolean hasVoted = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_election);

        voteLayout = findViewById(R.id.vote_layout);
        btnPartyA = findViewById(R.id.btn_party_a);
        btnPartyB = findViewById(R.id.btn_party_b);
        btnPartyC = findViewById(R.id.btn_party_c);
        voteLayout.setVisibility(View.GONE);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        databaseRef = FirebaseDatabase.getInstance().getReference();

        checkIfAlreadyVoted();
    }

    private void checkIfAlreadyVoted() {
        String uid = currentUser.getUid();
        databaseRef.child("Voters").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    hasVoted = true;
                    Toast.makeText(ElectionActivity.this, "You have already voted", Toast.LENGTH_LONG).show();
                    finish(); // exit or show a message
                } else {
                    authenticateBiometric();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ElectionActivity.this, "Error checking vote status", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void authenticateBiometric() {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate() != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Biometric not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        voteLayout.setVisibility(View.VISIBLE);
                        setupVoteButtons();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(ElectionActivity.this, "Authentication error", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(ElectionActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm Your Identity")
                .setSubtitle("Authenticate to vote")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void setupVoteButtons() {
        btnPartyA.setOnClickListener(v -> castVote("Party A"));
        btnPartyB.setOnClickListener(v -> castVote("Party B"));
        btnPartyC.setOnClickListener(v -> castVote("Party C"));
    }

    private void castVote(String partyName) {
        if (hasVoted) {
            Toast.makeText(this, "You have already voted", Toast.LENGTH_SHORT).show();
            return;
        }

        // Record vote
        DatabaseReference partyRef = databaseRef.child("Votes").child(partyName);
        partyRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long currentVotes = currentData.getValue(Long.class);
                if (currentVotes == null) currentVotes = 0L;
                currentData.setValue(currentVotes + 1);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (committed) {
                    databaseRef.child("Voters").child(currentUser.getUid()).setValue(true);
                    Toast.makeText(ElectionActivity.this, "Vote casted for " + partyName, Toast.LENGTH_SHORT).show();
                    finish(); // prevent voting again
                } else {
                    Toast.makeText(ElectionActivity.this, "Failed to cast vote", Toast.LENGTH_SHORT).show();
                }
            }
            });
        }
    }

