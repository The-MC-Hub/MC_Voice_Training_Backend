package com.mchub.enums;

public enum AuditAction {
    
        AUTH_LOGIN,
        AUTH_LOGOUT,
        AUTH_REGISTER,
        AUTH_CHANGE_PASSWORD,
        AUTH_RESET_PASSWORD,

    
        BOOKING_CREATE,
        BOOKING_ACCEPT,
        BOOKING_REJECT,
        BOOKING_CANCEL,
        BOOKING_COMPLETE,

    
        PAYMENT_INITIATE,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        PAYMENT_REFUND,

    
        PROFILE_UPDATE,
        PROFILE_AVATAR_UPDATE,
        MC_PROFILE_UPDATE,

    
        ADMIN_BAN_USER,
        ADMIN_UNBAN_USER,
        ADMIN_RESOLVE_REPORT,
        ADMIN_VERIFY_CERTIFICATE,
        ADMIN_CREATE_COUPON,

    
        SYSTEM_AUTO_ACTION
}
