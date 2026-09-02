package com.offerhub.identity.repository;

import com.offerhub.identity.entity.Role;
import com.offerhub.identity.entity.StaffUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StaffUserRepository extends JpaRepository<StaffUser, UUID> {

    Optional<StaffUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);
}