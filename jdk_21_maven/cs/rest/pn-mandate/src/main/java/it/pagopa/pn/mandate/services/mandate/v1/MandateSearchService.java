package it.pagopa.pn.mandate.services.mandate.v1;

import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.dto.PaSummaryDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcaregroups.v1.dto.PgGroupDto;
import it.pagopa.pn.mandate.mapper.MandateEntityMandateDtoMapper;
import it.pagopa.pn.mandate.middleware.db.MandateDao;
import it.pagopa.pn.mandate.middleware.db.PnLastEvaluatedKey;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.middleware.msclient.PnDataVaultClient;
import it.pagopa.pn.mandate.middleware.msclient.PnExtRegPrvtClient;
import it.pagopa.pn.mandate.middleware.msclient.PnInfoPaClient;
import it.pagopa.pn.mandate.model.InputSearchMandateDto;
import it.pagopa.pn.mandate.model.PageResultDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MandateSearchService {

    public static final int FILTER_EXPRESSION_MULTIPLIER = 4;

    private final MandateDao mandateDao;
    private final MandateEntityMandateDtoMapper entityMandateDtoMapper;
    private final PnDataVaultClient pnDataVaultClient;
    private final PnInfoPaClient pnInfoPaClient;
    private final PnExtRegPrvtClient pnExtRegPrvtClient;

    public MandateSearchService(MandateDao mandateDao,
                                MandateEntityMandateDtoMapper entityMandateDtoMapper,
                                PnDataVaultClient pnDataVaultClient,
                                PnInfoPaClient pnInfoPaClient,
                                PnExtRegPrvtClient pnExtRegPrvtClient) {
        this.mandateDao = mandateDao;
        this.entityMandateDtoMapper = entityMandateDtoMapper;
        this.pnDataVaultClient = pnDataVaultClient;
        this.pnInfoPaClient = pnInfoPaClient;
        this.pnExtRegPrvtClient = pnExtRegPrvtClient;
    }

    public Mono<PageResultDto<MandateDto, String>> searchByDelegate(InputSearchMandateDto searchDto,
                                                                    PnLastEvaluatedKey lastEvaluatedKey) {
        String delegateId = searchDto.getDelegateId();
        List<String> delegatorIds = searchDto.getDelegatorIds();
        List<String> groups = searchDto.getGroups();
        log.info("searchByDelegate {}, delegatorIds: {}, groups: {}", delegateId, delegatorIds, groups);

        List<Integer> partitions = generatePartitions(searchDto.getStatuses());

        int requiredSize = searchDto.getSize() * searchDto.getMaxPageNumber() + 1;
        int dynamoDbPageSize;
        if (!CollectionUtils.isEmpty(delegatorIds) || !CollectionUtils.isEmpty(groups)) {
            dynamoDbPageSize = requiredSize * FILTER_EXPRESSION_MULTIPLIER;
        } else {
            dynamoDbPageSize = requiredSize;
        }
        log.debug("searchByDelegate {}, requiredSize: {}, dynamoDbPageSize: {}", delegateId, requiredSize, dynamoDbPageSize);

        AtomicInteger pIdx = selectStartPartitionIdx(lastEvaluatedKey, partitions);
        Integer startPartition = partitions.isEmpty() ? null : partitions.get(pIdx.get());
        log.debug("searchByDelegate {}, partition: {}, idx: {}", delegateId, startPartition, pIdx);

        List<MandateEntity> cumulativeQueryResult = new ArrayList<>();
        return mandateDao.searchByDelegate(delegateId, startPartition, groups, delegatorIds, dynamoDbPageSize, lastEvaluatedKey)
                .expand(page -> readMoreData(cumulativeQueryResult.size(), requiredSize, dynamoDbPageSize, page, searchDto, partitions, pIdx))
                .doOnNext(page -> cumulativeQueryResult.addAll(page.items()))
                .last()
                .flatMap(lastPage -> prepareResult(cumulativeQueryResult, searchDto, requiredSize));
    }

    private Mono<Page<MandateEntity>> readMoreData(int currentSize,
                                                   int requiredSize,
                                                   int dynamoDbPageSize,
                                                   Page<MandateEntity> page,
                                                   InputSearchMandateDto searchDto,
                                                   List<Integer> partitions,
                                                   AtomicInteger pIdx) {
        log.trace("reading more data...");
        String delegateId = searchDto.getDelegateId();
        List<String> delegatorIds = searchDto.getDelegatorIds();
        List<String> groups = searchDto.getGroups();
        if (currentSize < requiredSize) {
            Integer currentPartition = getPartitionFromIdx(partitions, pIdx.get());
            if (page.lastEvaluatedKey() == null && pIdx.incrementAndGet() < partitions.size()) {
                // nella partizione corrente non ci sono più dati, si prosegue leggendo dalla partizione successiva
                Integer nextPartition = partitions.isEmpty() ? null : partitions.get(pIdx.get());
                log.debug("no more data in partition {}, next partition is {}", currentPartition, nextPartition);
                return mandateDao.searchByDelegate(delegateId, nextPartition, groups, delegatorIds, dynamoDbPageSize, null);
            } else if (page.lastEvaluatedKey() != null) {
                // nella partizione corrente ci sono ancora dati, si prosegue leggendo dalla partizione corrente
                PnLastEvaluatedKey nextPageKey = new PnLastEvaluatedKey();
                nextPageKey.setInternalLastEvaluatedKey(page.lastEvaluatedKey());
                Integer partition = partitions.isEmpty() ? null : partitions.get(pIdx.get());
                log.debug("more data in partition {}, lek: {}", currentPartition, nextPageKey);
                return mandateDao.searchByDelegate(delegateId, partition, groups, delegatorIds, dynamoDbPageSize, nextPageKey);
            }
            log.debug("no more data");
        } else {
            log.debug("size query results: {}, reached required size of {}", currentSize, requiredSize);
        }
        log.trace("...stop reading more data");
        return Mono.empty();
    }

    private Mono<PageResultDto<MandateDto, String>> prepareResult(List<MandateEntity> results,
                                                                  InputSearchMandateDto searchDto,
                                                                  int requiredSize) {
        var entityToConvert = results.stream()
                .limit(searchDto.getSize())
                .toList();
        log.info("size query results: {}, size requested: {}", results.size(), entityToConvert.size());
        var userInfo = callDataVault(entityToConvert);
        var paInfo = callExternalRegistries(entityToConvert);
        var pgInfo = callExternalRegistries(searchDto.getDelegateId());
        return Mono.zip(userInfo, paInfo, pgInfo)
                .map(tuple -> {
                    List<MandateDto> dtoList = entityToConvert.stream().map(entity -> {
                        MandateDto dto = entityMandateDtoMapper.toDto(entity);
                        dto.setVerificationCode(null);
                        deanonimizeResult(entity, dto, tuple.getT1(), tuple.getT2(), tuple.getT3());
                        return dto;
                    }).toList();
                    return PageResultDto.<MandateDto, String>builder()
                            .page(dtoList)
                            .more(results.size() >= requiredSize)
                            .nextPagesKey(computeLastEvaluatedKeys(results, searchDto))
                            .build();
                });
    }

    private void deanonimizeResult(MandateEntity entity,
                                   MandateDto dto,
                                   Map<String, BaseRecipientDtoDto> mapUserInfo,
                                   Map<String, PaSummaryDto> mapPaInfo,
                                   Map<String, PgGroupDto> mapPgGroup) {
        var recipientDto = mapUserInfo.get(entity.getDelegator());
        if (recipientDto != null && dto.getDelegator() != null) {
            UserDto user = dto.getDelegator();
            user.setDisplayName(recipientDto.getDenomination());
            user.setFiscalCode(recipientDto.getTaxId());
        }
        if (!mapPaInfo.isEmpty() && dto.getVisibilityIds() != null) {
            dto.getVisibilityIds().forEach(orgId -> {
                PaSummaryDto paSummaryDto = mapPaInfo.get(orgId.getUniqueIdentifier());
                if (paSummaryDto != null) {
                    orgId.setName(paSummaryDto.getName());
                }
            });
        }
        if (!mapPgGroup.isEmpty() && dto.getGroups() != null) {
            dto.getGroups().forEach(group -> {
                PgGroupDto pgGroupDto = mapPgGroup.get(group.getId());
                if (pgGroupDto != null) {
                    group.setName(pgGroupDto.getName());
                }
            });
        }
    }

    private List<String> computeLastEvaluatedKeys(List<MandateEntity> entities,
                                                  InputSearchMandateDto searchDto) {
        List<String> lastEvaluatedKeys = new ArrayList<>();
        for (int i = 1; i <= searchDto.getMaxPageNumber(); i++) {
            int index = searchDto.getSize() * i;
            if (entities.size() <= index) {
                break;
            }
            var keyEntity = entities.get(index - 1);
            var lastEvaluatedKey = computeLastEvaluatedKey(keyEntity);
            lastEvaluatedKeys.add(lastEvaluatedKey.serialize());
        }
        return lastEvaluatedKeys;
    }

    private PnLastEvaluatedKey computeLastEvaluatedKey(MandateEntity entity) {
        var lastEvaluatedKey = new PnLastEvaluatedKey();
        lastEvaluatedKey.setExternalLastEvaluatedKey(Integer.toString(entity.getState()));
        lastEvaluatedKey.setInternalLastEvaluatedKey(Map.of(
                MandateEntity.COL_PK, AttributeValue.builder().s(entity.getDelegator()).build(),
                MandateEntity.COL_SK, AttributeValue.builder().s(entity.getSk()).build(),
                MandateEntity.COL_I_STATE, AttributeValue.builder().n(Integer.toString(entity.getState())).build(),
                MandateEntity.COL_S_DELEGATE, AttributeValue.builder().s(entity.getDelegate()).build()
        ));
        return lastEvaluatedKey;
    }

    private AtomicInteger selectStartPartitionIdx(PnLastEvaluatedKey lastEvaluatedKey, List<Integer> partitions) {
        AtomicInteger pIdx = new AtomicInteger();
        if (lastEvaluatedKey != null) {
            int startOf = partitions.indexOf(Integer.parseInt(lastEvaluatedKey.getExternalLastEvaluatedKey()));
            if (startOf >= 0) {
                pIdx.set(startOf);
            } else {
                log.warn("can not find partition index from lek - skip to partition 0");
            }
            log.debug("starting search from partition index: {}", pIdx);
        }
        return pIdx;
    }

    private @Nullable Integer getPartitionFromIdx(List<Integer> partitions, int idx) {
        return partitions.isEmpty() ? null : partitions.get(idx);
    }

    private Mono<Map<String, BaseRecipientDtoDto>> callDataVault(List<MandateEntity> entities) {
        Set<String> delegators = entities.stream()
                .map(MandateEntity::getDelegator)
                .collect(Collectors.toSet());
        if (!delegators.isEmpty()) {
            log.info("calling data vault for {} delegator ids", delegators.size());
            return pnDataVaultClient.getRecipientDenominationByInternalId(new ArrayList<>(delegators))
                    .collectMap(BaseRecipientDtoDto::getInternalId, Function.identity());
        }
        return Mono.just(Collections.emptyMap());
    }

    private Mono<Map<String, PaSummaryDto>> callExternalRegistries(List<MandateEntity> entities) {
        Set<String> paIds = entities.stream()
                .map(MandateEntity::getVisibilityIds)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        if (!paIds.isEmpty()) {
            log.info("calling external registry for {} PA ids", paIds.size());
            return pnInfoPaClient.getManyPa(new ArrayList<>(paIds))
                    .collectMap(PaSummaryDto::getId, Function.identity());
        }
        return Mono.just(Collections.emptyMap());
    }

    private Mono<Map<String, PgGroupDto>> callExternalRegistries(String delegateId) {
        return pnExtRegPrvtClient.getGroups(delegateId, false)
                .collectMap(PgGroupDto::getId, Function.identity());
    }

    private List<Integer> generatePartitions(List<Integer> statuses) {
        if (statuses == null) {
            return Collections.emptyList();
        }
        List<Integer> partitions = new ArrayList<>(statuses);
        Collections.sort(partitions);
        log.debug("partitions: {}", partitions);
        return partitions;
    }
}
