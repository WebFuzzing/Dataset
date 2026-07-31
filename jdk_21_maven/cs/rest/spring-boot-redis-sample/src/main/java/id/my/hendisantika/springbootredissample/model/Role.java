package id.my.hendisantika.springbootredissample.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-redis-sample
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 05/04/25
 * Time: 07.38
 * To change this template use File | Settings | File Templates.
 */

/**
 * Represents a role entity stored in Redis.
 *
 * <p>This class is annotated with {@link Builder} to provide a builder pattern for creating instances, and
 * {@link Data} to generate getters, setters, equals, hashCode, and toString methods. The class is also annotated
 * with {@link RedisHash} to indicate that it is a Redis hash stored in Redis.</p>
 */
@Data
@Builder
@RedisHash
@AllArgsConstructor
@NoArgsConstructor
public class Role {

    /**
     * The unique identifier for the role.
     *
     * <p>This field is marked with {@link Id} to indicate it is the primary key in Redis.</p>
     */
    @Id
    private String id;

    /**
     * The name of the role.
     *
     * <p>This field is indexed to allow for efficient querying by role name.</p>
     */
    @Indexed
    private String name;
}
