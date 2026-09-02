package com.example.joinUs.service;

import com.example.joinUs.Utils;
import com.example.joinUs.model.mongodb.User;
import com.example.joinUs.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final MongoTemplate mongoTemplate;

    public CustomUserDetailsService(UserRepository userRepository, MongoTemplate mongoTemplate) {
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        List<User> users = userRepository.findMemberById(username);
        //TODO need to change this later in order to guarantee uniqueness by username , (for now best I can think of is just using member_id)

        if (Utils.isNullOrEmpty(users))
            throw new UsernameNotFoundException("No User is  found with username " + username);

        User user = users.get(0);

        return user;
    }
}

