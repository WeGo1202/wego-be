package com.ssafy.trip.repository;

import com.ssafy.trip.domain.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    Optional<Attraction> findByContentId(Long contentId);

    boolean existsByContentId(Long contentId);

    List<Attraction> findBySido(String sido);
    List<Attraction> findBySidoAndGugun(String sido, String gugun);
}