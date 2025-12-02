package com.ssafy.trip.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "plan")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String thumbnail;

    private Integer period;

    @Column(name="is_public")
    @ColumnDefault("false")
    private boolean isPublic;

    @Embedded
    private Address location;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
