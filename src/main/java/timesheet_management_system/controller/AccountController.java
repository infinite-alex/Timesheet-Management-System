package timesheet_management_system.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import timesheet_management_system.dto.ChangePasswordDto;
import timesheet_management_system.service.AccountService;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordDto dto, Authentication authentication) {
        accountService.changePassword(authentication.getName(), dto.currentPassword(), dto.newPassword(), dto.confirmPassword());
        return ResponseEntity.noContent().build();
    }
}
