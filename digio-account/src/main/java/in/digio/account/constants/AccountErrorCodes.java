package in.digio.account.constants;

import in.digio.core.exception.GenericErrorConstant;
import in.digio.core.exception.GenericErrorConstantInterface;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum AccountErrorCodes implements GenericErrorConstantInterface {

    ACCOUNT_LIMIT_EXCEEDED(6001, "Accounts limit exceeded"),
    NO_ACCOUNT_FOUND(6002, "No account associated with user");


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
