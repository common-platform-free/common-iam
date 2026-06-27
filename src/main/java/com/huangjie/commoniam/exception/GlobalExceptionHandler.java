package com.huangjie.commoniam.exception;

import com.huangjie.commoniam.common.ApiResult;
import com.huangjie.commoniam.common.ErrorCode;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p>把参数校验异常、业务异常和未捕获异常统一转换成 ApiResult，
 * 避免 Controller 中重复 try/catch。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常，并尽量使用错误码对应的 HTTP 状态码。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getErrorCode().getCode());
        return ResponseEntity
                .status(status == null ? HttpStatus.BAD_REQUEST : status)
                .body(ApiResult.error(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * 处理 @Valid 参数校验失败。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResult.error(ErrorCode.BAD_REQUEST, message));
    }

    /**
     * 兜底异常处理，避免异常堆栈直接暴露给调用方。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(ErrorCode.INTERNAL_ERROR, ex.getMessage()));
    }

    /**
     * 提取字段校验错误中的用户可读提示。
     */
    private String formatFieldError(FieldError fieldError) {
        return fieldError.getDefaultMessage() == null ? fieldError.getField() + " 参数错误" : fieldError.getDefaultMessage();
    }
}
