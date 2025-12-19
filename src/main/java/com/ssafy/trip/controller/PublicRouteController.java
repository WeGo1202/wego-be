package com.ssafy.trip.controller;

import com.ssafy.trip.domain.Comment;
import com.ssafy.trip.dto.CommentRequest;
import com.ssafy.trip.dto.CommentResponse;
import com.ssafy.trip.dto.RouteDetailResponse;
import com.ssafy.trip.dto.RouteSummaryResponse;
import com.ssafy.trip.service.RouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/routes/public")
@RequiredArgsConstructor
public class PublicRouteController {

    private final RouteService routeService;

    // 🔹 공개 루트 게시판
    @GetMapping()
    public ResponseEntity<Page<RouteSummaryResponse>> getPublicRoutes(
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Authentication authentication
    ) {
        String email = null;
        if (authentication != null) email = authentication.getName();
        Page<RouteSummaryResponse> result = routeService.getPublicRoutes(sort, page, size, email);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public RouteDetailResponse getPublicRouteDetail(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = null;
        if (authentication != null) email = authentication.getName();
        return routeService.getPublicRouteDetail(email, id);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> getComments(
            @PathVariable Long id
    ) {
        return routeService.getComments(id);
    }

    @PostMapping("/{id}/comments")
    public Comment postComment(
            @PathVariable Long id,
            @RequestBody CommentRequest request,
            Authentication authentication
    ) {

        if (authentication == null) throw new IllegalArgumentException("게스트는 댓글을 작성할 수 없습니다");
        String email = authentication.getName();
        return routeService.postComment(email, request, id);
    }
}
