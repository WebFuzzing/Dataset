package it.pagopa.pn.papertracker.model;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import lombok.Getter;

import java.util.List;
import java.util.stream.Stream;

import static it.pagopa.pn.papertracker.model.DeliveryFailureCauseEnum.*;

@Getter
public enum EventStatusCodeEnum {
    // AR
    RECRN006(EventTypeEnum.RETRYABLE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(F01, F02, F03, F04), "Furto/Smarrimento/deterioramento", false),
    RECRN013(EventTypeEnum.RETRYABLE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(), "Non Rendicontabile ", false),
    RECRN015(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(C01, C02, C03, C04, C05, C06), "Causa Forza Maggiore", false),
    RECRN001C(EventTypeEnum.FINAL_EVENT, ProductType.AR, EventStatus.OK, List.of(),"Consegnato - Fascicolo Chiuso", false),
    RECRN002C(EventTypeEnum.FINAL_EVENT, ProductType.AR, EventStatus.OK, List.of(CHECK_IF_REQUIRED),"Mancata consegna - Fascicolo Chiuso", false),
    RECRN002F(EventTypeEnum.FINAL_EVENT, ProductType.AR, EventStatus.KO, List.of(CHECK_IF_REQUIRED), "Irreperibilità Assoluta - Fascicolo Chiuso", false),
    RECRN003C(EventTypeEnum.FINAL_EVENT, ProductType.AR, EventStatus.OK, List.of(),"Consegnato presso Punti di Giacenza - Fascicolo Chiuso", false),
    RECRN004C(EventTypeEnum.FINAL_EVENT, ProductType.AR, EventStatus.OK, List.of(),"Mancata consegna presso Punti di Giacenza - Fascicolo Chiuso", false),
    RECRN005C(EventTypeEnum.FINAL_EVENT, ProductType.AR, EventStatus.OK, List.of(),"Compiuta giacenza - Fascicolo Chiuso", false),
    RECRN011(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(), "In giacenza", false),
    RECRN001A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(), "Consegnato - pre-esito", false),
    RECRN002A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(M02, M05, M06, M07, M08, M09, M10), "Mancata consegna - pre-esito", false),
    RECRN002D(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(M01, M03, M04), "Irreperibilità Assoluta - pre-esito", false),
    RECRN003A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(), "Consegnato presso Punti di Giacenza - pre-esito", false),
    RECRN004A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(), "Mancata consegna presso Punti di Giacenza - pre-esito", false),
    RECRN005A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(), "Compiuta giacenza pre-esito", false),
    RECRN001B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(), "Consegnato - In Dematerializzazione", true),
    RECRN002B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(CHECK_IF_REQUIRED), "Mancata consegna - In Dematerializzazione", true),
    RECRN002E(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(CHECK_IF_REQUIRED), "Irreperibilità Assoluta - In Dematerializzazione", true),
    RECRN003B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(), "Consegnato presso Punti di Giacenza - In Dematerializzazione", true),
    RECRN004B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(), "Mancata consegna presso Punti di Giacenza - In Dematerializzazione", true),
    RECRN005B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(), "Compiuta giacenza - In Dematerializzazione", true),
    RECRN010(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.AR, EventStatus.PROGRESS, List.of(), "Inesito", false),
    PNRN012(EventTypeEnum.FINAL_EVENT, ProductType.AR, EventStatus.OK, List.of(),"Perfezionamento per decorrenza termini", false),

    // RIR
    RECRI001(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RIR, EventStatus.PROGRESS, List.of(), "Avviato all'estero", false),
    RECRI002(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RIR, EventStatus.PROGRESS, List.of(), "Ingresso nel paese estero", false),
    RECRI005(EventTypeEnum.RETRYABLE_EVENT, ProductType.RIR, EventStatus.PROGRESS, List.of(F01, F02, F03, F04), "Furto, smarrimento o deterioramento", false),
    RECRI003A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RIR, EventStatus.PROGRESS, List.of(), "Consegnato - pre-esito", false),
    RECRI004A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RIR, EventStatus.PROGRESS, List.of(CHECK_IF_REQUIRED), "Mancata consegna - pre-esito", false),
    RECRI003B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RIR, EventStatus.PROGRESS, List.of(), "Consegnato - In Dematerializzazione", true),
    RECRI004B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RIR, EventStatus.PROGRESS, List.of(CHECK_IF_REQUIRED), "Mancata consegna - In Dematerializzazione", true),
    RECRI003C(EventTypeEnum.FINAL_EVENT, ProductType.RIR, EventStatus.OK, List.of(),"Consegnato - Fascicolo Chiuso", false),
    RECRI004C(EventTypeEnum.FINAL_EVENT, ProductType.RIR, EventStatus.KO, List.of(CHECK_IF_REQUIRED), "Mancata consegna - Fascicolo Chiuso", false),

    // CON
    CON998(EventTypeEnum.NOT_RETRYABLE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Scartato NODOC", false),
    CON997(EventTypeEnum.NOT_RETRYABLE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Scartato CAP/INTERNAZIONALE", false),
    CON996(EventTypeEnum.CON996_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Scartato PDF", false),
    CON995(EventTypeEnum.NOT_RETRYABLE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Errore stampa", false),
    CON993(EventTypeEnum.NOT_RETRYABLE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Errore Stampa (parziale)", false),
    CON080(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Stampato ed Imbustato", false),
    CON020(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Affido conservato", false),
    CON010(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Distinta Elettronica inviata a Recapitista", false),
    CON011(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Distinta Elettronica Sigillata", false),
    CON012(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "OK Distinta Elettronica da Recapitista", false),
    CON09A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Materialità Pronta", false),
    CON09B(EventTypeEnum.SAVE_ONLY_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Prenotazione Pickup", false),
    CON016(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "PICKUP Sigillata", false),
    CON018(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Accettazione Recapitista", false),

    REC016(EventTypeEnum.SAVE_ONLY_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Materialità Ritirata", false),
    REC018(EventTypeEnum.SAVE_ONLY_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Accettazione Recapitista", false),
    REC991(EventTypeEnum.SAVE_ONLY_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Mancata Accettazione Recapitista", false),
    //eventi non gestiti da paper-channel ma previsti dal consolidatore
    //CON992(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "KO Distinta Elettronica da Recapitista", false),
    //CON991(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Mancata Accetazione Recapitsita ", false);

    P000(EventTypeEnum.SAVE_ONLY_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Affidato al consolidatore", false),
    P001(EventTypeEnum.SAVE_ONLY_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "In progress", false),
    P011(EventTypeEnum.SAVE_ONLY_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Errore di sintassi", false),
    P012(EventTypeEnum.SAVE_ONLY_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Errore semantico", false),
    P013(EventTypeEnum.SAVE_ONLY_EVENT, ProductType.ALL, EventStatus.PROGRESS, List.of(), "Errore di trasformazione", false),

    // 890
    RECAG001A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Consegnato - pre-esito", false),
    RECAG002A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Consegnato a persona abilitata - pre-esito", false),
    RECAG003A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(M02, M05, M06, M07, M08, M09, M10), "Mancata consegna - pre-esito", false),
    RECAG003D(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(M01, M03, M04), "Irreperibilità Assoluta - pre-esito", false),
    RECAG002B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Consegnato a persona abilitata - In Dematerializzazione", true),
    RECAG003B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(CHECK_IF_REQUIRED), "Mancata consegna - In Dematerializzazione", true),
    RECAG001B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Consegnato - In Dematerializzazione", true),
    RECAG003E(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(CHECK_IF_REQUIRED), "Irreperibilità Assoluta - In Dematerializzazione", true),
    RECAG002C(EventTypeEnum.FINAL_EVENT, ProductType._890, EventStatus.OK, List.of(), "Consegnato a persona abilitata - Fascicolo Chiuso", false),
    RECAG003C(EventTypeEnum.FINAL_EVENT, ProductType._890, EventStatus.OK, List.of(CHECK_IF_REQUIRED), "Mancata consegna - Fascicolo Chiuso", false),
    RECAG001C(EventTypeEnum.FINAL_EVENT, ProductType._890, EventStatus.OK, List.of(), "Consegnato - Fascicolo Chiuso", false),
    RECAG003F(EventTypeEnum.FINAL_EVENT, ProductType._890, EventStatus.KO, List.of(CHECK_IF_REQUIRED), "Irreperibilità Assoluta - Fascicolo Chiuso", false),
    RECAG010(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Inesito", false),
    RECAG010A(EventTypeEnum.SAVE_ONLY_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Invio CAD", false),
    RECAG004(EventTypeEnum.RETRYABLE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(F01, F02, F03, F04), "Furto/Smarrimento/deterioramento", false),
    RECAG015(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(C01, C02, C03, C04, C05, C06), "Causa Forza Maggiore", false),
    RECAG013(EventTypeEnum.RETRYABLE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Non Rendicontabile", false),
    RECAG011A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "In giacenza", false),
    RECAG011B(EventTypeEnum.STOCK_INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "In giacenza - In Dematerializzazione", true),
    RECAG012(EventTypeEnum.RECAG012_EVENT, ProductType._890, EventStatus.OK, List.of(), "Accettazione 23L", false),
    RECAG012A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Accettazione 23L - pre-esito", false),
    RECAG005A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Consegnato presso Punti di Giacenza - pre-esito", false),
    RECAG005B(EventTypeEnum.STOCK_INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Consegnato presso Punti di Giacenza - In Dematerializzazione", true),
    RECAG005C(EventTypeEnum.FINAL_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Consegnato presso Punti di Giacenza - Fascicolo Chiuso", false),
    RECAG006A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Consegna a persona abilitata presso Punti di Giacenza - pre-esito", false),
    RECAG006B(EventTypeEnum.STOCK_INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Consegna a persona abilitata presso Punti di Giacenza - In Dematerializzazione", true),
    RECAG006C(EventTypeEnum.FINAL_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Consegna a persona abilitata presso Punti di Giacenza - Fascicolo Chiuso", false),
    RECAG007A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Mancata consegna presso Punti di Giacenza - pre-esito", false),
    RECAG007B(EventTypeEnum.STOCK_INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Mancata consegna presso Punti di Giacenza - In Dematerializzazione", true),
    RECAG007C(EventTypeEnum.FINAL_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Mancata consegna presso Punti di Giacenza - Fascicolo Chiuso", false),
    RECAG008A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Compiuta giacenza - pre-esito", false),
    RECAG008B(EventTypeEnum.STOCK_INTERMEDIATE_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Compiuta giacenza - In Dematerializzazione", true),
    RECAG008C(EventTypeEnum.FINAL_EVENT, ProductType._890, EventStatus.PROGRESS, List.of(), "Compiuta giacenza - Fascicolo Chiuso", false),

    // RS
    RECRS002A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RS, EventStatus.PROGRESS, List.of(M02, M05, M06, M07, M08, M09, M10), "Mancata consegna - pre-esito", false),
    RECRS002D(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RS, EventStatus.PROGRESS, List.of(M01, M03, M04), "Irreperibilità Assoluta - pre-esito", false),
    RECRS004A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RS, EventStatus.PROGRESS, List.of(), "Mancata consegna presso Punti di Giacenza - pre-esito", false),
    RECRS005A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RS, EventStatus.PROGRESS, List.of(), "Compiuta giacenza - pre-esito", false),
    RECRS015(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RS, EventStatus.PROGRESS, List.of(C01, C02, C03, C04, C05, C06), "Causa Forza Maggiore", false),
    RECRS010(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RS, EventStatus.PROGRESS, List.of(), "Inesito", false),
    RECRS002B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RS, EventStatus.PROGRESS, List.of(CHECK_IF_REQUIRED), "Mancata consegna - In Dematerializzazione", true),
    RECRS002E(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RS, EventStatus.PROGRESS, List.of(CHECK_IF_REQUIRED),"Irreperibilità Assoluta - In Dematerializzazione",true),
    RECRS004B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RS, EventStatus.PROGRESS, List.of(), "Mancata consegna presso Punti di Giacenza - In Dematerializzazione", true),
    RECRS005B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RS, EventStatus.PROGRESS, List.of(), "Compiuta giacenza - In Dematerializzazione", true),
    RECRS001C(EventTypeEnum.FINAL_EVENT, ProductType.RS, EventStatus.OK, List.of(), "Consegnato - Fascicolo Chiuso", false),
    RECRS002C(EventTypeEnum.FINAL_EVENT, ProductType.RS, EventStatus.KO, List.of(CHECK_IF_REQUIRED), "Mancata consegna - Fascicolo Chiuso", false),
    RECRS003C(EventTypeEnum.FINAL_EVENT, ProductType.RS, EventStatus.OK, List.of(), "Consegnato presso Punti di Giacenza - Fascicolo Chiuso", false),
    RECRS002F(EventTypeEnum.FINAL_EVENT, ProductType.RS, EventStatus.KO, List.of(CHECK_IF_REQUIRED), "Irreperibilità Assoluta - Fascicolo Chiuso", false),
    RECRS004C(EventTypeEnum.FINAL_EVENT, ProductType.RS, EventStatus.KO, List.of(), "Mancata consegna presso Punti di Giacenza - Fascicolo Chiuso", false),
    RECRS005C(EventTypeEnum.FINAL_EVENT, ProductType.RS, EventStatus.KO, List.of(), "Compiuta giacenza - Fascicolo Chiuso", false),
    RECRS006(EventTypeEnum.RETRYABLE_EVENT, ProductType.RS, EventStatus.PROGRESS, List.of(F01, F02, F03, F04), "Furto/Smarrimento/deterioramento", false),
    RECRS013(EventTypeEnum.RETRYABLE_EVENT, ProductType.RS, EventStatus.PROGRESS, List.of(), "Non Rendicontabile", false),
    RECRS011(EventTypeEnum.SAVE_ONLY_EVENT, ProductType.RS, EventStatus.PROGRESS, List.of(), "In giacenza", false),

    //RIS
    RECRSI004A(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RIS, EventStatus.PROGRESS, List.of(CHECK_IF_REQUIRED), "Non Consegnato - pre-esito", false),
    RECRSI004B(EventTypeEnum.INTERMEDIATE_EVENT, ProductType.RIS, EventStatus.PROGRESS, List.of(CHECK_IF_REQUIRED), "Non Consegnato - In Dematerializzazione", true),
    RECRSI003C(EventTypeEnum.FINAL_EVENT, ProductType.RIS, EventStatus.OK, List.of(), "Consegnato - Fascicolo Chiuso", false),
    RECRSI004C(EventTypeEnum.FINAL_EVENT, ProductType.RIS, EventStatus.KO, List.of(CHECK_IF_REQUIRED), "Non Consegnato - fascicolo Chiuso", false),
    RECRSI005(EventTypeEnum.RETRYABLE_EVENT, ProductType.RIS, EventStatus.PROGRESS, List.of(F01, F02, F03, F04), "Furto/Smarrimento/deterioramento", false),
    RECRSI001(EventTypeEnum.SAVE_ONLY_EVENT, ProductType.RIS, EventStatus.PROGRESS, List.of(), "Avviato all'estero", false),
    RECRSI002(EventTypeEnum.SAVE_ONLY_EVENT, ProductType.RIS, EventStatus.PROGRESS, List.of(), "Ingresso nel paese estero", false);


    private final EventTypeEnum codeType;
    private final ProductType productType;
    private final EventStatus status;
    private final List<DeliveryFailureCauseEnum> deliveryFailureCauseList;
    private final String statusCodeDescription;
    private final boolean isFinalDemat;

    EventStatusCodeEnum(EventTypeEnum codeType, ProductType productType,
                                EventStatus status, List<DeliveryFailureCauseEnum> deliveryFailureCauseList, String statusCodeDescription, boolean isFinalDemat) {
        this.codeType = codeType;
        this.productType = productType;
        this.status = status;
        this.statusCodeDescription = statusCodeDescription;
        this.deliveryFailureCauseList = deliveryFailureCauseList;
        this.isFinalDemat = isFinalDemat;
    }

    public static EventStatusCodeEnum fromKey(String key) {
        return Stream.of(values())
                .filter(definition -> definition.name().equals(key))
                .findFirst()
                .orElse(null);
    }
}