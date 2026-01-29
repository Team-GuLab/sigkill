package com.gulab.sigkillserver.domain.user.controller;


import com.gulab.sigkillserver.common.BaseResponse;
import com.gulab.sigkillserver.domain.user.dto.response.LoginResponse;
import com.gulab.sigkillserver.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 비회원 로그인
     */
    @PostMapping("/guest-login")
    public BaseResponse<LoginResponse> loginAsGuest() {
        log.info("POST /api/v1/users/guests - 비회원 로그인");
        LoginResponse loginResponse = userService.loginAsGuest();
        return BaseResponse.onSuccess(loginResponse);
    }
}
