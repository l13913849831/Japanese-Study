package com.jp.vocab.user.controller;

import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.shared.api.PageResponse;
import com.jp.vocab.user.dto.AdminResetPasswordRequest;
import com.jp.vocab.user.dto.AdminUserDetailResponse;
import com.jp.vocab.user.dto.AdminUserListItemResponse;
import com.jp.vocab.user.dto.AdminUserPasswordResetResponse;
import com.jp.vocab.user.dto.AdminUserStatusResponse;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.dto.SecurityAlertResponse;
import com.jp.vocab.user.dto.SecurityAuditEventResponse;
import com.jp.vocab.user.service.AdminUserService;
import com.jp.vocab.user.service.SecurityAlertService;
import com.jp.vocab.user.service.SecurityAuditService;
import com.jp.vocab.user.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserProfileService userProfileService;
    private final AdminUserService adminUserService;
    private final SecurityAuditService securityAuditService;
    private final SecurityAlertService securityAlertService;

    public AdminController(
            UserProfileService userProfileService,
            AdminUserService adminUserService,
            SecurityAuditService securityAuditService,
            SecurityAlertService securityAlertService
    ) {
        this.userProfileService = userProfileService;
        this.adminUserService = adminUserService;
        this.securityAuditService = securityAuditService;
        this.securityAlertService = securityAlertService;
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> getAdminProfile() {
        return ApiResponse.success(userProfileService.getCurrentUserProfile());
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<AdminUserListItemResponse>> listUsers(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role
    ) {
        return ApiResponse.success(adminUserService.listUsers(page, pageSize, keyword, status, role));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<AdminUserDetailResponse> getUserDetail(
            @PathVariable Long userId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(adminUserService.getUserDetail(userId, request));
    }

    @PostMapping("/users/{userId}/disable")
    public ApiResponse<AdminUserStatusResponse> disableUser(
            @PathVariable Long userId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(adminUserService.disableUser(userId, request));
    }

    @PostMapping("/users/{userId}/enable")
    public ApiResponse<AdminUserStatusResponse> enableUser(
            @PathVariable Long userId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(adminUserService.enableUser(userId, request));
    }

    @PostMapping("/users/{userId}/reset-password")
    public ApiResponse<AdminUserPasswordResetResponse> resetUserPassword(
            @PathVariable Long userId,
            @Valid @RequestBody AdminResetPasswordRequest resetPasswordRequest,
            HttpServletRequest request
    ) {
        return ApiResponse.success(adminUserService.resetUserPassword(userId, resetPasswordRequest, request));
    }

    @GetMapping("/audit-events")
    public ApiResponse<PageResponse<SecurityAuditEventResponse>> listAuditEvents(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String username
    ) {
        return ApiResponse.success(securityAuditService.listEvents(page, pageSize, eventType, outcome, username));
    }

    @GetMapping("/security-alerts")
    public ApiResponse<List<SecurityAlertResponse>> listSecurityAlerts(
            @RequestParam(required = false) @Min(1) @Max(168) Integer lookbackHours,
            @RequestParam(required = false) @Min(1) @Max(100) Integer limit
    ) {
        return ApiResponse.success(securityAlertService.listAlerts(lookbackHours, limit));
    }
}
