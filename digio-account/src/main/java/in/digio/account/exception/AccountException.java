package in.digio.account.exception;

import in.digio.core.dto.ErrorResponse;
import in.digio.core.dto.GenericErrorResponse;
import in.digio.core.exception.GenericErrorConstant;
import in.digio.core.exception.GenericErrorConstantInterface;
import org.springframework.lang.NonNull;

public class AccountException extends RuntimeException {

    @NonNull
    private final transient GenericErrorConstant errorConstant;

    public AccountException(@NonNull GenericErrorConstantInterface errorConstant) {
        super(errorConstant.getErrorConstant().getMessage());
        this.errorConstant = errorConstant.getErrorConstant();
    }

    public ErrorResponse getErrorResponse() {
        return ErrorResponse.builder().error(GenericErrorResponse.builder()
                        .message(errorConstant.getMessage())
                        .code(errorConstant.getErrorCode())
                        .build())
                .build();
    }
}
