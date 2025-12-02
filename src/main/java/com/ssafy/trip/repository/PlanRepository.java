package com.ssafy.trip.repository;

import com.ssafy.trip.domain.Plan;
import com.ssafy.trip.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByMember(Member member);
}
