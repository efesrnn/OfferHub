package com.offerhub.ai.repository;

import com.offerhub.ai.entity.SubscriberProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriberProfileRepository extends JpaRepository<SubscriberProfile, String> {
}