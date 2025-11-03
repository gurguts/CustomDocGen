package com.customsdocgen.customsdocgen.controllers;

import com.customsdocgen.customsdocgen.models.DocumentData;
import com.customsdocgen.customsdocgen.services.DocumentGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentGenerationService documentGenerationService;

    @GetMapping("/old")
    public String index() {
        return "index";
    }

    @PostMapping("/api/check-invoice-availability")
    @ResponseBody
    public ResponseEntity<Boolean> checkInvoiceAvailability(@RequestBody DocumentData data) {
        boolean isAvailable = documentGenerationService.isInvoiceDataComplete(data);
        return ResponseEntity.ok(isAvailable);
    }

    @PostMapping("/api/generate-invoice")
    public ResponseEntity<byte[]> generateInvoice(@RequestBody DocumentData data) {
        try {
            byte[] document = documentGenerationService.generateInvoice(data);
            
            String fileName = "Invoice_" + data.getContractNumber() + ".docx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
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

    private String encodeFileName(String fileName) {
        try {
            return java.net.URLEncoder.encode(fileName, "UTF-8")
                    .replace("+", "%20");
        } catch (Exception e) {
            return fileName;
        }
    }
}

