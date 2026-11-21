package it.pagopa.pn.ciechecker.generator.model;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public record Issuer(X509Certificate certificate, PrivateKey privateKey) {}

