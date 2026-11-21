package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.mandate.config.PnMandateConfig;
import it.pagopa.pn.mandate.generated.openapi.msclient.delivery.v1.dto.UserInfoQrCodeDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.model.SrcChannelType;
import it.pagopa.pn.mandate.model.WorkFlowType;
import it.pagopa.pn.mandate.utils.DateUtils;
import it.pagopa.pn.mandate.utils.MandateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
@AllArgsConstructor
@Slf4j
public class MandateEntityBuilderMapper {
    private final PnMandateConfig pnMandateConfig;
    private final MandateUtils mandateUtils;

    public MandateEntity buildMandateEntity(String delegatorUserId, UserInfoQrCodeDto dto, String mandateId, String delegateUserId) {
        MandateEntity entity = new MandateEntity();
        entity.setDelegate(delegateUserId);
        entity.setMandateId(mandateId);
        entity.setDelegator(delegatorUserId);
        entity.setDelegatorUid(removePfPrefix(delegatorUserId));
        entity.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.PENDING));
        entity.setValidationcode(mandateUtils.generateRandomCode());
        entity.setCreated(Instant.now());
        entity.setValidfrom(Instant.parse(String.valueOf(DateUtils.PN_EPOCH)));
        entity.setValidto(Instant.now().plus(pnMandateConfig.getCiePendingDuration()));
        entity.setDelegatorisperson(true);
        entity.setDelegateisperson(true);
        entity.setWorkflowType(WorkFlowType.CIE);
        entity.setSrcChannel(SrcChannelType.IO.name());
        entity.setIuns(Set.of(dto.getIun()));
        if (log.isInfoEnabled())
            log.info("creating mandate uuid: {} iuid: {} iutype_isPF: {} validfrom: {}",
                    entity.getMandateId(), entity.getDelegator(), true, entity.getValidfrom());
        return entity;
    }

    private String removePfPrefix(String delegatorUserId) {
        return delegatorUserId.replace("PF-", "");
    }
}
