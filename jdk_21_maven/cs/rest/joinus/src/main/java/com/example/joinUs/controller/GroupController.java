package com.example.joinUs.controller;

import com.example.joinUs.dto.GroupDTO;
import com.example.joinUs.dto.summary.EventSummaryDTO;
import com.example.joinUs.dto.summary.GroupSummaryDTO;
import com.example.joinUs.service.GroupService;
import com.example.joinUs.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Groups")
@RestController
@RequestMapping("/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;

    // READ: list all groups
    @GetMapping
    public Page<GroupSummaryDTO> getAllGroups(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return groupService.getAllGroups(page, size);
    }

    // READ: get one group by groupId
    @GetMapping("/{id}")
    public GroupDTO getGroupById(@PathVariable String id) {
        return groupService.getGroupById(id);
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity joinGroup(@PathVariable String groupId) {
        return ResponseEntity.ok(groupService.joinGroup(groupId));
    }

    // Leave group
    @PostMapping("/{groupId}/leave")
    public ResponseEntity leaveGroup(@PathVariable String groupId) {
        return ResponseEntity.ok(groupService.leaveGroup(groupId));
    }

    // Add organizer
    @PostMapping("/{groupId}/organizers/{organizerId}")
    public ResponseEntity addOrganizer(
            @PathVariable String groupId,
            @PathVariable String organizerId
    ) {
        return ResponseEntity.ok(groupService.addOrganizer(groupId, organizerId));
    }

    // CREATE: create a new group
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity createGroup(@RequestBody GroupDTO groupDTO) {
        return ResponseEntity.ok(groupService.createGroup(groupDTO));
    }

    // UPDATE (partial): update an existing group
    @PatchMapping("/{id}")
    public ResponseEntity updateGroup(@PathVariable String id, @RequestBody GroupDTO groupDTO) {

        return ResponseEntity.ok(groupService.updateGroup(id, groupDTO));
    }

    @GetMapping("/{groupId}/events")
    @Operation(summary = "Get all events (historical and upcoming) organized by a group")
    public List<EventSummaryDTO> getEventsOrganizedByGroup(
            @PathVariable String groupId
    ) {
        return groupService.findAllEventOrganizedByGroup(groupId);
    }

    @GetMapping("/search")
    public Page<GroupSummaryDTO> searchGroups(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer minMembers,
            @RequestParam(required = false) Integer maxMembers,
            @RequestParam(required = false) Integer minEvents,
            @RequestParam(required = false) Integer maxEvents,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "100") Integer pageSize

    ) {
        return groupService.search(
                name,
                city,
                category,
                minMembers,
                maxMembers,
                minEvents,
                maxEvents,
                page,
                pageSize
        );
    }

    // DELETE: delete a group
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable String id) {
        groupService.deleteGroup(id);
    }
}
