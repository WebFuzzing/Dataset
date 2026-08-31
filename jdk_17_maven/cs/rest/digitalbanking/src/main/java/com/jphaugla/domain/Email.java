package com.jphaugla.domain;
import lombok.*;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class Email implements Serializable {
        private String emailAddress;
        private String emailLabel;
        private String customerId;
}
