package com.ibetcha.wagering.api;

import com.ibetcha.wagering.application.WinCardService;
import com.ibetcha.wagering.application.WinCardService.WinCard;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bets")
public class WinCardController {

    private final WinCardService winCardService;

    public WinCardController(WinCardService winCardService) {
        this.winCardService = winCardService;
    }

    @GetMapping("/{betId}/win-card")
    public ResponseEntity<Map<String, Object>> getWinCard(@PathVariable UUID betId) {
        WinCard wc = winCardService.buildWinCard(betId);
        return ResponseEntity.ok(toResponse(wc));
    }

    private Map<String, Object> toResponse(WinCard wc) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("betId", wc.betId());
        response.put("title", wc.title());
        response.put("description", wc.description());
        response.put("stake", wc.stake());
        response.put("winner", participantInfoMap(wc.winner()));
        response.put("loser", wc.loser() != null ? participantInfoMap(wc.loser()) : null);
        response.put("allParticipants", wc.allParticipants().stream()
                .map(p -> Map.of(
                        "userId", p.userId().toString(),
                        "displayName", p.displayName(),
                        "won", p.won()
                ))
                .toList());
        response.put("resolvedAt", wc.resolvedAt());
        response.put("headToHeadRecord", wc.headToHeadRecord() != null
                ? Map.of("wins", wc.headToHeadRecord().wins(), "losses", wc.headToHeadRecord().losses())
                : null);
        return response;
    }

    private Map<String, Object> participantInfoMap(WinCardService.ParticipantInfo info) {
        return Map.of(
                "userId", info.userId().toString(),
                "displayName", info.displayName()
        );
    }
}
