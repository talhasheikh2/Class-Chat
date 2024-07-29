package com.example.classchat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.classchat.DB.User;
import com.example.classchat.DB.Course;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
//import com.pixplicity.easyprefs.library.Prefs;
//
import java.util.ArrayList;
import java.util.List;

public class AddCourse extends AppCompatActivity {

    String courseName;
    String year;
    String numberOfClassesInCourses;
    String courseCode;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "com.example.classchat.preferences";
    String newCourseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);
        sharedPreferences=getSharedPreferences(PREF_NAME,MODE_PRIVATE);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(100);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.addCourse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                courseName = ((EditText)findViewById(R.id.courseName)).getText().toString();
                year = ((EditText)findViewById(R.id.year)).getText().toString();
                numberOfClassesInCourses = ((EditText)findViewById(R.id.numberOfClasses)).getText().toString();
                courseCode = ((EditText)findViewById(R.id.courseCode)).getText().toString();

                if (courseName.isEmpty()) {
                    ((EditText)findViewById(R.id.courseName)).setError("Please enter a Course Name");
                    findViewById(R.id.courseName).requestFocus();
                    return;
                } else if (year.isEmpty()) {
                    ((EditText)findViewById(R.id.year)).setError("Please enter the Year which the course is in");
                    findViewById(R.id.year).requestFocus();
                    return;
                } else if (numberOfClassesInCourses.isEmpty()) {
                    ((EditText)findViewById(R.id.numberOfClasses)).setError("Please enter the Number of Classes/Lectures in the course");
                    findViewById(R.id.numberOfClasses).requestFocus();
                    return;
                } else if (courseCode.isEmpty()) {
                    ((EditText)findViewById(R.id.courseCode)).setError("Please enter the Course Code");
                    findViewById(R.id.courseCode).requestFocus();
                    return;
                }

                CourseData newCourseData = new CourseData(courseName, year, numberOfClassesInCourses, courseCode);
                newCourseId = courseCode.replace(' ', '-').toLowerCase() + System.nanoTime();
                new AddPersonGroupTask().execute(newCourseId, courseName, (new Gson()).toJson(newCourseData)); //largeGroupId, name, userInfo

                findViewById(R.id.addCourseProgress).setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(AddCourse.this, EditCourses.class));
    }

    private class CourseData {
        String courseName;
        String year;
        String numberOfClasses;
        String courseCode;

        public CourseData(String courseName, String year, String numberOfClasses, String courseCode) {
            this.courseName = courseName;
            this.year = year;
            this.numberOfClasses = numberOfClasses;
            this.courseCode = courseCode;
        }
    }

    class AddPersonGroupTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try {
                // Start creating person group in server.
                faceServiceClient.createLargePersonGroup(
                        params[0],
                        params[1],
                        params[2]);

                return params[0];
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            findViewById(R.id.addCourseProgress).setVisibility(View.INVISIBLE);
            if (result != null) {
                // Update Firebase with new course information
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    final DatabaseReference userRef = database.getReference("users").child(user.getUid());
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            User currentUser = dataSnapshot.getValue(User.class);
                            if (currentUser != null) {
                                List<String> courseIds = currentUser.getCourseIds();
                                if (courseIds == null) {
                                    courseIds = new ArrayList<>();
                                }
                                courseIds.add(newCourseId);
                                currentUser.setCourseIds(courseIds);
                                userRef.setValue(currentUser);

                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("UserCourseIds",  new Gson().toJson(currentUser.getCourseIds()));
                                editor.apply(); // Use apply() for asynchronous save, or use commit() for synchronous save

//                                Prefs.putString("UserCourseIds", new Gson().toJson(currentUser.getCourseIds()));
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Toast.makeText(AddCourse.this, "Failed to update course list", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                // Success message
                Toast.makeText(AddCourse.this, "Course successfully created", Toast.LENGTH_LONG).show();
                startActivity(new Intent(AddCourse.this, EditCourses.class));
            } else {
                // Failure message
                Toast.makeText(AddCourse.this, "The course could not be created at this time", Toast.LENGTH_LONG).show();
            }
        }
    }
}
