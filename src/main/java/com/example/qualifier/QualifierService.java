package com.example.qualifier;

import com.example.qualifier.dto.GenerateWebhookRequest;
import com.example.qualifier.dto.GenerateWebhookResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class QualifierService {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.name}")
    private String name;
    @Value("${app.regNo}")
    private String regNo;
    @Value("${app.email}")
    private String email;
    @Value("${app.finalQuery}")
    private String finalQuery;

    @Value("${endpoints.generate}")
    private String generateUrl;
    @Value("${endpoints.submitFallback}")
    private String submitFallbackUrl;

    public QualifierService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void runFlow() {
        System.out.println("➡️ Starting Qualifier flow at " + Instant.now());
        System.out.println("   Using regNo = " + regNo);

        // 1) Generate webhook
        GenerateWebhookResponse response = generateWebhook();
        if (response == null) {
            System.err.println("Could not generate webhook. Exiting.");
            return;
        }

        String webhookUrl = (response.getWebhook() != null && !response.getWebhook().isBlank())
                ? response.getWebhook()
                : submitFallbackUrl;

        String accessToken = response.getAccessToken();
        if (accessToken == null || accessToken.isBlank()) {
            System.err.println("Missing accessToken in response. Exiting.");
            return;
        }

        // 2) Store solution locally
        storeSolutionToFile(finalQuery);

        // 3) Submit SQL
        submitFinalQuery(webhookUrl, accessToken, finalQuery);

        System.out.println("Done.");
    }

    private GenerateWebhookResponse generateWebhook() {
        try {
            GenerateWebhookRequest body = new GenerateWebhookRequest(name, regNo, email);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<GenerateWebhookRequest> entity = new HttpEntity<>(body, headers);

            ResponseEntity<GenerateWebhookResponse> resp = restTemplate.exchange(
                    generateUrl, HttpMethod.POST, entity, GenerateWebhookResponse.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                System.out.println("   Generated webhook successfully.");
                return resp.getBody();
            } else {
                System.err.println("   Generate webhook failed: " + resp.getStatusCode());
                return null;
            }
        } catch (HttpStatusCodeException e) {
            System.err.println(
                    "   HTTP " + e.getStatusCode() + " while generating webhook: " + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            System.err.println("   Error while generating webhook: " + e.getMessage());
            return null;
        }
    }

    private void storeSolutionToFile(String sql) {
        try {
            Path out = Path.of("solution.txt");
            Files.writeString(out, sql == null ? "" : sql);
            System.out.println("   Stored final SQL to: " + out.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("   Could not write solution file: " + e.getMessage());
        }
    }

    private void submitFinalQuery(String webhookUrl, String accessToken, String finalQuery) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken); // if fails, try headers.setBearerAuth(accessToken)

            Map<String, String> payload = new HashMap<>();
            payload.put("finalQuery", finalQuery);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    webhookUrl, HttpMethod.POST, entity, String.class);

            if (resp.getStatusCode().is2xxSuccessful()) {
                System.out.println("   Submitted successfully. Response: " + resp.getBody());
            } else {
                System.err.println("   Submit failed: " + resp.getStatusCode() + " Body: " + resp.getBody());
            }
        } catch (HttpStatusCodeException e) {
            System.err.println("   HTTP " + e.getStatusCode() + " while submitting: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("   Error while submitting: " + e.getMessage());
        }
    }
}
