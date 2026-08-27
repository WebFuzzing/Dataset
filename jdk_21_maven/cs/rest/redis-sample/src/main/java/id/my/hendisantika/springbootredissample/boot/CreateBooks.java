package id.my.hendisantika.springbootredissample.boot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.my.hendisantika.springbootredissample.model.Book;
import id.my.hendisantika.springbootredissample.model.Category;
import id.my.hendisantika.springbootredissample.repository.BookRepository;
import id.my.hendisantika.springbootredissample.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-redis-sample
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 05/04/25
 * Time: 07.44
 * To change this template use File | Settings | File Templates.
 */
@Slf4j
@Order(3)
@Component
@RequiredArgsConstructor
public class CreateBooks implements CommandLineRunner {

    private final BookRepository bookRepository;

    private final CategoryRepository categoryRepository;
    private final ResourceLoader resourceLoader;

    @Override
    public void run(String... args) throws Exception {
        if (bookRepository.count() == 0) {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<List<Book>> typeReference = new TypeReference<>() {
            };

            //          MODIFIED
            // In the packaged jar getFile() throws FileNotFoundException: class path resource
            // [data/books/] cannot be resolved to absolute file path -> startup dies.
            File[] files = Arrays.stream(new PathMatchingResourcePatternResolver()
                            .getResources("classpath*:/data/books/*.json"))
                    .map(r -> new File(r.getFilename())).toArray(File[]::new);
            //          MODIFIED

            if (files == null || files.length == 0) {
                log.warn("No JSON files found in /data/books/ directory.");
                return;
            }

            Map<String, Category> categories = new HashMap<>();
            log.info("files -> {}", files);

            Arrays.stream(files).forEach(file -> {
                try {
                    log.info(">>>> Processing Book File: {}", file.getPath());
                    String categoryName = file.getName().substring(0, file.getName().lastIndexOf("_"));
                    log.info(">>>> Category: {}", categoryName);

                    Category category;
                    if (!categories.containsKey(categoryName)) {
                        category = Category.builder().name(categoryName).build();
                        categoryRepository.save(category);
                        categories.put(categoryName, category);
                    } else {
                        category = categories.get(categoryName);
                    }

                    InputStream inputStream = resourceLoader.getResource("classpath:/data/books/" + file.getName()).getInputStream();
                    List<Book> books = mapper.readValue(inputStream, typeReference);
                    books.forEach((book) -> {
                        book.addCategory(category);
                        bookRepository.save(book);
                    });
                    log.info(">>>> {} Books Saved!", books.size());
                } catch (IOException e) {
                    log.error("Unable to import books from file: {}", file.getName(), e);
                }
            });

            log.info(">>>> Loaded Book Data and Created books...");
        }
    }
}
