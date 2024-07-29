package com.example.classchat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.classchat.Utilities.ImagePicker;
import com.example.classchat.DB.AppDatabase;
import com.example.classchat.DB.Student;
import com.google.gson.Gson;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
//import com.pixplicity.easyprefs.library.Prefs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class TakeAttendance extends AppCompatActivity {

    private static final int PICK_IMAGE_ID = 200;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "com.example.classchat.preferences";
    private CircleImageView takenImage;

    private TextView resultText;
    private String personGroupId;

    private ListView identifiedStudentsListView;

    private boolean isFirstAttendance = true;
    private List<Student> identifiedStudents = new ArrayList<>();
    private List<String> studentIdAttendanceIncremented = new ArrayList<>();
    private List<UUID> faceIds;
    boolean imageSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_attendance);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(100);

        resultText = findViewById(R.id.resultText);

        takenImage = findViewById(R.id.takenImage);
        takenImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.takeAttendanceProgress).setVisibility(View.VISIBLE);
                identifiedStudentsListView.setVisibility(View.GONE);

                Intent chooseImageIntent = ImagePicker.getPickImageIntent(getApplicationContext(), getString(R.string.pick_image_intent_text));
                startActivityForResult(chooseImageIntent, PICK_IMAGE_ID);
            }
        });

        identifiedStudentsListView = findViewById(R.id.identifiedStudentsListView);
    }

    @Override
    protected void onStart() {
        super.onStart();

        personGroupId = sharedPreferences.getString("courseId", "");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_ID) {
            final Bitmap bitmap = ImagePicker.getImageFromResult(this, resultCode, data);
            if (bitmap != null) {
                imageSelected = true;

                if (isFirstAttendance)
                    identifiedStudents.clear();

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

                takenImage.setImageBitmap(bitmap);

                new DetectionTask().execute(inputStream);

                isFirstAttendance = false;
            } else {
                imageSelected = false;
                takenImage.setImageResource(R.drawable.attendance_logo);

                findViewById(R.id.takeAttendanceProgress).setVisibility(View.GONE);
                identifiedStudentsListView.setVisibility(View.VISIBLE);
            }
        }
    }

    private class DetectionTask extends AsyncTask<InputStream, Void, Face[]> {
        @Override
        protected Face[] doInBackground(InputStream... params) {
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try {
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,      /* Whether to return face landmarks */
                        null);      /* Which face attributes to analyze */
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("FaceDetection", e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Face[] faces) {
            if (faces != null) {
                if (faces.length == 0) {
                    Toast.makeText(TakeAttendance.this, "No faces detected in the picture", Toast.LENGTH_SHORT).show();
                    takenImage.setImageResource(R.drawable.attendance_logo);
                } else {
                    faceIds = new ArrayList<>();
                    for (Face face : faces) {
                        faceIds.add(face.faceId);
                    }
                    new TrainPersonGroupTask().execute(personGroupId);
                }
            } else {
                Toast.makeText(TakeAttendance.this, "No faces detected in the picture", Toast.LENGTH_SHORT).show();
                takenImage.setImageResource(R.drawable.attendance_logo);
            }
            findViewById(R.id.takeAttendanceProgress).setVisibility(View.GONE);
            identifiedStudentsListView.setVisibility(View.VISIBLE);
        }
    }

    private class TrainPersonGroupTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try {
                faceServiceClient.trainLargePersonGroup(params[0]);
                return params[0];
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("TrainPersonGroup", e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (s == null) {
                Toast.makeText(TakeAttendance.this, "The Person Group could not be trained", Toast.LENGTH_SHORT).show();
                takenImage.setImageResource(R.drawable.attendance_logo);
            } else {
                new IdentificationTask().execute(faceIds.toArray(new UUID[0]));
            }
        }
    }

    private class IdentificationTask extends AsyncTask<UUID, Void, IdentifyResult[]> {
        @Override
        protected IdentifyResult[] doInBackground(UUID... params) {
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try {
                TrainingStatus trainingStatus = faceServiceClient.getLargePersonGroupTrainingStatus(personGroupId);
                if (!trainingStatus.status.toString().equals("Succeeded")) {
                    return null;
                }
                return faceServiceClient.identityInLargePersonGroup(personGroupId, params, 1);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("IdentificationTask", e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(IdentifyResult[] identifyResults) {
            if (identifyResults != null) {
                List<String> personIdsOfIdentified = new ArrayList<>();
                for (IdentifyResult identifyResult : identifyResults) {
                    if (!identifyResult.candidates.isEmpty()) {
                        personIdsOfIdentified.add(identifyResult.candidates.get(0).personId.toString());
                    }
                }
                AppDatabase db = AppDatabase.getAppDatabase(getApplicationContext());
                identifiedStudents.clear();
                for (String personId : personIdsOfIdentified) {
                    identifiedStudents.add(db.studentDao().getStudentFromId(personId));
                }
                Set<Student> hs = new HashSet<>(identifiedStudents);
                identifiedStudents.clear();
                identifiedStudents.addAll(hs);
                for (Student identifiedStudent : identifiedStudents) {
                    if (identifiedStudent.studentId != null && !studentIdAttendanceIncremented.contains(identifiedStudent.studentId)) {
                        db.attendanceDao().incrementAttendance(identifiedStudent.courseId, identifiedStudent.regNo);
                        studentIdAttendanceIncremented.add(identifiedStudent.studentId);
                    }
                }
                studentListAdapter = new StudentListAdapter(TakeAttendance.this, R.layout.list_identified_students_row, identifiedStudents);
                identifiedStudentsListView.setAdapter(studentListAdapter);
            } else {
                Toast.makeText(TakeAttendance.this, "No faces found in the picture. Try Again.", Toast.LENGTH_SHORT).show();
            }
            takenImage.setImageResource(R.drawable.attendance_logo);
            findViewById(R.id.takeAttendanceProgress).setVisibility(View.GONE);
            identifiedStudentsListView.setVisibility(View.VISIBLE);
        }
    }

    private StudentListAdapter studentListAdapter;

    private class StudentListAdapter extends ArrayAdapter<Student> {


        private Context context;
        private List<Student> students;

        public StudentListAdapter(Context context, int resource, List<Student> students) {
            super(context, resource, students);
            this.context = context;
            this.students = students;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.list_identified_students_row, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.studentName = convertView.findViewById(R.id.studentName);
                viewHolder.studentFaceImage = convertView.findViewById(R.id.studentFaceImage);
                viewHolder.studentRegNo = convertView.findViewById(R.id.studentRegNo);
                viewHolder.attendanceText = convertView.findViewById(R.id.attendanceText);
                viewHolder.maxAttendanceText = convertView.findViewById(R.id.maxAttendanceText);
                viewHolder.decrementAttendanceButton = convertView.findViewById(R.id.decrementAttendanceButton);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final Student student = getItem(position);

            if (student != null) {
                viewHolder.studentName.setText(student.studentName);
                viewHolder.studentRegNo.setText(student.regNo);

                AppDatabase db = AppDatabase.getAppDatabase(context);
                final int attendanceNumber = db.attendanceDao().getAttendance(student.courseId, student.regNo).attendanceNumber;
                int maxAttendance = db.courseDao().getNumberOfClasses(student.courseId);

                viewHolder.attendanceText.setText(String.valueOf(attendanceNumber));
                viewHolder.maxAttendanceText.setText("/" + maxAttendance);

                viewHolder.decrementAttendanceButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AppDatabase db = AppDatabase.getAppDatabase(context);
                        db.attendanceDao().decrementAttendance(student.courseId, student.regNo);
                        viewHolder.attendanceText.setText(String.valueOf(attendanceNumber - 1));
                        viewHolder.attendanceText.setTextColor(Color.RED);
                        viewHolder.decrementAttendanceButton.setVisibility(View.INVISIBLE);
                    }
                });

                String[] faceIDs = new Gson().fromJson(student.faceArrayJson, String[].class);

                if (faceIDs.length != 0) {
                    String photoPath = Environment.getExternalStorageDirectory() + "/Faces/" + faceIDs[0] + ".jpg";
                    if (!(new File(photoPath).exists())) {
                        viewHolder.studentFaceImage.setImageResource(R.drawable.person_icon);
                    } else {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 8;
                        final Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);
                        viewHolder.studentFaceImage.setImageBitmap(bitmap);
                    }
                }
            }

            return convertView;
        }


        private class ViewHolder {
            TextView studentName;
            CircleImageView studentFaceImage;
            TextView studentRegNo;
            TextView attendanceText;
            TextView maxAttendanceText;
            CircleImageView decrementAttendanceButton;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(TakeAttendance.this, Menu.class));
    }
}
