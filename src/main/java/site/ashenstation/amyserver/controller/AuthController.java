package site.ashenstation.amyserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.ashenstation.amyserver.annotation.rest.AnonymousPostMapping;
import site.ashenstation.amyserver.dto.AuthByUsernamePasswordDto;
import site.ashenstation.amyserver.service.AuthService;
import site.ashenstation.amyserver.vo.AuthResVo;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @AnonymousPostMapping("login-by-username-password")
    private ResponseEntity<AuthResVo> loginByUsernamePassword(@RequestBody @Valid AuthByUsernamePasswordDto dto, HttpServletRequest request) {
        return ResponseEntity.ok(authService.loginByUsernamePassword(dto, request));
    }
}
