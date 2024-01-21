package in.digio.acls.exception;


import in.digio.core.dto.ErrorResponse;
import in.digio.core.dto.GenericErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static in.digio.acls.exception.AclErrorCodes.ACCESS_DENIED;

@ControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AclExceptionHandler {

    public static final String EXCEPTION = "Exception";

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    ErrorResponse processAclErrors(AccessDeniedException ex) {
        log.error(EXCEPTION, ex);
        return ErrorResponse.builder().error(GenericErrorResponse.builder()
                .code(ACCESS_DENIED.getErrorConstant().getErrorCode())
                .message(ACCESS_DENIED.getErrorConstant().getMessage()).build()).build();
    }

}
