package reciter.database.dynamodb.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.Gender;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class GenderRepository {

	private final DynamoDbTable<Gender> genderTable;

	public GenderRepository(DynamoDbEnhancedClient enhancedClient) {
		this.genderTable = enhancedClient.table("Gender", TableSchema.fromBean(Gender.class));
	}

	public void save(Gender gender) {
		genderTable.putItem(gender);
	}

	public void saveAll(List<Gender> genders) {
		genders.forEach(gender -> genderTable.putItem(gender));
	}

	public Optional<Gender> findById(String id) {
		return Optional.ofNullable(genderTable.getItem(r -> r.key(k -> k.partitionValue(id))));
	}

	public Iterable<Gender> findAll() {
		return genderTable.scan().items();
	}

	public void deleteById(String id) {
		Gender gender = new Gender();
		gender.setName(id);
		genderTable.deleteItem(gender);
	}

	public void deleteAll() {
		genderTable.scan().items().forEach(entity -> genderTable.deleteItem(entity));
	}

	@SuppressWarnings("unused")
	public long count() {
		long count = 0;

		for (Gender gender : genderTable.scan().items()) {
			count++;
		}
		return count;
	}

	public List<Gender> findAllById(List<String> uids) {
		List<Gender> genderList = new ArrayList<>();
		for (String uid : uids) {
			Gender gender = genderTable.getItem(r -> r.key(k -> k.partitionValue(uid)));
			if (gender != null) {
				genderList.add(gender);
			}
		}
		return genderList;
	}

	public boolean existsById(String genderSource) {
		Gender gender = genderTable.getItem(r -> r.key(k -> k.partitionValue(genderSource)));
		return gender != null;
	}
}
