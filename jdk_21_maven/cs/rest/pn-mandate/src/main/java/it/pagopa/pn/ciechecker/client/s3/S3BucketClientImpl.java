package it.pagopa.pn.ciechecker.client.s3;


import it.pagopa.pn.ciechecker.exception.CieCheckerException;
import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import it.pagopa.pn.ciechecker.utils.LogsConstant;
import it.pagopa.pn.ciechecker.utils.ValidateUtils;
import it.pagopa.pn.mandate.config.PnMandateConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j
@Data
public class S3BucketClientImpl  implements S3BucketClient {

    private PnMandateConfig pnMandateConfig;
    private S3Client clientS3;

    private static String[] s3UriInfo;

    @Override
    public InputStream getObjectContent(String s3Uri) throws CieCheckerException {

        if(Objects.isNull(this.clientS3 )) {
            this.clientS3 = pnMandateConfig.s3Client();
        }

        log.info(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.S3BUCKETCLIENTIMPL_GET_OBJECT_CONTENT, "Call s3 bucket for read content object with s3Uri: {}", s3Uri);
        s3UriInfo = ValidateUtils.extractS3Components( s3Uri);

        if(Objects.isNull(s3UriInfo)) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.S3BUCKETCLIENTIMPL_GET_OBJECT_CONTENT, ResultCieChecker.KO_EXC_NOVALID_URI_CSCA_ANCHORS.getValue());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_NOVALID_URI_CSCA_ANCHORS);
        }
        try {
            return clientS3.getObject(GetObjectRequest.builder().bucket(s3UriInfo[0]).key(s3UriInfo[1]).build());

        } catch (Exception e){
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.S3BUCKETCLIENTIMPL_GET_OBJECT_CONTENT, e.getClass().getName()+" Message: " +e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_NOVALID_CONNECT_S3,e);
        }
    }

    @Override
    public void uploadContent(String s3Uri, InputStream file, long size, String checksum) throws CieCheckerException {

        if(Objects.isNull(this.clientS3 )) {
            this.clientS3 = pnMandateConfig.s3Client();
        }

        log.info(LogsConstant.INVOKING_OPERATION_LABEL, LogsConstant.S3BUCKETCLIENTIMPL_PUT_OBJECT_CONTENT, "Call s3 bucket for read content object with s3Uri: {}", s3Uri);
        s3UriInfo = ValidateUtils.extractS3Components( s3Uri);
        if(Objects.isNull(s3UriInfo)) {
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.S3BUCKETCLIENTIMPL_GET_OBJECT_CONTENT, ResultCieChecker.KO_EXC_NOVALID_URI_CSCA_ANCHORS.getValue());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_NOVALID_URI_CSCA_ANCHORS);
        }
        try {
            log.trace("Call s3 bucket for upload content object with bucket: {} key: {}", s3UriInfo[0], s3UriInfo[3] + "new_" + s3UriInfo[2]);
            clientS3.putObject(PutObjectRequest.builder().bucket(s3UriInfo[0]).key(s3UriInfo[3] + "new_" + s3UriInfo[2])
                    .contentMD5(checksum).build(), RequestBody.fromInputStream(file, size));
        }catch (Exception e ){
            log.error(LogsConstant.EXCEPTION_IN_PROCESS, LogsConstant.S3BUCKETCLIENTIMPL_PUT_OBJECT_CONTENT, e.getClass().getName()+" Message: " +e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO_EXC_NOVALID_CONNECT_S3,e);
        }
    }

}
