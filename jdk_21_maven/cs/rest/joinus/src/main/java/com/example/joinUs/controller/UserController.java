package com.example.joinUs.controller;

import com.example.joinUs.dto.GroupDTO;
import com.example.joinUs.dto.UserDTO;
import com.example.joinUs.dto.summary.EventSummaryDTO;
import com.example.joinUs.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public ResponseEntity getUserProfile() {
        return ResponseEntity.ok(userService.getUserProfile());
    }

    @PutMapping("/profile")
    public ResponseEntity editUserProfile(@RequestBody UserDTO user) {
        return ResponseEntity.ok().body(userService.editUserProfile(user));
    }

    @DeleteMapping("/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUserProfile(HttpServletRequest request) {
        userService.deleteProfile();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    /**
     * Get all events the authenticated user is attending
     */
    @GetMapping("/allEvents")
    @Operation(summary = "Get all events (upcoming and historical) of the authenticated user")
    public List<EventSummaryDTO> getMyEvents() {
        return userService.findAllEvents();
    }

    /**
     * Get all groups the authenticated user belongs to
     */
    @GetMapping("/allGroups")
    @Operation(summary = "Get all groups (upcoming and historical) of the authenticated user")
    public List<GroupDTO> getMyGroups() {
        return userService.findAllGroups();
    }

}
