package com.mchub.dto;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApiResponse<T> {
  private String status;
  private String message;
  private T data;
  private boolean success;
  private String errorCode;

  public ApiResponse(String status, String message, T data) {
    this.status = Objects.requireNonNull(status);
    this.message = message;
    this.data = data;
    this.success = "success".equals(status);
  }

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>("success", null, data);
  }

  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>("success", message, data);
  }

  public static <T> ApiResponse<T> fail(String message) {
    return new ApiResponse<>("fail", Objects.requireNonNull(message), null);
  }
}
