package com.ssafy.trip.repository;

import com.ssafy.trip.domain.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    Optional<Attraction> findByContentId(Long contentId);

    /**
     * 검색용 쿼리
     * - q: 제목 또는 주소(시도/구군/addr1/addr2) LIKE 검색
     * - typeId: content_type_id 필터 (null이면 무시)
     * - LIMIT 200 으로 결과 상한 (성능용)
     */
    @Query("""
    SELECT a
    FROM Attraction a
    WHERE (:q IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%')))
      AND (:typeId IS NULL OR a.contentTypeId = :typeId)
    """)
    List<Attraction> searchAttractions(
            @Param("q") String q,
            @Param("typeId") Integer typeId
    );

    boolean existsByContentId(Long contentId);

    List<Attraction> findBySido(String sido);
    List<Attraction> findBySidoAndGugun(String sido, String gugun);
}