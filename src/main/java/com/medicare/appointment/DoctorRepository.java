package com.medicare.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findAllByActiveTrueOrderByFullNameAsc();
    List<Doctor> findAllByActiveTrueAndSpecialtyContainingIgnoreCaseOrderByFullNameAsc(String specialty);
}
