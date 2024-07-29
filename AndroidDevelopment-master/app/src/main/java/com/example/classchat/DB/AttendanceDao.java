package com.example.classchat.DB;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object (DAO) for Attendance entity.
 */
@Dao
public interface AttendanceDao {

    @Query("SELECT * FROM Attendance WHERE courseId = :courseId AND regNo = :regNo LIMIT 1")
    Attendance getAttendance(String courseId, String regNo);

    @Query("UPDATE Attendance SET attendanceNumber = attendanceNumber + 1 WHERE courseId = :courseId AND regNo = :regNo")
    void incrementAttendance(String courseId, String regNo);

    @Query("UPDATE Attendance SET attendanceNumber = attendanceNumber - 1 WHERE courseId = :courseId AND regNo = :regNo")
    void decrementAttendance(String courseId, String regNo);

    @Query("UPDATE Attendance SET attendanceNumber = :attendanceNumber WHERE courseId = :courseId AND regNo = :regNo")
    void setAttendance(String courseId, String regNo, int attendanceNumber);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(Attendance... attendances);

    @Query("DELETE FROM Attendance WHERE courseId = :courseId AND regNo = :regNo")
    void deleteByStudentId(String courseId, String regNo);

    // Add additional queries as needed for your application
}
