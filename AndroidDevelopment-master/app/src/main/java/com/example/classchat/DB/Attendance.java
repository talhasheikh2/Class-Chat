package com.example.classchat.DB;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

/**
 * Entity class representing Attendance for a Student in a Course.
 */
@Entity(primaryKeys = {"regNo", "courseId"},
        foreignKeys = {
                @ForeignKey(entity = Student.class, parentColumns = "regNo", childColumns = "regNo", onDelete = CASCADE),
                @ForeignKey(entity = Course.class, parentColumns = "courseId", childColumns = "courseId", onDelete = CASCADE)
        })
public class Attendance {
    @NonNull
    public String regNo;
    @NonNull
    public String courseId;
    public int attendanceNumber;

    public Attendance(String regNo, String courseId, int attendanceNumber) {
        this.regNo = regNo;
        this.courseId = courseId;
        this.attendanceNumber = attendanceNumber;
    }
}
