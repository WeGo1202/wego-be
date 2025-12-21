package com.ssafy.trip.repository;

import com.ssafy.trip.domain.RoutePlan;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoutePlanRepository extends JpaRepository<RoutePlan, Long> {
    @Query("SELECT rp FROM RoutePlan rp JOIN FETCH rp.plan WHERE rp.route.id = :routeId")
    List<RoutePlan> findAllByRouteIdWithPlan(@Param("routeId") Long routeId);
}
