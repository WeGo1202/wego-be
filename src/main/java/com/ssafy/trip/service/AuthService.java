package com.ssafy.trip.service;

import com.ssafy.trip.config.TokenProvider;
import com.ssafy.trip.domain.Member;
import com.ssafy.trip.dto.SignupRequest;
import com.ssafy.trip.repository.MemberRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Transactional
    public String login(String email, String password) {
        Member m = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if(!passwordEncoder.matches(password,m.getPassword())){
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return tokenProvider.generateToken(m.getEmail(),m.getRole());
    }

    @Transactional
    public void signup(SignupRequest signupRequest) {
        if(memberRepository.existsByEmail(signupRequest.getEmail())){
            throw new IllegalStateException("이미 가입된 이메일입니다.");
        }

        Member member = Member.builder()
                .email(signupRequest.getEmail())
                .password(passwordEncoder.encode(signupRequest.getPassword()))
                .name(signupRequest.getName())
                .nickname(signupRequest.getNickname())
                .phone(signupRequest.getPhone())
                .role("ROLE_USER")
                .build();
        memberRepository.save(member);
    }
}
