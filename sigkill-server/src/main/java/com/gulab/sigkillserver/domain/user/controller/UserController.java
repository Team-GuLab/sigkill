package com.gulab.sigkillserver.domain.user.controller;


import com.gulab.sigkillserver.common.BaseResponse;
import com.gulab.sigkillserver.domain.user.dto.rest.response.LoginResponse;
import com.gulab.sigkillserver.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "User API", description = "게스트 로그인 API")
public class UserController {

    private final UserService userService;

    /**
     * 비회원 로그인
     */
    @Operation(
            summary = "게스트 로그인",
            description = "현재 세션 기준으로 게스트 사용자를 조회하거나 생성하고 로그인 상태를 설정합니다."
    )
    @PostMapping("/users/guest-login")
    public BaseResponse<LoginResponse> loginAsGuest(@Parameter(hidden = true) HttpSession session) {
        LoginResponse loginResponse = userService.loginAsGuest(session);
        return BaseResponse.onSuccess(loginResponse);
    }
}
