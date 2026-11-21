package it.pagopa.pn.ciechecker.generator.files;

import it.pagopa.pn.ciechecker.exception.CieCheckerException;
import it.pagopa.pn.ciechecker.generator.model.CieCaAndKey;
import it.pagopa.pn.ciechecker.model.*;
import it.pagopa.pn.ciechecker.utils.LogsConstant;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.ciechecker.generator.constants.CieGeneratorConstants.CTX_DOCUMENT_SIGNER_INFO;
import static it.pagopa.pn.ciechecker.generator.constants.CieGeneratorConstants.CTX_VALIDATION_DATA;
import static it.pagopa.pn.ciechecker.utils.ValidateUtils.*;


@Slf4j
public class CieFilesExporter {

    private final Map<String, Object> sourceObjects;
    private final String outputBaseDir; // Directory base per l'output

    public CieFilesExporter(CieValidationData validationData, CieCaAndKey cieCaAndKey, String baseDir) throws CieCheckerException{

        if(!validateCieDataInput(validationData, cieCaAndKey, baseDir))
            throw new CieCheckerException(ResultCieChecker.KO_EXC_INPUT_PARAMETER_NULL);

        this.sourceObjects = new HashMap<>();
        this.sourceObjects.put(CTX_VALIDATION_DATA, validationData);
        this.sourceObjects.put(CTX_DOCUMENT_SIGNER_INFO, cieCaAndKey);

        this.outputBaseDir = baseDir;
    }


    /**
     * Itera su tutti gli attributi definiti nell'Enum e genera un file con il percorso definito.
     * @return Una mappa contenente il percorso del file generato e la dimensione in byte.
     * @throws CieCheckerException Se la Reflection fallisce o si verifica un errore I/O.
     */
    public Map<String, Long> exportCieArtifactsToFiles() throws CieCheckerException {

        log.info(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.CIEFILEGENERATOR_EXPORTFILES);
        Map<String, Long> exportResults = new HashMap<>();
        try {
            for (CieFileAttribute attr : CieFileAttribute.values()) {

                log.debug("AttributePath: {} - RelativeFilePath: {}", attr.getAttributePath() ,attr.getRelativeFilePath());
                // Determina il percorso completo
                String fullPath = outputBaseDir + File.separator + attr.getRelativeFilePath();
                if (attr.getAttributePath().indexOf("cieCaAndKey") == 0 && Files.exists(Path.of(fullPath))) {
                    continue;
                }
                //Cancello il file esistente prima di rigenerarlo
                deleteIfExists(fullPath);

                // Determina il byte[] tramite reflection
                byte[] data = getBytesByPath(attr.getAttributePath());

                if (data != null && data.length > 0) {
                    // Esporta il file
                    File exportedFile = exportBytesToFile(data, fullPath);
                    exportResults.put(fullPath, exportedFile.length());
                    log.debug("File esportato: {} ({} bytes)", exportedFile.getName(), exportedFile.length());
                } else {
                    log.error("Attributo {} è vuoto o nullo", attr.name());
                    throw new CieCheckerException(ResultCieChecker.KO_EXC_ATTRIBUTO_NULL);
                }
            }
            return exportResults;
        }catch (Exception e ){
            log.error(LogsConstant.CIEFILEGENERATOR_EXPORTFILES, false, e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO, e);
        }
    }

    private boolean deleteIfExists(String fileNamePath) throws CieCheckerException {
        try {
            Path fileToDelete = Paths.get(fileNamePath);
            return Files.deleteIfExists(fileToDelete);
        } catch (IOException e) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.CIEFILEGENERATOR_EXPORTFILES, ResultCieChecker.KO_EXC_DELETEEXISTFILE.getValue());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_DELETEEXISTFILE, e);
        }
    }

    /**
     * Utilizzo la Reflection per recuperare un byte[] basato su un percorso attributo (es. "cieIas.sod").
     */
    private byte[] getBytesByPath(String path) throws CieCheckerException {
        try{
            String[] parts = path.split("\\.", 3);
            if (parts.length < 2) {
                throw new IllegalArgumentException("Formato percorso attributo non valido. Serve almeno [oggetto].[campo] o [oggetto].[sotto-oggetto].[campo]: " + path);
            }

            String objectName = parts[0];
            Object currentObject = sourceObjects.get(objectName);
            if (currentObject == null) {
                throw new IllegalArgumentException("Oggetto non trovato: " + objectName);
            }
            Field targetField;
            String fieldPath = parts[1]; // Il percorso rimanente (es. "certPem" o "cieIas.sod")
            if (parts.length == 2) {
                // CASO 1: Campo di Primo Livello (es. cieCaAndKey.certPem o validationData.signedNonce)
                // fieldPath = "certPem" o "signedNonce"
                targetField = currentObject.getClass().getDeclaredField(fieldPath);
            } else { // parts.length == 3
                // CASO 2: Campo Annidato (es. validationData.cieIas.sod)
                // parts[1] è l'oggetto intermedio (es. "cieIas"), parts[2] è il campo finale (es. "sod")

                // Leggo il campo intermedio (es. cieIas)
                Field intermediateField = currentObject.getClass().getDeclaredField(parts[1]);
                intermediateField.setAccessible(true);
                currentObject = intermediateField.get(currentObject);
                if (currentObject == null) {
                    throw new NullPointerException("Oggetto nullo: " + objectName);
                }

                // Leggo il campo finale (es. sod)
                targetField = currentObject.getClass().getDeclaredField(parts[2]);
            }
            if (targetField == null) {
                throw new NoSuchFieldException("Impossibile trovare il campo finale per il percorso: " + path);
            }
            targetField.setAccessible(true);
            // Verifico che sia un byte[] e recupero il valore
            if (targetField.getType().isArray() && targetField.getType().getComponentType() == byte.class) {
                return (byte[]) targetField.get(currentObject);
            } else {
                throw new IllegalStateException("Attributo non è di tipo byte[]: " + path);
            }
        }catch (Exception e ){
            log.error(Exception.class + LogsConstant.MESSAGE  + e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO, e);
        }
    }

    private static File exportBytesToFile(byte[] binaryData, String destinationPath) throws IOException {
        Path targetPath = Path.of(destinationPath);
        Path parentDir = targetPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        String base64String = Base64.getEncoder().encodeToString(binaryData);
        Files.writeString(
              targetPath,
              base64String,
              StandardCharsets.UTF_8
        );
        return targetPath.toFile();
    }

}
