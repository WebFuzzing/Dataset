package it.pagopa.pn.mandate.services.mandate.utils;

import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.mandate.exceptions.*;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcaregroups.v1.dto.PgGroupDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.mandate.middleware.msclient.PnExtRegPrvtClient;
import it.pagopa.pn.mandate.model.InputSearchMandateDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.*;

@Component
@lombok.CustomLog
public class MandateValidationUtils {

    private static final String DELEGATE_FISCAL_CODE = "delegate.fiscalCode";
    private static final String DELEGATOR_FISCAL_CODE = "delegator.fiscalCode";
    private static final String VERIFICATION_CODE = "verificationCode";
    private static final String DELEGATE_PERSON = "delegate.person";
    private static final String DELEGATOR_PERSON = "delegator.person";
    private static final String DELEGATE = "delegate";
    private static final String DELEGATOR = "delegator";
    private static final String DATE_TO = "dateTo";

    private final ValidateUtils validateUtils;
    private final PnExtRegPrvtClient pnExtRegPrvtClient;

    public MandateValidationUtils(ValidateUtils validateUtils, PnExtRegPrvtClient pnExtRegPrvtClient) {
        this.validateUtils = validateUtils;
        this.pnExtRegPrvtClient = pnExtRegPrvtClient;
    }


    public Mono<Void> validateCountRequest(String status) {
        String process = "validating count request";
        log.logChecking(process);
        // per ora l'unico stato supportato è il pending, quindi il filtro non viene passato al count
        // Inoltre, ritorno un errore se status != pending
        if (status == null || !status.equals(MandateDto.StatusEnum.PENDING.getValue())) {
            log.logCheckingOutcome(process, false, "invalid status requested");
            return Mono.error(new PnUnsupportedFilterException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_ASSERTENUM, "status"));
        }

        log.logCheckingOutcome(process, true);
        return Mono.empty();
    }

    /**
     * Il metodo si occoupa di validare i gruppi passati, ovvero che facciano parte della PG
     * e che siano gruppi ATTIVI
     *
     * @param institutionId id della PG
     * @param groups gruppo/i su da validare
     * @return exception se alcuni gruppi non son validi
     */
    public Mono<Void> validatePGGroups(String institutionId, List<String> groups) {
        String process = "validating group";
        log.logChecking(process);

        if (groups == null || groups.isEmpty()) {
            log.logCheckingOutcome(process, true);
            return Mono.empty();
        }

        return pnExtRegPrvtClient.getGroups(institutionId, true)
                .collectMap(PgGroupDto::getId, Function.identity())
                .flatMap(mapInstitutionActiveGroups -> {
                    for (String group :
                            groups) {

                        if (!mapInstitutionActiveGroups.containsKey(group)) {
                            log.logCheckingOutcome(process, false, "invalid group");
                            return Mono.error(new PnInvalidGroupCodeException());
                        }
                    }

                    log.logCheckingOutcome(process, true);
                    return Mono.empty();
                });
    }

    public Mono<Void> validateVisibilityId(MandateDto mandateDto) {
        String process = "validating visibilityId";
        log.logChecking(process);

        if (mandateDto.getVisibilityIds() == null || mandateDto.getVisibilityIds().isEmpty()) {
            log.logCheckingOutcome(process, true);
            return Mono.empty();
        }


        return pnExtRegPrvtClient.checkAooUoV2Ids(mandateDto.getVisibilityIds().stream().map(OrganizationIdDto::getUniqueIdentifier).collect(Collectors.toList()))
                .flatMap(response -> {
                    if (response.getIds() != null && !response.getIds().isEmpty()) {
                        log.logCheckingOutcome(process, false, "invalid visibilityId");
                        return Mono.error(new PnInvalidVisibilityIdException());
                    }else{
                        log.logCheckingOutcome(process, true);
                        return Mono.empty();
                    }
                });
    }

    @NotNull
    public AcceptRequestDto validateAcceptMandateRequest(String mandateId, AcceptRequestDto m) {
        String process = "validating accept mandate";
        log.logChecking(process);
        if (m == null || m.getVerificationCode() == null)
        {
            log.logCheckingOutcome(process, false, "invalid verification code");
            throw new PnInvalidVerificationCodeException();
        }

        if (mandateId == null)
        {
            log.logCheckingOutcome(process, false, "mandate not found");
            throw new PnMandateNotFoundException();
        }

        log.logCheckingOutcome(process, true);
        return m;
    }


    public void validateCreationRequestHimself( CxTypeAuthFleet cxTypeAuthFleet, final String requesterInternaluserId, final String delegateInternaluserId) {
        String process = "validating create mandate himself";
        log.logChecking(process);
        if (!CxTypeAuthFleet.PG.equals(cxTypeAuthFleet) && delegateInternaluserId.equals(requesterInternaluserId)) {
            log.logCheckingOutcome(process, false, "cannot delegate himself");
            throw new PnMandateByHimselfException();
        }

        log.logCheckingOutcome(process, true);
    }

    public MandateDto validateCreationRequest(MandateDto mandateDto) {
        String process = "validating create mandate";
        log.logChecking(process);
        // valida delegato
        if (mandateDto.getDelegate() == null) {
            log.logCheckingOutcome(process, false, "missing delegate");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, DELEGATE);
        }
        if ((mandateDto.getDelegate().getFiscalCode() == null)) {
            log.logCheckingOutcome(process, false, "missing delegate taxid");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, DELEGATE_FISCAL_CODE);
        }

        if ((mandateDto.getDelegate().getPerson() == null)) {
            log.logCheckingOutcome(process, false, "missing delegate isperson");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, DELEGATE_PERSON);
        }

        if ((mandateDto.getDelegate().getPerson() && (mandateDto.getDelegate().getFirstName() == null || mandateDto.getDelegate().getLastName() == null))
                || (!mandateDto.getDelegate().getPerson() && mandateDto.getDelegate().getCompanyName() == null)) {
            log.logCheckingOutcome(process, false, "invalid delegate");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER, DELEGATE);
        }
        // codice verifica (5 caratteri)
        if (mandateDto.getVerificationCode() == null) {
            log.logCheckingOutcome(process, false, "invalid verification code");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, VERIFICATION_CODE);
        }
        if (!mandateDto.getVerificationCode().matches("\\d\\d\\d\\d\\d")) {
            log.logCheckingOutcome(process, false, "invalid verification code format");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_PATTERN, VERIFICATION_CODE);
        }

        if (Boolean.TRUE.equals(mandateDto.getDelegate().getPerson())
                && !validateUtils.validate(mandateDto.getDelegate().getFiscalCode(), true, false)) {
            log.logCheckingOutcome(process, false, "invalid delegate taxid");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_PATTERN, DELEGATE_FISCAL_CODE);
        }
        // le PG possono avere p.iva o CF!
        if (Boolean.FALSE.equals(mandateDto.getDelegate().getPerson())
                && !validateUtils.validate(mandateDto.getDelegate().getFiscalCode(), false,false)) {
            log.logCheckingOutcome(process, false, "invalid delegate taxid");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_PATTERN, DELEGATE_FISCAL_CODE);
        }

        // la delega richiede la data di fine
        if (!StringUtils.hasText(mandateDto.getDateto())) {
            log.logCheckingOutcome(process, false, "missing expire date");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, DATE_TO);
        }

        log.logCheckingOutcome(process, true);
        return mandateDto;
    }

    public MandateDtoRequest validateReverseMandateCreationRequest(MandateDtoRequest mandateDtoRequest) {
        String process = "validating create mandate b2b";
        log.logChecking(process);
        // valida delegante
        if (mandateDtoRequest.getDelegator() == null) {
            log.logCheckingOutcome(process, false, "missing delegator");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, DELEGATOR);
        }
        if ((mandateDtoRequest.getDelegator().getFiscalCode() == null)) {
            log.logCheckingOutcome(process, false, "missing delegator taxid");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, DELEGATOR_FISCAL_CODE);
        }

        if ((mandateDtoRequest.getDelegator().getPerson() == null)) {
            log.logCheckingOutcome(process, false, "missing delegator isperson");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, DELEGATOR_PERSON);
        }

        if ((mandateDtoRequest.getDelegator().getPerson() && (mandateDtoRequest.getDelegator().getFirstName() == null || mandateDtoRequest.getDelegator().getLastName() == null))
                || (!mandateDtoRequest.getDelegator().getPerson() && mandateDtoRequest.getDelegator().getCompanyName() == null)) {
            log.logCheckingOutcome(process, false, "invalid delegator");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER, DELEGATOR);
        }

        if (Boolean.TRUE.equals(mandateDtoRequest.getDelegator().getPerson())
                && !validateUtils.validate(mandateDtoRequest.getDelegator().getFiscalCode(), true, false)) {
            log.logCheckingOutcome(process, false, "invalid delegator taxid");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_PATTERN, DELEGATOR_FISCAL_CODE);
        }
        // le PG possono avere p.iva o CF!
        if (Boolean.FALSE.equals(mandateDtoRequest.getDelegator().getPerson())
                && !validateUtils.validate(mandateDtoRequest.getDelegator().getFiscalCode(), false,false)) {
            log.logCheckingOutcome(process, false, "invalid delegator taxid");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_PATTERN, DELEGATOR_FISCAL_CODE);
        }

        // la delega richiede la data di fine
        if (!StringUtils.hasText(mandateDtoRequest.getDateto())) {
            log.logCheckingOutcome(process, false, "missing expire date");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_REQUIRED, DATE_TO);
        }

        LocalDate tomorrowDate = LocalDate.now().plusDays(1);
        LocalDate dateTo = LocalDate.parse(mandateDtoRequest.getDateto());
        if (dateTo.isBefore(tomorrowDate)) {
            log.logCheckingOutcome(process, false, "expire date cannot be today or in the past");
            throw new PnInvalidInputException(ERROR_CODE_PN_GENERIC_INVALIDPARAMETER_PATTERN, DATE_TO);
        }

        log.logCheckingOutcome(process, true);
        return mandateDtoRequest;
    }


    public void validateSearchRequest(InputSearchMandateDto searchDto) {
        String process = "validating create mandate";
        log.logChecking(process);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            Set<ConstraintViolation<InputSearchMandateDto>> errors = validator.validate(searchDto);
            if (!errors.isEmpty()) {
                log.warn("validation search input errors: {}", errors);
                log.logCheckingOutcome(process, false);
                List<ProblemError> problems = new ExceptionHelper(Optional.empty())
                        .generateProblemErrorsFromConstraintViolation(errors);
                throw new PnInvalidInputException(searchDto.getDelegateId(), problems);
            }
        }

        log.logCheckingOutcome(process, true);
    }

    public void validateListMandatesByDelegateRequest(
                CxTypeAuthFleet xPagopaPnCxType){

        if (CxTypeAuthFleet.PG.equals(xPagopaPnCxType) ) {
            throw new PnForbiddenException();
        }
    }
}
