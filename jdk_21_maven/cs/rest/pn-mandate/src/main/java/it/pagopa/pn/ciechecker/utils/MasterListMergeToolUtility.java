package it.pagopa.pn.ciechecker.utils;

import it.pagopa.pn.ciechecker.client.s3.S3BucketClient;
import it.pagopa.pn.ciechecker.client.s3.S3BucketClientImpl;
import it.pagopa.pn.ciechecker.exception.CieCheckerException;
import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static it.pagopa.pn.ciechecker.utils.CieCheckerConstants.OK;
import static it.pagopa.pn.ciechecker.utils.CieCheckerConstants.PROTOCOLLO_S3;

@Slf4j
public class MasterListMergeToolUtility {

    private final String cscaPath;
    private final S3BucketClient s3BucketClient;
    private InputStream inputStreamCscaAnchor;
    private InputStream inputStreamCscaPem;

    private String[] s3UriInfoCscaZip ;
    private String[] s3UriInfoCertPem ;

    private static final String RESOURCES_DIR = "src/test/resources/";
    private static final String NEW_FILE_ZIP = "new_IT_MasterListCSCA.zip";

    public MasterListMergeToolUtility(S3BucketClient s3BucketClient, String cscaPath) throws CieCheckerException {
        this.s3BucketClient = s3BucketClient;
        if(Objects.isNull(cscaPath) || cscaPath.isBlank())
            throw new CieCheckerException(ResultCieChecker.KO_EXC_NO_CSCA_ANCHORS_PROVIDED);
        if(!cscaPath.endsWith("/"))
            cscaPath += "/";
        this.cscaPath = cscaPath;
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            log.error("Errore: Percorso CSCA non fornito come argomento.");
            System.exit(1);
        }

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("it.pagopa.pn.ciechecker.client.s3")) {
            S3BucketClient localS3BucketClient = context.getBean(S3BucketClientImpl.class);
            MasterListMergeToolUtility master = new MasterListMergeToolUtility(localS3BucketClient, args[0]);
            ResultCieChecker result = master.merge();
            if(result.getValue().equals(OK)) {
                log.info(LogsConstant.SUCCESSFUL_OPERATION_ON_LABEL, LogsConstant.MASTERLISTMERGETOOL_MERGE, "ResultCieChecker", result.getValue());
                System.exit(0);
            }else
                System.exit(1);
        } catch (CieCheckerException e) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.MASTERLISTMERGETOOL_MERGE, e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.MASTERLISTMERGETOOL_MERGE, e.getMessage());
            System.exit(1);
        }
    }

    public ResultCieChecker merge() throws CieCheckerException{

        log.info(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.MASTERLISTMERGETOOL_MERGE);
        s3UriInfoCscaZip = ValidateUtils.extractS3Components( this.cscaPath + "IT_MasterListCSCA.zip");
        s3UriInfoCertPem = ValidateUtils.extractS3Components( this.cscaPath + "catest.pem");

        if (this.cscaPath.startsWith(PROTOCOLLO_S3) ){

            inputStreamCscaAnchor = s3BucketClient.getObjectContent(this.cscaPath+ "IT_MasterListCSCA.zip");
            inputStreamCscaPem = s3BucketClient.getObjectContent(this.cscaPath + "catest.pem");
            if (Objects.isNull(inputStreamCscaAnchor) || Objects.isNull(inputStreamCscaPem) ) {
                log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.MASTERLISTMERGETOOL_MERGE, ResultCieChecker.KO_EXC_NOFOUND_FILEARGS.getValue());
                return ResultCieChecker.KO_EXC_NOFOUND_FILEARGS;
            }
            log.debug("inputStreamCscaAnchor: {}", inputStreamCscaAnchor);
            log.debug("inputStreamCscaPem: {}", inputStreamCscaPem);
        } else {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.MASTERLISTMERGETOOL_MERGE,ResultCieChecker.KO_EXC_NOVALID_URI_CSCA_ANCHORS.getValue());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_NOVALID_URI_CSCA_ANCHORS);
        }

        try {
            ResultCieChecker result = addFileToMasterListZip();
            // Output atteso:
            // tre file: il file "catest.pem" da aggiungere al nuovo zip, "new_<fileNameZip>.zip" (il nuovo ZIP) e "fileNameZip.zip" (il originale ZIP)
            if(result.getValue().equals(OK)) {
                log.debug("UPLOAD del file prodotto sul bucket S3...");
                //UPLOAD del file prodotto sul bucket S3
                writeNewMasterZip(  new FileInputStream(RESOURCES_DIR + NEW_FILE_ZIP));
            }
            return ResultCieChecker.OK;
        } catch (Exception e) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.MASTERLISTMERGETOOL_MERGE, e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO, e);
        }
    }

    private ResultCieChecker addFileToMasterListZip() throws CieCheckerException, IOException {

        log.info(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.MASTERLISTMERGETOOL_ADDFILETOMASTERZIP);
        log.debug("s3UriInfoCscaZip[2]: {}" , s3UriInfoCscaZip[2]);
        log.debug("s3UriInfoCertPem[2]: {}" , s3UriInfoCertPem[2]);

        Path targetPath = Path.of(RESOURCES_DIR);
        Path parentDir = targetPath.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }

        //creo un file zip temporaneo
        File tempFile = new File(RESOURCES_DIR + "new_" + s3UriInfoCscaZip[2]);
        log.debug("tempFilePath CscaZip: {}", tempFile.getAbsolutePath());

        boolean success = false;
        String s3UriInfoCertPem2 = s3UriInfoCertPem[2];
        log.debug("s3UriInfoCertPem2: {}", s3UriInfoCertPem2);
        try (ZipInputStream zis = new ZipInputStream(inputStreamCscaAnchor );
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile))){
            ZipEntry entry;
            //Copia il contenuto del file ZIP originale in quello temporaneo
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.getName().equals(s3UriInfoCertPem2)) {
                    log.debug("Copia il contenuto del file ZIP originale in quello temporaneo: {}", entry.getName());
                    zos.putNextEntry(new ZipEntry(entry.getName()));
                    zis.transferTo(zos);
                    zos.closeEntry();
                }
            }

            //Aggiungo il nuovo file allo ZIP temporaneo
            log.debug("Aggiungo il nuovo file allo ZIP temporaneo - s3UriInfoCertPem2: {} " , s3UriInfoCertPem2);
            try (FileInputStream fis = convertToFileInputStream(inputStreamCscaPem, s3UriInfoCertPem2)) {
                ZipEntry newEntry = new ZipEntry(s3UriInfoCertPem2);
                zos.putNextEntry(newEntry);
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    zos.write(buffer, 0, bytesRead ); // attenzione: l’ordine dei parametri deve essere corretto
                }
                zos.closeEntry();
                zos.flush();
            }

            success = true; //file temporaneo creato
        }
        // Assicurati che il file temporaneo venga gestito correttamente
        if (success) {
            log.info(LogsConstant.SUCCESSFUL_OPERATION_NO_RESULT_LABEL, "File '" + s3UriInfoCertPem[1] + "' aggiunto con successo a '" + s3UriInfoCscaZip[3] +"new_"+s3UriInfoCscaZip[2]+ "'");
            return ResultCieChecker.OK;
        } else {
            // Delete del file temporaneo in caso di eccezione
            Files.deleteIfExists(parentDir);
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.MASTERLISTMERGETOOL_ADDFILETOMASTERZIP, ResultCieChecker.KO_EXC_CREATION_FILEZIPTEMP.getValue());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_CREATION_FILEZIPTEMP);
        }
    }

    private static FileInputStream convertToFileInputStream(InputStream genericStream, String fileName) throws IOException {

        //Crea un file temporaneo
        File tempFile = Files.createTempFile(fileName, ".tmp").toFile();
        // Scrive il contenuto del flusso generico nel file temporaneo
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            genericStream.transferTo(fos); // Metodo efficiente per copiare tutti i byte
        }
        // Crea il nuovo FileInputStream dal file temporaneo salvato
        FileInputStream fileInputStream = new FileInputStream(tempFile);

        if(tempFile.exists())
            tempFile.deleteOnExit(); // Utile per la pulizia automatica all'uscita dalla JVM

        return fileInputStream;
    }


    public ResultCieChecker writeNewMasterZip( InputStream newFileZipZos) throws CieCheckerException {

        try {
            byte[] newZipFileBytes = newFileZipZos.readAllBytes();
            InputStream newZip = new ByteArrayInputStream(newZipFileBytes);
            s3BucketClient.uploadContent(cscaPath, newZip, newZipFileBytes.length, null);
            log.info(LogsConstant.SUCCESSFUL_OPERATION_NO_RESULT_LABEL, "New File Archive uploaded on S3Bucket");
            return ResultCieChecker.OK;
        }catch (Exception e ){
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.MASTERLISTMERGETOOL_UPLOADMASTERZIPTOS3, e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_UPLOAD_NEWFILEZIP_TO_S3, e);
        }
    }
}
