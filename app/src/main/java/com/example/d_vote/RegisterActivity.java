package com.example.d_vote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    EditText etName, etEmail, etPassword, etDOB, etAge;
    Button btnRegister;
    Calendar dobCalendar;

    FirebaseAuth auth;
    DatabaseReference dbRef;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.nameEditText);
        etEmail = findViewById(R.id.emailEditText);
        etPassword = findViewById(R.id.passwordEditText); // Make sure to add this in XML
        etDOB = findViewById(R.id.dobEditText);
        etAge = findViewById(R.id.ageEditText);
        btnRegister = findViewById(R.id.registerButtton);

        auth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("users");

        etDOB.setInputType(InputType.TYPE_NULL);
        etDOB.setFocusable(false);

        etDOB.setOnClickListener(v -> showDatePicker());
        btnRegister.setOnClickListener(v -> validateAndRegister());
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    dobCalendar = Calendar.getInstance();
                    dobCalendar.set(selectedYear, selectedMonth, selectedDay);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    etDOB.setText(sdf.format(dobCalendar.getTime()));
                },
                year, month, day
        );
        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePicker.show();
    }

    private void validateAndRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim(); // Make sure this EditText exists in layout
        String dobInput = etDOB.getText().toString().trim();
        String ageInput = etAge.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || dobInput.isEmpty() || ageInput.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int enteredAge;
        try {
            enteredAge = Integer.parseInt(ageInput);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid age format", Toast.LENGTH_SHORT).show();
            return;
        }

        int calculatedAge = calculateAgeFromDOB(dobCalendar);
        if (calculatedAge < 18) {
            Toast.makeText(this, "You must be at least 18 years old", Toast.LENGTH_SHORT).show();
            return;
        }

        if (enteredAge != calculatedAge) {
            Toast.makeText(this, "Entered age does not match Date of Birth", Toast.LENGTH_SHORT).show();
            return;
        }

        // Proceed with Firebase registration
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            // Save extra user data
                            UserModel userModel = new UserModel(name, email, dobInput, enteredAge, false);
                            dbRef.child(uid).setValue(userModel)
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(this, "Registration successful ✅", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Auth failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private int calculateAgeFromDOB(Calendar dob) {
        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    // User model class
    public static class UserModel {
        public String name, email, dob;
        public int age;
        public boolean voted;

        public UserModel() {} // Needed for Firebase

        public UserModel(String name, String email, String dob, int age, boolean voted) {
            this.name = name;
            this.email = email;
            this.dob = dob;
            this.age = age;
            this.voted = voted;
        }
    }
}

