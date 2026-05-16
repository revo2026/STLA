package com.stla.services;

import com.stla.core.session.SessionManager;
import com.stla.data.repositories.InstructorRepositoryImpl;
import com.stla.data.repositories.ProfileRepositoryImpl;
import com.stla.domain.models.Instructor;
import com.stla.domain.models.Profile;
import com.stla.patterns.observer.AppEvent;
import com.stla.patterns.observer.EventBus;

import java.io.File;
import java.util.Optional;

/**
 * Service for instructor-specific operations.
 * Controllers call this service; service delegates to repositories and storage.
 */
public class InstructorService {

    private final InstructorRepositoryImpl instructorRepo = new InstructorRepositoryImpl();
    private final ProfileRepositoryImpl profileRepo = new ProfileRepositoryImpl();
    private final SupabaseStorageService storageService = SupabaseStorageService.getInstance();

    /**
     * Get current instructor + profile from session.
     */
    public Instructor getCurrentInstructorProfile() {
        SessionManager session = SessionManager.getInstance();
        Instructor inst = session.getCurrentInstructor();
        if (inst != null) {
            inst.setProfile(session.getCurrentProfile());
        }
        return inst;
    }

    /**
     * Re-fetch instructor record from DB and update session.
     */
    public Instructor refreshInstructorFromDb() {
        SessionManager session = SessionManager.getInstance();
        String profileId = session.getCurrentUserId();
        if (profileId == null) return null;

        Optional<Instructor> opt = instructorRepo.findByProfileId(profileId);
        opt.ifPresent(inst -> {
            Instructor current = session.getCurrentInstructor();
            if (current != null) {
                current.setVerificationStatus(inst.getVerificationStatus());
                current.setRejectionReason(inst.getRejectionReason());
                current.setIdFrontUrl(inst.getIdFrontUrl());
                current.setIdBackUrl(inst.getIdBackUrl());
                current.setExperienceCertificateUrl(inst.getExperienceCertificateUrl());
                current.setCvUrl(inst.getCvUrl());
                current.setInstructorBio(inst.getInstructorBio());
                current.setTitle(inst.getTitle());
                current.setExpertiseTags(inst.getExpertiseTags());
                current.setYearsExperience(inst.getYearsExperience());
                current.setTotalCourses(inst.getTotalCourses());
                current.setTotalStudents(inst.getTotalStudents());
                current.setRatingAvg(inst.getRatingAvg());
            }
        });
        return session.getCurrentInstructor();
    }

    /**
     * Upload verification documents to Supabase Storage.
     * Returns String[] { idFrontUrl, idBackUrl, certUrl, cvUrl }.
     */
    public String[] uploadVerificationDocuments(File idFront, File idBack, File cert, File cv, String profileId) {
        String[] urls = new String[4];
        if (idFront != null) {
            urls[0] = storageService.uploadInstructorDocument(idFront, profileId, "id_front");
        }
        if (idBack != null) {
            urls[1] = storageService.uploadInstructorDocument(idBack, profileId, "id_back");
        }
        if (cert != null) {
            urls[2] = storageService.uploadInstructorDocument(cert, profileId, "experience_certificate");
        }
        if (cv != null) {
            urls[3] = storageService.uploadInstructorCV(cv, profileId);
        }
        return urls;
    }

    /**
     * Save document URLs to instructor DB record.
     */
    public void saveVerificationDocuments(String instructorId, String idFrontUrl, String idBackUrl, String certUrl, String cvUrl) {
        instructorRepo.updateVerificationDocuments(instructorId, idFrontUrl, idBackUrl, certUrl, cvUrl);
    }

    /**
     * Resubmit verification documents — updates docs + resets status to PENDING.
     */
    public void resubmitVerificationDocuments(String instructorId, String idFrontUrl, String idBackUrl, String certUrl, String cvUrl) {
        instructorRepo.updateVerificationDocuments(instructorId, idFrontUrl, idBackUrl, certUrl, cvUrl);
        instructorRepo.updateVerificationStatus(instructorId, "PENDING", null);

        // Update session
        SessionManager session = SessionManager.getInstance();
        if (session.getCurrentInstructor() != null) {
            session.getCurrentInstructor().setVerificationStatus("PENDING");
            session.getCurrentInstructor().setRejectionReason(null);
            session.getCurrentInstructor().setIdFrontUrl(idFrontUrl);
            session.getCurrentInstructor().setIdBackUrl(idBackUrl);
            session.getCurrentInstructor().setExperienceCertificateUrl(certUrl);
            session.getCurrentInstructor().setCvUrl(cvUrl);
        }

        EventBus.getInstance().publish(new AppEvent(
            AppEvent.EventType.INSTRUCTOR_RESUBMITTED,
            session.getCurrentUserId(), instructorId, "Instructor resubmitted verification documents"
        ));
    }

    /**
     * Get verification status for the given instructor.
     */
    public String getVerificationStatus(String instructorId) {
        Optional<Instructor> opt = instructorRepo.findById(instructorId);
        return opt.map(Instructor::getVerificationStatus).orElse("PENDING");
    }

    /**
     * Check if instructor can create courses (must be VERIFIED).
     */
    public boolean canCreateCourse(String instructorId) {
        return instructorRepo.isInstructorVerified(instructorId);
    }

    /**
     * Validate that instructor can create a course. Throws if not verified.
     */
    public void validateInstructorCanCreateCourse(String instructorId) {
        if (!canCreateCourse(instructorId)) {
            throw new IllegalStateException("Instructor must be verified before creating or publishing courses.");
        }
    }
}
