package it.pagopa.pn.ciechecker.generator.challenge;

import it.pagopa.pn.ciechecker.exception.CieCheckerException;
import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import it.pagopa.pn.ciechecker.utils.LogsConstant;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;

import java.nio.charset.StandardCharsets;
import java.security.*;

@Slf4j
public class ChallengeResponseBuilder {

    private ChallengeResponseBuilder(){}

    public static byte[] signNonce(String nonce, PrivateKey privateKey) throws CieCheckerException {
        try {
            byte[] nonceBytes = nonce.getBytes(StandardCharsets.UTF_8);

            AsymmetricKeyParameter privParam = PrivateKeyFactory.createKey(privateKey.getEncoded());

            PKCS1Encoding signer = new PKCS1Encoding(new RSAEngine());
            signer.init(true, privParam);

            return signer.processBlock(nonceBytes, 0, nonceBytes.length);
        }catch (Exception e ){
            log.error(Exception.class + LogsConstant.MESSAGE  + e.getMessage());
            throw new CieCheckerException(ResultCieChecker.KO, e);
        }
    }

}
