package com.stla.patterns.factory;

import com.stla.domain.enums.AppRole;
import com.stla.domain.models.*;

import java.util.UUID;

/**
 * Factory Pattern: Creates role-specific user records.
 */
public class UserFactory{

    public static Object createRoleRecord(AppRole role, Profile profile) {
        return switch (role) {
            case STUDENT -> createStudent(profile);
            case INSTRUCTOR -> createInstructor(profile);
            case ADMIN -> createAdmin(profile);
        };
    }

    public static Student createStudent(Profile profile) {
        Student s = new Student();
        s.setId(UUID.randomUUID().toString());
        s.setProfileId(profile.getId());
        s.setProfile(profile);
        return s;
    }

    public static Instructor createInstructor(Profile profile) {
        Instructor i = new Instructor();
        i.setId(UUID.randomUUID().toString());
        i.setProfileId(profile.getId());
        i.setProfile(profile);
        return i;
    }

    public static Admin createAdmin(Profile profile) {
        Admin a = new Admin();
        a.setId(UUID.randomUUID().toString());
        a.setProfileId(profile.getId());
        a.setAdminLevel(1);
        a.setPermissions("{}");
        a.setProfile(profile);
        return a;
    }
}
