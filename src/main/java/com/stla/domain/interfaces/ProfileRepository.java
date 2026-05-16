package com.stla.domain.interfaces;

import com.stla.domain.models.Profile;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Profile CRUD operations.
 */
public interface ProfileRepository {
    Optional<Profile> findById(String id);
    Optional<Profile> findByEmail(String email);
    List<Profile> findAll();
    List<Profile> findByRole(String role);
    void save(Profile profile);
    void update(Profile profile);
    void updateAvatarUrl(String profileId, String avatarUrl);
    void updateLastLogin(String id);
    void deactivate(String id);
    void activate(String id);
    int countAll();
    int countByRole(String role);
}
