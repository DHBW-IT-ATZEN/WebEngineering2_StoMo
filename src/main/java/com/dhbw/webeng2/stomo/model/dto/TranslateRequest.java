package com.dhbw.webeng2.stomo.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class TranslateRequest {
    private List<String> texts;
    private String target; // ISO code, e.g. "de"; defaults to German server-side when omitted
}
