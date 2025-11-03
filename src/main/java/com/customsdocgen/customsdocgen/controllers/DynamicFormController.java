package com.customsdocgen.customsdocgen.controllers;

import com.customsdocgen.customsdocgen.models.AppConfig;
import com.customsdocgen.customsdocgen.models.FieldConfig;
import com.customsdocgen.customsdocgen.models.TemplateConfig;
import com.customsdocgen.customsdocgen.services.ConfigService;
import com.customsdocgen.customsdocgen.services.DocumentGenerationService;
import com.customsdocgen.customsdocgen.services.FieldValuesHistoryService;
import com.customsdocgen.customsdocgen.services.FormulaCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DynamicFormController {

    private final ConfigService configService;
    private final DocumentGenerationService documentGenerationService;
    private final FormulaCalculationService formulaCalculationService;
    private final FieldValuesHistoryService fieldValuesHistoryService;

    @GetMapping("/")
    public String index() {
        return "dynamic-form";
    }

    @GetMapping("/api/form-config")
    @ResponseBody
    public ResponseEntity<AppConfig> getFormConfig() {
        return ResponseEntity.ok(configService.getConfig());
    }

    @PostMapping("/api/check-template-availability")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkTemplateAvailability(@RequestBody Map<String, String> formData) {
        AppConfig config = configService.getConfig();
        Map<String, Object> response = new HashMap<>();
        
        List<TemplateConfig> availableTemplates = config.getTemplates().stream()
                .filter(template -> isTemplateAvailable(template, formData, config.getFields()))
                .toList();
        
        response.put("availableTemplates", availableTemplates);
        response.put("allTemplates", config.getTemplates());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/generate-document/{templateId}")
    public ResponseEntity<byte[]> generateDocument(
            @PathVariable String templateId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> formData = (Map<String, String>) request.get("formData");

            Boolean convertToPdf = (Boolean) request.getOrDefault("convertToPdf", Boolean.FALSE);
            
            TemplateConfig template = configService.getTemplateById(templateId);
            String fileName = generateFileName(template.getDownloadPattern(), formData);

            saveFieldValuesToHistory(formData);

            byte[] document = documentGenerationService.generateDocument(templateId, formData, convertToPdf);

            if (convertToPdf) {
                fileName = fileName.replaceAll("\\.(docx|xlsx)$", ".pdf");
            }
            
            HttpHeaders headers = new HttpHeaders();
            if (convertToPdf) {
                headers.setContentType(MediaType.APPLICATION_PDF);
            } else {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }
            headers.add("Content-Disposition", 
                "attachment; filename*=UTF-8''" + encodeFileName(fileName));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(document);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    

    private void saveFieldValuesToHistory(Map<String, String> formData) {
        AppConfig config = configService.getConfig();
        Map<String, Boolean> fieldRememberFlags = new HashMap<>();
        Map<String, String> fieldIdToPlaceholder = new HashMap<>();

        for (FieldConfig field : config.getFields()) {
            if (field.isRememberValues()) {
                fieldRememberFlags.put(field.getId(), Boolean.TRUE);
                fieldIdToPlaceholder.put(field.getPlaceholder(), field.getId());
            }
        }

        Map<String, String> fieldValuesByFieldId = new HashMap<>();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            String placeholder = entry.getKey();
            String value = entry.getValue();
            String fieldId = fieldIdToPlaceholder.get(placeholder);
            if (fieldId != null && value != null && !value.trim().isEmpty()) {
                fieldValuesByFieldId.put(fieldId, value);
            }
        }

        fieldValuesHistoryService.saveValuesForFields(fieldValuesByFieldId, fieldRememberFlags);
    }

    @GetMapping("/api/field-values-history")
    @ResponseBody
    public ResponseEntity<Map<String, List<String>>> getFieldValuesHistory() {
        return ResponseEntity.ok(fieldValuesHistoryService.getAllHistory());
    }

    @DeleteMapping("/api/field-values-history/{fieldId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeFieldValue(
            @PathVariable String fieldId,
            @RequestParam String value) {
        boolean removed = fieldValuesHistoryService.removeValue(fieldId, value);
        Map<String, Object> response = new HashMap<>();
        response.put("success", Boolean.valueOf(removed));
        return removed ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    @PostMapping("/api/calculate-formulas")
    @ResponseBody
    public ResponseEntity<Map<String, String>> calculateFormulas(@RequestBody Map<String, String> formData) {
        AppConfig config = configService.getConfig();
        Map<String, String> calculatedValues = formulaCalculationService.calculateAllFormulas(
                config.getFields(), formData);
        return ResponseEntity.ok(calculatedValues);
    }

    @PostMapping("/api/generate-archive")
    public ResponseEntity<byte[]> generateArchive(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> formData = (Map<String, String>) request.get("formData");
            
            @SuppressWarnings("unchecked")
            List<String> templateIds = (List<String>) request.get("templateIds");
            
            @SuppressWarnings("unchecked")
            Map<String, Boolean> originalFlags = (Map<String, Boolean>) request.get("originalFlags");
            
            @SuppressWarnings("unchecked")
            Map<String, Boolean> pdfFlags = (Map<String, Boolean>) request.get("pdfFlags");
            
            if (templateIds == null || templateIds.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            if (originalFlags == null) {
                originalFlags = new HashMap<>();
            }
            if (pdfFlags == null) {
                pdfFlags = new HashMap<>();
            }
            
            byte[] archive = documentGenerationService.generateArchive(templateIds, formData, originalFlags, pdfFlags);

            saveFieldValuesToHistory(formData);

            String archiveName = "Documents";
            String contractNumber = formData.get("{{CONTRACT_NUMBER}}");
            if (contractNumber != null && !contractNumber.isEmpty()) {
                archiveName = "Documents_" + contractNumber;
            }
            archiveName += ".zip";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.add("Content-Disposition", 
                "attachment; filename*=UTF-8''" + encodeFileName(archiveName));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(archive);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean isTemplateAvailable(TemplateConfig template, Map<String, String> formData, List<FieldConfig> fields) {

        for (String fieldId : template.getRequiredFieldIds()) {
            FieldConfig field = fields.stream()
                    .filter(f -> f.getId().equals(fieldId))
                    .findFirst()
                    .orElse(null);
            
            if (field == null) continue;
            
            String value = formData.get(field.getPlaceholder());
            if (value == null || value.trim().isEmpty()) {
                return false;
            }
        }
        
        return true;
    }

    private String generateFileName(String pattern, Map<String, String> formData) {
        String fileName = pattern;

        for (Map.Entry<String, String> entry : formData.entrySet()) {
            fileName = fileName.replace(entry.getKey(), entry.getValue());
        }

        fileName = fileName.replaceAll("[<>:\"/\\\\|?*]", "_");
        
        return fileName;
    }

    private String encodeFileName(String fileName) {
        try {
            return java.net.URLEncoder.encode(fileName, "UTF-8")
                    .replace("+", "%20");
        } catch (Exception e) {
            return fileName;
        }
    }
}
