package reciter.database.dynamodb.aspect;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reciter.database.dynamodb.model.VersionedItem;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Aspect
@Component
public class SchemaMigrationAspect {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationAspect.class);
    private final MigrationRegistry registry;

    @Autowired
    private DynamoDbEnhancedClient enhancedClient; // Replaces DynamoDBMapper

    public SchemaMigrationAspect(MigrationRegistry registry) {
        this.registry = registry;
    }

    /**
     * Intercepts service methods annotated with @MigrateSchema.
     */
    @Around("@annotation(migrateSchema)")
    public Object checkAndMigrate(ProceedingJoinPoint joinPoint, MigrateSchema migrateSchema) throws Throwable {
        Object result = joinPoint.proceed();
        if (result == null) return null;

        int target = registry.getTargetVersion(migrateSchema.tableName());

        if (result instanceof VersionedItem) {
            handleMigration((VersionedItem) result, migrateSchema.tableName(), target);
        } else if (result instanceof Collection) {
            for (Object obj : (Collection<?>) result) {
                if (obj instanceof VersionedItem) {
                    handleMigration((VersionedItem) obj, migrateSchema.tableName(), target);
                }
            }
        }
        
        return result;
    }

    /**
     * Pointcut updated for SDK v2: Intercepts all save/saveAll methods executing 
     * inside your custom repository implementation layer package.
     */
    @Around("execution(* reciter.database.dynamodb.repository..*.save*(..))")
    public Object ensureDefaultsOnSave(ProceedingJoinPoint joinPoint) throws Throwable {
        Object arg = joinPoint.getArgs()[0];
        
        // Handle single entity saves
        if (arg instanceof VersionedItem) {
            processItemBeforeSave((VersionedItem) arg);
        } 
        // Handle batch list saves (e.g., saveAll(List<Entity>))
        else if (arg instanceof Collection) {
            for (Object obj : (Collection<?>) arg) {
                if (obj instanceof VersionedItem) {
                    processItemBeforeSave((VersionedItem) obj);
                }
            }
        }
        
        return joinPoint.proceed(joinPoint.getArgs());
    }

    private void processItemBeforeSave(VersionedItem vItem) {
        String tableName = getTableNameFromEntity(vItem);
        int target = registry.getTargetVersion(tableName);
        
        // Force the version and defaults right before the Repo sends it to DynamoDB
        if (vItem.getSchemaVersion() == 0 || vItem.getSchemaVersion() < target) {
            vItem.setSchemaVersion(target);
            populateDefaults(vItem); // Ensure blank values are handled
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMigration(VersionedItem item, String tableName, int target) {
        int current = item.getSchemaVersion();
        
        if (current == 0 || current < target) {
            item.setSchemaVersion(target);
            
            // 1. Recursively fill missing/null fields
            populateDefaults(item);

            // 2. Dynamic table target generation using Enhanced Client
            Class<VersionedItem> clazz = (Class<VersionedItem>) item.getClass();
            DynamoDbTable<VersionedItem> table = enhancedClient.table(tableName, TableSchema.fromBean(clazz));

            /*
             * SDK v2 equivalent of UPDATE_SKIP_NULL_ATTRIBUTES:
             * Calling updateItem() natively builds a shallow update expression containing 
             * only populated bean properties, ignoring and preserving unmentioned null paths.
             */
            table.updateItem(item);
            log.info("DEBUG: Migrated {} to version {} in table {}", item.getClass().getSimpleName(), target, tableName);
        }
    }

    /**
     * Recursively populates empty field values using reflection definitions.
     */
    private void populateDefaults(Object obj) {
        if (obj == null) return;
        
        for (Field field : obj.getClass().getDeclaredFields()) {
            String fieldName = field.getName();
            try {
                field.setAccessible(true);
                if (field.get(obj) == null) {
                    Class<?> type = field.getType();
                    
                    if (type.equals(String.class)) {
                        field.set(obj, " ");
                    } else if (type.equals(Integer.class) || type.equals(int.class)) {
                        field.set(obj, 0);
                    } else if (type.equals(Long.class) || type.equals(long.class)) {
                        field.set(obj, 0L);
                    } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
                        field.set(obj, false);
                    } else if (type.equals(Date.class)) {
                        field.set(obj, new Date());
                    } else if (type.equals(java.util.List.class)) {
                        field.set(obj, new ArrayList<>());
                    } else if (type.equals(java.util.Map.class)) {
                        field.set(obj, new HashMap<>());
                    } 
                    // Handle Nested Objects Bean Mappings
                    else if (!type.isPrimitive() && !type.getName().startsWith("java.")) {
                        try {
                            Object nested = type.getDeclaredConstructor().newInstance();
                            field.set(obj, nested);
                            populateDefaults(nested); // RECURSION
                        } catch (NoSuchMethodException e) {
                            log.error("MIGRATION ERROR: Field '{}' in class '{}' missing default constructor.", 
                                    fieldName, obj.getClass().getSimpleName());
                        }
                    } else {
                        log.warn("MIGRATION WARNING: No default rule for field '{}' (Type: {}) in class '{}'. Field remains null.", 
                                 fieldName, type.getSimpleName(), obj.getClass().getSimpleName());
                    }
                }
            } catch (Exception e) {
                log.error("MIGRATION CRITICAL: Unexpected error accessing field '{}' in class '{}'.", 
                        fieldName, obj.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * Resolves table name mapping from the entity context.
     */
    private String getTableNameFromEntity(Object item) {
        String simpleName = item.getClass().getSimpleName();
        
        // Special mapping override logic replicated from your central DynamoDbConfig
        if (simpleName.equalsIgnoreCase("AnalysisOutput")) {
            return "Analysis";
        }
        
        return simpleName;
    }
}