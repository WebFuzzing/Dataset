package reciter.database.dynamodb.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.ApplicationUser;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class ApplicationUserRepository {
	private final DynamoDbTable<ApplicationUser> applicationUserTable;

	public ApplicationUserRepository(DynamoDbEnhancedClient enhancedClient) {
		this.applicationUserTable = enhancedClient.table("ApplicationUser",
				TableSchema.fromBean(ApplicationUser.class));
	}

	public void save(ApplicationUser applicationUser) {
		applicationUserTable.putItem(applicationUser);
	}

	public void saveAll(List<ApplicationUser> applicationUsers) {
		applicationUsers.forEach(applicationUser -> applicationUserTable.putItem(applicationUser));
	}

	public Optional<ApplicationUser> findById(String id) {
		return Optional.ofNullable(applicationUserTable.getItem(r -> r.key(k -> k.partitionValue(id))));
	}

	public Iterable<ApplicationUser> findAll() {
		return applicationUserTable.scan().items();
	}

	public void deleteById(String id) {
		ApplicationUser applicationUser = new ApplicationUser();
		applicationUser.setId(id);
		applicationUserTable.deleteItem(applicationUser);
	}

	public void deleteAll() {
		applicationUserTable.scan().items().forEach(user -> applicationUserTable.deleteItem(user));
	}

	@SuppressWarnings("unused")
	public long getItemCount() {
		long count = 0;

		for (ApplicationUser applicationUser : applicationUserTable.scan().items()) {
			count++;
		}
		return count;
	}

	public List<ApplicationUser> findAllById(List<String> uids) {
		List<ApplicationUser> applicationUsers = new ArrayList<>();
		for (String uid : uids) {
			ApplicationUser applicationUser = applicationUserTable.getItem(r -> r.key(k -> k.partitionValue(uid)));
			if (applicationUser != null) {
				applicationUsers.add(applicationUser);
			}
		}
		return applicationUsers;
	}
}
