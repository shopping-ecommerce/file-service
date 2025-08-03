package iuh.fit.fe.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Getter
@NoArgsConstructor
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(1001, "Uncategorized exception", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_EXITS(1002, "User already exists", HttpStatus.BAD_REQUEST),
    NAME_INVALID(1003, "Name must not contain numbers or special characters", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1004, "Email must be in the correct format", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1005, "User not found", HttpStatus.NOT_FOUND),
    PASSWORD_INCORRECT(1006, "Password is incorrect", HttpStatus.UNAUTHORIZED),
    PASSWORD_INVALID(
            1007,
            "Password must contain at least one uppercase letter,"
                    + " one special character, and be at least 8 characters long"
                    + "Ex: Thinh@123",
            HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1008, "Email or password is incorrect", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1009, "You are not authorized to perform this action", HttpStatus.BAD_REQUEST),
    ;
    int code;
    String message;
    private HttpStatusCode httpStatusCode;
}
