package org.streamhub.api.base.config;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.member.entity.Church;
import org.streamhub.api.v1.member.repository.ChurchRepository;
import org.streamhub.api.v1.worship.entity.BaptismType;
import org.streamhub.api.v1.worship.entity.Gender;
import org.streamhub.api.v1.worship.entity.RegisterDept;
import org.streamhub.api.v1.worship.entity.RegistrationFamily;
import org.streamhub.api.v1.worship.entity.RegistrationStatus;
import org.streamhub.api.v1.worship.entity.WorshipRegistration;
import org.streamhub.api.v1.worship.repository.RegistrationFamilyRepository;
import org.streamhub.api.v1.worship.repository.WorshipRegistrationRepository;

/**
 * Seeds the worship/new-family registration demo dataset (C2) on top of the churches produced
 * by {@link DataInitializer}. Runs after {@code DataInitializer} (@Order(1)) and
 * {@code PortfolioSeeder} (@Order(2)). Idempotent (skips when the registration table already
 * holds rows) and fully deterministic (fixed-seed {@link Random}). All PII is virtual/masked
 * and every row is {@code test_mode='Y'}.
 */
@Slf4j
@Component
@Order(3)
public class WorshipSeeder implements CommandLineRunner {

    /** Deterministic baseline window: the most recent 120 days. */
    private static final int WINDOW_DAYS = 120;

    private static final long SEED = 2001L;
    private static final int TARGET_REGISTRATIONS = 36;

    private static final String[] SURNAMES = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임"};
    private static final String[] GIVEN_NAMES = {"민준", "서연", "도윤", "지우", "예준", "하은", "주원", "지호", "수아", "지민"};
    private static final String[] FAMILY_RELATIONS = {"배우자", "자녀", "자녀", "부모"};

    private static final Gender[] GENDERS = Gender.values();
    private static final RegisterDept[] DEPTS = RegisterDept.values();
    private static final BaptismType[] BAPTISMS = BaptismType.values();

    private final ChurchRepository churchRepository;
    private final WorshipRegistrationRepository worshipRegistrationRepository;
    private final RegistrationFamilyRepository registrationFamilyRepository;

    public WorshipSeeder(
            ChurchRepository churchRepository,
            WorshipRegistrationRepository worshipRegistrationRepository,
            RegistrationFamilyRepository registrationFamilyRepository) {
        this.churchRepository = churchRepository;
        this.worshipRegistrationRepository = worshipRegistrationRepository;
        this.registrationFamilyRepository = registrationFamilyRepository;
    }

    @Override
    public void run(String... args) {
        if (worshipRegistrationRepository.count() > 0) {
            return;
        }
        List<Church> openChurches = churchRepository.findByOpenYn("Y");
        if (openChurches.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Random rnd = new Random(SEED);
        Map<String, Integer> daySeq = new HashMap<>();

        int familyRowCount = 0;
        for (int i = 0; i < TARGET_REGISTRATIONS; i++) {
            Church church = openChurches.get(i % openChurches.size());
            LocalDateTime createdAt = distributedDateTime(now, rnd);
            RegistrationStatus status = resolveStatus(now, createdAt, rnd);

            String name = SURNAMES[i % SURNAMES.length]
                    + GIVEN_NAMES[(i / SURNAMES.length) % GIVEN_NAMES.length];
            String maskedPhone = String.format("010-****-%04d", 1000 + (i % 9000));
            String churchExperience = (i % 2 == 0) ? "Y" : "N";
            boolean hasLeader = i % 2 == 0;

            WorshipRegistration registration = WorshipRegistration.builder()
                    .churchId(church.getId())
                    .regNo(buildRegNo(createdAt, daySeq))
                    .status(status)
                    .name(name)
                    .gender(GENDERS[i % GENDERS.length])
                    .birthDate(LocalDate.of(1960 + (i * 7) % 50, 1 + (i % 12), 1 + (i % 28)))
                    .phone(maskedPhone)
                    .email(String.format("applicant%02d@streamhub.test", i + 1))
                    .zipcode(String.format("0%04d", (i * 13) % 9999))
                    .addr1("서울특별시 강남구 데모로 " + (1 + i % 200) + "길 " + (1 + i % 50))
                    .addr2((100 + i % 900) + "호")
                    .registerDept(DEPTS[i % DEPTS.length])
                    .churchExperience(churchExperience)
                    .prevChurch("Y".equals(churchExperience) ? "데모 이전교회 " + (1 + i % 20) : null)
                    .baptismType(BAPTISMS[i % BAPTISMS.length])
                    .leaderName(hasLeader ? SURNAMES[(i + 3) % SURNAMES.length]
                            + GIVEN_NAMES[(i + 5) % GIVEN_NAMES.length] : null)
                    .leaderPhone(hasLeader ? String.format("010-****-%04d", 2000 + (i % 7000)) : null)
                    .privacyAgreed("Y")
                    .memo(status == RegistrationStatus.CONTACTED || status == RegistrationStatus.COMPLETED
                            ? "데모 상담 메모" : null)
                    .testMode("Y")
                    .createdAt(createdAt)
                    .build();
            WorshipRegistration saved = worshipRegistrationRepository.save(registration);

            int familyTotal = rnd.nextInt(4); // 0..3 family rows
            List<RegistrationFamily> families = new ArrayList<>();
            for (int f = 0; f < familyTotal; f++) {
                families.add(RegistrationFamily.builder()
                        .registrationId(saved.getId())
                        .name(SURNAMES[i % SURNAMES.length]
                                + GIVEN_NAMES[(i + f + 1) % GIVEN_NAMES.length])
                        .relation(FAMILY_RELATIONS[f % FAMILY_RELATIONS.length])
                        .birthDate(LocalDate.of(1985 + (i + f) % 35, 1 + ((i + f) % 12), 1 + ((i + f) % 28)))
                        .sort(f + 1)
                        .build());
            }
            registrationFamilyRepository.saveAll(families);
            familyRowCount += families.size();
        }
        log.info("Seeded {} worship registrations ({} family rows)",
                TARGET_REGISTRATIONS, familyRowCount);
    }

    /**
     * Resolves status from the 50/25/15/10 COMPLETED/CONTACTED/RECEIVED/CANCELED mix, forbidding
     * COMPLETED/CANCELED for registrations created in the last 3 days (lead-time realism →
     * RECEIVED/CONTACTED only).
     */
    private RegistrationStatus resolveStatus(LocalDateTime now, LocalDateTime createdAt, Random rnd) {
        boolean recent = createdAt.isAfter(now.minusDays(3));
        int r = rnd.nextInt(100);
        if (recent) {
            return rnd.nextBoolean() ? RegistrationStatus.RECEIVED : RegistrationStatus.CONTACTED;
        }
        if (r < 50) {
            return RegistrationStatus.COMPLETED;
        }
        if (r < 75) {
            return RegistrationStatus.CONTACTED;
        }
        if (r < 90) {
            return RegistrationStatus.RECEIVED;
        }
        return RegistrationStatus.CANCELED;
    }

    /** Builds a unique {@code WR-yyyyMMdd-NNNN} with a per-day running sequence. */
    private String buildRegNo(LocalDateTime createdAt, Map<String, Integer> daySeq) {
        String day = String.format("%04d%02d%02d",
                createdAt.getYear(), createdAt.getMonthValue(), createdAt.getDayOfMonth());
        int seq = daySeq.merge(day, 1, Integer::sum);
        return "WR-" + day + "-" + String.format("%04d", seq);
    }

    /**
     * Produces a {@link LocalDateTime} within the last {@link #WINDOW_DAYS} days, biased toward
     * recent dates ({@code 1 - sqrt(u)} weighting → uptrend) with a weekend density boost.
     */
    private LocalDateTime distributedDateTime(LocalDateTime now, Random rnd) {
        int daysAgo = (int) Math.round((1.0 - Math.sqrt(rnd.nextDouble())) * WINDOW_DAYS);
        LocalDateTime when = now.minusDays(daysAgo);

        DayOfWeek dow = when.getDayOfWeek();
        boolean isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
        if (!isWeekend && rnd.nextInt(100) < 33) {
            int shift = (dow.getValue() >= DayOfWeek.THURSDAY.getValue())
                    ? (DayOfWeek.SUNDAY.getValue() - dow.getValue())
                    : -(dow.getValue() % 7);
            when = when.plusDays(shift);
        }

        when = when.withHour(9 + rnd.nextInt(12)).withMinute(rnd.nextInt(60)).withSecond(0).withNano(0);
        return when.isAfter(now)
                ? now.minusHours(1 + rnd.nextInt(8)).withSecond(0).withNano(0)
                : when;
    }
}
