package com.customsdocgen.customsdocgen.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class FieldValuesHistoryService {

    private static final String HISTORY_FILE = "field-values-history.json";
    private static final Logger log = LoggerFactory.getLogger(FieldValuesHistoryService.class);
    private static final int MAX_HISTORY_PER_FIELD = 50;
    
    private final ObjectMapper objectMapper;
    private Map<String, List<String>> fieldValuesHistory;

    public FieldValuesHistoryService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.fieldValuesHistory = new HashMap<>();
    }

    @PostConstruct
    public void init() {
        loadHistory();
    }


    private void loadHistory() {
        File historyFile = new File(HISTORY_FILE);
        
        if (historyFile.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(historyFile);
                 java.io.InputStreamReader reader = new java.io.InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8)) {
                @SuppressWarnings("unchecked")
                Map<String, List<String>> loaded = objectMapper.readValue(reader, Map.class);
                if (loaded != null) {
                    fieldValuesHistory = loaded;
                }
                log.info("Field values history loaded: {} fields", Integer.valueOf(fieldValuesHistory.size()));
            } catch (IOException e) {
                log.warn("Failed to read field values history, starting with empty history: {}", e.getMessage());
                fieldValuesHistory = new HashMap<>();
            }
        } else {
            log.info("Field values history file not found, starting with empty history");
            fieldValuesHistory = new HashMap<>();
        }
    }


    private void saveHistory() {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(HISTORY_FILE);
             java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8)) {
            objectMapper.writeValue(writer, fieldValuesHistory);
        } catch (IOException e) {
            log.warn("Failed to save field values history: {}", e.getMessage());
        }
    }


    public void addValue(String fieldId, String value) {
        if (fieldId == null || value == null || value.trim().isEmpty()) {
            return;
        }
        
        String trimmedValue = value.trim();
        fieldValuesHistory.computeIfAbsent(fieldId, k -> new ArrayList<>());
        
        List<String> values = fieldValuesHistory.get(fieldId);

        values.remove(trimmedValue);

        values.add(0, trimmedValue);

        if (values.size() > MAX_HISTORY_PER_FIELD) {
            values.subList(MAX_HISTORY_PER_FIELD, values.size()).clear();
        }
        
        saveHistory();
    }


    public List<String> getHistory(String fieldId) {
        return fieldValuesHistory.getOrDefault(fieldId, new ArrayList<>());
    }


    public void saveValuesForFields(Map<String, String> fieldValues, Map<String, Boolean> fieldRememberFlags) {
        for (Map.Entry<String, String> entry : fieldValues.entrySet()) {
            String fieldId = entry.getKey();
            String value = entry.getValue();

            if (fieldRememberFlags != null && fieldRememberFlags.getOrDefault(fieldId, Boolean.FALSE)) {
                addValue(fieldId, value);
            }
        }
    }


    public Map<String, List<String>> getAllHistory() {
        return new HashMap<>(fieldValuesHistory);
    }
    

    public boolean removeValue(String fieldId, String value) {
        if (fieldId == null || value == null) {
            return false;
        }
        
        List<String> values = fieldValuesHistory.get(fieldId);
        if (values == null) {
            return false;
        }

        boolean removed = values.remove(value);
        if (removed) {
            if (values.isEmpty()) {
                fieldValuesHistory.remove(fieldId);
            }
            saveHistory();
        }
        
        return removed;
    }
}

