package com.ats.bajaj.finserv;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.ats.bajaj.finserv.dto.GenerateWebhookRequest;
import com.ats.bajaj.finserv.dto.SubmitFinalQueryRequest;
import com.ats.bajaj.finserv.dto.WebhookResponse;

import lombok.extern.java.Log;

@Service
@Log
public class BajajFinserv {
    private final RestTemplate restTemplate;
    public BajajFinserv(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // setup all values
    private String regNo = "22BCE0592";
    private String name = "Abhijay Tilak Singh";
    private String email = "abhijay.tilak@gmail.com";
    private String finalQuery = """
            WITH high_paid AS (
              SELECT DISTINCT
                e.emp_id,
                e.first_name,
                e.last_name,
                e.dob,
                e.department AS department_id
              FROM employee e
              JOIN payments p ON p.emp_id = e.emp_id
              WHERE p.amount > 70000
            ),
            avg_age_per_dept AS (
              SELECT
                d.department_id,
                d.department_name,
                AVG(TIMESTAMPDIFF(YEAR, h.dob, CURRENT_DATE())) AS average_age
              FROM department d
              LEFT JOIN high_paid h ON h.department_id = d.department_id
              GROUP BY d.department_id, d.department_name
            ),
            named_rows AS (
              SELECT
                h.*,
                ROW_NUMBER() OVER (PARTITION BY h.department_id ORDER BY h.emp_id) AS rn
              FROM high_paid h
            ),
            list_per_dept AS (
              -- aggregate up to 10 names per department
              SELECT
                department_id,
                GROUP_CONCAT(CONCAT(first_name, ' ', last_name)
                            ORDER BY emp_id SEPARATOR ', ') AS employee_list
              FROM named_rows
              WHERE rn <= 10
              GROUP BY department_id
            )
            SELECT
              a.department_name,
              ROUND(a.average_age, 2) AS average_age,
              COALESCE(l.employee_list, '') AS employee_list
            FROM avg_age_per_dept a
            LEFT JOIN list_per_dept l ON l.department_id = a.department_id
            ORDER BY a.department_id DESC;
            """;

    public void work() {
        //generate post request
        GenerateWebhookRequest payload = new GenerateWebhookRequest(name, regNo, email);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GenerateWebhookRequest> request = new HttpEntity<>(payload, headers);
        
        //post the request
        String requestURL = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
        ResponseEntity<WebhookResponse> response = restTemplate.postForEntity(requestURL, request, WebhookResponse.class);

        String webhookURL;
        String accessToken;
        //verify response
        if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            // get access token and webhook from response
            WebhookResponse body = response.getBody();
            webhookURL = body.getWebhook();
            accessToken = body.getAccessToken();
            log.info("webhook: " + webhookURL);
            log.info("access token: " + (accessToken == null ? "null" : accessToken));
        }
        else {
            log.info("Failed to get response.");
            return;
        }

        //generate a post request to be sent which includes sql query and my details
        SubmitFinalQueryRequest anotherPayload = new SubmitFinalQueryRequest(finalQuery);
        HttpHeaders anotherHeader = new HttpHeaders();
        anotherHeader.setContentType(MediaType.APPLICATION_JSON);
        anotherHeader.set("Authorization", accessToken);
        HttpEntity<SubmitFinalQueryRequest> anotherRequest = new HttpEntity<>(anotherPayload, anotherHeader);

        //post the request containing sql query and my details
        ResponseEntity<String> anotherResponse = restTemplate.postForEntity(webhookURL, anotherRequest, String.class);
        
        //verify response
        if (anotherResponse != null && anotherResponse.getStatusCode().is2xxSuccessful()) {
            log.info("Successfully completed project");
        }
        else {
            log.info("Couldn't submit final POST request");
        }
    }
}
