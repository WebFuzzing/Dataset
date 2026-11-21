package it.pagopa.pn.mandate.services.mandate.v1;

import it.pagopa.pn.mandate.mapper.MandateEntityInternalMandateDtoMapper;
import it.pagopa.pn.mandate.mapper.StatusEnumMapper;
import it.pagopa.pn.mandate.middleware.db.MandateDao;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.DelegateType;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.InternalMandateDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateByDelegatorRequestDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto.StatusEnum;
import it.pagopa.pn.mandate.model.InputSearchMandateDto;
import it.pagopa.pn.mandate.utils.PgUtils;
import it.pagopa.pn.mandate.utils.TypeSegregatorFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.*;

@Service
@lombok.CustomLog
public class MandatePrivateService {


    private final MandateDao mandateDao;
    private final MandateEntityInternalMandateDtoMapper mandateEntityInternalMandateDtoMapper;

    public MandatePrivateService(MandateDao mandateDao, MandateEntityInternalMandateDtoMapper mandateEntityInternalMandateDtoMapper) {
        this.mandateDao = mandateDao;
        this.mandateEntityInternalMandateDtoMapper = mandateEntityInternalMandateDtoMapper;
    }

    public Flux<InternalMandateDto> listMandatesByDelegate(String internaluserId, String mandateId,
                                                           CxTypeAuthFleet xPagopaPnCxType, List<String> groups) {
        // nelle invocazioni tra servizi mi interessano SEMPRE solo le deleghe ATTIVE
        log.info("listing private mandates by delegate for internaluserId={} mandateId={}", internaluserId, mandateId);
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(internaluserId)
                .status(StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE))
                .mandateId(mandateId)
                .cxType(xPagopaPnCxType)
                .groups(groups)
                .build();
        return mandateDao.listMandatesByDelegate(inputSearchMandateDto, TypeSegregatorFilter.STANDARD)
                .map(mandateEntityInternalMandateDtoMapper::toDto)
                .doOnNext(mand -> log.info("listMandatesByDelegate found mandate={}", mand));
    }

    public Flux<InternalMandateDto> listMandatesByDelegateV2(InputSearchMandateDto inputSearchMandateDto) {
        // nelle invocazioni tra servizi mi interessano SEMPRE solo le deleghe ATTIVE
        String internaluserId = inputSearchMandateDto.getDelegateId();
        String mandateId = inputSearchMandateDto.getMandateId();
        log.info("listing private mandates by delegate v2 for internaluserId={} mandateId={}", internaluserId, mandateId);
        return mandateDao.listMandatesByDelegate(inputSearchMandateDto, null)
                .map(mandateEntityInternalMandateDtoMapper::toDto)
                .doOnNext(mand -> log.info("listMandatesByDelegateV2 found mandate={}", mand));
    }

    public Flux<InternalMandateDto> listMandatesByDelegator(String internaluserId,
                                                            String mandateId,
                                                            CxTypeAuthFleet xPagopaPnCxType,
                                                            List<String> cxGroups,
                                                            String cxRole,
                                                            DelegateType delegateType) {
        // nelle invocazioni tra servizi mi interessano SEMPRE solo le deleghe ATTIVE
        log.info("listing private mandates by delegate for internaluserId={} mandateId={}", internaluserId, mandateId);

        Integer status = StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE);
        return PgUtils.validaAccessoOnlyAdmin(xPagopaPnCxType, cxRole, cxGroups)
                .flatMapMany(obj -> mandateDao.listMandatesByDelegator(internaluserId, status, mandateId, delegateType))
                .map(mandateEntityInternalMandateDtoMapper::toDto)
                .doOnNext(mand -> log.info("listMandatesByDelegator found mandate={}", mand));
    }

    public Flux<InternalMandateDto> listMandatesByDelegators(DelegateType delegateType,
                                                             List<String> delegateGroups,
                                                             Flux<MandateByDelegatorRequestDto> mandateByDelegatorRequestDto) {
        log.info("listing private mandates by delegators, delegateType={}, delegateGroups={}", delegateType, delegateGroups);
        Integer filterState = StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE);
        Set<String> filterDelegateGroups;
        if (CollectionUtils.isEmpty(delegateGroups)) {
            filterDelegateGroups = Collections.emptySet();
        } else {
            filterDelegateGroups = new HashSet<>(delegateGroups);
        }
        return mandateByDelegatorRequestDto.collectList()
                .flatMapMany(mandateDao::listMandatesByDelegators)
                .filter(mandate -> filterState.equals(mandate.getState())
                        && (DelegateType.PG != delegateType
                            || CollectionUtils.isEmpty(filterDelegateGroups)
                            || !Collections.disjoint(filterDelegateGroups, mandate.getGroups()))
                )
                .map(mandateEntityInternalMandateDtoMapper::toDto);
    }
}
