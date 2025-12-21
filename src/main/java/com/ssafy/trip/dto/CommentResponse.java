package com.ssafy.trip.dto;

import com.ssafy.trip.domain.Comment;
import com.ssafy.trip.domain.Member;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CommentResponse {

    private Long id;
    private String authorName;
    private String authorNickname;
    private LocalDateTime createdAt;
    private String content;

    public static CommentResponse from(Comment comment, Member member) {
        return CommentResponse.builder()
                .id(comment.getId())
                .authorName(member.getName())
                .authorNickname(member.getNickname())
                .createdAt(comment.getCreatedAt())
                .content(comment.getContent())
                .build();
    }
}
