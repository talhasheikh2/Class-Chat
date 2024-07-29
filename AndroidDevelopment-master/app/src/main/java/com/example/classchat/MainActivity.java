package com.example.classchat;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private Button goToAppButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        goToAppButton = findViewById(R.id.goToAppButton);

        goToAppButton.setOnClickListener(view -> checkUserStatus());

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setElevation(100);
        }

        // Handle back button press using OnBackPressedDispatcher
        handleBackPress();
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // User is signed in, navigate to MyCourses activity
            Intent intent = new Intent(MainActivity.this, Menu.class);
            startActivity(intent);
            finish();
        } else {
            // No user is signed in, navigate to SignUp activity
            Intent intent = new Intent(MainActivity.this, SignIn.class);
            startActivity(intent);
            finish();
        }
    }

    private void handleBackPress() {
        // Register a callback for the back press
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                // Custom back press behavior
                moveTaskToBack(true);
            }
        };

        // Add the callback to the dispatcher
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
}
