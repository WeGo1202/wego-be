package com.ssafy.trip.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;


/** Address엔티티 : Emeddable하게 선언하여 Plan과 Route, Hotplace 엔티티에서 사용 
 * @author 최서현
 */

@Embeddable
@Data
@NoArgsConstructor
public class Address {

    private String name; //장소명
    
    //주소
    private String sido; //시도
    private String gugun; //구군
    private String town; //읍면동

    //좌표
    @Column(name = "latitude", columnDefinition = "FLOAT")
    private Float latitude; //위도
    @Column(name = "longitude", columnDefinition = "FLOAT")
    private Float longitude; //경도

    //상세주소
    @Column(name="detail_Address")
    private String detailAddress;
}
