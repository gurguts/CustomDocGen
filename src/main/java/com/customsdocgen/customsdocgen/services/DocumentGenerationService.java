package com.customsdocgen.customsdocgen.services;

import com.customsdocgen.customsdocgen.models.DocumentData;
import com.customsdocgen.customsdocgen.models.FieldConfig;
import com.customsdocgen.customsdocgen.models.TemplateConfig;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class DocumentGenerationService {

    private final ConfigService configService;
    private final FormulaCalculationService formulaCalculationService;
    private final PdfConversionService pdfConversionService;
    
    @Autowired
    public DocumentGenerationService(ConfigService configService, 
                                      FormulaCalculationService formulaCalculationService,
                                      @Autowired(required = false) PdfConversionService pdfConversionService) {
        this.configService = configService;
        this.formulaCalculationService = formulaCalculationService;
        this.pdfConversionService = pdfConversionService;
    }

    public byte[] generateDocument(String templateId, Map<String, String> fieldValues) throws Exception {
        return generateDocument(templateId, fieldValues, false);
    }

    public byte[] generateDocument(String templateId, Map<String, String> fieldValues, boolean convertToPdf) throws Exception {
        TemplateConfig template = configService.getTemplateById(templateId);
        if (template == null) {
            throw new Exception("Шаблон не знайдено: " + templateId);
        }

        Map<String, String> calculatedValues = formulaCalculationService.calculateAllFormulas(
            configService.getConfig().getFields(), fieldValues);

        Map<String, String> allFieldValues = new java.util.HashMap<>(fieldValues);
        allFieldValues.putAll(calculatedValues);

        addEmptyValuesForOptionalFields(template, allFieldValues);
        
        String templatePath = "templates/" + template.getFileName();

        byte[] documentBytes;
        if (template.getFileName().endsWith(".xlsx")) {
            documentBytes = generateExcelFromTemplate(templatePath, allFieldValues);
        } else {
            documentBytes = generateWordFromTemplate(templatePath, allFieldValues);
        }

        if (convertToPdf) {
            if (pdfConversionService == null) {
                throw new Exception("PDF конвертація недоступна. Встановіть LibreOffice та налаштуйте jodconverter.local.enabled=true");
            }
            try {
                return pdfConversionService.convertToPdf(documentBytes, template.getFileName());
            } catch (Exception e) {
                throw new Exception("Помилка конвертації в PDF: " + e.getMessage(), e);
            }
        }
        
        return documentBytes;
    }

    private void addEmptyValuesForOptionalFields(TemplateConfig template, Map<String, String> fieldValues) {
        List<FieldConfig> allFields = configService.getConfig().getFields();
        if (allFields == null) {
            return;
        }

        java.util.Set<String> requiredFieldIds = new java.util.HashSet<>();
        if (template.getRequiredFieldIds() != null) {
            requiredFieldIds.addAll(template.getRequiredFieldIds());
        }

        for (FieldConfig field : allFields) {
            String placeholder = field.getPlaceholder();

            if (field.isCalculated()) {
                continue;
            }

            if (!requiredFieldIds.contains(field.getId()) && !fieldValues.containsKey(placeholder)) {
                fieldValues.put(placeholder, "");
            }
        }
    }

    public byte[] generateInvoice(DocumentData data) throws Exception {
        Map<String, String> fieldValues = Map.ofEntries(
            Map.entry("{{CONTRACT_NUMBER}}", getOrEmpty(data.getContractNumber())),
            Map.entry("{{CONTRACT_DATE}}", getOrEmpty(data.getContractDate())),
            Map.entry("{{VEHICLE_NUMBER}}", getOrEmpty(data.getVehicleNumber())),
            Map.entry("{{WEIGHT}}", getOrEmpty(data.getWeight())),
            Map.entry("{{CONSIGNEE_NAME}}", getOrEmpty(data.getConsigneeName())),
            Map.entry("{{CONSIGNEE_ADDRESS}}", getOrEmpty(data.getConsigneeAddress())),
            Map.entry("{{PRODUCT_NAME_EN}}", getOrEmpty(data.getProductNameEn())),
            Map.entry("{{PRODUCT_NAME_UK}}", getOrEmpty(data.getProductNameUk())),
            Map.entry("{{RECEIVER_NAME}}", getOrEmpty(data.getReceiverName())),
            Map.entry("{{RECEIVER_ADDRESS}}", getOrEmpty(data.getReceiverAddress())),
            Map.entry("{{BATCH_NUMBER}}", getOrEmpty(data.getBatchNumber())),
            Map.entry("{{UNLOADING_PLACE}}", getOrEmpty(data.getUnloadingPlace())),
            Map.entry("{{UNLOADING_COUNTRY}}", getOrEmpty(data.getUnloadingCountry()))
        );
        
        return generateWordFromTemplate("templates/invoice_template.docx", fieldValues);
    }


    private byte[] generateWordFromTemplate(String templatePath, Map<String, String> fieldValues) throws Exception {
        java.io.File templateFile = new java.io.File(templatePath);
        
        try (InputStream templateStream = new java.io.FileInputStream(templateFile);
             XWPFDocument document = new XWPFDocument(templateStream);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                replacePlaceholders(paragraph, fieldValues);
            }

            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            replacePlaceholders(paragraph, fieldValues);
                        }
                    }
                }
            }
            
            document.write(out);
            return out.toByteArray();
        }
    }


    private void replacePlaceholders(XWPFParagraph paragraph, Map<String, String> fieldValues) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) {
            return;
        }

        StringBuilder fullText = new StringBuilder();
        java.util.List<RunStyleInfo> styleInfoList = new java.util.ArrayList<>();
        
        int currentPos = 0;
        for (XWPFRun run : runs) {
            String runText = run.getText(0);
            if (runText != null && !runText.isEmpty()) {

                RunStyleInfo styleInfo = new RunStyleInfo(
                    currentPos,
                    currentPos + runText.length(),
                    run.getFontFamily(),
                    run.getFontSizeAsDouble(),
                    run.isBold(),
                    run.isItalic(),
                    run.getColor(),
                    run.getUnderline()
                );
                styleInfoList.add(styleInfo);
                
                fullText.append(runText);
                currentPos += runText.length();
            }
        }
        
        String text = fullText.toString();
        boolean hasPlaceholder = false;

        java.util.regex.Pattern placeholderPattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
        java.util.regex.Matcher matcher = placeholderPattern.matcher(text);
        java.util.Set<String> allPlaceholders = new java.util.HashSet<>();
        while (matcher.find()) {
            allPlaceholders.add(matcher.group(0));
            hasPlaceholder = true;
        }

        if (hasPlaceholder) {

            for (String placeholder : allPlaceholders) {
                String value = fieldValues.getOrDefault(placeholder, "");
                text = text.replace(placeholder, value);
            }

            for (int i = runs.size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }

            createRunsWithStyles(paragraph, text, styleInfoList, fullText.toString(), fieldValues);
        } else {

            for (XWPFRun run : runs) {
                String runText = run.getText(0);
                if (runText != null) {

                    for (Map.Entry<String, String> entry : fieldValues.entrySet()) {
                        runText = runText.replace(entry.getKey(), entry.getValue());
                    }

                    java.util.regex.Pattern remainingPattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
                    java.util.regex.Matcher remainingMatcher = remainingPattern.matcher(runText);
                    while (remainingMatcher.find()) {
                        String placeholder = remainingMatcher.group(0);
                        if (!fieldValues.containsKey(placeholder)) {
                            runText = runText.replace(placeholder, "");
                        }
                    }
                    run.setText(runText, 0);
                }
            }
        }
    }
    

    private void createRunsWithStyles(XWPFParagraph paragraph, String newText, 
                                      java.util.List<RunStyleInfo> styleInfoList,
                                      String originalText, Map<String, String> fieldValues) {
        if (styleInfoList.isEmpty()) {

            paragraph.createRun().setText(newText);
            return;
        }

        java.util.List<TextSegment> segments = buildTextSegments(originalText, newText, 
            styleInfoList, fieldValues);

        for (TextSegment segment : segments) {
            if (!segment.text.isEmpty()) {
                XWPFRun run = paragraph.createRun();
                run.setText(segment.text);
                applyStyle(run, segment.style);
            }
        }
    }
    

    private java.util.List<TextSegment> buildTextSegments(String originalText, String newText,
        java.util.List<RunStyleInfo> styleInfoList, Map<String, String> fieldValues) {
        
        java.util.List<TextSegment> segments = new java.util.ArrayList<>();
        java.util.List<PlaceholderRange> placeholderRanges = new java.util.ArrayList<>();

        for (Map.Entry<String, String> entry : fieldValues.entrySet()) {
            String placeholder = entry.getKey();
            String replacement = entry.getValue();
            int pos = originalText.indexOf(placeholder);
            while (pos >= 0) {

                RunStyleInfo placeholderStyle = findStyleForOriginalPosition(pos, styleInfoList);
                placeholderRanges.add(new PlaceholderRange(pos, pos + placeholder.length(), 
                    replacement, placeholderStyle));
                pos = originalText.indexOf(placeholder, pos + 1);
            }
        }

        placeholderRanges.sort((a, b) -> Integer.compare(a.startPos, b.startPos));

        int currentOriginalPos = 0;
        int currentNewPos = 0;
        
        for (PlaceholderRange range : placeholderRanges) {

            if (range.startPos > currentOriginalPos) {
                String beforeText = originalText.substring(currentOriginalPos, range.startPos);

                String beforeNewText = newText.substring(currentNewPos, 
                    currentNewPos + beforeText.length());

                splitTextByStyles(beforeText, beforeNewText, currentOriginalPos, segments, styleInfoList);
                
                currentNewPos += beforeText.length();
                currentOriginalPos = range.startPos;
            }

            String replacementText = range.replacementValue;
            if (!replacementText.isEmpty()) {
                segments.add(new TextSegment(replacementText, range.style));
                currentNewPos += replacementText.length();
            }
            
            currentOriginalPos = range.endPos;
        }

        if (currentOriginalPos < originalText.length()) {
            String afterText = originalText.substring(currentOriginalPos);
            String afterNewText = newText.substring(currentNewPos);
            
            splitTextByStyles(afterText, afterNewText, currentOriginalPos, segments, styleInfoList);
        }
        
        return segments;
    }
    

    private void splitTextByStyles(String originalText, String newText, int startOffset,
        java.util.List<TextSegment> segments, java.util.List<RunStyleInfo> styleInfoList) {
        
        if (originalText.length() != newText.length()) {

            RunStyleInfo style = findStyleForOriginalPosition(startOffset, styleInfoList);
            segments.add(new TextSegment(newText, style));
            return;
        }

        int textPos = 0;
        while (textPos < newText.length()) {
            int originalPos = startOffset + textPos;
            RunStyleInfo currentStyle = findStyleForOriginalPosition(originalPos, styleInfoList);

            int segmentEnd = textPos;
            for (int i = textPos + 1; i < newText.length(); i++) {
                RunStyleInfo nextStyle = findStyleForOriginalPosition(startOffset + i, styleInfoList);
                if (!stylesMatch(currentStyle, nextStyle)) {
                    break;
                }
                segmentEnd = i;
            }
            
            String segmentText = newText.substring(textPos, segmentEnd + 1);
            segments.add(new TextSegment(segmentText, currentStyle));
            textPos = segmentEnd + 1;
        }
    }
    

    private RunStyleInfo findStyleForOriginalPosition(int pos, java.util.List<RunStyleInfo> styleInfoList) {
        for (RunStyleInfo styleInfo : styleInfoList) {
            if (pos >= styleInfo.startPos && pos < styleInfo.endPos) {
                return styleInfo;
            }
        }
        return styleInfoList.isEmpty() ? null : styleInfoList.get(styleInfoList.size() - 1);
    }
    

    private static class PlaceholderRange {
        int startPos;
        int endPos;
        String replacementValue;
        RunStyleInfo style;
        
        PlaceholderRange(int startPos, int endPos, String replacementValue, RunStyleInfo style) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.replacementValue = replacementValue;
            this.style = style;
        }
    }
    

    private static class TextSegment {
        String text;
        RunStyleInfo style;
        
        TextSegment(String text, RunStyleInfo style) {
            this.text = text;
            this.style = style;
        }
    }
    

    private boolean stylesMatch(RunStyleInfo style1, RunStyleInfo style2) {
        if (style1 == null || style2 == null) {
            return style1 == style2;
        }
        return java.util.Objects.equals(style1.fontFamily, style2.fontFamily) &&
               java.util.Objects.equals(style1.fontSize, style2.fontSize) &&
               style1.bold == style2.bold &&
               style1.italic == style2.italic &&
               java.util.Objects.equals(style1.color, style2.color) &&
               style1.underline == style2.underline;
    }
    

    private void applyStyle(XWPFRun run, RunStyleInfo styleInfo) {
        if (styleInfo == null) return;
        
        if (styleInfo.fontFamily != null) {
            run.setFontFamily(styleInfo.fontFamily);
        }
        if (styleInfo.fontSize != null && styleInfo.fontSize > 0) {
            run.setFontSize(styleInfo.fontSize);
        }
        run.setBold(styleInfo.bold);
        run.setItalic(styleInfo.italic);
        if (styleInfo.color != null) {
            run.setColor(styleInfo.color);
        }
        if (styleInfo.underline != null && styleInfo.underline != UnderlinePatterns.NONE) {
            run.setUnderline(styleInfo.underline);
        }
    }
    

    private static class RunStyleInfo {
        int startPos;
        int endPos;
        String fontFamily;
        Double fontSize;
        boolean bold;
        boolean italic;
        String color;
        UnderlinePatterns underline;
        
        RunStyleInfo(int startPos, int endPos, String fontFamily, Double fontSize,
                    boolean bold, boolean italic, String color, UnderlinePatterns underline) {
            this.startPos = startPos;
            this.endPos = endPos;
            this.fontFamily = fontFamily;
            this.fontSize = fontSize;
            this.bold = bold;
            this.italic = italic;
            this.color = color;
            this.underline = underline;
        }
    }


    private byte[] generateExcelFromTemplate(String templatePath, Map<String, String> fieldValues) throws Exception {

        java.io.File templateFile = new java.io.File(templatePath);
        
        try (InputStream templateStream = new java.io.FileInputStream(templateFile);
             XSSFWorkbook workbook = new XSSFWorkbook(templateStream);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                replacePlaceholdersInSheet(sheet, fieldValues);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }


    private void replacePlaceholdersInSheet(Sheet sheet, Map<String, String> fieldValues) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.STRING) {
                    String cellValue = cell.getStringCellValue();

                    java.util.regex.Pattern placeholderPattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
                    java.util.regex.Matcher matcher = placeholderPattern.matcher(cellValue);
                    boolean hasPlaceholder = matcher.find();

                    if (hasPlaceholder) {

                        CellStyle originalStyle = cell.getCellStyle();

                        java.util.Set<String> allPlaceholders = new java.util.HashSet<>();
                        matcher = placeholderPattern.matcher(cellValue);
                        while (matcher.find()) {
                            allPlaceholders.add(matcher.group(0));
                        }

                        for (String placeholder : allPlaceholders) {
                            String value = fieldValues.getOrDefault(placeholder, "");
                            cellValue = cellValue.replace(placeholder, value);
                        }
                        
                        cell.setCellValue(cellValue);

                        if (originalStyle != null) {
                            cell.setCellStyle(originalStyle);
                        }
                    }
                }
            }
        }
    }


    private String getOrEmpty(String value) {
        return value != null ? value : "";
    }


    public byte[] generateArchive(List<String> templateIds, Map<String, String> fieldValues) throws Exception {
        return generateArchive(templateIds, fieldValues, new java.util.HashMap<>(), new java.util.HashMap<>());
    }
    
    public byte[] generateArchive(List<String> templateIds, Map<String, String> fieldValues, 
                                   Map<String, Boolean> pdfFlags) throws Exception {
        return generateArchive(templateIds, fieldValues, new java.util.HashMap<>(), pdfFlags);
    }
    
    public byte[] generateArchive(List<String> templateIds, Map<String, String> fieldValues,
                                   Map<String, Boolean> originalFlags, Map<String, Boolean> pdfFlags) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            
            for (String templateId : templateIds) {
                try {
                    TemplateConfig template = configService.getTemplateById(templateId);
                    if (template == null) continue;

                    String fileName = generateFileName(template.getDownloadPattern(), fieldValues);

                    boolean needOriginal = originalFlags.getOrDefault(templateId, Boolean.TRUE);
                    boolean needPdf = pdfFlags.getOrDefault(templateId, Boolean.FALSE);
                    
                    if (needOriginal) {

                        byte[] originalData = generateDocument(templateId, fieldValues, false);
                        ZipEntry originalEntry = new ZipEntry(fileName);
                        zos.putNextEntry(originalEntry);
                        zos.write(originalData);
                        zos.closeEntry();
                    }
                    
                    if (needPdf) {

                        byte[] pdfData = generateDocument(templateId, fieldValues, true);
                        String pdfFileName = fileName.replaceAll("\\.(docx|xlsx)$", ".pdf");
                        ZipEntry pdfEntry = new ZipEntry(pdfFileName);
                        zos.putNextEntry(pdfEntry);
                        zos.write(pdfData);
                        zos.closeEntry();
                    }
                } catch (Exception e) {

                }
            }
            
            zos.finish();
            return baos.toByteArray();
        }
    }
    

    private String generateFileName(String pattern, Map<String, String> fieldValues) {
        String fileName = pattern;

        for (Map.Entry<String, String> entry : fieldValues.entrySet()) {
            fileName = fileName.replace(entry.getKey(), entry.getValue());
        }

        fileName = fileName.replaceAll("[<>:\"/\\\\|?*]", "_");
        
        return fileName;
    }


    public boolean isInvoiceDataComplete(DocumentData data) {
        return data.getContractNumber() != null && !data.getContractNumber().isEmpty() &&
               data.getContractDate() != null && !data.getContractDate().isEmpty() &&
               data.getVehicleNumber() != null && !data.getVehicleNumber().isEmpty() &&
               data.getWeight() != null && !data.getWeight().isEmpty() &&
               data.getConsigneeName() != null && !data.getConsigneeName().isEmpty() &&
               data.getConsigneeAddress() != null && !data.getConsigneeAddress().isEmpty() &&
               data.getProductNameEn() != null && !data.getProductNameEn().isEmpty() &&
               data.getProductNameUk() != null && !data.getProductNameUk().isEmpty();
    }
}

