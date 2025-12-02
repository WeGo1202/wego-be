package com.ssafy.trip.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "route")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 코스 이름 (예: "공주 당일치기 코스")
    @Column(nullable = false)
    private String title;

    // 간단 설명
    @Column(length = 1000)
    private String description;

    // 총 여행 일수 (옵션)
    private Integer totalDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnore // 직렬화 루프 방지 (DTO 따로 쓰는 게 더 베스트)
    private Member member;

    // Route - RoutePlan(중간 테이블) 1:N
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayIndex ASC, orderIndex ASC")
    private List<RoutePlan> routePlans = new ArrayList<>();
}
