package com.customsdocgen.customsdocgen.services;

import com.customsdocgen.customsdocgen.models.AppConfig;
import com.customsdocgen.customsdocgen.models.FieldConfig;
import com.customsdocgen.customsdocgen.models.TemplateConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@Service
public class ConfigService {

    private static final String CONFIG_FILE = "config.json";
    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;
    private AppConfig appConfig;

    public ConfigService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        this.objectMapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);
    }

    @PostConstruct
    public void init() {
        loadConfig();
    }


    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        
        if (configFile.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile);
                 java.io.InputStreamReader reader = new java.io.InputStreamReader(fis, java.nio.charset.StandardCharsets.UTF_8)) {
                appConfig = objectMapper.readValue(reader, AppConfig.class);
                if (appConfig == null) {
                    appConfig = new AppConfig();
                }
                if (appConfig.getFields() == null) {
                    appConfig.setFields(new ArrayList<>());
                }
                if (appConfig.getTemplates() == null) {
                    appConfig.setTemplates(new ArrayList<>());
                }
                log.info("Config loaded: {} (fields: {}, templates: {})", CONFIG_FILE,
                        Integer.valueOf(appConfig.getFields().size()), 
                        Integer.valueOf(appConfig.getTemplates().size()));
            } catch (IOException e) {
                log.warn("Failed to read config.json, running without configuration: {}", e.getMessage());
                appConfig = new AppConfig();
                appConfig.setFields(new ArrayList<>());
                appConfig.setTemplates(new ArrayList<>());
            }
        } else {
            log.warn("Config not found: {}. Running without configuration.", CONFIG_FILE);
            appConfig = new AppConfig();
            appConfig.setFields(new ArrayList<>());
            appConfig.setTemplates(new ArrayList<>());
        }

        try {
            String[] templates = fileStorageService.listTemplates();
            if (templates == null || templates.length == 0) {
                log.info("Templates directory scanned: no templates found in 'templates/'");
            } else {
                log.info("Templates found ({}): {}", Integer.valueOf(templates.length), String.join(", ", templates));
            }
        } catch (Exception ex) {
            log.warn("Failed to scan templates directory: {}", ex.getMessage());
        }
    }


    public void saveConfig() {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(CONFIG_FILE);
             java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8)) {
            objectMapper.writeValue(writer, appConfig);
        } catch (IOException e) {

        }
    }

    public AppConfig getConfig() {
        return appConfig;
    }

    public void updateConfig(AppConfig newConfig) {
        this.appConfig = newConfig;
        saveConfig();
    }

    public void addField(FieldConfig field) {
        if (appConfig.getFields() == null) {
            appConfig.setFields(new ArrayList<>());
        }
        appConfig.getFields().add(field);
        saveConfig();
    }

    public void updateField(String fieldId, FieldConfig updatedField) {
        appConfig.getFields().removeIf(f -> f.getId().equals(fieldId));
        appConfig.getFields().add(updatedField);
        saveConfig();
    }

    public void deleteField(String fieldId) {
        appConfig.getFields().removeIf(f -> f.getId().equals(fieldId));
        saveConfig();
    }

    public void addTemplate(TemplateConfig template) {
        if (appConfig.getTemplates() == null) {
            appConfig.setTemplates(new ArrayList<>());
        }
        appConfig.getTemplates().add(template);
        saveConfig();
    }

    public void updateTemplate(String templateId, TemplateConfig updatedTemplate) {
        appConfig.getTemplates().removeIf(t -> t.getId().equals(templateId));
        appConfig.getTemplates().add(updatedTemplate);
        saveConfig();
    }

    public void deleteTemplate(String templateId) {
        appConfig.getTemplates().removeIf(t -> t.getId().equals(templateId));
        saveConfig();
    }

    public TemplateConfig getTemplateById(String templateId) {
        return appConfig.getTemplates().stream()
                .filter(t -> t.getId().equals(templateId))
                .findFirst()
                .orElse(null);
    }

    public FieldConfig getFieldById(String fieldId) {
        return appConfig.getFields().stream()
                .filter(f -> f.getId().equals(fieldId))
                .findFirst()
                .orElse(null);
    }
}

