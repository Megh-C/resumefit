package com.resumematcher.resumefit.service;

import com.resumematcher.resumefit.dto.response.ResumeAnalysisResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.session-ttl-minutes}")
    private long sessionTtlMinutes;

    private static final String KEY_PREFIX = "resume:session";

    public void save(String sessionId, ResumeAnalysisResponseDto response){
        try{
            String key = KEY_PREFIX + sessionId;
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key,json,sessionTtlMinutes, TimeUnit.MINUTES);
            log.info("Cached analysis result for session {} (TTL: {} minutes)",sessionId,sessionTtlMinutes);
        }catch(Exception e){
            log.warn("Failed to cache result for session {} - proceeding without caching: {}",sessionId, e.getMessage());
        }
    }

    public Optional<ResumeAnalysisResponseDto> get(String sessionId){
        try{
            String key = KEY_PREFIX + sessionId;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return Optional.empty();
            ResumeAnalysisResponseDto response = objectMapper.readValue(json, ResumeAnalysisResponseDto.class);
            log.info("Cache HIT for session {}", sessionId);
            return Optional.of(response);
        }catch(Exception e){
            log.warn("Failed to read cache for session {} — will reprocess: {}", sessionId, e.getMessage());
            return Optional.empty();
        }
    }

}
