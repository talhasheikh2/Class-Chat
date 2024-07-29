package com.example.classchat.DB;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity class representing a Course.
 */
@Entity
public class Course {
    @PrimaryKey
    @NonNull
    public String courseId;

    public String courseName;
    public String year;
    public int numberOfClasses;
    public String courseCode;

    public Course(@NonNull String courseId, String courseName, String year, int numberOfClasses, String courseCode) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.year = year;
        this.numberOfClasses = numberOfClasses;
        this.courseCode = courseCode;
    }
}
