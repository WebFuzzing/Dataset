package id.my.hendisantika.springbootredissample.boot;

import id.my.hendisantika.springbootredissample.model.Book;
import id.my.hendisantika.springbootredissample.model.BookRating;
import id.my.hendisantika.springbootredissample.model.User;
import id.my.hendisantika.springbootredissample.repository.BookRatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-redis-sample
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 05/04/25
 * Time: 07.43
 * To change this template use File | Settings | File Templates.
 */
@Slf4j
@Order(4)
@Component
@RequiredArgsConstructor
public class CreateBookRatings implements CommandLineRunner {

    private final RedisTemplate<String, String> redisTemplate;
    private final BookRatingRepository bookRatingRepo;
    @Value("${app.numberOfRatings}")
    private Integer numberOfRatings;
    @Value("${app.ratingStars}")
    private Integer ratingStars;

    @Override
    public void run(String... args) throws Exception {
        if (bookRatingRepo.count() == 0) {
            Random random = new Random();
            IntStream.range(0, numberOfRatings).forEach(n -> {
                String bookId = redisTemplate.opsForSet().randomMember(Book.class.getName());
                String userId = redisTemplate.opsForSet().randomMember(User.class.getName());
                int stars = random.nextInt(ratingStars) + 1;

                User user = new User();
                user.setId(userId);

                Book book = new Book();
                book.setId(bookId);

                BookRating rating = BookRating.builder() //
                        .user(user) //
                        .book(book) //
                        .rating(stars).build();
                bookRatingRepo.save(rating);
            });

            log.info(">>>> BookRating created...");
        }
    }
}
