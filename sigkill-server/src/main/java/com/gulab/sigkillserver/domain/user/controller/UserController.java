package com.gulab.sigkillserver.domain.user.controller;


import com.gulab.sigkillserver.common.BaseResponse;
import com.gulab.sigkillserver.domain.user.dto.rest.response.LoginResponse;
import com.gulab.sigkillserver.domain.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 비회원 로그인
     */
    @PostMapping("/users/guest-login")
    public BaseResponse<LoginResponse> loginAsGuest(HttpSession session) {
        LoginResponse loginResponse = userService.loginAsGuest(session);
        return BaseResponse.onSuccess(loginResponse);
    }
}
