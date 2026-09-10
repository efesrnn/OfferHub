package com.offerhub.gamification.controller;

import com.offerhub.gamification.dto.ApiResponse;
import com.offerhub.gamification.dto.BadgeResponse;
import com.offerhub.gamification.dto.LeaderboardResponse;
import com.offerhub.gamification.dto.ProfileResponse;
import com.offerhub.gamification.security.CallerIdentity;
import com.offerhub.gamification.security.Role;
import com.offerhub.gamification.service.Period;
import com.offerhub.gamification.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The documented responses spell out io.swagger...ApiResponse in full: this project already
 * has an ApiResponse of its own, the response envelope imported above, so the short name is
 * taken.
 */
@RestController
@Tag(name = "Gamification", description = "Points, badges, levels and the leaderboard")
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
public class GameController {

    private final ProfileService profileService;

    /** Always the caller's own profile - no id in the path, so there is nothing to tamper with. */
    @Operation(summary = "The caller's own profile",
            description = """
                    Total points, level, earned badges, daily and weekly rank, cases resolved and
                    the average points per case.

                    Levels follow section 7.3: BRONZ from 0, GUMUS from 500, ALTIN from 1500,
                    PLATIN from 3000.

                    There is no id in the path on purpose. The profile answered is always the one
                    in the X-User-Id header, so there is no other expert's record to ask for.

                    Roles: EXPERT, SUPERVISOR.""")
    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> profile(CallerIdentity caller) {
        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR);
        return ApiResponse.ok(profileService.profileOf(caller.userId()));
    }

    @Operation(summary = "Leaderboard, top ten by points",
            description = """
                    period is daily or weekly. Anything else is rejected rather than quietly
                    treated as daily, so a typo does not look like an empty board.

                    Served from a Redis sorted set that is updated as points are awarded, so it is
                    current on every read rather than on a schedule.

                    The name field is null: this service stores expert ids only, since names belong
                    to Identity and copying them here would mean holding a second, stale copy of
                    somebody else's data.

                    Roles: EXPERT, SUPERVISOR.""")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "The board"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "period was neither daily nor weekly, error.code VALIDATION_ERROR",
                    content = @Content)
    })
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
    @Operation(summary = "Rebuild the leaderboard from the point ledger",
            description = """
                    A repair action, not part of the normal flow. Redis holds only a ranking derived
                    from point_entries, which is the record of truth, so the board can always be
                    rebuilt from it after a cache loss.

                    Returns how many entries were replayed.

                    Roles: SUPERVISOR, ADMIN.""")
    @PostMapping("/leaderboard/rebuild")
    public ApiResponse<Integer> rebuildLeaderboard(CallerIdentity caller) {
        caller.requireAnyOf(Role.SUPERVISOR, Role.ADMIN);
        return ApiResponse.ok(profileService.rebuildLeaderboard());
    }

    @Operation(summary = "Every badge with whether the caller has earned it",
            description = """
                    All six badges of section 7.2 are returned, earned or not, so a client can show
                    the locked ones too. Conditions as implemented:

                    ILK_KAMPANYA after the first completed optimization. HIZ_USTASI after 10
                    optimizations finished in under 2 hours. DONUSUM_KRALI after beating the
                    conversion target 10 times. MARATONCU after 20 optimizations inside one 24 hour
                    window. CHURN_AVCISI after 10 completed RISKLI_KAYIP cases. UZMAN after 50
                    completed cases in a single segment.

                    Roles: EXPERT, SUPERVISOR.""")
    @GetMapping("/badges")
    public ApiResponse<List<BadgeResponse>> badges(CallerIdentity caller) {
        caller.requireAnyOf(Role.EXPERT, Role.SUPERVISOR);
        return ApiResponse.ok(profileService.badgesOf(caller.userId()));
    }
}
