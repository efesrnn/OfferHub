package com.offerhub.gamification.controller;

import com.offerhub.gamification.dto.ApiResponse;
import com.offerhub.gamification.dto.BadgeResponse;
import com.offerhub.gamification.dto.LeaderboardResponse;
import com.offerhub.gamification.dto.ProfileResponse;
import com.offerhub.gamification.security.CallerIdentity;
import com.offerhub.gamification.security.Role;
import com.offerhub.gamification.service.Period;
import com.offerhub.gamification.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
public class GameController {

    private final ProfileService profileService;

    /** Always the caller's own profile - no id in the path, so there is nothing to tamper with. */
    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile(CallerIdentity caller) {
        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR);
        return ApiResponse.ok(profileService.profileOf(caller.userId()));
    }

    @GetMapping("/leaderboard")
    public ApiResponse<LeaderboardResponse> leaderboard(
            @RequestParam(defaultValue = "daily") String period,
            CallerIdentity caller) {

        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR);
        return ApiResponse.ok(profileService.leaderboard(Period.fromParam(period)));
    }

    /**
     * Redis is derived from point_entries, so it can always be rebuilt. Kept behind
     * supervisor and admin: it is a repair action, not something a client calls routinely.
     */
    @PostMapping("/leaderboard/rebuild")
    public ApiResponse<Integer> rebuildLeaderboard(CallerIdentity caller) {
        caller.requireAnyOf(Role.SUPERVISOR, Role.ADMIN);
        return ApiResponse.ok(profileService.rebuildLeaderboard());
    }

    @GetMapping("/badges")
    public ApiResponse<List<BadgeResponse>> badges(CallerIdentity caller) {
        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR);
        return ApiResponse.ok(profileService.badgesOf(caller.userId()));
    }
}
