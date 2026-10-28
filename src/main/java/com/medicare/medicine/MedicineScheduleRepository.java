package com.medicare.medicine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface MedicineScheduleRepository extends JpaRepository<MedicineSchedule, Long> {
    List<MedicineSchedule> findAllByOwnerIdOrderByLocalTimeAsc(Long ownerId);
    Optional<MedicineSchedule> findByIdAndOwnerId(Long id, Long ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from MedicineSchedule s where s.active = true")
    List<MedicineSchedule> findActiveForUpdate();
}
