package com.example.classchat.DB;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity class representing a Student.
 */
@Entity(indices = {@Index(value = "regNo", unique = true)})
public class Student {

    @PrimaryKey(autoGenerate = true)
    public int id; // Room requires a primary key field

    public String studentId;
    public String courseId;
    public String studentName;

    @NonNull
    public String regNo; // No @PrimaryKey annotation here, but it is unique due to the index

    public String faceArrayJson;

    // Constructor
    public Student(String studentId, String courseId, String studentName, @NonNull String regNo, String faceArrayJson) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.studentName = studentName;
        this.regNo = regNo;
        this.faceArrayJson = faceArrayJson;
    }

    // Getters and setters if needed

    @Override
    public int hashCode() {
        return studentId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Student student = (Student) obj;

        return studentId.equals(student.studentId);
    }
}
