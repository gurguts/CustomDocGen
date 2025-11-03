package com.customsdocgen.customsdocgen.controllers;

import com.customsdocgen.customsdocgen.models.AppConfig;
import com.customsdocgen.customsdocgen.models.FieldConfig;
import com.customsdocgen.customsdocgen.models.TemplateConfig;
import com.customsdocgen.customsdocgen.services.ConfigService;
import com.customsdocgen.customsdocgen.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final ConfigService configService;
    private final FileStorageService fileStorageService;


    @GetMapping("/settings")
    public String settingsPage() {
        return "settings";
    }


    @GetMapping("/api/config")
    @ResponseBody
    public ResponseEntity<AppConfig> getConfig() {
        return ResponseEntity.ok(configService.getConfig());
    }


    @PutMapping("/api/config")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateConfig(@RequestBody AppConfig config) {
        configService.updateConfig(config);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Конфигурация успешно обновлена");
        return ResponseEntity.ok(response);
    }


    @PostMapping("/api/fields")
    @ResponseBody
    public ResponseEntity<Map<String, String>> addField(@RequestBody FieldConfig field) {
        configService.addField(field);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Поле успешно добавлено");
        return ResponseEntity.ok(response);
    }


    @PutMapping("/api/fields/{fieldId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateField(
            @PathVariable String fieldId,
            @RequestBody FieldConfig field) {
        configService.updateField(fieldId, field);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Поле успешно обновлено");
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/api/fields/{fieldId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteField(@PathVariable String fieldId) {
        configService.deleteField(fieldId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Поле успешно удалено");
        return ResponseEntity.ok(response);
    }


    @PostMapping("/api/templates/upload")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadTemplate(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = fileStorageService.saveTemplate(file);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Шаблон успешно загружен");
            response.put("fileName", fileName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }


    @PostMapping("/api/templates")
    @ResponseBody
    public ResponseEntity<Map<String, String>> addTemplate(@RequestBody TemplateConfig template) {
        configService.addTemplate(template);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Шаблон успешно добавлен");
        return ResponseEntity.ok(response);
    }


    @PutMapping("/api/templates/{templateId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateTemplate(
            @PathVariable String templateId,
            @RequestBody TemplateConfig template) {
        try {
            TemplateConfig oldTemplate = configService.getTemplateById(templateId);

            if (oldTemplate != null && 
                !oldTemplate.getFileName().equals(template.getFileName())) {
                try {
                    fileStorageService.deleteTemplate(oldTemplate.getFileName());
                } catch (Exception e) {
                }
            }

            configService.updateTemplate(templateId, template);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Шаблон успешно обновлен");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/api/templates/{templateId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteTemplate(@PathVariable String templateId) {
        try {
            TemplateConfig template = configService.getTemplateById(templateId);
            if (template != null) {
                fileStorageService.deleteTemplate(template.getFileName());
            }
            configService.deleteTemplate(templateId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Шаблон успешно удален");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/templates/files")
    @ResponseBody
    public ResponseEntity<String[]> listTemplateFiles() {
        return ResponseEntity.ok(fileStorageService.listTemplates());
    }
}

