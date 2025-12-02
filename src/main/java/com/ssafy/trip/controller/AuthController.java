package com.ssafy.trip.controller;

import com.ssafy.trip.dto.SignupRequest;
import com.ssafy.trip.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid SignupRequest signupRequest){
        try {
            authService.signup(signupRequest);
            return ResponseEntity.ok().build();
        }
        catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest){
        try {
            String token = authService.login(loginRequest.getEmail(), loginRequest.getPassword());
            return ResponseEntity.ok(new TokenResponse(token));
        }
        catch (IllegalArgumentException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "이메일 또는 비밀번호가 올바르지 않습니다."));
        }
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @Email
        @NotBlank
        private String email;

        @NotBlank
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
    }
}

