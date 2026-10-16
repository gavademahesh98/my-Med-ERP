package com.edublitz.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserServiceApplicationTest {

    @Test
    void applicationClassHasSpringBootConfiguration() {
        SpringBootApplication annotation =
                UserServiceApplication.class.getAnnotation(SpringBootApplication.class);

        assertNotNull(annotation, "Application class should be annotated with @SpringBootApplication");
    }
}
