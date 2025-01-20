package com.sh.cicd.api;

import lombok.Getter;

@Getter
public class ApiResponse {

    private final String message;

    ApiResponse(String message) {
        this.message = message;
    }

    public static ApiResponse success(String message) {
        return new ApiResponse(message);
    }
}
