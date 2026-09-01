package com.offerhub.ai.repository;

import com.offerhub.ai.entity.MisclassificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MisclassificationLogRepository extends JpaRepository<MisclassificationLog, UUID> {
}