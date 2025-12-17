package com.ssafy.trip.repository;

import com.ssafy.trip.domain.Member;
import com.ssafy.trip.domain.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByMember(Member member);
    List<Route> findByMemberEmailOrderByCreatedAtDesc(String email);
    Page<Route> findByIsPublicTrue(Pageable pageable);
    // 공개 루트 최신순
    List<Route> findByIsPublicTrueOrderByIdDesc();

    // ❤️ 좋아요 기능 붙일 때 나중에 사용 (지금 likeCount 없으면 일단 주석 처리)
     List<Route> findByIsPublicTrueOrderByLikeCountDesc();
}
