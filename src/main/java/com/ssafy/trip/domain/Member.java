package com.ssafy.trip.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", nullable = true)
    private String name;
    @Column(name = "email", length = 2048, nullable = false, unique = true)
    private String email;
    private String password;
    private String nickname;
    private String profile;
    private String phone;
    private String address;
    private String role;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Plan> plans = new ArrayList<>();

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Route> routes = new ArrayList<>();
}
