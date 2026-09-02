package com.example.joinUs.service;

import com.example.joinUs.Utils;
import com.example.joinUs.dto.EventDTO;
import com.example.joinUs.dto.ResponseMessage;
import com.example.joinUs.dto.summary.EventSummaryDTO;
import com.example.joinUs.mapping.EventMapper;
import com.example.joinUs.mapping.embedded.EventEmbeddedMapper;
import com.example.joinUs.mapping.embedded.GroupEmbeddedMapper;
import com.example.joinUs.mapping.summary.EventSummaryMapper;
import com.example.joinUs.model.embedded.EventEmbedded;
import com.example.joinUs.model.embedded.GroupEmbedded;
import com.example.joinUs.model.mongodb.*;
import com.example.joinUs.model.neo4j.EventNeo4J;
import com.example.joinUs.repository.EventNeo4JRepository;
import com.example.joinUs.repository.EventRepository;
import com.example.joinUs.repository.GroupRepository;
import com.example.joinUs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;


@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private CityService cityService;

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private EventEmbeddedMapper eventEmbeddedMapper;
    @Autowired
    private GroupEmbeddedMapper groupEmbeddedMapper;

    @Autowired
    private EventSummaryMapper eventSummaryMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private EventNeo4JRepository eventNeo4JRepository;

    //    @Autowired
    //    private Neo4jClient neo4jClient;

    @Autowired
    private EventNeo4JRepository eventNeo4JRepo;

    public List<EventNeo4J> getEventsFromNeo4j() {
        return eventNeo4JRepo.findAll();
    }

    public Page<EventSummaryDTO> getAllEvents(int page, int size) {
        //        Page<EventDTO> events = eventRepository.findAllEvents(PageRequest.of(page, size));
        Page<Event> events = eventRepository.findAll(PageRequest.of(page, size));
        return events.map(e -> eventSummaryMapper.toDTO(e));
    }

    public EventDTO getEventById(String id) {

        //     return    eventRepository.findEventWithId(id);

        Event event = getEventOrThrow(id);
        EventDTO eventDTO = eventMapper.toDTO(event);
        //
        return eventDTO;
    }

    public ResponseMessage attendEvent(String id) {
        Event event = getEventOrThrow(id);
        try {
            User user = Utils.getUserFromContext();
            List<EventEmbedded> userUpcomingEvents = user.getUpcomingEvents();
            userUpcomingEvents.add(eventEmbeddedMapper.toDTO(event));
            user.setUpcomingEvents(userUpcomingEvents);
            user.setEventCount(user.getEventCount() + 1);
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(id)),
                    new Update().inc("member_count", 1),
                    Event.class
            );// This is atomic  operation per document ,so it is thread safe if 2 users attend the same event simultaneously

            userRepository.save(user);
            eventNeo4JRepo.addUserAttending(id, user.getId());

            return new ResponseMessage("successful",
                    "Your attendance in the event " + event.getEventName() + " is confirmed");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public ResponseMessage revokeAttendance(String id) {

        try {
            User user = Utils.getUserFromContext();
            user.removeUpcomingEvent(id);
            user.setEventCount(user.getEventCount() - 1);
            userRepository.save(user);

            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(id)),
                    new Update().inc("member_count", -1),
                    Event.class
            ); // This is atomic  operation per document ,so it is thread safe if 2 users attend the same event simultaneously

            eventNeo4JRepo.revokeUserAttending(id, user.getId());
            //TODO complete the part for the Neo4J too
            return new ResponseMessage("successful", "Your attendance in the event  is revoked ");

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public EventDTO createEvent(EventDTO eventDTO) {

        EventDTO eventDTO1 = createEventDTO(eventDTO);
        try {
            Event entity = eventMapper.toEntity(eventDTO1);
            Event saved = eventRepository.save(entity);
            eventDTO1.setId(saved.getId());
            eventNeo4JRepo.save(eventMapper.toNeo4jEntity(eventDTO1));
            return eventMapper.toDTO(saved);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    private EventDTO createEventDTO(EventDTO eventDTO) {

        if (Utils.isNullOrEmpty(eventDTO.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId must be provided");
        }
        if (eventRepository.existsById(eventDTO.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event already exists: " + eventDTO.getId());
        }

        if (Utils.isNullOrEmpty(eventDTO.getEventName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "eventName must be provided");
        }
        if (Utils.isNullOrEmpty(eventDTO.getEventTime())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "eventTime must be provided");
        }

        eventDTO.setCreated(new Date());
        eventDTO.setUpdated(new Date());
        eventDTO.setMemberCount(0);

        if (Utils.isNullOrEmpty(eventDTO.getFee())) eventDTO.setFee(Fee.getDefault());

        Venue venue = updateVenueForEvent(eventDTO);
        if (venue != null) eventDTO.setVenue(venue);

        setCreatorGroupForCreate(eventDTO);

        return eventDTO;

    }

    public EventDTO updateEvent(EventDTO eventDTO, String id) {

        userService.checkUserHasPermissionToEditEvent(id);

        Event event = getEventOrThrow(id);

        try {

            //            eventDTO.setCreatorGroup(groupEmbeddedMapper.toDTO(event.getCreatorGroup()));
            eventDTO.setCreatorGroup(event.getCreatorGroup());
            if (Utils.isNullOrEmpty(eventDTO.getEventName())) eventDTO.setEventName(event.getEventName());
            if (Utils.isNullOrEmpty(eventDTO.getEventTime())) eventDTO.setEventTime(event.getEventTime());
            if (Utils.isNullOrEmpty(eventDTO.getDescription())) eventDTO.setDescription(event.getDescription());
            if (Utils.isNullOrEmpty(eventDTO.getFee())) {
                eventDTO.setFee(event.getFee());
            }
            if (Utils.isNullOrEmpty(eventDTO.getDuration())) eventDTO.setDuration(event.getDuration());
            if (Utils.isNullOrEmpty(eventDTO.getCategory())) eventDTO.setCategory(event.getCategory());

            if (Utils.isNullOrEmpty(eventDTO.getVenue())) {
                eventDTO.setVenue(event.getVenue());
            } else cityService.parseCity(event.getVenue().getCity());

            //Fields that should remain the same
            eventDTO.setUpdated(new Date());
            eventDTO.setCreated(event.getCreated());
            eventDTO.setMemberCount(event.getMemberCount());

            //            eventDTO.getCreatorGroup().setId(event.getCreatorGroup().getId());
            //            eventDTO.getCreatorGroup().setGroupName(event.getEventName());
            eventDTO.setId(event.getId());
            eventRepository.save(eventMapper.toEntity(eventDTO));
            eventNeo4JRepo.save(eventMapper.toNeo4jEntity(eventDTO));//TODO this can be parallelized
            return eventDTO;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    private Event getEventOrThrow(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Event not found: " + id));
    }

    public void deleteEvent(String id) {
        Event event = getEventOrThrow(id);
        userService.checkUserHasPermissionToEditEvent(id);

        try {
            User user = Utils.getUserFromContext();

            user.removeUpcomingEvent(id);//TODO also complete the edge removal for Neo4J

            GroupEmbedded group = event.getCreatorGroup();

            eventRepository.deleteById(id);
            eventNeo4JRepo.deleteEvent(id);

            userRepository.save(user);

            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(group.getGroupId())),
                    new Update().pull("upcoming_events",
                                    Query.query(Criteria.where("event_id").is(event.getId())))
                            .inc("event_count", -1)
                    , Group.class);

        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }

    }

    public Page<EventSummaryDTO> search(
            String name,
            String city,
            String category,
            Integer minMembers,
            Integer maxMembers,
            Date fromDate,
            Date toDate,
            Integer maxFee,
            int page,
            int pageSize
    ) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Query query = new Query();

        if (name != null)
            query.addCriteria(Criteria.where("event_name").regex(name, "i"));

        if (city != null)
            query.addCriteria(Criteria.where("venue.city.name").regex(city, "i"));

        if (minMembers != null || maxMembers != null) {
            Criteria memberCountCriteria = Criteria.where("member_count");

            if (minMembers != null)
                memberCountCriteria.gte(minMembers);

            if (maxMembers != null)
                memberCountCriteria.lte(maxMembers);

            query.addCriteria(memberCountCriteria);
        }

        if (fromDate != null || toDate != null) {
            Criteria dateCriteria = Criteria.where("event_time");

            if (fromDate != null)
                dateCriteria.gte(fromDate);

            if (toDate != null)
                dateCriteria.lte(toDate);

            query.addCriteria(dateCriteria);
        }

        if (category != null)
            query.addCriteria(Criteria.where("category.name").is(category));

        if (maxFee != null)
            query.addCriteria(Criteria.where("fee.amount").lte(maxFee));
        query.with(pageable);

        List<EventSummaryDTO> results = mongoTemplate.find(query, Event.class)
                .stream()
                .map(e -> eventSummaryMapper.toDTO(e))
                .toList();

        return new PageImpl<EventSummaryDTO>(results, pageable, -1);
        //        return results;

    }

    private Venue updateVenueForEvent(EventDTO eventDTO) {
        Venue venue = eventDTO.getVenue();
        if (venue != null) {
            cityService.parseCity(venue.getCity());
        }
        return venue;
    }

    private void setVenueAndCityForUpdate(Event newEvent, EventDTO eventDTO) {
        Venue venue = updateVenueForEvent(eventDTO);
        if (venue != null) newEvent.setVenue(venue);
    }

    public void setCreatorGroupForCreate(EventDTO eventDTO) {

        if (Utils.isNullOrEmpty(eventDTO.getCreatorGroup())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Please provide a groupId or a groupName that will represent the creatorGroup of the event");
        }
        String groupName = eventDTO.getCreatorGroup().getGroupName();
        String groupId = eventDTO.getCreatorGroup().getGroupId();

        List<Group> groups = groupRepository.findGroupByGroupIdOrGroupName(groupId, groupName);
        if (Utils.isNullOrEmpty(groups)) {
            throw new ResponseStatusException(NOT_FOUND,
                    "No group exists with groupId " + groupId + " or groupName " + groupName);
        }

        Group group = groups.get(0);

        mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(group.getId())),
                new Update().push("organizer_members",
                                eventEmbeddedMapper.toDTO(eventMapper.toEntity(eventDTO)))
                        .inc("event_count", 1),
                Group.class); //To ensure atomicity if from the same groups are being created events simultaneously

        userService.checkUserHasPermissionToEditGroup(group.getId());

        eventDTO.setCreatorGroup(groupEmbeddedMapper.toDTO(group));

        eventNeo4JRepo.addGroupEventRelation(eventDTO.getCreatorGroup().getGroupId(), eventDTO.getId());
        //Neo4j operation
    }

    public List<Event> findEventsByCreatorGroup(String creatorGroupId) {
        return eventRepository.findEventsByCreatorGroup(creatorGroupId);
    }

    public List<EventSummaryDTO> findEventsOfUser(String userId) {
        return eventNeo4JRepo.findAllEventsAttendedByUser(userId).stream()
                .map(e -> eventMapper.toDTOFromNeo4j(e)).toList();
    }

    public List<EventSummaryDTO> findEventsOrganizedByGroup(String groupId) {
        return eventNeo4JRepo.findAllEventsOrganizedByGroup(groupId)
                .stream().map(e -> eventMapper.toDTOFromNeo4j(e)).toList();
    }

    //    public Page<EventDTO> filterByCategory(String category, int page, int size) {
    //        Pageable pageable = PageRequest.of(page, size + 1);
    //        return eventRepository.findByCategoryName(category, pageable);
    //    }
    //
    //
    //    public Page<EventDTO> filterByMemberCount(
    //            int min,
    //            int max,
    //            int page, int size
    //    ) {
    //        long startTime = System.currentTimeMillis();
    //
    //        Pageable pageable =
    //                PageRequest.of(page, size + 1);
    //
    //        Page<EventDTO> page1 = eventRepository.findByMemberCountBetween(min, max, pageable);
    //        long endTime = System.currentTimeMillis();
    //        System.out.println("PROCESS USING QUERY LASTED : " + (endTime - startTime) + " milliseconds");
    //        return page1;
    //    }
    //
    //    public Page<EventDTO> filterByDateRange(LocalDateTime from, LocalDateTime to, int page, int size) {
    //
    //        try {
    //            LocalDateTime date = LocalDateTime.now();
    //
    //            if (to != null)
    //                return eventRepository.findByEventTimeBetween(from != null ? from : date, to, PageRequest.of(page, size + 1));
    //            else {
    //                return eventRepository.findByEventTimeAfter(from != null ? from : date, PageRequest.of(page, size + 1));
    //            }
    //        } catch (Exception e) {
    //            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    //        }
    //
    //
    //    }
    //
    //    public Page<EventDTO> filterByMaxFee(int maxFee, int page, int size) {
    //        return eventRepository.findByEventFeeIsLessOrEqual(maxFee, PageRequest.of(page, size + 1));
    //    }
    //
    //    public Page<EventDTO> filterByCity(String city, int page, int size) {
    //        return eventRepository.findByCityName(city, PageRequest.of(page, size + 1));
    //    }
    //
    //    public Page<EventDTO> findByEventNameContainingIgnoreCase(String name, int page, int size) {
    //        Pageable pageable = PageRequest.of(page, size + 1);
    //
    //        return eventRepository.searchByEventName(name, pageable);
    //    }

}


