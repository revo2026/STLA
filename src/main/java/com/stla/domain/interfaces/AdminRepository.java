package com.stla.domain.interfaces;

import com.stla.domain.models.Admin;
import java.util.Optional;

public interface AdminRepository {
    Optional<Admin> findById(String id);
    Optional<Admin> findByProfileId(String profileId);
    void save(Admin admin);
    void update(Admin admin);
}
