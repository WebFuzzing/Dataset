package reciter.database.dynamodb.aspect;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MigrateSchema {

	String tableName();
}
