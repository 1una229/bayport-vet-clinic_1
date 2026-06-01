package com.bayport.service;

import com.bayport.dto.VaccinationScheduleItem;
import com.bayport.entity.MedicalRecordType;
import com.bayport.entity.Pet;
import com.bayport.entity.PetMedicalRecord;
import com.bayport.entity.Reminder;
import com.bayport.entity.ReminderType;
import com.bayport.exception.ResourceNotFoundException;
import com.bayport.repository.PetMedicalRecordRepository;
import com.bayport.repository.PetRepository;
import com.bayport.repository.ReminderRepository;
import com.bayport.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PetMedicalRecordService {

    private final PetMedicalRecordRepository recordRepository;
    private final PetRepository petRepository;
    private final ReminderRepository reminderRepository;
    private final VaccineReminderService vaccineReminderService;
    private final FileStorageService fileStorageService;

    public PetMedicalRecordService(
            PetMedicalRecordRepository recordRepository,
            PetRepository petRepository,
            ReminderRepository reminderRepository,
            VaccineReminderService vaccineReminderService,
            FileStorageService fileStorageService) {
        this.recordRepository = recordRepository;
        this.petRepository = petRepository;
        this.reminderRepository = reminderRepository;
        this.vaccineReminderService = vaccineReminderService;
        this.fileStorageService = fileStorageService;
    }

    public List<PetMedicalRecord> listByPet(Long petId) {
        requirePet(petId);
        return recordRepository.findByPetIdOrderByRecordDateDescCreatedAtDesc(petId);
    }

    public PetMedicalRecord getById(Long petId, Long recordId) {
        PetMedicalRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found"));
        if (!record.getPetId().equals(petId)) {
            throw new ResourceNotFoundException("Medical record not found for this pet");
        }
        return record;
    }

    public PetMedicalRecord create(Long petId, PetMedicalRecord record) {
        Pet pet = requirePet(petId);
        validate(record);
        record.setId(null);
        record.setPetId(petId);
        if (record.getRecordDate() == null) {
            record.setRecordDate(LocalDate.now());
        }
        PetMedicalRecord saved = recordRepository.save(record);
        if (record.getRecordType() == MedicalRecordType.VACCINATION) {
            vaccineReminderService.syncPetVaccineReminders(pet);
        }
        return saved;
    }

    public PetMedicalRecord update(Long petId, Long recordId, PetMedicalRecord updated) {
        Pet pet = requirePet(petId);
        PetMedicalRecord existing = getById(petId, recordId);
        validate(updated);
        existing.setRecordType(updated.getRecordType());
        existing.setRecordDate(updated.getRecordDate() != null ? updated.getRecordDate() : existing.getRecordDate());
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setSourceClinic(updated.getSourceClinic());
        existing.setVeterinarian(updated.getVeterinarian());
        existing.setVaccineType(updated.getVaccineType());
        existing.setDoseNumber(updated.getDoseNumber());
        existing.setNextDueDate(updated.getNextDueDate());
        existing.setDiagnosis(updated.getDiagnosis());
        existing.setTreatmentPlan(updated.getTreatmentPlan());
        existing.setTestResults(updated.getTestResults());
        existing.setMedications(updated.getMedications());
        existing.setExternalRecord(updated.isExternalRecord());
        PetMedicalRecord saved = recordRepository.save(existing);
        if (saved.getRecordType() == MedicalRecordType.VACCINATION) {
            vaccineReminderService.syncPetVaccineReminders(pet);
        }
        return saved;
    }

    public void delete(Long petId, Long recordId) {
        PetMedicalRecord existing = getById(petId, recordId);
        recordRepository.delete(existing);
    }

    public PetMedicalRecord attachFile(Long petId, Long recordId, MultipartFile file) throws IOException {
        PetMedicalRecord record = getById(petId, recordId);
        String url = fileStorageService.store(file);
        record.setAttachmentUrl(url);
        record.setAttachmentName(file.getOriginalFilename());
        return recordRepository.save(record);
    }

    public List<VaccinationScheduleItem> getVaccinationSchedule(Long petId) {
        requirePet(petId);
        Map<String, VaccinationScheduleItem> merged = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        for (PetMedicalRecord r : recordRepository.findByPetIdAndRecordTypeOrderByRecordDateDesc(petId, MedicalRecordType.VACCINATION)) {
            String key = (r.getVaccineType() != null ? r.getVaccineType() : r.getTitle()) + "|ext";
            merged.putIfAbsent(key, new VaccinationScheduleItem(
                    r.getVaccineType() != null ? r.getVaccineType() : nullToEmpty(r.getTitle()),
                    r.getRecordDate(),
                    r.getNextDueDate(),
                    statusFor(r.getNextDueDate(), today),
                    r.isExternalRecord() ? nullToEmpty(r.getSourceClinic()) : "Bayport Veterinary Clinic",
                    false
            ));
        }

        for (Reminder rem : reminderRepository.findByPetId(petId)) {
            if (rem.getType() != ReminderType.PET || !rem.isAutoGenerated()) {
                continue;
            }
            String vaccine = extractVaccineName(rem.getMessage());
            String key = vaccine + "|auto|" + rem.getDate();
            merged.put(key, new VaccinationScheduleItem(
                    vaccine,
                    null,
                    rem.getDate(),
                    rem.isSent() ? "Sent" : statusFor(rem.getDate(), today),
                    "Auto-scheduled reminder",
                    true
            ));
        }

        return merged.values().stream()
                .sorted(Comparator.comparing(VaccinationScheduleItem::nextDue, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public MedicalHistoryBundle loadHistoryBundle(Long petId) {
        Pet pet = requirePet(petId);
        List<PetMedicalRecord> records = listByPet(petId);
        List<VaccinationScheduleItem> schedule = getVaccinationSchedule(petId);
        List<Reminder> reminders = reminderRepository.findByPetId(petId);
        return new MedicalHistoryBundle(pet, records, schedule, reminders);
    }

    private Pet requirePet(Long petId) {
        return petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
    }

    private void validate(PetMedicalRecord record) {
        if (record.getRecordType() == null) {
            throw new IllegalArgumentException("Record type is required");
        }
        if (record.getTitle() == null || record.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
    }

    private static String statusFor(LocalDate due, LocalDate today) {
        if (due == null) return "Scheduled";
        if (due.isBefore(today)) return "Overdue";
        if (due.isEqual(today)) return "Due today";
        return "Upcoming";
    }

    private static String extractVaccineName(String message) {
        if (message == null) return "Vaccination";
        for (String line : message.split("\n")) {
            if (line.toLowerCase().contains("next vaccine:")) {
                return line.replaceAll("(?i).*next vaccine:\\s*", "").trim();
            }
        }
        if (message.toLowerCase().contains("rabies")) return "Anti-Rabies";
        if (message.toLowerCase().contains("5-in-1") || message.toLowerCase().contains("dhppi")) return "5-in-1";
        if (message.toLowerCase().contains("3-in-1") || message.toLowerCase().contains("fvrcp")) return "3-in-1";
        if (message.toLowerCase().contains("deworm")) return "Deworming";
        return "Vaccination";
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public record MedicalHistoryBundle(
            Pet pet,
            List<PetMedicalRecord> records,
            List<VaccinationScheduleItem> vaccinationSchedule,
            List<Reminder> reminders
    ) {}
}
