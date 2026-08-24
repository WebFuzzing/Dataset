package jasper.util;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;

public class Crypto {

    public static KeyPair keyPair() throws NoSuchAlgorithmException {
		var kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(4096);
		return kpg.generateKeyPair();
    }

    public static String writeRsaPrivatePem(PrivateKey privateKey) throws IOException {
        var pkout = new StringWriter();
        try (var writer = new PemWriter(pkout)) {
            writer.writeObject(new PemObject(
                "PRIVATE KEY",
                privateKey.getEncoded()));
        }
        return pkout.toString();
    }

    public static String writeSshRsa(RSAPublicKey publicKey, String comment) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt("ssh-rsa".getBytes().length);
        dos.write("ssh-rsa".getBytes());
        dos.writeInt(publicKey.getPublicExponent().toByteArray().length);
        dos.write(publicKey.getPublicExponent().toByteArray());
        dos.writeInt(publicKey.getModulus().toByteArray().length);
        dos.write(publicKey.getModulus().toByteArray());
        return "ssh-rsa " + DatatypeConverter.printBase64Binary(os.toByteArray()) + " " + comment;
    }
}
