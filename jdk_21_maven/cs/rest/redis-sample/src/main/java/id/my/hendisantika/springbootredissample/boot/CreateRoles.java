package id.my.hendisantika.springbootredissample.boot;

import id.my.hendisantika.springbootredissample.model.Role;
import id.my.hendisantika.springbootredissample.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-redis-sample
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 05/04/25
 * Time: 07.45
 * To change this template use File | Settings | File Templates.
 */
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class CreateRoles implements CommandLineRunner {
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.count() == 0) {
            Role adminRole = Role.builder().name("admin").build();
            roleRepository.save(adminRole);
            Role customerRole = Role.builder().name("customer").build();
            roleRepository.save(customerRole);
            log.info(">>>> Created admin and customer roles...");
        }
    }
}
