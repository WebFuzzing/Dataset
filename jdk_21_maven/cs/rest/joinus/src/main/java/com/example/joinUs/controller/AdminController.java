package com.example.joinUs.controller;

import com.example.joinUs.dto.GroupCommunityDTO;
import com.example.joinUs.dto.analytics.*;
import com.example.joinUs.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * Admin analytics & graph endpoints.
 *
 * Protect this controller in SecurityConfig: .requestMatchers("/admin/**").hasRole("ADMIN")
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin", description = "Admin-only analytics ")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/analytics/users/activity-score")
    @Operation(
            summary = "Top users by activity score",
            description = "Returns the most active users based on a computed activity score (for example: eventCount + groupCount + other signals)."
    )
    public ResponseEntity<List<ActivityScorePerUserAnalytic>> topUsersByActivityScore(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(adminService.topUsersByActivityScore(limit));
    }

    @GetMapping("/analytics/cities/activity")
    @Operation(
            summary = "Most active cities",
            description = "Ranks cities by activityRatio (activeMembers/totalMembers). A user is considered active if they have at least one upcoming event."
    )
    public ResponseEntity<List<CityActivityAnalytic>> mostActiveCities(
            @RequestParam(defaultValue = "10") int limit
    ) {
        // NOTE: your service currently ignores the limit (repository method returns fixed top 10).
        // Keep the param for swagger/API consistency; optionally update the aggregation pipeline to accept ?0.
        return ResponseEntity.ok(adminService.mostActiveCities(limit));
    }

//    @GetMapping("/analytics/cities/groups-last-10-years")
//    @Operation(
//            summary = "Top cities by groups created in last 10 years",
//            description = "Counts how many groups were created per city during the last 10 years and returns the highest-ranking cities. Helps identify geographic growth."
//    )
//    public ResponseEntity<List<GroupsPerCityAnalytic>> topCitiesByGroupsLast10Years(
//            @RequestParam(defaultValue = "20") int limit
//    ) {
//        return ResponseEntity.ok(adminService.topCitiesByGroupsLast10Years(limit));
//    }

    @GetMapping("/analytics/topics/trending-per-city")
    @Operation(
            summary = "Top trending topics per city",
            description = "For each city, returns the top topics found in users' profiles, along with how many users have each topic. topicCount controls how many topics per city are returned."
    )
    public ResponseEntity<List<TrendingTopicPerCityAnalytic>> topTrendingTopicsPerCity(
            @RequestParam(defaultValue = "5") int topicCount
    ) {
        return ResponseEntity.ok(adminService.topTrendingTopicsPerCity(topicCount));
    }

    @GetMapping("/analytics/events/paid-vs-free")
    @Operation(
            summary = "Paid vs Free event popularity",
            description = "Compares FREE vs PAID events using totalAttendance, eventsCount, and avgAttendance. An event is FREE if fee is missing/null or fee.amount is 0."
    )
    public ResponseEntity paidVsFreePopularity() {
        return ResponseEntity.ok(adminService.paidVsFreePopularity());
    }

    @GetMapping("/graph/groups/communities")
    @Operation(
            summary = "Find group communities (Neo4j)",
            description = "Finds related group communities using Neo4j by measuring how many members groups share. sharedMembers is the minimum overlap; limit caps the results."
    )
    public ResponseEntity<List<GroupCommunityDTO>> findGroupCommunities(
            @RequestParam(defaultValue = "5") int sharedMembers,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(adminService.findGroupCommunities(sharedMembers, limit));
    }
}
