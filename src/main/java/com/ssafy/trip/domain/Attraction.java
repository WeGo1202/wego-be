package com.ssafy.trip.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attractions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 공공데이터 contentId (유일값)
    @Column(nullable = false, unique = true)
    private Long contentId;

    @Column(nullable = false, length = 200)
    private String title;

    private String addr1;
    private String addr2;

    private String sido;
    private String gugun;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "content_type_id")
    private Integer contentTypeId;
    private String imageUrl;
    private String tel;
}
