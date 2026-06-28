package org.streamhub.api.base.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.streamhub.api.v1.admin.entity.AdminAccount;
import org.streamhub.api.v1.admin.entity.Role;
import org.streamhub.api.v1.admin.repository.AdminAccountRepository;
import org.streamhub.api.v1.member.entity.Church;
import org.streamhub.api.v1.member.entity.Country;
import org.streamhub.api.v1.member.entity.Member;
import org.streamhub.api.v1.member.entity.Region;
import org.streamhub.api.v1.member.entity.UserStatus;
import org.streamhub.api.v1.member.repository.ChurchRepository;
import org.streamhub.api.v1.member.repository.CountryRepository;
import org.streamhub.api.v1.member.repository.MemberRepository;
import org.streamhub.api.v1.member.repository.RegionRepository;
import org.streamhub.api.v1.church.entity.Denomination;
import org.streamhub.api.v1.church.entity.WorshipKind;
import org.streamhub.api.v1.church.entity.WorshipTime;
import org.streamhub.api.v1.church.repository.WorshipTimeRepository;
import org.streamhub.api.v1.content.entity.Channel;
import org.streamhub.api.v1.content.entity.Content;
import org.streamhub.api.v1.content.entity.ContentHashtag;
import org.streamhub.api.v1.content.entity.ContentStatus;
import org.streamhub.api.v1.content.entity.ContentType;
import org.streamhub.api.v1.content.entity.Hashtag;
import org.streamhub.api.v1.content.repository.ChannelRepository;
import org.streamhub.api.v1.content.repository.ContentHashtagRepository;
import org.streamhub.api.v1.content.repository.ContentRepository;
import org.streamhub.api.v1.content.repository.HashtagRepository;
import org.streamhub.api.v1.statistics.entity.WatchHistory;
import org.streamhub.api.v1.statistics.repository.WatchHistoryRepository;
import org.streamhub.api.v1.actionlog.entity.ActionLog;
import org.streamhub.api.v1.actionlog.repository.ActionLogRepository;
import org.streamhub.api.v1.post.entity.Post;
import org.streamhub.api.v1.post.entity.PostStatus;
import org.streamhub.api.v1.post.repository.PostRepository;

/**
 * Seeds default operator accounts and demo reference/member data on startup so the
 * cloned app is usable immediately after {@code docker-compose up}. Passwords are
 * BCrypt-hashed here rather than hardcoded in SQL. Idempotent: each seed step
 * skips when its data already exists.
 */
@Slf4j
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final String[] SURNAMES = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임"};
    private static final String[] GIVEN_NAMES = {"민준", "서연", "도윤", "지우", "예준", "하은", "주원", "지호", "수아", "지민"};

    private final AdminAccountRepository adminRepository;
    private final CountryRepository countryRepository;
    private final RegionRepository regionRepository;
    private final ChurchRepository churchRepository;
    private final WorshipTimeRepository worshipTimeRepository;
    private final MemberRepository memberRepository;
    private final ChannelRepository channelRepository;
    private final ContentRepository contentRepository;
    private final HashtagRepository hashtagRepository;
    private final ContentHashtagRepository contentHashtagRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final ActionLogRepository actionLogRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            AdminAccountRepository adminRepository,
            CountryRepository countryRepository,
            RegionRepository regionRepository,
            ChurchRepository churchRepository,
            WorshipTimeRepository worshipTimeRepository,
            MemberRepository memberRepository,
            ChannelRepository channelRepository,
            ContentRepository contentRepository,
            HashtagRepository hashtagRepository,
            ContentHashtagRepository contentHashtagRepository,
            WatchHistoryRepository watchHistoryRepository,
            ActionLogRepository actionLogRepository,
            PostRepository postRepository,
            PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.countryRepository = countryRepository;
        this.regionRepository = regionRepository;
        this.churchRepository = churchRepository;
        this.worshipTimeRepository = worshipTimeRepository;
        this.memberRepository = memberRepository;
        this.channelRepository = channelRepository;
        this.contentRepository = contentRepository;
        this.hashtagRepository = hashtagRepository;
        this.contentHashtagRepository = contentHashtagRepository;
        this.watchHistoryRepository = watchHistoryRepository;
        this.actionLogRepository = actionLogRepository;
        this.postRepository = postRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdmins();
        seedOrganization();
        seedMembers();
        seedContent();
        seedWatchHistory();
        seedActionLogs();
        seedPosts();
    }

    private void seedAdmins() {
        seedAdmin("admin", "admin1234", "홍길동", Role.SYSTEM, null);
        seedAdmin("manager", "manager1234", "김영희", Role.CHURCH_MANAGER, 1L);
        seedAdmin("viewer", "viewer1234", "이몽룡", Role.VIEWER, null);
    }

    private void seedAdmin(String loginId, String rawPassword, String name, Role role, Long churchId) {
        if (adminRepository.existsByLoginId(loginId)) {
            return;
        }
        adminRepository.save(AdminAccount.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(rawPassword))
                .name(name)
                .role(role)
                .churchId(churchId)
                .build());
        log.info("Seeded admin account: {} ({})", loginId, role);
    }

    // --- Church seed (C1 church-finder) ------------------------------------
    // Deterministic, masked, fictional. Coordinates are real dong-level anchors (Seoul/Gyeonggi)
    // nudged by an index-based ±offset so no seed row pinpoints a real address. Church names are
    // synthesized ({지명}+{형용}+교회) and never match a real church; phones are masked virtual
    // numbers; pastor names are fictional. dataSource="SEED" is the demo-badge basis.

    /** A Seoul dong-level coordinate anchor and its 구(district)/주소 prefix. */
    private static final String[][] SEOUL_ANCHORS = {
            // {dongName, district, lat, lng}
            {"역삼", "강남구", "37.5006", "127.0364"},
            {"삼성", "강남구", "37.5140", "127.0565"},
            {"논현", "강남구", "37.5111", "127.0218"},
            {"청담", "강남구", "37.5197", "127.0473"},
            {"잠실", "송파구", "37.5132", "127.1001"},
            {"방이", "송파구", "37.5106", "127.1162"},
            {"목동", "양천구", "37.5301", "126.8755"},
            {"여의도", "영등포구", "37.5215", "126.9242"},
            {"당산", "영등포구", "37.5346", "126.9020"},
            {"마포", "마포구", "37.5400", "126.9457"},
            {"상암", "마포구", "37.5790", "126.8895"},
            {"연남", "마포구", "37.5634", "126.9256"},
            {"신촌", "서대문구", "37.5598", "126.9425"},
            {"홍제", "서대문구", "37.5889", "126.9437"},
            {"종로", "종로구", "37.5704", "126.9921"},
            {"혜화", "종로구", "37.5824", "127.0019"},
            {"명동", "중구", "37.5636", "126.9869"},
            {"용산", "용산구", "37.5311", "126.9810"},
            {"이태원", "용산구", "37.5345", "126.9946"},
            {"성수", "성동구", "37.5446", "127.0560"},
            {"왕십리", "성동구", "37.5614", "127.0378"},
            {"건대", "광진구", "37.5403", "127.0695"},
            {"노원", "노원구", "37.6542", "127.0568"},
            {"수유", "강북구", "37.6383", "127.0254"},
            {"불광", "은평구", "37.6105", "126.9298"},
            {"신림", "관악구", "37.4842", "126.9296"},
            {"사당", "동작구", "37.4765", "126.9816"},
            {"구로", "구로구", "37.5034", "126.8821"},
    };

    /** A Gyeonggi dong/시-level coordinate anchor and its 시/주소 prefix. */
    private static final String[][] GYEONGGI_ANCHORS = {
            {"수원", "수원시 영통구", "37.2595", "127.0466"},
            {"분당", "성남시 분당구", "37.3826", "127.1186"},
            {"판교", "성남시 분당구", "37.3947", "127.1112"},
            {"일산", "고양시 일산동구", "37.6584", "126.7700"},
            {"안양", "안양시 동안구", "37.3924", "126.9568"},
            {"부천", "부천시 원미구", "37.5035", "126.7660"},
            {"광명", "광명시", "37.4785", "126.8644"},
            {"용인", "용인시 수지구", "37.3220", "127.0967"},
            {"평촌", "안양시 동안구", "37.3942", "126.9637"},
            {"화성", "화성시 동탄", "37.2007", "127.0727"},
            {"김포", "김포시", "37.6152", "126.7156"},
            {"의정부", "의정부시", "37.7381", "127.0337"},
    };

    /** Place adjectives → synthesized church names ({지명}{형용}교회). */
    private static final String[] CHURCH_ADJ = {"은혜", "소망", "사랑", "중앙", "제일", "비전", "한빛", "새순", "충성", "샘물"};

    /** Round-robin denomination pool, weighted toward presbyterian (PCK/HAPDONG) so filters aren't empty. */
    private static final Denomination[] DENOM_POOL = {
            Denomination.PCK, Denomination.HAPDONG, Denomination.METHODIST,
            Denomination.PCK, Denomination.HAPDONG, Denomination.HOLINESS,
            Denomination.GOSPEL, Denomination.PCK, Denomination.BAPTIST, Denomination.ETC,
    };

    /** Facility CSV combinations, picked by index%. */
    private static final String[] FACILITY_SETS = {
            "주차,승강기,영유아실,카페",
            "주차,영유아실",
            "주차,승강기,카페",
            "승강기,영유아실,장애인편의",
            "주차,카페,주일학교",
            "주차,승강기,영유아실,카페,장애인편의",
    };

    private static final String[] PASTOR_SURNAMES = {"김", "이", "박", "최", "정", "강", "조", "윤", "장", "임", "한", "신"};
    private static final String[] PASTOR_GIVENS = {"성", "현", "준", "광", "은", "재", "영", "호", "찬", "환", "석", "민"};

    /** Seeds the country &gt; region &gt; church hierarchy. Church id 1 must exist for the manager account. */
    private void seedOrganization() {
        if (countryRepository.count() > 0) {
            return;
        }
        Country korea = countryRepository.save(Country.builder().name("대한민국").code("KR").build());
        Country usa = countryRepository.save(Country.builder().name("미국").code("US").build());

        Region seoul = regionRepository.save(Region.builder().countryId(korea.getId()).name("서울").build());
        Region gyeonggi = regionRepository.save(Region.builder().countryId(korea.getId()).name("경기").build());
        // 부산/California regions remain in the hierarchy for member/reference data; the 40 seeded
        // churches live in 서울/경기 only (real dong-level anchors).
        regionRepository.save(Region.builder().countryId(korea.getId()).name("부산").build());
        regionRepository.save(Region.builder().countryId(usa.getId()).name("California").build());

        // 40 churches: 28 Seoul + 12 Gyeonggi. The FIRST saved church becomes id=1 (manager mapping).
        int idx = 0;
        for (String[] anchor : SEOUL_ANCHORS) {
            seedChurch(seoul.getId(), anchor, idx++);
        }
        for (String[] anchor : GYEONGGI_ANCHORS) {
            seedChurch(gyeonggi.getId(), anchor, idx++);
        }
        log.info("Seeded organization: {} countries, {} regions, {} churches, {} worship times",
                countryRepository.count(), regionRepository.count(),
                churchRepository.count(), worshipTimeRepository.count());
    }

    /**
     * Seeds one deterministic church row plus its worship-time rows. {@code seq} drives every
     * derived value so a fresh database always materialises the identical 40-church picture.
     */
    private void seedChurch(Long regionId, String[] anchor, int seq) {
        String place = anchor[0];
        String district = anchor[1];
        double baseLat = Double.parseDouble(anchor[2]);
        double baseLng = Double.parseDouble(anchor[3]);

        // Index-based ±offset (≤ ±0.0019°) so coordinates never pinpoint a real address.
        double lat = baseLat + ((seq % 7) - 3) * 0.0006;
        double lng = baseLng + ((seq % 5) - 2) * 0.0007;

        String name = place + CHURCH_ADJ[seq % CHURCH_ADJ.length] + "교회";
        Denomination denomination = DENOM_POOL[seq % DENOM_POOL.length];
        // Seoul rows are seeded first (seq < SEOUL_ANCHORS.length); the rest are Gyeonggi.
        boolean isSeoul = seq < SEOUL_ANCHORS.length;
        String areaCode = isSeoul ? "02" : "031";
        String phone = areaCode + "-***-" + String.format("%04d", 1000 + seq * 7 % 9000);
        String pastorName = PASTOR_SURNAMES[seq % PASTOR_SURNAMES.length]
                + PASTOR_GIVENS[(seq / PASTOR_SURNAMES.length) % PASTOR_GIVENS.length] + "목사";
        String facilities = FACILITY_SETS[seq % FACILITY_SETS.length];
        String address = (isSeoul ? "서울특별시 " : "경기도 ")
                + district + " " + place + "로 " + (10 + seq * 3 % 200);
        String zipcode = String.format("%05d", 10000 + seq * 137 % 89999);
        // Reuse the verified Unsplash church thumbnails (StorageService.publicUrl pass-through).
        String thumbnail = CONTENT_THUMBS[seq % CONTENT_THUMBS.length];
        // 1~2 rows hidden from public search as a "노출 제외" demo (id=1 always visible).
        String useYn = (seq == 9 || seq == 33) ? "N" : "Y";

        Church church = churchRepository.save(Church.builder()
                .regionId(regionId)
                .name(name)
                .openYn("Y")
                .denomination(denomination)
                .latitude(lat)
                .longitude(lng)
                .address(address)
                .addressDetail((seq % 3 + 1) + "층 본당")
                .zipcode(zipcode)
                .phone(phone)
                .pastorName(pastorName)
                .facilities(facilities)
                .introduction(name + "는 " + place + " 지역의 데모용 가상 교회입니다. (실제 교회 정보 아님)")
                .thumbnailKey(thumbnail)
                .dataSource("SEED")
                .useYn(useYn)
                .build());

        seedWorshipTimes(church.getId(), seq);
    }

    /** Seeds 3~4 deterministic worship-time rows for a church. */
    private void seedWorshipTimes(Long churchId, int seq) {
        List<WorshipTime> times = new ArrayList<>();
        times.add(WorshipTime.builder().churchId(churchId).kind(WorshipKind.SUNDAY)
                .dayLabel("주일").startTime("11:00").place("본당").target("전교인").sort(1).build());
        times.add(WorshipTime.builder().churchId(churchId).kind(WorshipKind.DAWN)
                .dayLabel("매일").startTime("05:30").place("본당").target("전교인").sort(2).build());
        times.add(WorshipTime.builder().churchId(churchId).kind(WorshipKind.WEDNESDAY)
                .dayLabel("수요").startTime("19:30").place("본당").target("전교인").sort(3).build());
        // Every 3rd church also offers a youth service.
        if (seq % 3 == 0) {
            times.add(WorshipTime.builder().churchId(churchId).kind(WorshipKind.YOUTH)
                    .dayLabel("주일").startTime("14:00").place("교육관").target("청년/학생").sort(4).build());
        }
        worshipTimeRepository.saveAll(times);
    }

    /** Seeds demo members spread across churches, statuses, and signup dates. */
    private void seedMembers() {
        if (memberRepository.count() > 0) {
            return;
        }
        List<Long> churchIds = churchRepository.findAll().stream().map(Church::getId).toList();
        if (churchIds.isEmpty()) {
            return;
        }
        String password = passwordEncoder.encode("member1234");
        UserStatus[] statuses = {
                UserStatus.CONFIRMED, UserStatus.CONFIRMED, UserStatus.CONFIRMED,
                UserStatus.PENDING, UserStatus.INACTIVE
        };
        LocalDateTime now = LocalDateTime.now();

        List<Member> members = new ArrayList<>();
        int count = 60;
        for (int i = 0; i < count; i++) {
            String name = SURNAMES[i % SURNAMES.length] + GIVEN_NAMES[(i / SURNAMES.length) % GIVEN_NAMES.length];
            Long churchId = churchIds.get(i % churchIds.size());
            UserStatus status = statuses[i % statuses.length];
            String liveYn = (i % 2 == 0) ? "Y" : "N";
            LocalDateTime createdAt = now.minusDays(i).minusHours(i % 24);
            members.add(Member.builder()
                    .churchId(churchId)
                    .email(String.format("member%02d@streamhub.test", i + 1))
                    .password(password)
                    .name(name)
                    .phone(String.format("010-%04d-%04d", 1000 + i, 2000 + i))
                    .userStatus(status)
                    .liveYn(liveYn)
                    .createdAt(createdAt)
                    .build());
        }
        memberRepository.saveAll(members);
        log.info("Seeded {} demo members", members.size());
    }

    private static final String[] HASHTAGS = {"예배", "찬양", "설교", "큐티", "특송", "세미나", "기도회", "청년부"};
    private static final String[] TITLE_PREFIX = {"주일", "수요", "새벽", "금요", "특별"};
    private static final String[] TITLE_SUFFIX = {"예배 실황", "찬양 모음", "말씀 나눔", "기도회", "성가대 특송"};

    // Video content is served via YouTube embeds (no media hosting/storage needed) — the user-site
    // VideoPlayer detects a YouTube link and embeds it. Public Blender open-movie uploads are used
    // as stable demo videos; a direct .mp4 URL would still play via the player's fallback.
    private static final String[] SAMPLE_VIDEOS = {
            "https://youtu.be/aqz-KE-bpKQ", // Big Buck Bunny
            "https://youtu.be/eRsGyueVLvQ", // Sintel
            "https://youtu.be/R6MlUcmOul8", // Tears of Steel
            "https://youtu.be/TLkA0RELQ1g", // Elephant's Dream
            "https://youtu.be/Z3rNk5wPjjE", // Caminandes: Gran Dillama
            "https://youtu.be/WhWc3b3KhnY", // Spring
            "https://youtu.be/UXqq0ZvbOnk", // Charge
            "https://youtu.be/GfO-3Oir-qM", // Cosmos Laundromat
    };
    private static final String[] SAMPLE_AUDIOS = {
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
    };

    // License-safe (Unsplash) church/worship photos used as content thumbnails so the user site
    // looks like a real church-broadcast service. One pooled list indexed by the running item index
    // so EVERY item (video or sound) gets a distinct image — previously SOUND items, which only
    // occur at i%3==2, all collapsed onto WORSHIP_THUMBS[2] (one identical thumbnail).
    // URLs verified to resolve (HTTP 200, image/jpeg).
    private static final String THUMB_Q = "?w=640&q=80&auto=format&fit=crop";
    private static final String[] CONTENT_THUMBS = {
            "https://images.unsplash.com/photo-1438032005730-c779502df39b" + THUMB_Q, // stained-glass sanctuary
            "https://images.unsplash.com/photo-1507692049790-de58290a4334" + THUMB_Q, // worship, hands raised
            "https://images.unsplash.com/photo-1473177104440-ffee2f376098" + THUMB_Q, // cathedral nave
            "https://images.unsplash.com/photo-1438232992991-995b7058bbb3" + THUMB_Q, // praise night, stage
            "https://images.unsplash.com/photo-1519491050282-cf00c82424b4" + THUMB_Q, // wooden chapel pews
            "https://images.unsplash.com/photo-1506157786151-b8491531f063" + THUMB_Q, // worship crowd, lights
            "https://images.unsplash.com/photo-1510590337019-5ef8d3d32116" + THUMB_Q, // open Bible, congregation
            "https://images.unsplash.com/photo-1529070538774-1843cb3265df" + THUMB_Q, // small-group / study
    };

    /** Seeds channels (one per church), demo contents, and hashtags. */
    private void seedContent() {
        if (contentRepository.count() > 0) {
            return;
        }
        List<Church> churches = churchRepository.findAll();
        if (churches.isEmpty()) {
            return;
        }

        // One channel per church.
        List<Long> channelIds = new ArrayList<>();
        for (Church church : churches) {
            Channel channel = channelRepository.save(
                    Channel.builder().churchId(church.getId()).name(church.getName() + " 채널").build());
            channelIds.add(channel.getId());
        }

        // Hashtag pool.
        List<Hashtag> tags = new ArrayList<>();
        for (String name : HASHTAGS) {
            tags.add(hashtagRepository.save(Hashtag.builder().name(name).build()));
        }

        // Type cycles on %3; status cycles on %4 so the two don't correlate (otherwise every
        // SOUND item would land on DRAFT and the user site's music section would be empty).
        ContentType[] types = {ContentType.VIDEO, ContentType.VIDEO, ContentType.SOUND};
        LocalDateTime now = LocalDateTime.now();

        int count = 24;
        for (int i = 0; i < count; i++) {
            String title = TITLE_PREFIX[i % TITLE_PREFIX.length] + " "
                    + TITLE_SUFFIX[(i / TITLE_PREFIX.length) % TITLE_SUFFIX.length] + " #" + (i + 1);
            ContentType type = types[i % types.length];
            String mediaUrl = type == ContentType.VIDEO
                    ? SAMPLE_VIDEOS[i % SAMPLE_VIDEOS.length]
                    : SAMPLE_AUDIOS[i % SAMPLE_AUDIOS.length];
            String thumbUrl = CONTENT_THUMBS[i % CONTENT_THUMBS.length];
            Content content = contentRepository.save(Content.builder()
                    .channelId(channelIds.get(i % channelIds.size()))
                    .type(type)
                    .title(title)
                    .description(title + " 영상입니다.")
                    .mediaUrl(mediaUrl)
                    .thumbnailKey(thumbUrl)
                    .durationSec(300 + (i * 37) % 3000)
                    .status(i % 4 == 3 ? ContentStatus.DRAFT : ContentStatus.PUBLISHED)
                    .viewCount((long) ((i * 137) % 5000))
                    .createdAt(now.minusDays(i).minusHours(i % 24))
                    .build());

            // 2 hashtags per content.
            Hashtag t1 = tags.get(i % tags.size());
            Hashtag t2 = tags.get((i + 3) % tags.size());
            contentHashtagRepository.save(
                    ContentHashtag.builder().contentId(content.getId()).hashtagId(t1.getId()).build());
            if (!t2.getId().equals(t1.getId())) {
                contentHashtagRepository.save(
                        ContentHashtag.builder().contentId(content.getId()).hashtagId(t2.getId()).build());
            }
        }
        log.info("Seeded {} channels, {} hashtags, {} contents",
                channelIds.size(), tags.size(), contentRepository.count());
    }

    /** Seeds watch events spread across members, contents, and the last 30 days. */
    private void seedWatchHistory() {
        if (watchHistoryRepository.count() > 0) {
            return;
        }
        List<Long> memberIds = memberRepository.findAll().stream().map(Member::getId).toList();
        List<Long> contentIds = contentRepository.findAll().stream().map(Content::getId).toList();
        if (memberIds.isEmpty() || contentIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<WatchHistory> events = new ArrayList<>();
        int count = 800;
        for (int i = 0; i < count; i++) {
            events.add(WatchHistory.builder()
                    .memberId(memberIds.get((i * 13) % memberIds.size()))
                    .contentId(contentIds.get((i * 7) % contentIds.size()))
                    .watchedAt(now.minusDays(i % 30).minusMinutes(i % 1440))
                    .watchSeconds(30 + (i * 53) % 3600)
                    .build());
        }
        watchHistoryRepository.saveAll(events);
        log.info("Seeded {} watch-history events", events.size());
    }

    private static final String[][] ACTIONS = {
            {"LOGIN", "ADMIN", "로그인"},
            {"MEMBER_APPROVE", "MEMBER", "3건 승인"},
            {"MEMBER_DENY", "MEMBER", "1건 거부"},
            {"MEMBER_UPDATE", "MEMBER", "회원 정보 수정"},
            {"CONTENT_CREATE", "CONTENT", "콘텐츠 등록"},
            {"CONTENT_UPDATE", "CONTENT", "콘텐츠 수정"},
            {"CONTENT_DELETE", "CONTENT", "콘텐츠 삭제"},
    };

    /** Seeds sample audit-log rows so the log view isn't empty on a fresh start. */
    private void seedActionLogs() {
        if (actionLogRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<ActionLog> logs = new ArrayList<>();
        for (int i = 0; i < 18; i++) {
            String[] a = ACTIONS[i % ACTIONS.length];
            boolean bySystem = i % 3 != 0;
            logs.add(ActionLog.builder()
                    .adminId(bySystem ? 1L : 2L)
                    .adminName(bySystem ? "홍길동" : "김영희")
                    .action(a[0])
                    .targetType(a[1])
                    .targetId(String.valueOf(i + 1))
                    .detail(a[2])
                    .createdAt(now.minusHours(i * 5L))
                    .build());
        }
        actionLogRepository.saveAll(logs);
        log.info("Seeded {} action-log entries", logs.size());
    }

    private static final String[][] POSTS = {
            {"2026년 상반기 예배 안내", "성도 여러분께 2026년 상반기 예배 일정을 안내드립니다. 주일예배는 오전 11시, 수요예배는 저녁 7시 30분에 진행됩니다. 예배 영상은 영상 탭에서 다시 보실 수 있습니다."},
            {"온라인 예배 시청 방법 안내", "은혜온에서 모든 예배 영상을 무료로 시청하실 수 있습니다. 별도 로그인 없이 영상·음악·게시글을 자유롭게 둘러보세요. 모바일과 PC 모두 지원합니다."},
            {"청년부 여름 수련회 모집", "오는 7월 청년부 여름 수련회 참가자를 모집합니다. 찬양과 말씀, 교제의 시간을 통해 함께 은혜를 나누는 자리입니다. 신청은 각 교회 사무실로 문의해 주세요."},
            {"새가족 환영 안내", "처음 오신 분들을 진심으로 환영합니다. 새가족 등록을 원하시면 예배 후 안내데스크로 방문해 주세요. 따뜻한 공동체가 여러분을 기다립니다."},
            {"찬양집회 음원 업데이트", "지난 찬양집회 실황 음원이 업데이트되었습니다. 음악 메뉴에서 고음질로 감상하실 수 있습니다. 은혜의 찬양으로 한 주를 시작해 보세요."},
            {"설교 말씀 다시보기 오픈", "놓치신 주일 설교를 영상으로 다시 보실 수 있습니다. 영상 메뉴에서 최신 말씀부터 지난 설교까지 모두 제공됩니다."},
            {"성가대 단원 모집 공고", "성가대에서 함께 찬양할 새 단원을 모집합니다. 음악적 재능보다 헌신하는 마음이 중요합니다. 관심 있는 분들의 많은 지원 바랍니다."},
            {"교회 주차 안내", "주일 예배 시 주차 공간이 협소하오니 가급적 대중교통 이용을 권장드립니다. 부득이한 경우 인근 공영주차장을 이용해 주시기 바랍니다."},
            {"새벽기도회 안내", "매일 새벽 5시 30분 새벽기도회가 진행됩니다. 하루를 말씀과 기도로 시작하며 영적 충전의 시간을 가져보세요. 온라인 중계도 제공됩니다."},
            {"감사 헌금 및 후원 안내", "교회의 다양한 사역을 위한 후원에 감사드립니다. 투명한 운영을 약속드리며, 자세한 내용은 사무실로 문의해 주세요."},
    };

    /** Seeds published notice/announcement posts so the public site has text content. */
    private void seedPosts() {
        if (postRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Post> posts = new ArrayList<>();
        for (int i = 0; i < POSTS.length; i++) {
            posts.add(Post.builder()
                    .title(POSTS[i][0])
                    .body(POSTS[i][1])
                    .status(PostStatus.PUBLISHED)
                    .createdAt(now.minusDays(i).minusHours(i % 12))
                    .build());
        }
        postRepository.saveAll(posts);
        log.info("Seeded {} posts", posts.size());
    }
}
