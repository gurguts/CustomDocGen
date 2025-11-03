package com.customsdocgen.customsdocgen.services;

import com.customsdocgen.customsdocgen.models.FieldConfig;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FormulaCalculationService {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");


    public Map<String, String> calculateAllFormulas(List<FieldConfig> fields, Map<String, String> fieldValues) {
        Map<String, String> result = new HashMap<>(fieldValues);

        List<FieldConfig> formulaFields = fields.stream()
                .filter(field -> "formula".equals(field.getFieldType()) && field.getFormula() != null)
                .toList();

        formulaFields.stream()
                .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
                .forEach(field -> {
                    try {
                        String calculatedValue = calculateFormula(field.getFormula(), result, field.getDecimalPlaces());
                        result.put(field.getPlaceholder(), calculatedValue);
                    } catch (Exception e) {
                        result.put(field.getPlaceholder(), "ОШИБКА");
                    }
                });
        
        return result;
    }


    private String calculateFormula(String formula, Map<String, String> fieldValues, Integer decimalPlaces) {
        if (formula == null || formula.trim().isEmpty()) {
            return "";
        }

        String processedFormula = formula;

        Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(processedFormula);
        while (placeholderMatcher.find()) {
            String placeholder = placeholderMatcher.group(0);
            String value = fieldValues.getOrDefault(placeholder, "0");

            try {
                double numValue = Double.parseDouble(value);
                processedFormula = processedFormula.replace(placeholder, String.valueOf(numValue));
            } catch (NumberFormatException e) {

                processedFormula = processedFormula.replace(placeholder, "0");
            }
        }

        return evaluateExpression(processedFormula, decimalPlaces);
    }

    private String evaluateExpression(String expression, Integer decimalPlaces) {
        try {

            expression = expression.replaceAll("\\s+", "");

            while (expression.contains("*") || expression.contains("/")) {

                expression = processOperation(expression, "[*/]");
            }
            
            while (expression.contains("+") || expression.contains("-")) {

                expression = processOperation(expression, "[+-]");
            }
            
            double result = Double.parseDouble(expression);

            return formatNumber(result, decimalPlaces);
        } catch (Exception e) {
            return "ОШИБКА";
        }
    }

    private String formatNumber(double number, Integer decimalPlaces) {
        if (decimalPlaces == null) {

            if (number == (long) number) {
                return String.valueOf((long) number);
            } else {
                return String.format("%.2f", Double.valueOf(number));
            }
        } else if (decimalPlaces == 0) {

            return String.valueOf(Math.round(number));
        } else {

            return String.format("%." + decimalPlaces + "f", Double.valueOf(number));
        }
    }


    private String processOperation(String expression, String operationPattern) {
        Pattern pattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s*(" + operationPattern + ")\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(expression);
        
        if (matcher.find()) {
            double left = Double.parseDouble(matcher.group(1));
            String operator = matcher.group(2);
            double right = Double.parseDouble(matcher.group(3));
            
            double result;
            switch (operator) {
                case "+":
                    result = left + right;
                    break;
                case "-":
                    result = left - right;
                    break;
                case "*":
                    result = left * right;
                    break;
                case "/":
                    result = right != 0 ? left / right : 0;
                    break;
                default:
                    result = 0;
            }
            
            return expression.replace(matcher.group(0), String.valueOf(result));
        }
        
        return expression;
    }


    public boolean isCalculatedField(FieldConfig field) {
        return "formula".equals(field.getFieldType()) && 
               field.getFormula() != null && 
               !field.getFormula().trim().isEmpty();
    }


    public List<String> getFieldsUsedInFormula(String formula) {
        if (formula == null) return List.of();
        
        return PLACEHOLDER_PATTERN.matcher(formula)
                .results()
                .map(match -> match.group(1))
                .toList();
    }
}
