package com.offerhub.gamification.dto;

import java.util.List;

public record LeaderboardResponse(String period, List<LeaderboardEntry> items) {
}
