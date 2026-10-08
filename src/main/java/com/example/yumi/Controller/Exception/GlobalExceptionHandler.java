package com.example.yumi.Controller.Exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.yumi.Enum.ResultCode;
import com.example.yumi.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	// 輸入資料有誤：@Valid 驗證失敗、header 驗證失敗、JSON 格式錯誤、型別錯誤（例如 Idempotency-Key 不是 UUID）
	@ExceptionHandler({ MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
			HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception e) {
		return build(ResultCode.INVALID_REQUEST);
	}

	// 沒帶必要的 header
	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<ErrorResponse> handleMissingHeader(Exception e) {
		return build(ResultCode.MISSING_HEADER);
	}

	// 網址打錯
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(Exception e) {
		return build(ResultCode.API_NOT_FOUND);
	}

	// HTTP 方法錯誤，例如用 GET 打 POST 的 API
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotAllowed(Exception e) {
		return build(ResultCode.METHOD_NOT_ALLOWED);
	}

	// 其他沒預料到的錯誤：記 log，回傳 500
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		log.error("未預期的錯誤", e);
		return build(ResultCode.INTERNAL_ERROR);
	}

	private ResponseEntity<ErrorResponse> build(ResultCode resultCode) {
		return ResponseEntity.status(resultCode.getHttpStatus())
				.body(new ErrorResponse(resultCode.name(), resultCode.getMessage()));
	}

}
