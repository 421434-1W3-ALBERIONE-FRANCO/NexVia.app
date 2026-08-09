package com.nexvia.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = LongitudeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLongitude {
    String message() default "Longitud debe estar entre -180 y 180";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
