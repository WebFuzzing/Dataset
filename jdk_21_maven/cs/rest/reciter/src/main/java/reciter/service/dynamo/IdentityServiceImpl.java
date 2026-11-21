package reciter.service.dynamo;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import reciter.database.dynamodb.DynamoDbS3Operations;
import reciter.database.dynamodb.repository.IdentityRepository;
import reciter.model.identity.Identity;
import reciter.service.IdentityService;
import reciter.storage.s3.AmazonS3Config;

@Primary
@Service
public class IdentityServiceImpl implements IdentityService {
	
	private static final Logger log = LoggerFactory.getLogger(IdentityServiceImpl.class);

    @Autowired
    private IdentityRepository identityRepository;

    @Autowired(required=true)
    private DynamoDbS3Operations ddbs3;
    
    @Value("${aws.s3.use}")
    private boolean isS3Use;
    
    @Value("${aws.dynamoDb.local}")
    private boolean isDynamoDbLocal;

    @Value("${aws.s3.use.cached.identityAll}")
    private boolean isIdentityAllS3Caching;

    @Value("${aws.s3.use.cached.identityAll.cacheTime}")
    private long s3CachingDays;
    @Override
    public void save(Collection<Identity> identities) {
        List<reciter.database.dynamodb.model.Identity> identitiesDynamos = new ArrayList<>();
        for (Identity identity : identities) {
            reciter.database.dynamodb.model.Identity identityDynamo = new reciter.database.dynamodb.model.Identity(
                    identity.getUid(), identity
            );
            identitiesDynamos.add(identityDynamo);
        }
        identityRepository.saveAll(identitiesDynamos);
    }

    @Override
    public void save(Identity identity) {
        reciter.database.dynamodb.model.Identity identityDynamo = new reciter.database.dynamodb.model.Identity(
                identity.getUid(), identity
        );
        identityRepository.save(identityDynamo);
    }

    @Override
    public List<Identity> findByUids(List<String> uids) {
        List<Identity> identities = new ArrayList<>();
        identityRepository.findAllById(uids).forEach(e -> identities.add(e.getIdentity()));
        return identities;
    }

    @Override
    public Identity findByUid(String uid) {
        reciter.database.dynamodb.model.Identity identity = identityRepository.findById(uid).orElseGet(() -> null);
        if (identity != null) {
            return identity.getIdentity();
        }
        return null;
    }

    @Override
    public List<Identity> findAll() {
        List<Identity> identities = null;
        if(isS3Use && !isDynamoDbLocal && isIdentityAllS3Caching) {
            log.info("Getting all identity information from S3");
            Date lastModifiedDate = ddbs3.getObjectSaveTimestamp(AmazonS3Config.BUCKET_NAME, Identity.class.getSimpleName() + "/" + "identityAll");
            if(lastModifiedDate != null) {
                long daysBetween = Duration.between(lastModifiedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(), LocalDate.now().atStartOfDay()).toDays();
                if(daysBetween >= this.s3CachingDays) {
                    log.info("Storing all identity information in S3 since cache time of " + this.s3CachingDays + " days has been invalidated");
                    log.info("Scanning Identity table for all identities");
                    Iterable<reciter.database.dynamodb.model.Identity> it = identityRepository.findAll();
                    identities = new ArrayList<>();
                    Iterator<reciter.database.dynamodb.model.Identity> iterator = it.iterator();
                    while (iterator.hasNext()) {
                        Identity identity = iterator.next().getIdentity();
                        identities.add(identity);
                    }
                    ddbs3.saveLargeItem(AmazonS3Config.BUCKET_NAME, identities, Identity.class.getSimpleName() + "/" + "identityAll");
                } else {
                    identities = (List<Identity>) ddbs3.retrieveLargeItem(AmazonS3Config.BUCKET_NAME,
                    Identity.class.getSimpleName() + "/" + "identityAll", Identity.class);
                }
            } 
            if(identities == null || lastModifiedDate == null) {
                log.info("Scanning Identity table for all identities");
                Iterable<reciter.database.dynamodb.model.Identity> it = identityRepository.findAll();
                identities = new ArrayList<>();
                Iterator<reciter.database.dynamodb.model.Identity> iterator = it.iterator();
                while (iterator.hasNext()) {
                    Identity identity = iterator.next().getIdentity();
                    identities.add(identity);
                }
                //Case when putting the object for first time
                log.info("Storing all identity information for first time in S3");
                ddbs3.saveLargeItem(AmazonS3Config.BUCKET_NAME, identities, Identity.class.getSimpleName() + "/" + "identityAll");
            }
        } else if (!isS3Use || !isIdentityAllS3Caching || isDynamoDbLocal) {
            log.info("Using Dynamodb scanning of identities since s3 caching is disabled or using dynamodb local.\nTo enable caching set both aws.s3.use and aws.s3.use.cached.identityAll flag to true");
            Iterable<reciter.database.dynamodb.model.Identity> it = identityRepository.findAll();
            identities = new ArrayList<>();
            Iterator<reciter.database.dynamodb.model.Identity> iterator = it.iterator();
            while (iterator.hasNext()) {
                Identity identity = iterator.next().getIdentity();
                identities.add(identity);
            }
        }
        return identities;
    }

    @Override
    public void deleteAll() {
        identityRepository.deleteAll();
    }

    @Override
    public void delete(String uid) {
        identityRepository.deleteById(uid);
    }
    
    @Override
	public long getItemCount() {
		return identityRepository.count();
	}
}
