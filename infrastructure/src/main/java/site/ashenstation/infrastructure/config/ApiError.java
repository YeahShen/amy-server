package site.ashenstation.infrastructure.config;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class ApiError {

    private HttpStatus status = HttpStatus.BAD_REQUEST;
    private Long timestamp;
    private String message;

    private ApiError() {
        timestamp = System.currentTimeMillis();
    }

    public static ApiError error(String message) {
        ApiError apiError = new ApiError();
        apiError.setMessage(message);
        return apiError;
    }

    public static ApiError error(HttpStatus status, String message) {
        ApiError apiError = new ApiError();
        apiError.setStatus(status);
        apiError.setMessage(message);
        return apiError;
    }
}
