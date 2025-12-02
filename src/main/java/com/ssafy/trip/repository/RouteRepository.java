package com.ssafy.trip.repository;

import com.ssafy.trip.domain.Member;
import com.ssafy.trip.domain.Route;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByMember(Member member);
}
