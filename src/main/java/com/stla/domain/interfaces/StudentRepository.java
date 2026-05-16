package com.stla.domain.interfaces;

import com.stla.domain.models.Student;
import java.util.List;
import java.util.Optional;

public interface StudentRepository {
    Optional<Student> findById(String id);
    Optional<Student> findByProfileId(String profileId);
    List<Student> findAll();
    void save(Student student);
    void update(Student student);
    int countAll();
}
