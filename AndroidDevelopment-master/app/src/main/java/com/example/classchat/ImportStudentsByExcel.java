package com.example.classchat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.AddPersistedFaceResult;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;
import com.opencsv.CSVReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ImportStudentsByExcel extends AppCompatActivity {

    private static final String PREF_NAME = "com.example.classchat.preferences";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_students_by_excel);

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(100);

        findViewById(R.id.importCSV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<StudentDataCSV> studentDataCSVList = new ArrayList<>();

                try {
                    CSVReader reader = new CSVReader(new FileReader(Environment.getExternalStorageDirectory() + "/StudentFaces.csv"));
                    List<String[]> myEntries = reader.readAll();
                    for (String[] s : myEntries) {
                        Log.d("CSV data:", s[0] + " " + s[1]);

                        StudentDataCSV studentDataCSV = new StudentDataCSV(s[0], s[1]);
                        studentDataCSVList.add(studentDataCSV);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(ImportStudentsByExcel.this, "The specified file was not found", Toast.LENGTH_SHORT).show();
                }

                for (StudentDataCSV studentData : studentDataCSVList) {
                    File imageSrc = new File(Environment.getExternalStorageDirectory() + "/CSVImages/" + studentData.regNo + ".jpg");
                    if (imageSrc.exists()) {
                        addPersonTask(sharedPreferences.getString("courseId", ""), studentData.studentName, studentData.regNo);
                    } else {
                        Toast.makeText(ImportStudentsByExcel.this, "The image file could not be located for " + studentData.studentName, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        handleBackPress();
    }

    private void handleBackPress() {
        // Register a callback for the back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(ImportStudentsByExcel.this, AddStudent.class));
            }
        });
    }

    public class StudentDataCSV {
        public String studentName;
        public String regNo;

        public StudentDataCSV(String studentName, String regNo) {
            this.studentName = studentName;
            this.regNo = regNo;
        }
    }

    private void addPersonTask(String courseId, String studentName, String regNo) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> {
            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try {
                Log.v("", "Request: Creating Person in person group " + courseId);

                // Start the request to create a person.
                CreatePersonResult createPersonResult = faceServiceClient.createPersonInLargePersonGroup(
                        courseId, // personGroupID - courseId
                        studentName, // studentName
                        regNo // userData or regNo
                );

                String personId = createPersonResult.personId.toString();
                return personId;

            } catch (Exception e) {
                Log.v("", e.getMessage());
                return null;
            }
        });

        executor.execute(() -> {
            try {
                String personId = future.get();
                if (personId != null) {
                    Log.v("", "Response: Success. Person " + personId + " created.");
                    runOnUiThread(() -> Toast.makeText(ImportStudentsByExcel.this, "Person with personId " + personId + " successfully created", Toast.LENGTH_SHORT).show());

                    addFaceTask(personId, regNo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                executor.shutdown();
            }
        });
    }

    private void addFaceTask(String personId, String regNo) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try {
                Log.v("", "Adding face...");
                UUID personUUID = UUID.fromString(personId);
                File image = new File(Environment.getExternalStorageDirectory() + "/CSVImages/" + regNo + ".jpg");

                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath(), bmOptions);

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());

                AddPersistedFaceResult result = faceServiceClient.addPersonFaceInLargePersonGroup(
                        sharedPreferences.getString("courseId", ""),
                        personUUID,
                        imageInputStream,
                        "",
                        null
                );

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
        });

        executor.execute(() -> {
            try {
                String persistedFaceId = future.get();
                if (persistedFaceId != null) {
                    Log.v("", "Successfully added face with persistence id " + persistedFaceId);
                    runOnUiThread(() -> Toast.makeText(ImportStudentsByExcel.this, "Face with persistedFaceId " + persistedFaceId + " successfully created", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                executor.shutdown();
            }
        });
    }
}
