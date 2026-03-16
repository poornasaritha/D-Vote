package com.example.d_vote;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class ResultsActivity extends AppCompatActivity {

    TextView totalVotesTextView, partyAResult, partyBResult, partyCResult, winnerTextView;
    DatabaseReference databaseRef;
    FirebaseUser currentUser;

    long votesA = 0, votesB = 0, votesC = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        totalVotesTextView = findViewById(R.id.totalVotesTextView);
        partyAResult = findViewById(R.id.partyAResult);
        partyBResult = findViewById(R.id.partyBResult);
        partyCResult = findViewById(R.id.partyCResult);
        winnerTextView = findViewById(R.id.winnerTextView);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        checkIfUserHasVoted();
    }

    private void checkIfUserHasVoted() {
        DatabaseReference votersRef = FirebaseDatabase.getInstance().getReference("Voters");
        String uid = currentUser.getUid();

        votersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    fetchVoteResults();
                } else {
                    Toast.makeText(ResultsActivity.this, "You must vote to view results", Toast.LENGTH_LONG).show();
                    finish(); // Close activity
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ResultsActivity.this, "Error checking vote status", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void fetchVoteResults() {
        databaseRef = FirebaseDatabase.getInstance().getReference("Votes");

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                votesA = snapshot.child("Party A").getValue(Long.class) != null ? snapshot.child("Party A").getValue(Long.class) : 0;
                votesB = snapshot.child("Party B").getValue(Long.class) != null ? snapshot.child("Party B").getValue(Long.class) : 0;
                votesC = snapshot.child("Party C").getValue(Long.class) != null ? snapshot.child("Party C").getValue(Long.class) : 0;

                long totalVotes = votesA + votesB + votesC;

                double percentA = totalVotes > 0 ? (votesA * 100.0) / totalVotes : 0;
                double percentB = totalVotes > 0 ? (votesB * 100.0) / totalVotes : 0;
                double percentC = totalVotes > 0 ? (votesC * 100.0) / totalVotes : 0;

                totalVotesTextView.setText("Total Votes: " + totalVotes);
                partyAResult.setText("Party A: " + votesA + " votes (" + String.format("%.2f", percentA) + "%)");
                partyBResult.setText("Party B: " + votesB + " votes (" + String.format("%.2f", percentB) + "%)");
                partyCResult.setText("Party C: " + votesC + " votes (" + String.format("%.2f", percentC) + "%)");

                declareWinner();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                totalVotesTextView.setText("Failed to load results.");
            }
        });
    }

    private void declareWinner() {
        if (votesA > votesB && votesA > votesC) {
            winnerTextView.setText("Winner: Party A 🏆");
        } else if (votesB > votesA && votesB > votesC) {
            winnerTextView.setText("Winner: Party B 🏆");
        } else if (votesC > votesA && votesC > votesB) {
            winnerTextView.setText("Winner: Party C 🏆");
        } else {
            winnerTextView.setText("It's a Tie 🤝");
        }
    }
}