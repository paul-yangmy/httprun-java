package com.httprun.exception;

import com.httprun.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final String message;
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.errorCode = errorCode;
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage() + ": " + detail;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.errorCode = null;
        this.code = code;
        this.message = message;
    }

    public BusinessException(String message) {
        super(message);
        this.errorCode = null;
        this.code = 400;
        this.message = message;
    }
}
