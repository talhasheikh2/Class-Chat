package com.example.classchat.DB;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object (DAO) for Student entity.
 */
@Dao
public interface StudentDao {

    @Query("SELECT * FROM Student WHERE courseId=:courseId")
    List<Student> getAllByCourseId(String courseId);

    @Query("SELECT * FROM Student WHERE studentId=:studentId")
    Student getStudentFromId(String studentId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Student... students);

    @Query("SELECT COUNT(*) FROM Student")
    int countStudents();

    @Query("SELECT COUNT(*) FROM Student WHERE regNo=:regNo")
    int checkRegNoUnique(String regNo);

    @Query("DELETE FROM Student WHERE courseId=:courseId AND studentId=:studentId")
    void deleteByStudentId(String courseId, String studentId);

    @Query("DELETE FROM Student WHERE courseId=:courseId AND regNo=:regNo")
    void deleteByStudentRegNo(String courseId, String regNo);

    @Query("DELETE FROM Student")
    void deleteAllStudents();

    // Add additional queries as needed for your application
}
