package it.pagopa.pn.mandate.mapper;

import java.util.NoSuchElementException;

import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto.StatusEnum;

/**
 * Mappa lo stato da StatusEnum, Stringa o valore intero ad uno degli altri tipi.
 * Utile per conversioni tra stato salvato in DB e stato passato nei DTO/filtri.
 * NB: Il valore intero degli stati è crescente. Il valore di ACTIVE (20), definisce quindi lo spartiacque
 *     tra una delega "viva" ed una storicizzata.
 */
public class StatusEnumMapper {

    private StatusEnumMapper(){}
    
    public static int intValfromStatus(StatusEnum s)
    {
        if (s == StatusEnum.PENDING)
            return 10;
        if (s == StatusEnum.ACTIVE)
            return 20;
        if (s == StatusEnum.REJECTED)
            return 30;
        if (s == StatusEnum.REVOKED)
            return 40;
        if (s == StatusEnum.EXPIRED)
            return 50;
        
        throw new NoSuchElementException();
    }

    public static int intValfromValueConst(String val)
    {
       return intValfromStatus(fromValueConst(val));
    }

    // boilerplate
    public static StatusEnum fromValue(int val) {
        if (val == 10)
            return StatusEnum.PENDING;
        if (val == 20)
            return StatusEnum.ACTIVE;
        if (val == 30)
            return StatusEnum.REJECTED;
        if (val == 40)
            return StatusEnum.REVOKED;
        if (val == 50)
            return StatusEnum.EXPIRED;
        
        throw new NoSuchElementException();
    }

    public static StatusEnum fromValueConst(String val) {
       if (val == null)        
        throw new NoSuchElementException();
        
        return StatusEnum.fromValue(val);
    }
}