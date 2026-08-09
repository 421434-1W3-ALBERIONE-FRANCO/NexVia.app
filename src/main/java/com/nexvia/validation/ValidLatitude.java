package com.nexvia.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = LatitudeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLatitude {
    String message() default "Latitud debe estar entre -90 y 90";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
