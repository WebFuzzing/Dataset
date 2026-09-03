package com.example.joinUs.service;

import com.example.joinUs.Utils;
import com.example.joinUs.dto.GroupDTO;
import com.example.joinUs.dto.summary.EventSummaryDTO;
import com.example.joinUs.mapping.EventMapper;
import com.example.joinUs.mapping.GroupMapper;
import com.example.joinUs.mapping.TopicMapper;
import com.example.joinUs.model.mongodb.User;
import com.example.joinUs.repository.EventNeo4JRepository;
import com.example.joinUs.repository.GroupNeo4JRepository;
import com.example.joinUs.repository.TopicNeo4JRepository;
import com.example.joinUs.repository.UserNeo4JRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class RecommendationService {

    @Autowired
    private EventNeo4JRepository eventNeo4JRepository;
    @Autowired
    private GroupNeo4JRepository groupNeo4JRepository;
    @Autowired
    private UserNeo4JRepository userNeo4JRepository;
    @Autowired
    private TopicNeo4JRepository topicNeo4JRepository;

    @Autowired
    TopicMapper topicMapper;

    @Autowired
    EventMapper eventMapper;

    @Autowired
    GroupMapper groupMapper;

    public List<GroupDTO> recommendGroupsBySimilarMembers(int limit) {
        User user = Utils.getUserFromContext();
        return groupNeo4JRepository.recommendGroupsBySimilarMembers(user.getId(), limit).stream()
                .map(e -> groupMapper.toNeo4JDTO(e)).toList();
    }

    public List<GroupDTO> recommendGroupsByTopics(int limit) {

        User user = Utils.getUserFromContext();

        return groupNeo4JRepository.recommendGroupsByTopics(user.getId(), limit)
                .stream().map(e -> groupMapper.toNeo4JDTO(e)).toList();
    }

    public List<EventSummaryDTO> recommendEventsBySharedGroupTopics(int limit) {
        User user = Utils.getUserFromContext();

        return eventNeo4JRepository.recommendEventsBySharedGroupTopics(user.getId(), limit)
                .stream().map(e -> eventMapper.toDTOFromNeo4j(e)).toList();

    }

    public List<EventSummaryDTO> recommendEventsByMembers(int limit) {
        User user = Utils.getUserFromContext();

        return eventNeo4JRepository.recommendEventsByMembers(user.getId(), limit)
                .stream().map(e -> eventMapper.toDTOFromNeo4j(e)).toList();
    }

    public List<EventSummaryDTO> recommendEventsByPeerGroupAttendance(int limit) {
        User user = Utils.getUserFromContext();
        return eventNeo4JRepository.recommendEventsByPeerGroupAttendance(user.getId(), limit)
                .stream().map(e -> eventMapper.toDTOFromNeo4j(e)).toList();
    }

}
