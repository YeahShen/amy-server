package site.ashenstation.amyserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.ashenstation.amyserver.annotation.rest.AnonymousPostMapping;
import site.ashenstation.amyserver.dto.AuthByUsernamePasswordDto;
import site.ashenstation.amyserver.entity.User;
import site.ashenstation.amyserver.service.AuthService;
import site.ashenstation.amyserver.vo.AuthResVo;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @AnonymousPostMapping("login-by-username-password")
    public ResponseEntity<AuthResVo> loginByUsernamePassword(@RequestBody @Valid AuthByUsernamePasswordDto dto, HttpServletRequest request) {
        return ResponseEntity.ok(authService.loginByUsernamePassword(dto, request));
    }

    @GetMapping("/info")
    public ResponseEntity<User> getUserInfo() {
        return ResponseEntity.ok(authService.getInfo());
    }

    @DeleteMapping("/logout")
    public ResponseEntity<Boolean> logout(HttpServletRequest request) {
        return ResponseEntity.ok(authService.logout(request));
    }

}
