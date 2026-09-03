package com.example.joinUs.service;

import com.example.joinUs.Utils;
import com.example.joinUs.dto.GroupDTO;
import com.example.joinUs.dto.ResponseMessage;
import com.example.joinUs.dto.UserDTO;
import com.example.joinUs.dto.summary.EventSummaryDTO;
import com.example.joinUs.dto.summary.GroupSummaryDTO;
import com.example.joinUs.mapping.GroupMapper;
import com.example.joinUs.mapping.GroupPhotoMapper;
import com.example.joinUs.mapping.embedded.UserEmbeddedMapper;
import com.example.joinUs.mapping.summary.GroupSummaryMapper;
import com.example.joinUs.model.mongodb.Group;
import com.example.joinUs.model.mongodb.User;
import com.example.joinUs.repository.GroupNeo4JRepository;
import com.example.joinUs.repository.GroupRepository;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;


@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    GroupNeo4JRepository groupNeo4JRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CityService cityService;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private EventService eventService;

    @Autowired
    private GroupSummaryMapper groupSummaryMapper;

    @Autowired
    private UserEmbeddedMapper userEmbeddedMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private GroupPhotoMapper groupPhotoMapper;

    public ResponseMessage joinGroup(String id) {
        Group group = getGroupOrThrow(id);
        try {

            User user = Utils.getUserFromContext();  //TODO maybe throw error if user is already part of group

            user.setGroupCount(user.getGroupCount() + 1);

            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(id)),
                    new Update().inc("member_count", 1),
                    Group.class
            );// This is atomic  operation per document ,so it is thread safe if 2 users join the same group simultaneously

            userService.saveUser(user);
            userService.addUserToGroup(user.getId(), id); //Neo4J operation

            return new ResponseMessage("successful",
                    "Your attendance in the group " + group.getGroupName() + " is confirmed");

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public ResponseMessage leaveGroup(String id) {
        Group group = getGroupOrThrow(id);
        try {

            User user = Utils.getUserFromContext();//TODO maybe throw error if user is not part of group
            user.setGroupCount(user.getGroupCount() - 1);
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(id)),
                    new Update().inc("member_count", -1),
                    Group.class
            );// This is atomic  operation per document ,so it is thread safe if 2 users join the same group simultaneously

            userService.saveUser(user);
            userService.removeUserFromGroup(user.getId(), id); //Neo4J operation
            return new ResponseMessage("successful", "You left the group " + group.getGroupName() + " ");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    public ResponseMessage addOrganizer(String groupId, String organizerId) {
        Group group = getGroupOrThrow(groupId);

        if (group.getOrganizers()
                .stream()
                .anyMatch(u -> u.getMemberId().equals(organizerId))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User is  already an organizer at this group " + groupId);
        }

        userService.checkUserHasPermissionToEditGroup(groupId);
        User user = userService.findUserById(organizerId).orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "No user exists with id " + organizerId)
        );
        group.getOrganizers().add(userEmbeddedMapper.toDTO(user));
        groupRepository.save(group);
        userService.addUserToGroup(organizerId, groupId); //Neo4J Safe because the query is MERGE not create
        return new ResponseMessage("successful", "Organizer with id" + organizerId + " was added successfully");
    }

    public Page<GroupSummaryDTO> getAllGroups(int page, int size) {
        Page<Group> groups = groupRepository.findAll(PageRequest.of(page, size));
        return groups.map(e -> groupSummaryMapper.toDTO(e));
    }

    public GroupDTO getGroupById(String id) {
        Group group = getGroupOrThrow(id);
        return groupMapper.toDTO(group);
    }

    public PageImpl<GroupSummaryDTO> search(
            String name,
            String city,
            String category,
            Integer minMembers,
            Integer maxMembers,
            Integer minEvents,
            Integer maxEvents,
            int page,
            int pageSize
    ) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Query query = new Query();

        if (name != null)
            query.addCriteria(Criteria.where("group_name").regex(name, "i"));

        if (city != null)
            query.addCriteria(Criteria.where("city.name").regex(city, "i"));

        if (minMembers != null || maxMembers != null) {
            Criteria memberCountCriteria = Criteria.where("member_count");

            if (minMembers != null)
                memberCountCriteria.gte(minMembers);

            if (maxMembers != null)
                memberCountCriteria.lte(maxMembers);

            query.addCriteria(memberCountCriteria);
        }

        if (minEvents != null || maxEvents != null) {
            Criteria eventCountCriteria = Criteria.where("event_count");

            if (minEvents != null)
                eventCountCriteria.gte(minEvents);

            if (maxEvents != null)
                eventCountCriteria.lte(maxEvents);

            query.addCriteria(eventCountCriteria);
        }

        if (category != null)
            query.addCriteria(Criteria.where("category.name").is(category));

        query.with(pageable);

        List<GroupSummaryDTO> results = mongoTemplate.find(query, Group.class)
                .stream()
                .map(e -> groupSummaryMapper.toDTO(e))
                .toList();

        return new PageImpl<GroupSummaryDTO>(results, pageable, -1);
        //        return results;

    }

    public GroupDTO createGroup(GroupDTO groupDTO) {

        User user = Utils.getUserFromContext();

        // Minimal approach: require groupId to be present and unique
        //        if (Utils.isNullOrEmpty(groupDTO.getId())) {
        //            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be provided");
        //        }
        //        if (groupRepository.existsById(groupDTO.getId())) {
        //            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group already exists: " + groupDTO.getId());
        //        }
        groupDTO.setCreated(new Date());
        groupDTO.setUpcomingEvents(new ArrayList<>());
        groupDTO.setMemberCount(1);
        groupDTO.setEventCount(0);
        groupDTO.getOrganizers().add(userEmbeddedMapper.toDTO(user));
        cityService.parseCity(groupDTO.getCity());
        Group entity = groupMapper.toEntity(groupDTO);
        Group saved = groupRepository.save(entity);
        groupDTO.setId(saved.getId());
        groupNeo4JRepository.save(groupMapper.toNeo4JEntity(groupDTO));

        return groupMapper.toDTO(saved);
    }

    public GroupDTO updateGroup(String id, GroupDTO groupDTO) { // TODO revisit, especially PATCH semantics

        Group existing = getGroupOrThrow(id);

        userService.checkUserHasPermissionToEditGroup(id);
        // Minimal patch semantics:
        // - map incoming DTO to entity and copy non-null fields onto existing
        if (Utils.isNullOrEmpty(groupDTO.getGroupName())) groupDTO.setGroupName(existing.getGroupName());
        if (Utils.isNullOrEmpty(groupDTO.getGroupPhoto()))
            groupDTO.setGroupPhoto(groupPhotoMapper.toDTO(existing.getGroupPhoto()));
        if (Utils.isNullOrEmpty(groupDTO.getDescription())) groupDTO.setDescription(existing.getDescription());
        if (Utils.isNullOrEmpty(groupDTO.getCategory())) groupDTO.setCategory(existing.getCategory());
        if (Utils.isNullOrEmpty(groupDTO.getTopics())) groupDTO.setTopics(existing.getTopics());

        if (Utils.isNullOrEmpty(groupDTO.getCity())) {
            groupDTO.setCity(existing.getCity());
        } else cityService.parseCity(groupDTO.getCity());

        // Ensure the identifier stays consistent ,and other fields
        groupDTO.setId(existing.getId());
        groupDTO.setEventCount(existing.getEventCount());
        groupDTO.setMemberCount(existing.getMemberCount());
        groupDTO.setCreated(existing.getCreated());
        groupDTO.setUpcomingEvents(existing.getUpcomingEvents());
        groupDTO.setOrganizers(existing.getOrganizers());

        Group saved = groupRepository.save(groupMapper.toEntity(groupDTO));
        groupNeo4JRepository.save(groupMapper.toNeo4JEntity(groupDTO));

        return groupMapper.toDTO(saved);
    }

    public void deleteGroup(String id) {
        getGroupOrThrow(id);
        userService.checkUserHasPermissionToEditGroup(id);

        groupRepository.deleteById(id);
        groupNeo4JRepository.deleteGroup(id);
    }

    public List<Group> findGroupsByOrganizerId(String userId) {
        return groupRepository.findGroupsByOrganizerId(userId);
    }

    private Group getGroupOrThrow(String id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Event not found: " + id));
    }

    public List<GroupDTO> findGroupsOfUser(String userId) {
        return groupNeo4JRepository.findAllGroupsOfUser(userId).stream()
                .map(e -> groupMapper.toNeo4JDTO(e)).toList();
    }

    public List<UserDTO> findAllUsersOfGroup(String groupId) {
        return userService.findUsersOfGroup(groupId);
    }

    public List<EventSummaryDTO> findAllEventOrganizedByGroup(String groupId) {
        return eventService.findEventsOrganizedByGroup(groupId);
    }

}
