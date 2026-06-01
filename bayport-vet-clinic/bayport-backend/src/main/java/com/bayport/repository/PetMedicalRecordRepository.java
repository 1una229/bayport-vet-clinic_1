package com.bayport.repository;

import com.bayport.entity.MedicalRecordType;
import com.bayport.entity.PetMedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetMedicalRecordRepository extends JpaRepository<PetMedicalRecord, Long> {
    List<PetMedicalRecord> findByPetIdOrderByRecordDateDescCreatedAtDesc(Long petId);
    List<PetMedicalRecord> findByPetIdAndRecordTypeOrderByRecordDateDesc(Long petId, MedicalRecordType recordType);
    void deleteByPetId(Long petId);
}
