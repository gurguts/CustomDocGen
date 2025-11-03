package com.customsdocgen.customsdocgen.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {

    private static final String TEMPLATES_DIR = "templates/";

    public String saveTemplate(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Файл пустий");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || 
            (!originalFilename.endsWith(".docx") && !originalFilename.endsWith(".xlsx"))) {
            throw new IOException("Підтримуються тільки файли .docx та .xlsx");
        }

        File templatesDir = new File(TEMPLATES_DIR);
        if (!templatesDir.exists()) {
            templatesDir.mkdirs();
        }

        Path targetPath = Paths.get(TEMPLATES_DIR + originalFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return originalFilename;
    }


    public void deleteTemplate(String fileName) throws IOException {
        Path filePath = Paths.get(TEMPLATES_DIR + fileName);
        Files.deleteIfExists(filePath);
    }


    public boolean templateExists(String fileName) {
        File file = new File(TEMPLATES_DIR + fileName);
        return file.exists();
    }


    public String[] listTemplates() {
        File templatesDir = new File(TEMPLATES_DIR);
        if (!templatesDir.exists()) {
            return new String[0];
        }
        
        return templatesDir.list((_, name) -> 
            name.endsWith(".docx") || name.endsWith(".xlsx"));
    }
    

    public String getTemplateType(String fileName) {
        if (fileName.endsWith(".docx")) {
            return "docx";
        } else if (fileName.endsWith(".xlsx")) {
            return "xlsx";
        }
        return "unknown";
    }
}

