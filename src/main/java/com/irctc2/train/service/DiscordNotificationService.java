package com.irctc2.train.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class DiscordNotificationService {

    private static final String DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/1341027744569491486/6wlipG2pDmRFtzGAJIdEwqhOVzhgkNq6biliaMEtDZwx-HF8SHbcX5TaCteP3n2_KT5H"; // Replace with your Discord webhook URL

    private final RestTemplate restTemplate;


    @Autowired
    public DiscordNotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendDiscordMessage(String message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("content", message);  // `content` is the key used for text in Discord webhooks.

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        // Send POST request to Discord Webhook
        restTemplate.postForObject(DISCORD_WEBHOOK_URL, entity, String.class);
    }
}
