package com.sh.cicd.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping
    public ResponseEntity<ApiResponse> test() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("REQUEST SUCCESS."));
    }
}
