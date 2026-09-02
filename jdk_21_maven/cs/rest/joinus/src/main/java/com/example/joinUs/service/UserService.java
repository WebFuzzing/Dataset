package com.example.joinUs.service;

import com.example.joinUs.Utils;
import com.example.joinUs.dto.GroupDTO;
import com.example.joinUs.dto.LoginForm;
import com.example.joinUs.dto.UserDTO;
import com.example.joinUs.dto.summary.EventSummaryDTO;
import com.example.joinUs.mapping.UserMapper;
import com.example.joinUs.model.mongodb.Event;
import com.example.joinUs.model.mongodb.Group;
import com.example.joinUs.model.mongodb.User;
import com.example.joinUs.repository.UserNeo4JRepository;
import com.example.joinUs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class UserService {

    @Autowired
    AuthenticationManager authManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CityService cityService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private EventService eventService;

    @Autowired
    private UserNeo4JRepository userNeo4JRepo;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserDTO getUserProfile() {
        User user = Utils.getUserFromContext();
        return userMapper.toDTO(user);
    }

    public UserDTO editUserProfile(UserDTO userUpdate) {

        User existingUser = Utils.getUserFromContext();
        String userId = existingUser.getId();

        if (Utils.isNullOrEmpty(userUpdate.getBio())) userUpdate.setBio(existingUser.getBio());
        if (Utils.isNullOrEmpty(userUpdate.getMemberName())) userUpdate.setMemberName(existingUser.getMemberName());
        if (Utils.isNullOrEmpty(userUpdate.getTopics())) userUpdate.setTopics(existingUser.getTopics());
        if (Utils.isNullOrEmpty(userUpdate.getMemberName())) userUpdate.setMemberName(existingUser.getMemberName());
        if (Utils.isNullOrEmpty(userUpdate.getPassword())) {
            userUpdate.setPassword(existingUser.getPassword());
        } else userUpdate.setPassword(passwordEncoder.encode(userUpdate.getPassword()));

        if (userUpdate.getCity() != null) {
            cityService.parseCity(userUpdate.getCity());
            existingUser.setCity(userUpdate.getCity());
        }

        //Fields that should not change :
        userUpdate.setId(userId);
        userUpdate.setEventCount(existingUser.getEventCount());
        userUpdate.setGroupCount(existingUser.getGroupCount());
        userUpdate.setUpcomingEvents(existingUser.getUpcomingEvents());

        // Changing the approach here to modify the Dto instead of the entity ,so that we can use the DTO for Neo4J too,
        //        but saving the previous approach to rollback if needed

        //        if (userUpdate.getBio() != null) existingUser.setBio(userUpdate.getBio());
        //        if (userUpdate.getMemberName() != null) existingUser.setMemberName(userUpdate.getMemberName());
        //        if (userUpdate.getPassword()!=null) existingUser.setPassword(passwordEncoder.encode(userUpdate.getPassword()));

        saveUser(userMapper.toEntity(userUpdate));
        userNeo4JRepo.save(userMapper.toNeo4jEntity(userUpdate));

        return userUpdate;
    }

    protected void addUserToGroup(String userId, String groupId) {
        userNeo4JRepo.addUserToGroup(userId, groupId);
    }

    protected void removeUserFromGroup(String userId, String groupId) {
        userNeo4JRepo.removeUserFromGroup(userId, groupId);
    }

    public void checkUserHasPermissionToEditGroup(String groupId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated in the application");
        }
        User user = (User) authentication.getPrincipal();
        String userId = user.getId();
        List<Group> groups = groupService.findGroupsByOrganizerId(userId);
        if (groups == null || groups.isEmpty())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You do not have permission to edit this group");

        for (Group group : groups) {
            if (group.getId().equalsIgnoreCase(groupId)) return;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You do not have permission to edit this group");

    }

    public void checkUserHasPermissionToEditEvent(String eventId) {

        User user = Utils.getUserFromContext();
        String userId = user.getId();
        List<Group> groups = groupService.findGroupsByOrganizerId(userId);
        if (groups == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You do not have permission to edit this event");

        //        boolean foundEvent;
        for (Group group : groups) {
            List<Event> events = eventService.findEventsByCreatorGroup(group.getId());
            for (Event event : events) {
                if (event.getId().equalsIgnoreCase(eventId)) return;
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "You do not have permission to edit this event");
    }

    //TODO follow the same approach as for the editProfile, if we have time
    public SecurityContext registerUser(UserDTO userDTO) {

        if (!Utils.isNullOrEmpty(userRepository.findMemberByName(userDTO.getMemberName())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A user with the username " + userDTO.getMemberName() + " is already registered");
        if (Utils.isNullOrEmpty(userDTO.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please provide a password");
        }
        try {
            String password = userDTO.getPassword();
            if (userDTO.getCity() != null) {
                cityService.parseCity(userDTO.getCity());
            }
            User user = userMapper.toEntity(userDTO);
            user.setEventCount(0);
            user.setGroupCount(0);
            user.setIsAdmin(false);
            user.setPassword(passwordEncoder.encode(password));
            user.setUpcomingEvents(new ArrayList<>());

            if (userDTO.getBio() != null) user.setBio(userDTO.getBio());
            if (userDTO.getMemberName() != null) user.setMemberName(userDTO.getMemberName());

            if (userDTO.getCity() != null) {
                cityService.parseCity(userDTO.getCity());
                user.setCity(userDTO.getCity());
            }

            saveUser(user);

            userNeo4JRepo.save(userMapper.toNeo4jEntity(userDTO));

            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getMemberName(), password
                    )
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            return context;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Your registration failed " + "\n Error : " + e.getMessage());
        }

    }

    public SecurityContext loginUser(LoginForm loginForm) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginForm.userid, loginForm.password
                )
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        return context;

    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findUserById(String id) {
        return userRepository.findById(id);
    }

    public void deleteProfile() {
        User user = Utils.getUserFromContext();
        userRepository.delete(user);
        userNeo4JRepo.deleteUser(user.getId());
    }

    public List<EventSummaryDTO> findAllEvents() {
        User user = Utils.getUserFromContext();
        return eventService.findEventsOfUser(user.getId());
    }

    public List<GroupDTO> findAllGroups() {
        User user = Utils.getUserFromContext();
        return groupService.findGroupsOfUser(user.getId());
    }

    public List<UserDTO> findUsersOfGroup(String groupId) {
        return userNeo4JRepo.getMembersLinkedToGroup(groupId).stream()
                .map(e -> userMapper.toNeo4jDTO(e)).toList();
    }
}

