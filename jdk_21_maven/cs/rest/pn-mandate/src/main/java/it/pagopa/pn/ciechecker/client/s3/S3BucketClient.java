package it.pagopa.pn.ciechecker.client.s3;


import it.pagopa.pn.ciechecker.exception.CieCheckerException;

import java.io.InputStream;

public interface S3BucketClient {

  InputStream getObjectContent(String key);

  void uploadContent(String key, InputStream file, long size, String checksum) throws CieCheckerException;

}
