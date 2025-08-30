package com.carpool.carpoolingapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAddressFromCoordinates(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return "Unknown Location";
        }

        String url = String.format("https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s", lat, lng);

        try {
            // We need to set a user-agent header as per Nominatim's policy
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "CarpoolingApp/1.0");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            JsonNode response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, JsonNode.class).getBody();

            if (response != null && response.has("display_name")) {
                return response.get("display_name").asText();
            }
        } catch (Exception e) {
            // If the API call fails, return the raw coordinates
            System.err.println("Geocoding API failed: " + e.getMessage());
        }

        return String.format("Lat: %.4f, Lng: %.4f", lat, lng);
    }
}