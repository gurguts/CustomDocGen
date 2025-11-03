package com.customsdocgen.customsdocgen.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppConfig {
    private List<FieldConfig> fields = new ArrayList<>();
    private List<TemplateConfig> templates = new ArrayList<>();
}

