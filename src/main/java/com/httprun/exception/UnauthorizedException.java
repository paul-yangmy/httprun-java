package com.httprun.exception;

import com.httprun.enums.ErrorCode;
import lombok.Getter;

/**
 * 未授权异常
 */
@Getter
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }

    public UnauthorizedException(String detail) {
        super(ErrorCode.UNAUTHORIZED, detail);
    }
}
