package com.ssafy.trip.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "route")   // 기존 테이블 이름 유지!!
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EntityListeners(AuditingEntityListener.class)
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

    // 🔹 소유자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnore
    private Member member;

    // 🔹 Route - RoutePlan(중간 테이블) 1:N
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayIndex ASC, orderIndex ASC")
    private List<RoutePlan> routePlans = new ArrayList<>();

    // 🔹 공개 여부 (true = 공개, false = 비공개)
    @Column(nullable = false)
    private Boolean isPublic = true;

    // 🔹 좋아요 수
    @Column(nullable = false)
    private long likeCount = 0L;

    // 🔹 생성/수정 시간 (정렬용)
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // === 편의 메서드 ===
    public void increaseLike() {
        this.likeCount++;
    }

    public void decreaseLike() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void updateVisibility(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
