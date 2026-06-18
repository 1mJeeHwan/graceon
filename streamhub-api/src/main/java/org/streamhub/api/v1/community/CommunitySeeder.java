package org.streamhub.api.v1.community;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.community.entity.Board;
import org.streamhub.api.v1.community.entity.CommunityPost;
import org.streamhub.api.v1.community.repository.BoardRepository;
import org.streamhub.api.v1.community.repository.CommunityPostRepository;

/**
 * Seeds the community demo dataset: ~6 boards and ~80 posts. Idempotent (skips when either table
 * already holds rows). The fixed-seed {@link Random} makes the dataset <em>shape</em> (secret mix,
 * counts, board spread) reproducible across runs; absolute dates are <em>not</em> fixed — every
 * post is anchored to {@link LocalDateTime#now()} minus a random 0..120 day offset, so the window
 * rolls forward to stay current. All writer names and content are virtual/fictional (PII guard).
 */
@Slf4j
@Component
@Order(13)
public class CommunitySeeder implements CommandLineRunner {

    private static final long SEED = 913L;
    private static final int TARGET_POSTS = 80;
    private static final int WINDOW_DAYS = 120;

    /** Board definitions: {code, name, readLevel, writeLevel}. */
    private static final BoardSeed[] BOARDS = {
            new BoardSeed("notice", "공지사항", 1, 10),
            new BoardSeed("free", "자유게시판", 1, 1),
            new BoardSeed("share", "나눔", 1, 2),
            new BoardSeed("prayer", "기도제목", 1, 1),
            new BoardSeed("testimony", "간증", 1, 2),
            new BoardSeed("qna", "Q&A", 1, 1)
    };

    private static final String[] TITLES = {
            "이번 주 예배 안내", "기도 부탁드립니다", "찬양팀 모집", "간증 나눕니다",
            "주일학교 봉사자 모집", "수요예배 변경 안내", "감사한 한 주였습니다",
            "새가족 환영합니다", "셀모임 후기 공유", "성경공부 일정 문의",
            "나눔 물품 있습니다", "함께 기도해요", "은혜로운 말씀 나눔", "교회 행사 사진"
    };

    private static final String[] WRITER_NAMES = {
            "홍길동", "김은혜", "이찬양", "박소망", "정믿음", "최사랑", "강평강", "윤기쁨"
    };

    private final BoardRepository boardRepository;
    private final CommunityPostRepository postRepository;

    public CommunitySeeder(BoardRepository boardRepository, CommunityPostRepository postRepository) {
        this.boardRepository = boardRepository;
        this.postRepository = postRepository;
    }

    @Override
    public void run(String... args) {
        if (boardRepository.count() > 0 || postRepository.count() > 0) {
            return;
        }
        Random rnd = new Random(SEED);
        LocalDateTime now = LocalDateTime.now();

        List<Board> boards = new ArrayList<>();
        for (int i = 0; i < BOARDS.length; i++) {
            BoardSeed seed = BOARDS[i];
            boards.add(boardRepository.save(Board.builder()
                    .code(seed.code())
                    .name(seed.name())
                    .readLevel(seed.readLevel())
                    .writeLevel(seed.writeLevel())
                    .useYn("Y")
                    .sortOrder(i + 1)
                    .build()));
        }

        List<CommunityPost> posts = new ArrayList<>();
        for (int i = 0; i < TARGET_POSTS; i++) {
            Board board = boards.get(i % boards.size());
            int daysAgo = rnd.nextInt(WINDOW_DAYS + 1);
            LocalDateTime createdAt = now.minusDays(daysAgo)
                    .withHour(9 + rnd.nextInt(12)).withMinute(rnd.nextInt(60))
                    .withSecond(0).withNano(0);
            String secretYn = rnd.nextInt(100) < 12 ? "Y" : "N";

            posts.add(CommunityPost.builder()
                    .boardId(board.getId())
                    .category(board.getName())
                    .title(TITLES[rnd.nextInt(TITLES.length)] + " (" + (i + 1) + ")")
                    .content("데모 게시글 본문입니다. " + board.getName() + " 게시판에 작성된 가상의 글입니다.")
                    .writerName(WRITER_NAMES[rnd.nextInt(WRITER_NAMES.length)])
                    .secretYn(secretYn)
                    .recommendCount(rnd.nextInt(51))
                    .viewCount(rnd.nextInt(501))
                    .createdAt(createdAt)
                    .build());
        }
        postRepository.saveAll(posts);
        log.info("Seeded {} community boards ({} posts)", boards.size(), posts.size());
    }

    /** Board seed tuple. */
    private record BoardSeed(String code, String name, int readLevel, int writeLevel) {
    }
}
