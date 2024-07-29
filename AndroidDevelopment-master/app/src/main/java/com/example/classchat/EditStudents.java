package com.example.classchat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.classchat.DB.AppDatabase;
import com.example.classchat.DB.Attendance;
import com.example.classchat.DB.Student;
import com.google.gson.Gson;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Person;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditStudents extends AppCompatActivity {

    private ListView studentListView;
    private String courseId;
    private List<Student> students = null;
    private StudentListAdapter studentListAdapter;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "com.example.classchat.preferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_students);

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView toolbarSubtitle = toolbar.findViewById(R.id.toolbar_subtitle);

        AppDatabase db = AppDatabase.getAppDatabase(this);
        toolbarSubtitle.setText(db.courseDao().getCourseNameById(sharedPreferences.getString("courseId", "")));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(100);

        findViewById(R.id.addStudent).setOnClickListener(view -> startActivity(new Intent(EditStudents.this, AddStudent.class)));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(EditStudents.this, Menu.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        students = new ArrayList<>();

        studentListView = findViewById(R.id.studentListView);
        studentListView.setEmptyView(findViewById(R.id.editStudentsListProgress));

        studentListAdapter = new StudentListAdapter(this, R.layout.list_edit_student_row, students);
        studentListView.setAdapter(studentListAdapter);

        courseId = sharedPreferences.getString("courseId", "");

        studentListView.setOnItemClickListener((adapterView, view, i, l) -> {
            // Placeholder for item click functionality
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(this::fetchAllStudents);
    }

    private void fetchAllStudents() {
        FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));

        try {
            Person[] personArray = faceServiceClient.listPersonsInLargePersonGroup(courseId);
            if (personArray == null || personArray.length == 0) {
                runOnUiThread(() -> {
                    findViewById(R.id.editStudentsListProgress).setVisibility(View.INVISIBLE);
                    Toast.makeText(EditStudents.this, "No Students Found!", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            AppDatabase db = AppDatabase.getAppDatabase(getApplicationContext());
            List<Student> allStudents = new ArrayList<>();

            for (Person person : personArray) {
                Student student = new Student(person.personId.toString(), courseId, person.name, person.userData, new Gson().toJson(person.persistedFaceIds));
                allStudents.add(student);
                db.studentDao().insertAll(student);
                db.attendanceDao().insertAll(new Attendance(student.regNo, student.courseId, 0));
            }

            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Collections.sort(allStudents, Comparator.comparing(t -> t.studentName));
                }
                updateStudentList(allStudents);
            });
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                findViewById(R.id.editStudentsListProgress).setVisibility(View.INVISIBLE);
                Toast.makeText(EditStudents.this, "No Students Found!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateStudentList(List<Student> students) {
        findViewById(R.id.editStudentsListProgress).setVisibility(View.INVISIBLE);
        studentListAdapter = new StudentListAdapter(this, R.layout.list_edit_student_row, students);
        studentListView.setAdapter(studentListAdapter);
    }

    public class StudentListAdapter extends ArrayAdapter<Student> {

        private final Context context;

        public StudentListAdapter(Context context, int resource, List<Student> students) {
            super(context, resource, students);
            this.context = context;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Student student = getItem(position);

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_edit_student_row, parent, false);
            }

            TextView studentName = convertView.findViewById(R.id.studentName);
            ImageView studentFaceImage = convertView.findViewById(R.id.studentFaceImage);
            TextView studentRegNo = convertView.findViewById(R.id.studentRegNo);

            CircleImageView deleteStudentButton = convertView.findViewById(R.id.deleteStudentButton);
            deleteStudentButton.setOnClickListener(view -> confirmDelete(student));

            assert student != null;
            studentName.setText(student.studentName);
            studentRegNo.setText(student.regNo);

            String[] faceIDs = new Gson().fromJson(student.faceArrayJson, String[].class);
            if (faceIDs.length != 0) {
                String photoPath = Environment.getExternalStorageDirectory() + "/Faces/" + faceIDs[0] + ".jpg";

                if (!(new File(photoPath).exists())) {
                    studentFaceImage.setImageResource(R.drawable.person_icon);
                } else {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 8;
                    Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);
                    studentFaceImage.setImageBitmap(bitmap);
                }
            }

            return convertView;
        }

        private void confirmDelete(Student student) {
            DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> deletePerson(student));
                } else {
                    dialog.dismiss();
                }
            };

            AlertDialog.Builder ab = new AlertDialog.Builder(EditStudents.this);
            ab.setMessage("Are you sure to delete this person?")
                    .setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener)
                    .show();
        }
    }

    private void deletePerson(Student student) {
        FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));

        try {
            faceServiceClient.deletePersonInLargePersonGroup(student.courseId, UUID.fromString(student.studentId));
            runOnUiThread(() -> {
                Toast.makeText(EditStudents.this, "Person successfully deleted.", Toast.LENGTH_LONG).show();
                removeStudentFromDatabase(student);
                onStart();
            });
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(EditStudents.this, "Person could not be deleted.", Toast.LENGTH_LONG).show());
        }
    }

    private void removeStudentFromDatabase(Student student) {
        AppDatabase db = AppDatabase.getAppDatabase(getApplicationContext());

        Student deletedStudent = db.studentDao().getStudentFromId(student.studentId);
        String[] facesArray = new Gson().fromJson(deletedStudent.faceArrayJson, String[].class);

        db.studentDao().deleteByStudentId(student.courseId, student.studentId);
        db.studentDao().deleteByStudentRegNo(student.courseId, student.regNo);
        db.attendanceDao().deleteByStudentId(student.courseId, student.regNo);

        for (String face : facesArray) {
            String photoPath = Environment.getExternalStorageDirectory() + "/Faces/" + face + ".jpg";

            File file = new File(photoPath);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
