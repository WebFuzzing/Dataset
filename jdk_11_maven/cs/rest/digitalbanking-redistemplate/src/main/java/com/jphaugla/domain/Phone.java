package com.jphaugla.domain;
import lombok.*;

import java.io.Serializable;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class Phone implements Serializable {

        private String phoneNumber;
        private String phoneLabel;
        private String customerId;
}
