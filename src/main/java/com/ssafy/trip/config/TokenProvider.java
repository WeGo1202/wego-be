package com.ssafy.trip.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Collections;
import java.util.Date;

@Service
public class TokenProvider {

    private final JwtProperties props;
    private final Key key;

    public TokenProvider(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    /** accessToken 생성 */
    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + props.accessTokenExpireMs());

        return Jwts.builder()
                .setIssuer(props.issuer())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim("email", email)
                .claim("role", role)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 토큰 유효성 검사 */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /** 토큰 → Authentication */
    public Authentication getAuthentication(String token) {
        Claims c = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        String email = c.get("email", String.class);
        String role = c.get("role", String.class);

        var auths = Collections.singleton(
                new SimpleGrantedAuthority(role != null ? role : "ROLE_USER")
        );
        return new UsernamePasswordAuthenticationToken(email, token, auths);
    }
}
