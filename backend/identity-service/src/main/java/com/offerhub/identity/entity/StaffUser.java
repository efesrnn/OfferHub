package com.offerhub.identity.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "staff_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ElementCollection
    @CollectionTable(name = "staff_user_specialties", joinColumns = @JoinColumn(name = "staff_user_id"))
    @Column(name = "specialty")
    @Builder.Default
    private List<String> specialties = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "staff_user_regions", joinColumns = @JoinColumn(name = "staff_user_id"))
    @Column(name = "region")
    @Builder.Default
    private List<String> regions = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    private Instant lockedUntil;

    @Builder.Default
    @Column(nullable = false)
    private boolean mustChangePassword = true;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}