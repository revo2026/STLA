package com.stla.patterns;

import com.stla.domain.enums.AppRole;
import com.stla.domain.models.*;
import com.stla.patterns.factory.UserFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Factory Pattern — UserFactory.
 */
@DisplayName("Factory Pattern: UserFactory")
class FactoryPatternTest {

    private Profile createTestProfile() {
        Profile p = new Profile();
        p.setId("test-profile-id");
        p.setFullName("Test User");
        p.setEmail("test@stla.com");
        p.setRole(AppRole.STUDENT);
        return p;
    }

    @Test
    @DisplayName("Should create Student from Profile")
    void shouldCreateStudent() {
        Profile p = createTestProfile();
        Student student = UserFactory.createStudent(p);

        assertNotNull(student);
        assertNotNull(student.getId());
        assertEquals(p.getId(), student.getProfileId());
        assertEquals(p, student.getProfile());
    }

    @Test
    @DisplayName("Should create Instructor from Profile")
    void shouldCreateInstructor() {
        Profile p = createTestProfile();
        p.setRole(AppRole.INSTRUCTOR);
        Instructor instructor = UserFactory.createInstructor(p);

        assertNotNull(instructor);
        assertNotNull(instructor.getId());
        assertEquals(p.getId(), instructor.getProfileId());
    }

    @Test
    @DisplayName("Should create Admin from Profile")
    void shouldCreateAdmin() {
        Profile p = createTestProfile();
        p.setRole(AppRole.ADMIN);
        Admin admin = UserFactory.createAdmin(p);

        assertNotNull(admin);
        assertNotNull(admin.getId());
        assertEquals(1, admin.getAdminLevel());
        assertEquals("{}", admin.getPermissions());
    }

    @Test
    @DisplayName("createRoleRecord should dispatch correctly by role")
    void shouldDispatchByRole() {
        Profile p = createTestProfile();

        p.setRole(AppRole.STUDENT);
        assertInstanceOf(Student.class, UserFactory.createRoleRecord(AppRole.STUDENT, p));

        p.setRole(AppRole.INSTRUCTOR);
        assertInstanceOf(Instructor.class, UserFactory.createRoleRecord(AppRole.INSTRUCTOR, p));

        p.setRole(AppRole.ADMIN);
        assertInstanceOf(Admin.class, UserFactory.createRoleRecord(AppRole.ADMIN, p));
    }

    @Test
    @DisplayName("Each call should produce unique IDs")
    void shouldProduceUniqueIds() {
        Profile p = createTestProfile();
        Student s1 = UserFactory.createStudent(p);
        Student s2 = UserFactory.createStudent(p);
        assertNotEquals(s1.getId(), s2.getId());
    }
}
