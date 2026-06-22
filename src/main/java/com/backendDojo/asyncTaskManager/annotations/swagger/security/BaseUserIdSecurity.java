package com.backendDojo.asyncTaskManager.annotations.swagger.security;


import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.backendDojo.asyncTaskManager.configs.security.UserAuthHeaderFilter.AUTH_HEADER_NAME;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

@SecuritySchemes({
        @SecurityScheme(
                name = AUTH_HEADER_NAME,
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER,
                paramName = AUTH_HEADER_NAME
        )
})
@Target({METHOD, TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BaseUserIdSecurity {
}
