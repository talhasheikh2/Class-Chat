package com.example.classchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.classchat.DB.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class SignIn extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    Button signIn;
    EditText email, password;
    TextView notSignedUp;
    SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "com.example.classchat.preferences";

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        setSupportActionBar(findViewById(R.id.toolbar));

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(100);

        mAuth = FirebaseAuth.getInstance();

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        email = findViewById(R.id.signInEmail);
        password = findViewById(R.id.signInPassword);

        signIn = findViewById(R.id.signIn);
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d("", "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d("", "onAuthStateChanged:signed_out");
                }
            }
        };
        ///////////if not acount created /////////////
        notSignedUp=findViewById(R.id.notSignedUp);
        notSignedUp.setText(fromHtml(getString(R.string.notSignedUp)));
        notSignedUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), SignUp.class);
                startActivity(i);
            }
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // User is signed in
            startActivity(new Intent(SignIn.this, Menu.class));
        }
        
    }

    public void signIn(){
        String emailText = email.getText().toString().trim();
        String passText = password.getText().toString().trim();

        if(emailText.isEmpty()){
            email.setError("Please enter an Email Address");
            email.requestFocus();
            return;
        }

        if(passText.isEmpty()){
            password.setError("Please enter a Password");
            password.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(emailText, passText).
                addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){

                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            FirebaseDatabase d = FirebaseDatabase.getInstance();
                            DatabaseReference databaseReference = d.getReference().child("users").child(user.getUid());

                            databaseReference.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    User currentUser = new User();
                                    for (DataSnapshot ds: dataSnapshot.getChildren()){
                                        switch(ds.getKey()){
                                            case "email":
                                                currentUser.setEmail((String)ds.getValue());
                                                break;
                                            case "fullName":
                                                currentUser.setFullName((String)ds.getValue());
                                                break;
                                            case "courseIds":

                                                List<String> courseIds = new ArrayList<>();
                                                for (DataSnapshot courseData: ds.getChildren()){
                                                    courseIds.add((String)courseData.getValue());
                                                }
                                                currentUser.setCourseIds(courseIds);
                                                break;
                                        }
                                    }

                                    if (currentUser.getCourseIds()==null) currentUser.setCourseIds(new ArrayList<String>());
                                    Log.d("SignIn", currentUser.toString());

                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("UserID", new Gson().toJson(currentUser.getCourseIds()));
                                    editor.putString("UserEmail", currentUser.getEmail());
                                    editor.putString("UserDisplayName", currentUser.getFullName());
                                    editor.putString("UserCourseIds", new Gson().toJson(currentUser.getCourseIds()));
                                    editor.apply();

                                    Intent i = new Intent(SignIn.this, Menu.class);
                                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    Toast.makeText(SignIn.this,"sign in as"+emailText , Toast.LENGTH_SHORT).show();
                                    startActivity(i);
                                    finish();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });


                        }
                        else{
                            Toast.makeText(SignIn.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("SignIn", "signInWithEmailAndPassword:failure", task.getException());
                        }
                    }
                });
    }
    public static Spanned fromHtml(String html){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }
}
