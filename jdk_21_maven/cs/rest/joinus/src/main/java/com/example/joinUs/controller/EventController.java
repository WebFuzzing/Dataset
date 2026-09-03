package com.example.joinUs.controller;

import com.example.joinUs.dto.EventDTO;
import com.example.joinUs.dto.summary.EventSummaryDTO;
import com.example.joinUs.service.EventService;
import com.example.joinUs.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Tag(name = "Events")
@RestController()
@RequestMapping("/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private UserService userService;

    @GetMapping("")
    public Page<EventSummaryDTO> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        return eventService.getAllEvents(page, size);
    }

    @GetMapping("/{id}")
    public EventDTO getEventById(@PathVariable String id) {
        return eventService.getEventById(id);
    }

    @PostMapping("")
    public ResponseEntity createEvent(@RequestBody EventDTO eventDTO) {
        return ResponseEntity.ok(eventService.createEvent(eventDTO));
    }

    @PatchMapping("/{id}")
    public ResponseEntity editEvent(@PathVariable String id, @RequestBody EventDTO eventDTO) {

        return ResponseEntity.ok(eventService.updateEvent(eventDTO, id));
    }

    @PatchMapping("/{id}/attend")
    public ResponseEntity attendEvent(@PathVariable String id) {

        return ResponseEntity.ok(eventService.attendEvent(id));
    }

    @PatchMapping("/{id}/revokeAttendance")
    public ResponseEntity revokeAttendance(@PathVariable String id) {
        return ResponseEntity.ok(eventService.revokeAttendance(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable String id) {
        eventService.deleteEvent(id);
    }

    @GetMapping("/search")
    public Page<EventSummaryDTO> searchEvents(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer minMembers,
            @RequestParam(required = false) Integer maxMembers,
            @Parameter(example = "2026-12-15T01:45:30Z")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Date fromDate,
            @Parameter(example = "2027-12-15T01:45:30Z")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Date toDate,
            @RequestParam(required = false) Integer maxFee,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "100") Integer pageSize

    ) {
        return eventService.search(
                name,
                city,
                category,
                minMembers,
                maxMembers,
                fromDate,
                toDate,
                maxFee,
                page,
                pageSize
        );
    }

    //    @GetMapping("/searchByName")
    //    public Page<EventDTO> searchByName(@RequestParam String name,
    //                                 @RequestParam(defaultValue = "0") int page,
    //                                 @RequestParam(defaultValue = "100") int size ) {
    //        return eventService.findByEventNameContainingIgnoreCase(name,page,size);
    //    }
    //
    //    @GetMapping("/filterByCity")
    //    public Page<EventDTO> filterByCity(@RequestParam String city,
    //                                 @RequestParam(defaultValue = "0") int page,
    //                                 @RequestParam(defaultValue = "100") int size ) {
    //        return eventService.filterByCity(city,page,size);
    //    }
    //
    //    @GetMapping("/filterByCategory")
    //    public Page<EventDTO> filterByCategory(@RequestParam String category,
    //                                     @RequestParam(defaultValue = "0") int page,
    //                                     @RequestParam(defaultValue = "100") int size ) {
    //        return eventService.filterByCategory(category,page,size);
    //    }
    //
    //    @GetMapping("/filterByMembersCount")
    //    public Page<EventDTO> filterByMembersCount(
    //            @RequestParam int min,
    //            @RequestParam int max,
    //            @RequestParam int page,
    //            @RequestParam(defaultValue = "100") int size
    //    ) {
    //        return eventService.filterByMemberCount(min, max,page,size);
    //    }
    //
    //    @GetMapping("/filterByDate")
    //    public Page<EventDTO> filterByDate( //TODO Change Date's type to include only the Date and not the hh:mm:ssss
    //                                        @RequestParam(required = false)
    //                                        @Parameter(example = "2026-03-25T23:00:00.000")
    //                                        LocalDateTime from,
    //                                        @RequestParam(required = false)
    //                                            @Parameter(example = "2027-01-25T23:00:00.000")
    //                                            LocalDateTime to,
    //                                        @RequestParam(defaultValue = "0") int page,
    //                                        @RequestParam(defaultValue = "100") int size
    //    ) {
    //        return eventService.filterByDateRange(from, to,page,size);
    //    }
    //
    //    @GetMapping("/filterByFee")
    //    public Page<EventDTO> filterByFee(@RequestParam int maxFee,
    //                                   @RequestParam(defaultValue = "0") int page,
    //                                   @RequestParam(defaultValue = "100") int size ) {
    //        return eventService.filterByMaxFee(maxFee,page,size);
    //    }

}
