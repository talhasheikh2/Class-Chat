package com.example.classchat.DB;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object (DAO) for Course entity.
 */
@Dao
public interface CourseDao {
    @Query("SELECT * FROM Course")
    List<Course> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(Course... courses);

    @Query("SELECT COUNT(*) FROM Course")
    int countCourses();

    @Query("SELECT numberOfClasses FROM Course WHERE courseId = :courseId")
    int getNumberOfClasses(String courseId);

    @Query("SELECT courseName FROM Course WHERE courseId = :courseId")
    String getCourseNameById(String courseId);

    @Query("DELETE FROM Course WHERE courseId = :courseId")
    void deleteByCourseId(String courseId);

    // Add additional queries as needed for your application
}
