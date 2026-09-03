package com.example.joinUs.controller;

import com.example.joinUs.dto.GroupDTO;
import com.example.joinUs.dto.summary.EventSummaryDTO;
import com.example.joinUs.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Recommendations")
@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    /* ---------------- GROUP RECOMMENDATIONS ---------------- */

    @Operation(
            summary = "Recommend groups joined by members similar to the authenticated user"
    )
    @GetMapping("/groups/by-similar-members")
    public List<GroupDTO> recommendGroupsBySimilarMembers(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return recommendationService.recommendGroupsBySimilarMembers(limit);
    }

    @Operation(
            summary = "Recommend groups based on the user's  topics"
    )
    @GetMapping("/groups/by-topics")
    public List<GroupDTO> recommendGroupsByTopics(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return recommendationService.recommendGroupsByTopics(limit);
    }

    /* ---------------- EVENT RECOMMENDATIONS ---------------- */

    @Operation(
            summary = "Recommend events related to the user's group topics"
    )
    @GetMapping("/events/by-group-topics")
    public List<EventSummaryDTO> recommendEventsBySharedGroupTopics(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return recommendationService.recommendEventsBySharedGroupTopics(limit);
    }

    @Operation(
            summary = "Recommend events attended by members with similar attendance patterns"
    )
    @GetMapping("/events/by-members")
    public List<EventSummaryDTO> recommendEventsByMembers(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return recommendationService.recommendEventsByMembers(limit);
    }

    @Operation(
            summary = "Recommend events attended by members of the user's groups"
    )
    @GetMapping("/events/by-peer-group-attendance")
    public List<EventSummaryDTO> recommendEventsByPeerGroupAttendance(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return recommendationService.recommendEventsByPeerGroupAttendance(limit);
    }
}
