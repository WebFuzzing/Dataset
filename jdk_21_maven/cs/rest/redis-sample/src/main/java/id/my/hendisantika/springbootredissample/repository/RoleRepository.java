package id.my.hendisantika.springbootredissample.repository;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-redis-sample
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 05/04/25
 * Time: 07.42
 * To change this template use File | Settings | File Templates.
 */

import id.my.hendisantika.springbootredissample.model.Role;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for {@link Role} entities.
 *
 * <p>This interface extends {@link CrudRepository} to provide basic CRUD operations for {@link Role} entities.
 * It also includes a custom query method to find a {@link Role} by its name.</p>
 */
@Repository
public interface RoleRepository extends CrudRepository<Role, String> {

    /**
     * Finds the first {@link Role} entity by its name.
     *
     * <p>This method retrieves a {@link Role} entity where the name matches the specified role name.
     * If multiple roles have the same name, only the first one is returned.</p>
     *
     * @param role the name of the role to search for
     * @return the first {@link Role} with the given name, or {@code null} if no role is found
     */
    Role findFirstByname(String role);
}
