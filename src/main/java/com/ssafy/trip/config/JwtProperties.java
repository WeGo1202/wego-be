package com.ssafy.trip.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String issuer,
        String secretKey,
        long accessTokenExpireMs
) {}
