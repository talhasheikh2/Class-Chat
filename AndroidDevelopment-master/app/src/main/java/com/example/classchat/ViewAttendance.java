package com.example.classchat;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.classchat.DB.AppDatabase;
import com.example.classchat.DB.Attendance;
import com.example.classchat.DB.Student;
import com.google.gson.Gson;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewAttendance extends Fragment {

    private ListView viewAttendanceList;
    private StudentListAdapter studentListAdapter;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "com.example.classchat.preferences";

    private static final int PERMISSION_REQUEST_CODE = 1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        sharedPreferences = getActivity().getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        return inflater.inflate(R.layout.content_view_attendance, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewAttendanceList = view.findViewById(R.id.viewAttendanceList);
    }

    @Override
    public void onStart() {

        super.onStart();
        if (alreadyHasPermission()) {
            getAllStudents();
        } else {
            requestPermission();
        }
    }

    private boolean alreadyHasPermission() {
        int result = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getAllStudents();
            } else {
                Toast.makeText(requireContext(), "Permission denied to read your External storage", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void getAllStudents() {
        AppDatabase db = AppDatabase.getAppDatabase(requireContext());
        List<Student> allStudents = db.studentDao().getAllByCourseId(sharedPreferences.getString("courseId", ""));

        if (allStudents.isEmpty()) {
            viewAttendanceList.setVisibility(View.GONE);
            requireActivity().findViewById(R.id.noStudentsFoundText).setVisibility(View.VISIBLE);
            requireActivity().findViewById(R.id.takeAttendance).setVisibility(View.GONE);
        } else {
            requireActivity().findViewById(R.id.noStudentsFoundText).setVisibility(View.GONE);
            requireActivity().findViewById(R.id.takeAttendance).setVisibility(View.VISIBLE);
        }

        Collections.sort(allStudents, new Comparator<Student>() {
            @Override
            public int compare(Student t1, Student t2) {
                return t1.studentName.compareTo(t2.studentName);
            }
        });

        studentListAdapter = new StudentListAdapter(requireActivity(), R.layout.list_view_students_row, allStudents);
        viewAttendanceList.setAdapter(studentListAdapter);
    }

    public class StudentListAdapter extends ArrayAdapter<Student> {

        private Context context;

        public StudentListAdapter(Activity context, int resource, List<Student> students) {
            super(context, resource, students);
            this.context = context;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final Student student = getItem(position);

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_view_students_row, parent, false);
            }

            TextView studentName = convertView.findViewById(R.id.studentName);
            CircleImageView studentFaceImage = convertView.findViewById(R.id.studentFaceImage);
            TextView studentRegNo = convertView.findViewById(R.id.studentRegNo);
            TextView attendanceText = convertView.findViewById(R.id.attendanceText);
            TextView maxAttendanceText = convertView.findViewById(R.id.maxAttendanceText);

            if (student != null) {
                studentName.setText(student.studentName);
                studentRegNo.setText(student.regNo);

                AppDatabase db = AppDatabase.getAppDatabase(context);
                Attendance attendance = db.attendanceDao().getAttendance(student.courseId, student.regNo);
                if (attendance == null) {
                    db.attendanceDao().insertAll(new Attendance(student.regNo, student.courseId, 0));
                }

                int attendanceNumber = db.attendanceDao().getAttendance(student.courseId, student.regNo).attendanceNumber;
                int maxAttendance = db.courseDao().getNumberOfClasses(student.courseId);

                attendanceText.setText(String.valueOf(attendanceNumber));
                maxAttendanceText.setText("/" + maxAttendance);

                String[] faceIDs = new Gson().fromJson(student.faceArrayJson, String[].class);

                if (faceIDs.length > 0) {
                    String photoPath = Environment.getExternalStorageDirectory() + "/Faces/" + faceIDs[0] + ".jpg";

                    File photoFile = new File(photoPath);
                    if (photoFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
                        studentFaceImage.setImageBitmap(bitmap);
                    } else {
                        studentFaceImage.setImageResource(R.drawable.person_icon);
                    }
                } else {
                    studentFaceImage.setImageResource(R.drawable.person_icon);
                }
            }

            return convertView;
        }
    }
}
