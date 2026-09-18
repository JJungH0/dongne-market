package com.dongne.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/** 컨트롤러 파라미터에 붙이면 JWT subject(회원 ID)가 Long 으로 주입된다. 비로그인(anonymous)이면 null. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "#this instanceof T(org.springframework.security.oauth2.jwt.Jwt) ? T(java.lang.Long).valueOf(#this.subject) : null")
public @interface CurrentMember {
}
