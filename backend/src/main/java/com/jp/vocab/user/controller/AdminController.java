package com.jp.vocab.user.controller;

import com.jp.vocab.shared.api.ApiResponse;
import com.jp.vocab.user.dto.CurrentUserResponse;
import com.jp.vocab.user.service.UserProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserProfileService userProfileService;

    public AdminController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> getAdminProfile() {
        return ApiResponse.success(userProfileService.getCurrentUserProfile());
    }
}
