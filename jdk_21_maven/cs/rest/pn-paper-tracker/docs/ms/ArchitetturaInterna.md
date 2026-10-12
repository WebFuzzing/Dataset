# Logica di business

## Inizializzazione della spedizione tramite API initTracking
Il flusso operativo di pn-paper-tracker ha inizio con la chiamata all’API `initTracking`, esposta dal controller 
[`PaperTrackerTrackingController`](../../src/main/java/it/pagopa/pn/papertracker/rest/PaperTrackerTrackingController.java) e 
invocata dal microservizio pn-paper-channel.

Questa API viene utilizzata per inizializzare una nuova spedizione, salvando un oggetto `PaperTrackings` su DynamoDB. 
I dati inizializzati comprendono:
- **trackingId**: identificativo univoco della spedizione cartacea
- **productType**: tipologia di prodotto (AR, RIR, 890, RS, RIS)
- **unifiedDeliveryDriver**: recapitista
- **analogRequestClientId**: identificativo del client che ha effettuato la richiesta di spedizione
- **altri metadati**: configurazioni ed eventuali dati aggiuntivi utili al tracciamento e alla gestione della spedizione

Mentre l'`analogRequestClientId` viene ricevuto tramite header, gli altri dati vengono ricevuti tramite il payload della richiesta (`TrackingCreationRequest`) e 
salvati tramite il servizio [`PaperTrackerTrackingService`](../../src/main/java/it/pagopa/pn/papertracker/service/PaperTrackerTrackingService.java). 
L’inizializzazione della spedizione è un prerequisito per la corretta gestione degli eventi successivi relativi alla stessa spedizione.


La classe [`PnEventInboundService`](../../src/main/java/it/pagopa/pn/papertracker/middleware/queue/consumer/PnEventInboundService.java)
gestisce la ricezione dei messaggi da tre code distinte: `pn-external_channel_to_paper_channel`, `pn-external_channel_to_paper_tracker` e `pn-ocr_outputs`.
Ogni coda segue un flusso separato, descritto di seguito.

## Consumer pn-external_channel_to_paper_channel
I messaggi ricevuti vengono inoltrati, in definitiva, alla classe
[`SourceQueueProxyService`](../../src/main/java/it/pagopa/pn/papertracker/service/impl/SourceQueueProxyServiceImpl.java),
che si occupa di gestire il routing degli eventi provenienti da pn-ec in base alla modalità operativa associata alla spedizione 
(attributo `processingMode` della classe [`PaperTrackings`](../../src/main/java/it/pagopa/pn/papertracker/middleware/dao/dynamo/entity/PaperTrackings.java)).

La logica applicata è la seguente:
- **Spedizione NON presente su pn-PaperTrackings**: l'evento viene inoltrato solo a pn-paper-channel, unicamente se non è un duplicato (`isDuplicate == false` oppure `isDuplicate == null`).
- **Spedizione presente e modalità DRY**: l'evento viene inoltrato a pn-paper-tracker e a pn-paper-channel; l'inoltro a pn-paper-channel avviene solo se l'evento non è un duplicato (`isDuplicate == false` oppure `isDuplicate == null`).
- **Spedizione presente e modalità RUN**: l'evento viene inoltrato solo a pn-paper-tracker.
- **Spedizione con processingMode null**: l'evento viene gestito come in modalità DRY (inoltro a pn-paper-tracker e, solo se non duplicato, a pn-paper-channel).

Il messaggio viene arricchito con header e flag dry-run, e inoltrato ai consumer appropriati tramite i producer dedicati.

## Consumer pn-external_channel_to_paper_tracker
I messaggi ricevuti vengono inoltrati alla classe
[`ExternalChannelHandler`](../../src/main/java/it/pagopa/pn/papertracker/middleware/queue/consumer/internal/ExternalChannelHandler.java).
Qui, in base al `productType` e all' `eventType` (ricavato tramite lo `statusCode` e l’enum
[`EventStatusCodeEnum`](../../src/main/java/it/pagopa/pn/papertracker/model/EventStatusCodeEnum.java)), il messaggio
viene smistato, tramite l’[`HandlersRegistry`](../../src/main/java/it/pagopa/pn/papertracker/service/handler_step/generic/HandlersRegistry.java),
verso il relativo handler, definito nella classe astratta
[`AbstractHandlersFactory`](../../src/main/java/it/pagopa/pn/papertracker/service/handler_step/generic/AbstractHandlersFactory.java)
o sue estensioni (es.
[`HandlersFactory890`](../src/main/java/it/pagopa/pn/papertracker/service/handler_step/_890/HandlersFactory890.java)).

Ogni handler esegue una sequenza di step (classi che implementano l’interfaccia
[`HandlerStep`](../src/main/java/it/pagopa/pn/papertracker/service/handler_step/HandlerStep.java)),
utilizzando le informazioni condivise tramite la classe
[`HandlerContext`](../src/main/java/it/pagopa/pn/papertracker/model/HandlerContext.java).

### Esempio di sequence e gestione degli eventi
Esempio di configurazione di una sequence:

```json
{
    "sequenceName": "OK_AR",
    "sequence": "@sequence.5s-CON080.5s-CON020[DOC:7ZIP;PAGES:3].5s-CON018.5s-RECRN001A.5s-RECRN001B[DOC:AR].5s-RECRN001C"
}
```

In questo esempio del prodotto `AR`, i primi tre `statusCode` (`CON080`, `CON020`, `CON018`), 
essendo definiti nell’enum come `INTERMEDIATE_EVENT`, vengono processati dal rispettivo handler `buildIntermediateEventsHandler`.
Gli eventi `RECRN001A` e `RECRN001B`, essendo anch’essi eventi intermedi, vengono gestiti dallo stesso handler.
Diversamente, l’evento `RECRN001C`, essendo di tipo `EventTypeEnum.FINAL_EVENT`, viene processato dall’handler `buildFinalEventsHandler`.

```mermaid
flowchart LR

    %% Eventi → Handler
    A[CON080] -->|INTERMEDIATE_EVENT| INT
    B[CON020] -->|INTERMEDIATE_EVENT| INT
    C[CON018] -->|INTERMEDIATE_EVENT| INT
    D[RECRN001A] -->|INTERMEDIATE_EVENT| INT
    E[RECRN001B] -->|INTERMEDIATE_EVENT| INT

    F[RECRN001C] -->|FINAL_EVENT| FIN

    %% Intermediate Handler
    subgraph INT[buildIntermediateEventsHandler]
        H1[metadataUpserter] --> 
        H2[checkTrackingState] --> 
        H3[duplicatedEventFiltering] --> 
        H4[intermediateEventsBuilder] --> 
        H5[outputTargetSender]
    end

    %% Final Handler
    subgraph FIN[buildFinalEventsHandler]
        I1[metadataUpserter] --> 
        I2[checkTrackingState] --> 
        I3[sequenceValidator] --> 
        I4[dematValidator] --> 
        I5[finalEventBuilder] --> 
        I6[outputTargetSender]
    end
```

### Giacenza 890
Per quanto riguarda la giacenza 890, si tiene conto di due flussi paralleli: il perfezionamento della spedizione e la chiusura fascicolo.
Come mostrato nel diagramma sotto, il perfezionamento si conclude con l'invio del feedback `RECAG012`, qualora la condizione 
di refinement sia soddisfatta (ricezione del documento 23L e del `RECAG012`),
mentre, la chiusura fascicolo, termina con l'invio del progress `RECAG005C`, una volta validato l'ARCAD.

```mermaid
sequenceDiagram
participant pn-ec
participant pn-paper-tracker
participant OCR
participant pn-delivery-push

    pn-ec->>pn-paper-tracker: RECAG011B[23L, ARCAD]
    pn-ec->>pn-paper-tracker: RECAG005A
    pn-ec->>pn-paper-tracker: RECAG012
    pn-ec->>pn-paper-tracker: RECAG005C

    pn-paper-tracker->>pn-delivery-push: PROGRESS - RECAG011B
    pn-paper-tracker->>pn-delivery-push: PROGRESS - RECAG005A
    pn-paper-tracker->>OCR: 23L
    OCR->>pn-paper-tracker: 23L - OCR_OK
    pn-paper-tracker->>pn-delivery-push: FEEDBACK - RECAG012
    pn-paper-tracker->>OCR: ARCAD
    OCR->>pn-paper-tracker: ARCAD - OCR_OK
    pn-paper-tracker->>pn-delivery-push: PROGRESS - RECAG005C
```

## Consumer pn-ocr_outputs
I messaggi ricevuti vengono gestiti dalla classe
[`OcrEventHandler`](../../src/main/java/it/pagopa/pn/papertracker/middleware/queue/consumer/internal/OcrEventHandler.java),
che recupera l’oggetto `PaperTrackings` da DynamoDB tramite il `commandId` presente nel payload.
Successivamente, come per la coda precedente, il messaggio viene smistato al relativo handler in base al `productType`
e all’`EventTypeEnum.OCR_RESPONSE_EVENT`.

```mermaid
flowchart LR

    F[EVENTXXX] -->|OCR_RESPONSE_EVENT| OCR

    %% Ocr response handler
    subgraph OCR[buildOcrResponseHandler]
        I1[checkOcrResponse] -->  
        I2[finalEventBuilder] --> 
        I3[outputTargetSender]
    end
```
