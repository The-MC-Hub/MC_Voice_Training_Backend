package com.mchub.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

// Supabase Storage has no official Java SDK — it's a plain S3-compatible
// REST API, so we upload via RestTemplate PUT to /storage/v1/object/{bucket}/{path}.
@Service
public class SupabaseStorageService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.cv-bucket}")
    private String cvBucket;

    public String uploadCV(MultipartFile file, String userId) throws IOException {
        String safeName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        String path = userId + "/" + safeName;
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + cvBucket + "/" + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(serviceRoleKey);
        headers.setContentType(MediaType.parseMediaType(
                file.getContentType() != null ? file.getContentType() : "application/pdf"));

        HttpEntity<byte[]> request = new HttpEntity<>(file.getBytes(), headers);
        restTemplate.exchange(uploadUrl, HttpMethod.PUT, request, String.class);

        return supabaseUrl + "/storage/v1/object/public/" + cvBucket + "/" + path;
    }
}
