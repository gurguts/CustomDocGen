package com.customsdocgen.customsdocgen.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateConfig {
    private String id;
    private String fileName;
    private String displayName;
    private String downloadPattern;
    private List<String> requiredFieldIds;
}

