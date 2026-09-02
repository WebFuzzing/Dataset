package com.example.joinUs;

import com.example.joinUs.repository.UserNeo4JRepository;
import com.example.joinUs.repository.UserRepository;
import com.example.joinUs.service.EventService;
import com.example.joinUs.service.GroupService;
import com.example.joinUs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class JoinUsApplication implements CommandLineRunner {

    @Autowired
    public EventService eventService;

    @Autowired
    public UserService userService;
    @Autowired
    public UserRepository userRepository;

    @Autowired
    public UserNeo4JRepository userNeo4JRepository;

    @Autowired
    public GroupService groupService;

    public static void main(String[] args) {
        SpringApplication.run(JoinUsApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

    }
}
