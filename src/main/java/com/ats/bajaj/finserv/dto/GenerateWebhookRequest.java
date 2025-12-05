package com.ats.bajaj.finserv.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GenerateWebhookRequest {
    private String name;
    private String regNo;
    private String email;
}