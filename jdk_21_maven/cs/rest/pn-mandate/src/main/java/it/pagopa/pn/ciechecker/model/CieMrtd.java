package it.pagopa.pn.ciechecker.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Data
@Getter
@Setter
public class CieMrtd {
    private byte[] sod;
    @ToString.Exclude
    private byte[] dg1;
    @ToString.Exclude
    private byte[] dg11;
}