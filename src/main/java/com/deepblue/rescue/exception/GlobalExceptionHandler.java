package com.deepblue.rescue.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.deepblue.rescue.dto.response.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleResourceNotFound(
			ResourceNotFoundException ex
	) {
		HttpStatus status = HttpStatus.NOT_FOUND;
		ErrorResponse error = new ErrorResponse(
				LocalDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				ex.getMessage(),
				Map.of()
		);
		return ResponseEntity.status(status).body(error);
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ErrorResponse> handleBusinessRule(
			BusinessRuleException ex
	) {
		HttpStatus status = HttpStatus.CONFLICT;
		ErrorResponse error = new ErrorResponse(
				LocalDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				ex.getMessage(),
				Map.of()
		);
		return ResponseEntity.status(status).body(error);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(
			MethodArgumentNotValidException ex
	) {
		Map<String, String> details = ex.getBindingResult()
				.getFieldErrors()
				.stream()
				.collect(Collectors.toMap(
						FieldError::getField,
						fieldError -> fieldError.getDefaultMessage() == null
								? "Invalid value"
								: fieldError.getDefaultMessage(),
						(first, second) -> first
				));
		HttpStatus status = HttpStatus.BAD_REQUEST;
		ErrorResponse error = new ErrorResponse(
				LocalDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				"Request validation failed",
				details
		);
		return ResponseEntity.status(status).body(error);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleMessageNotReadable(
			HttpMessageNotReadableException ex
	) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		ErrorResponse error = new ErrorResponse(
				LocalDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				"Malformed or invalid JSON request",
				Map.of("body", "Check JSON syntax and enum values")
		);
		return ResponseEntity.status(status).body(error);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(
			MethodArgumentTypeMismatchException ex
	) {
		HttpStatus status = HttpStatus.BAD_REQUEST;
		Map<String, String> details = Map.of(
				ex.getName(), "Invalid value: " + String.valueOf(ex.getValue())
		);
		ErrorResponse error = new ErrorResponse(
				LocalDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				"Invalid request parameter",
				details
		);
		return ResponseEntity.status(status).body(error);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(
			Exception ex
	) {
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		ErrorResponse error = new ErrorResponse(
				LocalDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				"An unexpected error occurred",
				Map.of()
		);
		return ResponseEntity.status(status).body(error);
	}
}
