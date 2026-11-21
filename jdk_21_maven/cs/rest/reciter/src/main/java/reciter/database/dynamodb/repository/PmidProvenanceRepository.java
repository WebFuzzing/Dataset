package reciter.database.dynamodb.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.PmidProvenance;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Repository
public class PmidProvenanceRepository {
	
	private static final Logger log = LoggerFactory.getLogger(PmidProvenanceRepository.class);
	private static final String BACKFILL_STRATEGY = "BACKFILL_FROM_ESEARCHRESULT";

	private final DynamoDbTable<PmidProvenance> pmidProvenanceTable;

	public PmidProvenanceRepository(DynamoDbEnhancedClient enhancedClient) {
		this.pmidProvenanceTable = enhancedClient.table("PmidProvenance", TableSchema.fromBean(PmidProvenance.class));
	}

	public void save(PmidProvenance pmidProvenance) {
		pmidProvenanceTable.putItem(pmidProvenance);
	}

	public void saveIfNotExists(PmidProvenance pmidProvenance) {
		// v2 equivalent of expected.put("uid", new ExpectedAttributeValue(false))
		Expression conditionExpression = Expression.builder().expression("attribute_not_exists(uid)").build();

		try {
			pmidProvenanceTable.putItem(r -> r.item(pmidProvenance).conditionExpression(conditionExpression));
		} catch (ConditionalCheckFailedException e) {
			// Item already exists — this is expected, not an error
			log.debug("PmidProvenance already exists for uid={} pmid={}, skipping", pmidProvenance.getUid(),
					pmidProvenance.getPmid());
		}
	}

	public void saveAllIfNotExists(List<PmidProvenance> pmidProvenances) {
		int saved = 0;
		int skipped = 0;
		for (PmidProvenance pmidProvenance : pmidProvenances) {
			try {
				saveIfNotExists(pmidProvenance);
				saved++;
			} catch (Exception e) {
				skipped++;
				log.warn("Failed to save provenance for uid={} pmid={}: {}", pmidProvenance.getUid(),
						pmidProvenance.getPmid(), e.getMessage());
			}
		}
		log.info("PmidProvenance batch: saved={}, skipped={}, total={}", saved, skipped, pmidProvenances.size());
	}

	public List<PmidProvenance> findByUid(String uid) {
		// Uses QueryConditional to query by Partition Key (Hash Key) safely
		QueryConditional queryConditional = QueryConditional.keyEqualTo(k -> k.partitionValue(uid));

		return pmidProvenanceTable.query(r -> r.queryConditional(queryConditional)).items().stream()
				.collect(Collectors.toList());
	}

	public Set<Long> findPmidsByUid(String uid) {
		return findByUid(uid).stream().map(PmidProvenance::getPmid).collect(Collectors.toSet());
	}

	public Set<Long> findPmidsByUidAndStrategy(String uid, String strategy) {
		QueryConditional queryConditional = QueryConditional.keyEqualTo(k -> k.partitionValue(uid));

		// Maps filter expression and attributes using native v2 AttributeValues
		Expression filterExpression = Expression.builder().expression("retrievalStrategy = :strategy")
				.expressionValues(Map.of(":strategy", AttributeValue.fromS(strategy))).build();

		return pmidProvenanceTable.query(r -> r.queryConditional(queryConditional).filterExpression(filterExpression))
				.items().stream().map(PmidProvenance::getPmid).collect(Collectors.toSet());
	}

	public void updateStrategyIfBackfill(String uid, long pmid, String realStrategy) {
		// v2 loads composite keys cleanly using a single block
		PmidProvenance existing = pmidProvenanceTable.getItem(r -> r.key(k -> k.partitionValue(uid).sortValue(pmid)));

		if (existing == null || !BACKFILL_STRATEGY.equals(existing.getRetrievalStrategy())) {
			return;
		}

		existing.setRetrievalStrategy(realStrategy);

		// Conditional expression ensuring only fields matching BACKFILL_STRATEGY are
		// updated
		Expression conditionExpression = Expression.builder().expression("retrievalStrategy = :oldStrategy")
				.expressionValues(Map.of(":oldStrategy", AttributeValue.fromS(BACKFILL_STRATEGY))).build();

		try {
			pmidProvenanceTable.putItem(r -> r.item(existing).conditionExpression(conditionExpression));
			log.info("Healed backfill provenance for uid={} pmid={} to strategy={}", uid, pmid, realStrategy);
		} catch (ConditionalCheckFailedException e) {
			log.debug("Provenance for uid={} pmid={} already healed, skipping", uid, pmid);
		}
	}

}
