package id.my.hendisantika.springbootredissample.boot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.my.hendisantika.springbootredissample.model.Role;
import id.my.hendisantika.springbootredissample.model.User;
import id.my.hendisantika.springbootredissample.repository.RoleRepository;
import id.my.hendisantika.springbootredissample.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-redis-sample
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 05/04/25
 * Time: 07.46
 * To change this template use File | Settings | File Templates.
 */
@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class CreateUsers implements CommandLineRunner {

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder;

    private final ResourceLoader resourceLoader;


    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            // load the roles
            Role admin = roleRepository.findFirstByname("admin");
            Role customer = roleRepository.findFirstByname("customer");

            try {
                // create a Jackson object mapper
                ObjectMapper mapper = new ObjectMapper();
                // create a type definition to convert the array of JSON into a List of Users
                TypeReference<List<User>> typeReference = new TypeReference<>() {
                };
                // make the JSON data available as an input stream
                Resource resource = resourceLoader.getResource("classpath:data/users/users.json");
                InputStream inputStream = resource.getInputStream();
                // convert the JSON to objects
                List<User> users = mapper.readValue(inputStream, typeReference);

                users.stream().forEach((user) -> {
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    user.addRole(customer);
//                    userRepository.save(user);
                });
                userRepository.saveAll(users);
                log.info(">>>> {} Users Saved!", users.size());
            } catch (IOException e) {
                log.info(">>>> Unable to import users: {}", e.getMessage());
            }

            User adminUser = new User();
            adminUser.setName("Itadori Yuji");
            adminUser.setEmail("yuji@yopmail.com");
            adminUser.setPassword(passwordEncoder.encode("53cret"));//
            adminUser.addRole(admin);

            userRepository.save(adminUser);
            log.info(">>>> Loaded User Data and Created users...");
        }
    }
}
