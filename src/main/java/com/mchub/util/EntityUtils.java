package com.mchub.util;

import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public final class EntityUtils {

    private EntityUtils() {
        // Private constructor for utility class
    }

    public static <T, ID> T getOrThrow(CrudRepository<T, ID> repository, ID id, ErrorCode errorCode, String message) {
        return repository.findById(id)
                .orElseThrow(() -> new AppException(errorCode, message));
    }

    public static <T> T getOrThrow(Optional<T> optional, ErrorCode errorCode, String message) {
        return optional.orElseThrow(() -> new AppException(errorCode, message));
    }
}
