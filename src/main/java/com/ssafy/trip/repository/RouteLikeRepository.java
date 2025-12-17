package com.ssafy.trip.repository;

import com.ssafy.trip.domain.Member;
import com.ssafy.trip.domain.Route;
import com.ssafy.trip.domain.RouteLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RouteLikeRepository extends JpaRepository<RouteLike, Long> {
    Optional<RouteLike> findByRouteAndMember(Route route, Member member);
    long countByRoute(Route route);
}
