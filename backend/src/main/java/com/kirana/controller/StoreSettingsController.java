package com.kirana.controller;

import com.kirana.model.StoreSettings;
import com.kirana.repository.StoreSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/store")
@CrossOrigin(origins = "*")
public class StoreSettingsController {
    @Autowired
    private StoreSettingsRepository storeSettingsRepository;

    @GetMapping("/settings")
    public ResponseEntity<StoreSettings> getSettings() {
        StoreSettings settings = storeSettingsRepository.findById(1)
                .orElseGet(() -> {
                    StoreSettings defaultSettings = StoreSettings.builder()
                            .id(1)
                            .isOpen(true)
                            .storeAnnouncement("Welcome to Smart Kirana Store!")
                            .build();
                    return storeSettingsRepository.save(defaultSettings);
                });
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, Object> body) {
        StoreSettings settings = storeSettingsRepository.findById(1).orElseThrow();
        if (body.containsKey("isOpen")) {
            settings.setIsOpen((Boolean) body.get("isOpen"));
        }
        if (body.containsKey("storeAnnouncement")) {
            settings.setStoreAnnouncement((String) body.get("storeAnnouncement"));
        }
        settings.setUpdatedAt(LocalDateTime.now());
        StoreSettings updated = storeSettingsRepository.save(settings);
        return ResponseEntity.ok(updated);
    }
}
