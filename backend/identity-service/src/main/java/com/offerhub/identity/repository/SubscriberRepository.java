package com.offerhub.identity.repository;

import com.offerhub.identity.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriberRepository extends JpaRepository<Subscriber, UUID> {

    Optional<Subscriber> findByPhone(String phone);

    boolean existsByPhone(String phone);
}