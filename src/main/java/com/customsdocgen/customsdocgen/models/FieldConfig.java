package com.customsdocgen.customsdocgen.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldConfig {
    private String id;
    private String placeholder;
    private String displayName;
    private String fieldType;
    private boolean required;
    private int order;
    private String formula;
    
    @JsonProperty("isCalculated")
    private boolean isCalculated;
    
    private boolean rememberValues;
    
    private Integer decimalPlaces;
}

