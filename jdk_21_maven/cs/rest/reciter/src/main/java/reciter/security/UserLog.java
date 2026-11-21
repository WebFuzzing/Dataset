package reciter.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLog {
    private String clientId;
    private String clientName;
    private String apiEndPoint;
    private String personIdentifier;
    private String timestamp;
    private int apiRetrievalStatus;
}

