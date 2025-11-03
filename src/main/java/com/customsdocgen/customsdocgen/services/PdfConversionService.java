package com.customsdocgen.customsdocgen.services;

import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@ConditionalOnProperty(name = "jodconverter.local.enabled", havingValue = "true", matchIfMissing = false)
public class PdfConversionService {

    private final DocumentConverter documentConverter;
    
    @Autowired(required = false)
    public PdfConversionService(DocumentConverter documentConverter) {
        this.documentConverter = documentConverter;
    }


    public byte[] convertDocxToPdf(byte[] docxBytes) throws IOException, OfficeException {
        if (documentConverter == null) {
            throw new IllegalStateException("LibreOffice не встановлено або JODConverter не налаштовано");
        }
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(docxBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            documentConverter
                    .convert(inputStream)
                    .as(org.jodconverter.core.document.DefaultDocumentFormatRegistry.DOCX)
                    .to(outputStream)
                    .as(org.jodconverter.core.document.DefaultDocumentFormatRegistry.PDF)
                    .execute();
            
            return outputStream.toByteArray();
        }
    }


    public byte[] convertXlsxToPdf(byte[] xlsxBytes) throws IOException, OfficeException {
        if (documentConverter == null) {
            throw new IllegalStateException("LibreOffice не встановлено або JODConverter не налаштовано");
        }
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(xlsxBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            documentConverter
                    .convert(inputStream)
                    .as(org.jodconverter.core.document.DefaultDocumentFormatRegistry.XLSX)
                    .to(outputStream)
                    .as(org.jodconverter.core.document.DefaultDocumentFormatRegistry.PDF)
                    .execute();
            
            return outputStream.toByteArray();
        }
    }


    public byte[] convertToPdf(byte[] documentBytes, String fileName) throws IOException, OfficeException {
        if (fileName.endsWith(".docx")) {
            return convertDocxToPdf(documentBytes);
        } else if (fileName.endsWith(".xlsx")) {
            return convertXlsxToPdf(documentBytes);
        } else {
            throw new IllegalArgumentException("Непідтримуваний формат файлу: " + fileName);
        }
    }


    public boolean isAvailable() {
        try {
            return documentConverter != null;
        } catch (Exception e) {
            return false;
        }
    }
}

