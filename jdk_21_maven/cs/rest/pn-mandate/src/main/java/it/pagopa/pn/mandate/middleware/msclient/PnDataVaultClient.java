package it.pagopa.pn.mandate.middleware.msclient;


import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.api.MandatesApi;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.api.RecipientsApi;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.dto.DenominationDtoDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.dto.MandateDtoDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.dto.RecipientTypeDto;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Classe wrapper di pn-data-vault, con gestione del backoff
 */
@Component
@lombok.CustomLog
public class PnDataVaultClient {
    
    private final RecipientsApi recipientsApi;
    private final MandatesApi mandatesApi;

    public PnDataVaultClient(RecipientsApi recipientsApi, MandatesApi mandatesApi) {
        this.recipientsApi = recipientsApi;
        this.mandatesApi = mandatesApi;
    }


    /**
     * Ritorna una lista di nominativi in base alla lista di iuid passati
     *
     * @param internalIds lista di iuid
     * @return lista di nominativi
     */
    public Flux<BaseRecipientDtoDto> getRecipientDenominationByInternalId(List<String> internalIds)
    {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "Opaque Ids Resolution");
        return recipientsApi.getRecipientDenominationByInternalId(internalIds);
            
    }

    /**
     * Genera (o recupera) un internaluserId in base al CF/PIVA
     * @param isPerson true per PF, false per PG
     * @param fiscalCode CF o PIVA
     * @return iuid
     */
    public Mono<String> ensureRecipientByExternalId(boolean isPerson, String fiscalCode)
    {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "Opaque Id Creation");
        return recipientsApi.ensureRecipientByExternalId(isPerson?RecipientTypeDto.PF:RecipientTypeDto.PG, fiscalCode);
            
    }

    /**
     * Salva per una certa delega, le info riguardanti il DELEGATO
     *
     * @param mandateId id della delega
     * @param name nome
     * @param surname cognome
     * @param businessName ragione sociale
     * @return void
     */
    public Mono<String> updateMandateById(String mandateId, String name, String surname, String businessName)
    {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "Securing mandate info");

        DenominationDtoDto addressdto = new DenominationDtoDto();
        addressdto.setDestName(name);
        addressdto.setDestSurname(surname);
        addressdto.setDestBusinessName(businessName);
        return mandatesApi.updateMandateById(mandateId, addressdto)
                .then(Mono.just("OK"));

    }

    /**
     * Elimina le informazioni per una certa delega
     *
     * @param mandateId id della delega
     * @return void
     */
    public Mono<Void> deleteMandateById(String mandateId)
    {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "Removing mandate info");
        return mandatesApi.deleteMandateById(mandateId);
    }

    /**
     * Ritorna le info sui DELEGATI in base agli id delle deleghe passati
     * @param mandateIds lista di id deleghe
     * @return lista userinfo deleghe
     */
    public Flux<MandateDtoDto> getMandatesByIds(List<String> mandateIds)
    {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DATA_VAULT, "Retrieving mandate info");
        return mandatesApi.getMandatesByIds(mandateIds);
    }

}
