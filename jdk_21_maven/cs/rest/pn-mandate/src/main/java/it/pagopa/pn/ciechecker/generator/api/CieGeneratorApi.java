package it.pagopa.pn.ciechecker.generator.api;

import it.pagopa.pn.ciechecker.model.CieValidationData;

import java.nio.file.Path;
import java.time.LocalDate;

public interface CieGeneratorApi {
    public CieValidationData generateCieValidationData(Path outputDir,
                                                       String codiceFiscaleDelegante,
                                                       String codiceFiscaleCIE,
                                                       LocalDate expirationDate,
                                                       String nonce);
}
