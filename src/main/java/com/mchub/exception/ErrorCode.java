package com.mchub.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "ERR_1001", "Invalid email or password"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "ERR_1002", "Invalid or expired token"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ERR_1003", "You do not have permission to perform this action"),
    USER_NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "ERR_1004", "You need to log in to continue"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "ERR_1005", "This email is already registered"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "ERR_1006", "Your account is disabled"),
    USER_LOCKED(HttpStatus.FORBIDDEN, "ERR_1007", "Your account is locked"),

    
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_2001", "User not found"),
    MC_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_2002", "MC profile not found"),
    MC_PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "ERR_2003", "MC profile already exists for this account"),
    INVALID_PROFILE_DATA(HttpStatus.BAD_REQUEST, "ERR_2004", "Invalid profile data"),
    PROFILE_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_2005", "Failed to update profile"),

    
    SERVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_3001", "Service not found"),
    SERVICE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ERR_3002", "You do not have permission to access this service"),
    MC_NOT_AVAILABLE(HttpStatus.CONFLICT, "ERR_3003", "The MC is busy during this time"),
    SERVICE_ALREADY_DECIDED(HttpStatus.CONFLICT, "ERR_3004", "This request has already been processed"),
    INVALID_SERVICE_DATA(HttpStatus.BAD_REQUEST, "ERR_3006", "Invalid service data"),
    SERVICE_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_3007", "Service details not found"),

    
    PAYMENT_INIT_FAILED(HttpStatus.BAD_GATEWAY, "ERR_4001", "Payment initialization failed, please try again"),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_4002", "Payment not found"),
    PAYMENT_ALREADY_PAID(HttpStatus.CONFLICT, "ERR_4003", "This transaction is already paid"),
    PAYOUT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_4004", "Payout request failed"),
    LIMIT_EXCEEDED(HttpStatus.PAYMENT_REQUIRED, "ERR_4005", "Practice session limit exceeded. Premium upgrade required."),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_4006", "Transaction not found"),
    WEBHOOK_INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "ERR_4007", "Webhook signature verification failed"),

    
    
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_7001", "Course not found"),
    COURSE_SLUG_EXISTS(HttpStatus.CONFLICT, "ERR_7002", "Course slug already exists"),
    COURSE_ALREADY_ENROLLED(HttpStatus.CONFLICT, "ERR_7003", "You are already enrolled in this course"),
    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_7004", "Enrollment not found"),
    QUIZ_ANSWER_MISMATCH(HttpStatus.BAD_REQUEST, "ERR_7005", "Number of answers does not match number of questions"),
    CERTIFICATE_ALREADY_ISSUED(HttpStatus.CONFLICT, "ERR_7006", "Certificate already issued for this course"),

    CERTIFICATE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_5003", "Certificate not found"),
    CERTIFICATE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ERR_5004", "You do not have permission to manipulate this certificate"),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_5005", "Review not found"),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "ERR_5006", "You have already reviewed this MC"),
    REVIEW_NOT_ALLOWED(HttpStatus.FORBIDDEN, "ERR_5007", "You can only review after completion"),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_5008", "Report not found"),

    
    
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_6001", "Conversation not found"),
    CONVERSATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ERR_6002", "You do not have permission to access this conversation"),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_6003", "Notification not found"),
    FAVORITE_ALREADY_EXISTS(HttpStatus.CONFLICT, "ERR_6004", "This MC is already in your favorites"),
    FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_6005", "Favorite not found"),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_6006", "Coupon does not exist or has expired"),
    COUPON_ALREADY_USED(HttpStatus.CONFLICT, "ERR_6007", "Coupon has already been used"),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "ERR_6008", "Coupon has expired"),
    AVAILABILITY_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_6009", "Availability not found"),
    AVAILABILITY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ERR_6010", "You do not have permission to delete this availability schedule"),

    
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_9001", "System error, please try again later"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "ERR_9002", "Invalid input data"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_9003", "Resource not found"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "ERR_9004", "HTTP method not supported");

    

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    @Override
    public String toString() {
        return "[" + code + "] " + defaultMessage + " (HTTP " + httpStatus.value() + ")";
    }
}
