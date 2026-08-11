package com.jphaugla.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class TransactionReturn implements Serializable {

    private String reasonCode;
    private String reasonDescription;
}
