package com.example.classchat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.classchat.DB.AppDatabase;
import com.example.classchat.Utilities.ImagePicker;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class AddStudent extends AppCompatActivity {

    String studentName;
    String regNo;

    ImageView takenImageForStudent;
    private static final int PICK_IMAGE_ID = 200;
    Bitmap bitmap;

    boolean imageTaken = false;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "com.example.classchat.preferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences=getSharedPreferences(PREF_NAME,MODE_PRIVATE);
        setContentView(R.layout.activity_add_student);

        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView toolbarSubtitle = toolbar.findViewById(R.id.toolbar_subtitle);
        toolbarSubtitle.setText((AppDatabase.getAppDatabase(this)).courseDao().getCourseNameById(sharedPreferences.getString("courseId", "")));

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(100);

        takenImageForStudent = findViewById(R.id.takenImageForStudent);

        takenImageForStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chooseImageIntent = ImagePicker.getPickImageIntent(getApplicationContext(), getString(R.string.pick_image_intent_text_student));
                startActivityForResult(chooseImageIntent, PICK_IMAGE_ID);
            }
        });

        findViewById(R.id.rotate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bitmap = ImagePicker.rotate(bitmap, 90);
                takenImageForStudent.setImageBitmap(bitmap);
            }
        });

        final Button addStudent = findViewById(R.id.addStudent);

        addStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (alreadyHasPermission()) {
                    studentName = ((EditText) findViewById(R.id.studentName)).getText().toString();
                    regNo = ((EditText) findViewById(R.id.regNo)).getText().toString();

                    AppDatabase database = AppDatabase.getAppDatabase(getApplicationContext());

                    if (!imageTaken) {
                        Toast.makeText(AddStudent.this, "Select an image for the student", Toast.LENGTH_SHORT).show();
                    } else if (studentName.equals("")) {
                        ((EditText) findViewById(R.id.studentName)).setError("Please enter a Student Name");
                        findViewById(R.id.studentName).requestFocus();
                    } else if (regNo.equals("")) {
                        ((EditText) findViewById(R.id.regNo)).setError("Please enter a Registration Number");
                        findViewById(R.id.regNo).requestFocus();
                    } else if (database.studentDao().checkRegNoUnique(regNo) == 1) {
                        ((EditText) findViewById(R.id.regNo)).setError("Registration number should be unique");
                        findViewById(R.id.regNo).requestFocus();
                    } else {
                        new AddPersonTask().execute(getSharedPreferences("helloworld" + "_preferences", MODE_PRIVATE).getString("courseId", ""), studentName, regNo);
                    }
                } else {
                    ActivityCompat.requestPermissions(AddStudent.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
            }
        });

        findViewById(R.id.importByExcel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), ImportStudentsByExcel.class));
            }
        });

        // Handle back press using OnBackPressedCallback
        handleBackPress();
    }

    private void handleBackPress() {
        // Register a callback for the back press
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(AddStudent.this, EditStudents.class));
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private boolean alreadyHasPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onStart();
            } else {
                Toast.makeText(getApplicationContext(), "Permission denied to write your External storage", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_ID) {
            bitmap = ImagePicker.getImageFromResult(this, resultCode, data);
            takenImageForStudent.setImageBitmap(bitmap);

            imageTaken = true;

            if (bitmap == null) {
                takenImageForStudent.setImageDrawable(getDrawable(R.drawable.circle_icon));
                imageTaken = false;
            } else {
                findViewById(R.id.rotate).setVisibility(View.VISIBLE);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    class AddPersonTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try {
                publishProgress("Syncing with server to add person...");
                Log.v("", "Request: Creating Person in person group" + params[0]);

                CreatePersonResult createPersonResult = faceServiceClient.createPersonInLargePersonGroup(
                        params[0], //personGroupID
                        params[1], //name
                        params[2]); //userData or regNo

                return createPersonResult.personId.toString();
            } catch (Exception e) {
                publishProgress(e.getMessage());
                Log.v("", e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String personId) {
            if (personId != null) {
                Log.v("", "Response: Success. Person " + personId + " created.");
                Toast.makeText(AddStudent.this, "Student was successfully created", Toast.LENGTH_SHORT).show();
                new AddFaceTask().execute(personId);
            }
        }
    }

    class AddFaceTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try {
                Log.v("", "Adding face...");
                UUID personId = UUID.fromString(params[0]);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());

                AddPersistedFaceResult result = faceServiceClient.addPersonFaceInLargePersonGroup(
                        getSharedPreferences("helloworld" + "_preferences", MODE_PRIVATE).getString("courseId", ""),
                        personId,
                        imageInputStream,
                        "",
                        null);

                File folder = new File(Environment.getExternalStorageDirectory(), "/Faces/");
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                File photo = new File(Environment.getExternalStorageDirectory(), "/Faces/" + result.persistedFaceId.toString() + ".jpg");
                if (photo.exists()) {
                    photo.delete();
                }

                try {
                    FileOutputStream fos = new FileOutputStream(photo.getPath());
                    fos.write(stream.toByteArray());
                    fos.close();
                    Log.v("Store face in storage", "Face stored with name " + photo.getName() + " and path " + photo.getAbsolutePath());
                } catch (java.io.IOException e) {
                    Log.e("Store face in storage", "Exception in photoCallback", e);
                }

                return result.persistedFaceId.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return e.toString();
            }
        }

        @Override
        protected void onPostExecute(String persistedFaceId) {
            Log.v("", "Successfully added face with persistence id " + persistedFaceId);
            Toast.makeText(AddStudent.this, "Face was successfully added to the student", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(AddStudent.this, EditStudents.class));
        }
    }
}
