package it.pagopa.pn.ciechecker.model;

import lombok.Data;

@Data
public class CieIas {
    private byte[] sod;
    private byte[] nis;
    private byte[] publicKey;
}