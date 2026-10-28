package com.medicare.caregiver;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CaregiverRepository extends JpaRepository<CaregiverRelation, Long> {
    List<CaregiverRelation> findAllByPatientId(Long patientId);
    List<CaregiverRelation> findAllByCaregiverId(Long caregiverId);
    List<CaregiverRelation> findAllByCaregiverIdAndStatus(Long caregiverId, String status);
    Optional<CaregiverRelation> findByIdAndPatientId(Long id, Long patientId);
    Optional<CaregiverRelation> findByIdAndCaregiverId(Long id, Long caregiverId);
    Optional<CaregiverRelation> findByPatientIdAndCaregiverIdAndStatus(Long patientId, Long caregiverId, String status);
    Optional<CaregiverRelation> findByPatientIdAndCaregiverId(Long patientId, Long caregiverId);
}
