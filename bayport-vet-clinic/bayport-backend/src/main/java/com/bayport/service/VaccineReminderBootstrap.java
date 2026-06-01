package com.bayport.service;

import com.bayport.entity.Pet;
import com.bayport.repository.PetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ensures auto-generated vaccine reminders exist for all pets with vaccination procedures
 * (runs once on startup).
 */
@Slf4j
@Component
public class VaccineReminderBootstrap {

    private final PetRepository petRepository;
    private final VaccineReminderService vaccineReminderService;

    public VaccineReminderBootstrap(PetRepository petRepository, VaccineReminderService vaccineReminderService) {
        this.petRepository = petRepository;
        this.vaccineReminderService = vaccineReminderService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncAllPetVaccineReminders() {
        try {
            List<Pet> pets = petRepository.findAll();
            int synced = 0;
            for (Pet pet : pets) {
                if (pet.getOwnerId() != null) {
                    vaccineReminderService.syncPetVaccineReminders(pet);
                    synced++;
                }
            }
            log.info("Vaccine reminder sync completed for {} pet(s)", synced);
        } catch (Exception e) {
            log.warn("Vaccine reminder bootstrap failed: {}", e.getMessage());
        }
    }
}
