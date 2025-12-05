package com.ats.bajaj.finserv.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class WebhookResponse {
    private String webhook;
    private String accessToken;
}
