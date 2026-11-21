package reciter.consumer.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResponse<T> {
    private String data;
    private String error;
    private int status;

    public ApiResponse() {}

    public ApiResponse(String data, String error, int status) {
        this.data = data;
        this.error = error;
        this.status = status;
    }

   
}