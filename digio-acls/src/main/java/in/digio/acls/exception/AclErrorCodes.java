package in.digio.acls.exception;

import in.digio.core.exception.GenericErrorConstant;
import in.digio.core.exception.GenericErrorConstantInterface;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AclErrorCodes implements GenericErrorConstantInterface {

    /**
     * All 5XX Errors Start from here
     */
    ACCESS_DENIED(6001, "Access Denied"),

    SYSTEM_ERROR(6002, "System Error has occured");


    private final int errorCode;
    private final String message;

    @Override
    public GenericErrorConstant getErrorConstant() {
        return GenericErrorConstant.builder()
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}
