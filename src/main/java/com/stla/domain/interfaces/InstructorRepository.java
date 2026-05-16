package com.stla.domain.interfaces;

import com.stla.domain.models.Instructor;
import java.util.List;
import java.util.Optional;

public interface InstructorRepository {
    Optional<Instructor> findById(String id);
    Optional<Instructor> findByProfileId(String profileId);
    List<Instructor> findAll();
    void save(Instructor instructor);
    void update(Instructor instructor);
    int countAll();

    // Verification methods
    void updateVerificationDocuments(String instructorId, String idFrontUrl, String idBackUrl, String certUrl, String cvUrl);
    void updateVerificationStatus(String instructorId, String status, String rejectionReason);
    List<Instructor> findPendingVerificationRequests();
    boolean isInstructorVerified(String instructorId);
}
