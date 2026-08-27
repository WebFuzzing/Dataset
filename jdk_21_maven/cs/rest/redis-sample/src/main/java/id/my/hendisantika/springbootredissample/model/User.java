package id.my.hendisantika.springbootredissample.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.annotation.Transient;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-redis-sample
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 05/04/25
 * Time: 07.40
 * To change this template use File | Settings | File Templates.
 */

/**
 * Represents a user entity stored in Redis with associated attributes and behavior.
 *
 * <p>This class is annotated with {@link EqualsAndHashCode} and {@link ToString} to include only
 * explicitly specified fields in equality checks and string representation, respectively. It also
 * uses the {@link Data} annotation to generate getters, setters, and other common methods.
 * </p>
 *
 * <p>Each user has an ID, a name, an email, a password, and an optional password confirmation.
 * Additionally, users can be associated with multiple roles.
 * </p>
 *
 * <p>The class is annotated with {@link RedisHash} to indicate that it is a Redis hash stored in Redis.</p>
 */
@Data
@RedisHash
@JsonIgnoreProperties(value = {"password", "passwordConfirm"}, allowSetters = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
public class User {

    /**
     * The unique identifier for the user.
     *
     * <p>This field is marked with {@link Id} to indicate it is the primary key in Redis. It is also
     * included in the string representation of the user.</p>
     */
    @Id
    @ToString.Include
    private String id;

    /**
     * The name of the user.
     *
     * <p>This field must be non-null and have a length between 2 and 48 characters. It is included in
     * the string representation of the user.</p>
     */
    @NotNull
    @Size(min = 2, max = 48)
    @ToString.Include
    private String name;

    /**
     * The email address of the user.
     *
     * <p>This field must be non-null, a valid email address, and is included in both the equality
     * checks and string representation of the user. It is also indexed for faster lookups.</p>
     */
    @Email
    @NotNull
    @EqualsAndHashCode.Include
    @ToString.Include
    @Indexed
    private String email;

    /**
     * The password for the user.
     *
     * <p>This field is required but not included in equality checks or the string representation of the
     * user for security reasons.</p>
     */
    @NotNull
    private String password;

    /**
     * A confirmation password used for validation purposes.
     *
     * <p>This field is transient and not persisted in Redis. It is used for password confirmation
     * during user creation or updates.</p>
     */
    @Transient
    private String passwordConfirm;

    /**
     * The roles associated with the user.
     *
     * <p>This field is a set of {@link Role} objects and is used to manage the user's roles. It is not
     * included in equality checks or the string representation of the user. The default is an empty
     * set.</p>
     */
    @Reference
    private Set<Role> roles = new HashSet<Role>();

    /**
     * Adds a role to the user.
     *
     * <p>This method allows adding a {@link Role} to the user's set of roles.</p>
     *
     * @param role the {@link Role} to be added
     */
    public void addRole(Role role) {
        roles.add(role);
    }
}
