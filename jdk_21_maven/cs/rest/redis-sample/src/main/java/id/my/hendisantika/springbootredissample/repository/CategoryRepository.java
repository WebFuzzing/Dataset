package id.my.hendisantika.springbootredissample.repository;

import id.my.hendisantika.springbootredissample.model.Category;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-redis-sample
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 05/04/25
 * Time: 07.41
 * To change this template use File | Settings | File Templates.
 */
@Repository
public interface CategoryRepository extends CrudRepository<Category, String> {
}
