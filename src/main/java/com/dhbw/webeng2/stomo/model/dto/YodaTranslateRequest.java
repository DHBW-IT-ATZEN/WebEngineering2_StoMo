package com.dhbw.webeng2.stomo.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class YodaTranslateRequest {
    private List<String> texts;
}
