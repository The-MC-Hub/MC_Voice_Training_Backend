package com.mchub.services.impl;

import com.mchub.dto.MinigameLeaderboardEntryDTO;
import com.mchub.dto.MinigamePromptDTO;
import com.mchub.dto.MinigameResultDTO;
import com.mchub.dto.MinigameSubmitRequest;
import com.mchub.models.MinigameResult;
import com.mchub.models.User;
import com.mchub.repositories.MinigameResultRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.GamificationService;
import com.mchub.services.MinigameService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
@RequiredArgsConstructor
public class MinigameServiceImpl implements MinigameService {

    private static final String GAME_TYPE_SPEED_READER = "SPEED_READER";

    private final MinigameResultRepository minigameResultRepository;
    private final UserRepository userRepository;
    private final GamificationService gamificationService;
    private final MongoTemplate mongoTemplate;

    // Curated short lines, grouped by difficulty. Kept short (5-14 words) so a
    // read-aloud pass fits inside the round's time budget at any speaking pace.
    private static final List<String> EASY_LINES = List.of(
            "Xin chào quý vị và các bạn.",
            "Chúc mừng năm mới đến tất cả mọi người.",
            "Cảm ơn quý vị đã đến tham dự buổi lễ hôm nay.",
            "Chương trình của chúng ta sẽ bắt đầu ngay sau đây.",
            "Xin một tràng pháo tay thật lớn cho tất cả mọi người.",
            "Kính chúc quý vị một buổi tối vui vẻ và ý nghĩa.",
            "Sau đây là phần giới thiệu các vị khách mời đặc biệt.",
            "Mời quý vị cùng hướng mắt lên sân khấu."
    );

    private static final List<String> NORMAL_LINES = List.of(
            "Không khí buổi lễ hôm nay thật sự vô cùng ấm áp và trang trọng.",
            "Xin mời đại diện hai gia đình cùng lên sân khấu để phát biểu.",
            "Đây là khoảnh khắc mà tất cả chúng ta đã cùng nhau mong chờ.",
            "Xin cảm ơn các đơn vị tài trợ đã đồng hành cùng chương trình.",
            "Sau đây, chúng ta sẽ cùng nhau ôn lại những dấu mốc quan trọng.",
            "Chúc cho tình yêu của đôi bạn trẻ sẽ mãi mãi bền vững theo năm tháng.",
            "Kính mời quý vị đại biểu, khách quý an tọa để chương trình được bắt đầu.",
            "Một lần nữa, xin gửi lời chúc mừng nồng nhiệt nhất đến cô dâu chú rể."
    );

    private static final List<String> HARD_LINES = List.of(
            "Trong không khí trang nghiêm và xúc động này, chúng tôi xin trân trọng giới thiệu các vị khách quý đã có mặt.",
            "Xuyên suốt hành trình đã qua, chúng ta đã cùng nhau vượt qua không ít khó khăn để có được thành quả hôm nay.",
            "Kính thưa quý vị đại biểu, quý phụ huynh cùng toàn thể các bạn học sinh thân mến có mặt trong hội trường ngày hôm nay.",
            "Chương trình văn nghệ chào mừng xin phép được bắt đầu bằng một tiết mục hết sức đặc sắc và giàu cảm xúc.",
            "Ban tổ chức xin trân trọng cảm ơn sự có mặt đông đủ của quý vị trong một buổi tối nhiều ý nghĩa như thế này.",
            "Sự kiện ngày hôm nay đánh dấu một cột mốc vô cùng quan trọng trong chặng đường phát triển của toàn thể công ty chúng ta."
    );

    private static final Map<String, List<String>> LINES_BY_DIFFICULTY = Map.of(
            "EASY", EASY_LINES,
            "NORMAL", NORMAL_LINES,
            "HARD", HARD_LINES
    );

    // Reading speed budget used to compute the per-round timer.
    // Lower WPM budget = stricter timer = harder difficulty.
    private static final Map<String, Integer> WPM_BUDGET = Map.of(
            "EASY", 90,
            "NORMAL", 130,
            "HARD", 170
    );

    private static final Map<String, Double> XP_PER_ROUND = Map.of(
            "EASY", 1.5,
            "NORMAL", 2.5,
            "HARD", 4.0
    );

    @Override
    public List<MinigamePromptDTO> getSpeedReaderPrompts(String difficulty, int roundCount) {
        String diff = normalizeDifficulty(difficulty);
        List<String> pool = new ArrayList<>(LINES_BY_DIFFICULTY.get(diff));
        Collections.shuffle(pool);

        int wpm = WPM_BUDGET.get(diff);
        int count = Math.min(Math.max(roundCount, 1), pool.size());

        List<MinigamePromptDTO> prompts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String line = pool.get(i);
            int wordCount = line.trim().split("\\s+").length;
            // time budget = words / wpm (minutes) -> ms, plus a flat buffer for reaction time
            int timeLimitMs = (int) Math.round((wordCount / (double) wpm) * 60_000) + 1500;
            prompts.add(MinigamePromptDTO.builder()
                    .text(line)
                    .timeLimitMs(timeLimitMs)
                    .roundIndex(i)
                    .build());
        }
        return prompts;
    }

    @Override
    public MinigameResultDTO submitSpeedReaderRun(String userId, MinigameSubmitRequest request) {
        String diff = normalizeDifficulty(request.getDifficulty());
        int roundsCleared = Math.max(0, request.getRoundsCleared());
        int bestCombo = Math.max(0, request.getBestCombo());

        double xpPerRound = XP_PER_ROUND.get(diff);
        double xpEarned = roundsCleared * xpPerRound + (bestCombo >= 5 ? 5.0 : 0.0);
        int score = roundsCleared * 100 + bestCombo * 20;

        MinigameResult result = MinigameResult.builder()
                .userId(userId)
                .gameType(GAME_TYPE_SPEED_READER)
                .difficulty(diff)
                .roundsCleared(roundsCleared)
                .bestCombo(bestCombo)
                .score(score)
                .xpEarned(xpEarned)
                .build();
        minigameResultRepository.save(result);

        gamificationService.addMinigameXP(userId, xpEarned);

        MinigameResult bestRun = minigameResultRepository
                .findTopByUserIdAndGameTypeOrderByScoreDesc(userId, GAME_TYPE_SPEED_READER);
        int previousBest = bestRun != null ? bestRun.getScore() : 0;
        boolean isNewBest = score >= previousBest;

        return MinigameResultDTO.builder()
                .score(score)
                .xpEarned(xpEarned)
                .isNewPersonalBest(isNewBest)
                .personalBestScore(Math.max(score, previousBest))
                .build();
    }

    @Override
    public List<MinigameLeaderboardEntryDTO> getLeaderboard(String gameType, int limit) {
        String type = (gameType == null || gameType.isBlank()) ? GAME_TYPE_SPEED_READER : gameType.toUpperCase();
        int cappedLimit = Math.min(Math.max(limit, 1), 100);

        Aggregation aggregation = newAggregation(
                match(org.springframework.data.mongodb.core.query.Criteria.where("gameType").is(type)),
                sort(org.springframework.data.domain.Sort.Direction.DESC, "score"),
                group("userId")
                        .first("score").as("bestScore")
                        .first("bestCombo").as("bestCombo"),
                sort(org.springframework.data.domain.Sort.Direction.DESC, "bestScore"),
                limit(cappedLimit)
        );
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "minigame_results", Document.class);
        List<Document> top = results.getMappedResults();

        List<String> userIds = top.stream().map(d -> d.getString("_id")).collect(Collectors.toList());
        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<MinigameLeaderboardEntryDTO> result = new ArrayList<>();
        int rank = 1;
        for (Document entry : top) {
            String userId = entry.getString("_id");
            User u = userMap.get(userId);
            result.add(MinigameLeaderboardEntryDTO.builder()
                    .rank(rank++)
                    .userId(userId)
                    .userName(u != null ? u.getName() : "Ẩn danh")
                    .avatar(u != null ? u.getAvatar() : null)
                    .bestScore(entry.getInteger("bestScore", 0))
                    .bestCombo(entry.getInteger("bestCombo", 0))
                    .build());
        }
        return result;
    }

    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null) return "NORMAL";
        String d = difficulty.trim().toUpperCase();
        return LINES_BY_DIFFICULTY.containsKey(d) ? d : "NORMAL";
    }
}
