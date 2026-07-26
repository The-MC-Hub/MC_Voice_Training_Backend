package com.mchub.util;

import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * Generic entity-retrieval helpers — eliminate orElseThrow boilerplate.
 */
public final class EntityUtils {

    private EntityUtils() {}

    /**
     * Fetch entity by ID or throw AppException with the given error code and message.
     */
    public static <T, ID> T getOrThrow(CrudRepository<T, ID> repository, ID id, ErrorCode errorCode, String message) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException(errorCode, message));
    }

    /**
     * Unwrap an Optional or throw AppException with the given error code and message.
     */
    public static <T> T getOrThrow(Optional<T> optional, ErrorCode errorCode, String message) {
        return optional.orElseThrow(() -> new AppException(errorCode, message));
    }

    /**
     * Fetch entity by ID or throw USER_NOT_FOUND.
     * Shorthand for the most common lookup: userRepository.findById(id).
     */
    public static <T, ID> T getUserOrThrow(CrudRepository<T, ID> repository, ID id) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + id));
    }

    /**
     * Fetch entity by ID or throw RESOURCE_NOT_FOUND.
     * Shorthand for generic resource lookups.
     */
    public static <T, ID> T getResourceOrThrow(CrudRepository<T, ID> repository, ID id, String resourceName) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, resourceName + " not found: " + id));
    }

    /**
     * Verify the current user owns the resource (or is admin). Throws ACCESS_DENIED if not.
     */
    public static void verifyOwnership(String ownerId, String currentUserId) {
        if (!SecurityUtils.isAdmin() && (ownerId == null || !ownerId.equals(currentUserId))) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "Access denied");
        }
    }
}
