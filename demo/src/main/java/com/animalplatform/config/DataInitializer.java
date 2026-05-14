package com.animalplatform.config;

import com.animalplatform.adoption.domain.AdoptionApplication;
import com.animalplatform.adoption.domain.AdoptionApplicationRepository;
import com.animalplatform.adoption.domain.AdoptionApplicationStatus;
import com.animalplatform.adoption.domain.HousingType;
import com.animalplatform.campaign.domain.DonationCampaign;
import com.animalplatform.campaign.domain.DonationCampaignRepository;
import com.animalplatform.chat.domain.ChatQuickAnswer;
import com.animalplatform.chat.domain.ChatQuickAnswerRepository;
import com.animalplatform.donation.domain.Donation;
import com.animalplatform.donation.domain.DonationRepository;
import com.animalplatform.donation.domain.DonationStatus;
import com.animalplatform.donation.domain.DonationTargetType;
import com.animalplatform.donation.domain.DonationType;
import com.animalplatform.favorite.domain.UserFavorite;
import com.animalplatform.favorite.domain.UserFavoriteRepository;
import com.animalplatform.shelter.domain.Shelter;
import com.animalplatform.shelter.domain.ShelterRepository;
import com.animalplatform.statistics.domain.BatchExecutionLog;
import com.animalplatform.statistics.domain.BatchExecutionLogRepository;
import com.animalplatform.statistics.domain.BatchExecutionStatus;
import com.animalplatform.story.domain.AdoptionStory;
import com.animalplatform.story.domain.AdoptionStoryRepository;
import com.animalplatform.user.domain.User;
import com.animalplatform.user.domain.UserRepository;
import com.animalplatform.user.domain.UserRole;
import com.animalplatform.user.domain.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class DataInitializer implements ApplicationRunner {

    public static final String DEMO_ADMIN_EMAIL = "admin@dasigajok.com";
    public static final String DEMO_ADMIN_PASSWORD = "admin1234";
    public static final String DEMO_USER_EMAIL = "user@dasigajok.com";
    public static final String DEMO_USER_PASSWORD = "user1234";

    private static final String DEMO_SHELTER_CODE = "YANGSAN-DEMO-SHELTER";
    private static final String DEMO_SHELTER_NAME = "양산시 동물보호센터";

    private static final List<String> SAMPLE_ANIMAL_NOS = List.of(
            "448548202600395",
            "450650202600876",
            "411000202400001"
    );

    private final UserRepository userRepository;
    private final ShelterRepository shelterRepository;
    private final DonationRepository donationRepository;
    private final UserFavoriteRepository userFavoriteRepository;
    private final AdoptionApplicationRepository adoptionApplicationRepository;
    private final AdoptionStoryRepository adoptionStoryRepository;
    private final DonationCampaignRepository donationCampaignRepository;
    private final BatchExecutionLogRepository batchExecutionLogRepository;
    private final ChatQuickAnswerRepository chatQuickAnswerRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            ShelterRepository shelterRepository,
            DonationRepository donationRepository,
            UserFavoriteRepository userFavoriteRepository,
            AdoptionApplicationRepository adoptionApplicationRepository,
            AdoptionStoryRepository adoptionStoryRepository,
            DonationCampaignRepository donationCampaignRepository,
            BatchExecutionLogRepository batchExecutionLogRepository,
            ChatQuickAnswerRepository chatQuickAnswerRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.shelterRepository = shelterRepository;
        this.donationRepository = donationRepository;
        this.userFavoriteRepository = userFavoriteRepository;
        this.adoptionApplicationRepository = adoptionApplicationRepository;
        this.adoptionStoryRepository = adoptionStoryRepository;
        this.donationCampaignRepository = donationCampaignRepository;
        this.batchExecutionLogRepository = batchExecutionLogRepository;
        this.chatQuickAnswerRepository = chatQuickAnswerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (isMavenTestRun()) {
            return;
        }

        seedAdminUser();
        User demoUser = seedDemoUser();
        Shelter demoShelter = seedDemoShelter();

        seedFavorites(demoUser);
        seedDonations(demoUser, demoShelter);
        seedAdoptionApplications(demoUser);
        seedStories(demoUser);
        seedDonationCampaigns();
        seedBatchHistory();
        seedChatQuickAnswers();
    }

    private boolean isMavenTestRun() {
        return System.getProperty("surefire.test.class.path") != null;
    }

    private User seedAdminUser() {
        if (!userRepository.existsByEmail(DEMO_ADMIN_EMAIL)) {
            return userRepository.save(new User(
                    UserRole.ADMIN,
                    DEMO_ADMIN_EMAIL,
                    DEMO_ADMIN_EMAIL,
                    passwordEncoder.encode(DEMO_ADMIN_PASSWORD),
                    "관리자",
                    "010-0000-0000",
                    UserStatus.ACTIVE
            ));
        }
        return userRepository.findByEmail(DEMO_ADMIN_EMAIL)
                .orElseThrow(() -> new IllegalStateException("Admin account exists but cannot be loaded by email."));
    }

    private User seedDemoUser() {
        return userRepository.findByUsername(DEMO_USER_EMAIL)
                .orElseGet(() -> userRepository.save(new User(
                        UserRole.MEMBER,
                        DEMO_USER_EMAIL,
                        DEMO_USER_EMAIL,
                        passwordEncoder.encode(DEMO_USER_PASSWORD),
                        "일반사용자",
                        "010-1234-5678",
                        UserStatus.ACTIVE
                )));
    }

    private Shelter seedDemoShelter() {
        return shelterRepository.findAll()
                .stream()
                .filter(shelter -> DEMO_SHELTER_CODE.equals(shelter.getExternalShelterCode()))
                .findFirst()
                .orElseGet(() -> shelterRepository.save(new Shelter(
                        DEMO_SHELTER_CODE,
                        DEMO_SHELTER_NAME,
                        "055-392-5631",
                        "경상남도 양산시 동면 석산리 392",
                        "GYEONGNAM",
                        "https://www.yangsan.go.kr",
                        "발표 시연을 위한 보호소 샘플 데이터입니다."
                )));
    }

    private void seedFavorites(User user) {
        for (String animalNo : SAMPLE_ANIMAL_NOS) {
            if (!userFavoriteRepository.existsByUserIdAndAnimalNo(user.getId(), animalNo)) {
                userFavoriteRepository.save(new UserFavorite(user.getId(), animalNo));
            }
        }
    }

    private void seedDonations(User user, Shelter shelter) {
        seedDonation(user, shelter, 10_000, DonationType.ONE_TIME, "demo-paid-응원합니다-001", 21);
        seedDonation(user, shelter, 30_000, DonationType.ONE_TIME, "demo-paid-힘내세요-002", 12);
        seedDonation(user, shelter, 50_000, DonationType.SUBSCRIPTION, "demo-paid-건강하게 자라렴-003", 3);
        seedDonation(user, shelter, 100_000, DonationType.ONE_TIME, "demo-paid-의료비응원-004", 1);
        seedDonation(user, shelter, 20_000, DonationType.ONE_TIME, "demo-paid-오늘후원-005", 0);
    }

    private void seedDonation(User user, Shelter shelter, long amount, DonationType type, String paymentKey, int daysAgo) {
        if (donationRepository.existsByPaymentKey(paymentKey)) {
            return;
        }
        donationRepository.save(new Donation(
                user,
                shelter,
                null,
                DonationTargetType.SHELTER,
                type,
                BigDecimal.valueOf(amount),
                DonationStatus.PAID,
                paymentKey,
                LocalDateTime.now().minusDays(daysAgo)
        ));
    }

    private void seedAdoptionApplications(User user) {
        seedAdoptionApplication(user,
                SAMPLE_ANIMAL_NOS.get(0),
                "김민준",
                "010-1111-2222",
                DEMO_USER_EMAIL,
                "서울특별시 마포구 월드컵로 10",
                HousingType.APARTMENT,
                true,
                "오래전부터 강아지를 키우고 싶었습니다",
                AdoptionApplicationStatus.PENDING
        );
        seedAdoptionApplication(user,
                SAMPLE_ANIMAL_NOS.get(1),
                "이서연",
                "010-3333-4444",
                DEMO_USER_EMAIL,
                "경기도 성남시 분당구 판교로 20",
                HousingType.HOUSE,
                true,
                "오래전부터 강아지를 키우고 싶었습니다",
                AdoptionApplicationStatus.APPROVED
        );
        seedAdoptionApplication(user,
                SAMPLE_ANIMAL_NOS.get(2),
                "박지훈",
                "010-5555-6666",
                DEMO_USER_EMAIL,
                "부산광역시 해운대구 센텀중앙로 30",
                HousingType.APARTMENT,
                false,
                "가족들과 충분히 상의했고 책임감 있게 돌보고 싶습니다.",
                AdoptionApplicationStatus.REJECTED
        );
        seedAdoptionApplication(user,
                "441393202600407",
                "최유나",
                "010-7777-8888",
                DEMO_USER_EMAIL,
                "경기도 안산시 상록구 청곡길 50",
                HousingType.HOUSE,
                true,
                "마당이 있는 집에서 오래 기다린 아이에게 안정적인 환경을 주고 싶습니다.",
                AdoptionApplicationStatus.PENDING
        );
    }

    private void seedAdoptionApplication(
            User user,
            String animalNo,
            String applicantName,
            String applicantPhone,
            String applicantEmail,
            String address,
            HousingType housingType,
            boolean hasExperience,
            String reason,
            AdoptionApplicationStatus status
    ) {
        boolean exists = adoptionApplicationRepository.findByUserIdOrderByAppliedAtDesc(user.getId())
                .stream()
                .anyMatch(application -> animalNo.equals(application.getAnimalNo()) && applicantName.equals(application.getApplicantName()));
        if (exists) {
            return;
        }
        AdoptionApplication application = new AdoptionApplication(
                animalNo,
                applicantName,
                applicantPhone,
                applicantEmail,
                address,
                housingType,
                hasExperience,
                reason,
                user.getId()
        );
        application.changeStatus(status);
        adoptionApplicationRepository.save(application);
    }

    private void seedStories(User user) {
        if (adoptionStoryRepository.count() > 0) {
            return;
        }

        adoptionStoryRepository.save(new AdoptionStory(
                user.getId(),
                user.getName(),
                SAMPLE_ANIMAL_NOS.get(0),
                "드디어 가족이 생겼어요 🐾",
                "처음엔 낯을 많이 가렸는데 이제는 제 발을 졸졸 따라다녀요.",
                null
        ));
        adoptionStoryRepository.save(new AdoptionStory(
                user.getId(),
                user.getName(),
                SAMPLE_ANIMAL_NOS.get(1),
                "작은 발걸음이 집 안을 채웠어요 🐾",
                "처음엔 낯을 많이 가렸는데 이제는 제 발을 졸졸 따라다녀요.",
                null
        ));
    }

    private void seedDonationCampaigns() {
        seedDonationCampaign("사료 후원", "배고픈 아이들에게 따뜻한 한 끼를", "/src/assets/campaign1.png", 2_000_000L, 1_250_000L, 342);
        seedDonationCampaign("의료비 후원", "아픈 아이들이 건강하게 자랄 수 있도록", "/src/assets/campaign2.png", 1_500_000L, 890_000L, 215);
        seedDonationCampaign("보호소 운영", "안전한 보금자리를 지켜주세요", "/src/assets/campaign3.png", 5_000_000L, 3_200_000L, 891);
        seedDonationCampaign("입양 지원", "새로운 가족을 만나는 순간", "/src/assets/campaign4.png", 1_000_000L, 450_000L, 128);
    }

    private void seedDonationCampaign(
            String category,
            String title,
            String imageUrl,
            Long goalAmount,
            Long currentAmount,
            int participants
    ) {
        if (donationCampaignRepository.existsByTitle(title)) {
            return;
        }
        donationCampaignRepository.save(new DonationCampaign(
                category,
                title,
                imageUrl,
                goalAmount,
                currentAmount,
                participants,
                true,
                null
        ));
    }

    private void seedBatchHistory() {
        if (batchExecutionLogRepository.count() > 0) {
            return;
        }

        seedBatchExecution(1, BatchExecutionStatus.SUCCESS, 7_118, 7_118, 0, "");
        seedBatchExecution(2, BatchExecutionStatus.PARTIAL, 6_842, 6_790, 3, "12, 18, 27페이지 수집 재시도 실패");
        seedBatchExecution(3, BatchExecutionStatus.SUCCESS, 7_003, 6_998, 0, "");
        seedBatchExecution(4, BatchExecutionStatus.FAILED, 0, 0, 18, "공공 API 일시 장애로 배치 실패");
    }

    private void seedBatchExecution(
            int daysAgo,
            BatchExecutionStatus status,
            int totalFetched,
            int totalUpserted,
            int totalErrors,
            String errorSummary
    ) {
        BatchExecutionLog executionLog = new BatchExecutionLog(LocalDateTime.now().minusDays(daysAgo).minusMinutes(8));
        executionLog.complete(status, totalFetched, totalUpserted, totalErrors, errorSummary);
        batchExecutionLogRepository.save(executionLog);
    }

    private void seedChatQuickAnswers() {
        seedChatQuickAnswer(
                "입양 전 체크리스트",
                "입양 전 체크리스트",
                """
                        입양 전 꼭 확인해야 할 체크리스트예요! 🐾

                        ✅ 거주 환경 확인
                        - 반려동물 허용 주택인지 확인
                        - 충분한 공간이 있는지 확인
                        - 탈출 방지 안전장치 설치

                        ✅ 가족 동의
                        - 모든 가족 구성원의 동의 확인
                        - 알레르기 여부 확인
                        - 아이/노인 있을 경우 적합한 동물 선택

                        ✅ 경제적 준비
                        - 월 평균 사료비: 3~10만원
                        - 정기 건강검진: 연 1~2회
                        - 예방접종/중성화 비용 준비

                        ✅ 시간적 여유
                        - 하루 최소 1~2시간 함께하는 시간
                        - 산책 가능 여부 (강아지의 경우)
                        - 출장/여행 시 돌봄 계획

                        ✅ 용품 준비
                        - 사료, 물그릇, 밥그릇
                        - 이동장, 목줄/하네스
                        - 화장실 용품 (고양이의 경우)
                        - 장난감, 스크래처

                        모두 준비됐다면 입양할 준비가 된 거예요! 💛
                        """,
                1
        );
        seedChatQuickAnswer(
                "강아지 식단 가이드",
                "강아지 식단 가이드",
                """
                        강아지 올바른 식단 가이드예요! 🐶

                        🍚 기본 식사
                        - 나이/체중에 맞는 사료 선택
                        - 하루 2~3회 규칙적으로
                        - 신선한 물은 항상 제공

                        📏 급여량 기준
                        - 소형견(~10kg): 하루 80~150g
                        - 중형견(10~25kg): 하루 150~300g
                        - 대형견(25kg~): 하루 300~500g
                        ※ 사료 포장지 권장량 참고

                        ✅ 먹어도 되는 음식
                        - 삶은 닭가슴살, 소고기
                        - 당근, 브로콜리, 고구마
                        - 블루베리, 사과(씨 제거)

                        ❌ 절대 금지 음식
                        - 초콜릿, 포도, 건포도
                        - 양파, 마늘, 파
                        - 자일리톨 (껌, 사탕)
                        - 카페인, 알코올
                        - 날달걀, 생뼈

                        💊 영양 보충
                        - 오메가3 (피부/털 건강)
                        - 관절 영양제 (대형견, 노령견)
                        - 프로바이오틱스 (장 건강)
                        """,
                2
        );
        seedChatQuickAnswer(
                "산책 꿀팁 알려줘",
                "산책 꿀팁 알려줘",
                """
                        강아지 산책 꿀팁을 알려드릴게요! 🦮

                        ⏰ 산책 시간
                        - 소형견: 하루 30분~1시간
                        - 중형견: 하루 1~2시간
                        - 대형견: 하루 2시간 이상
                        - 아침/저녁 하루 2회 권장

                        🌡️ 날씨별 주의사항
                        - 여름: 오전 7시 이전, 오후 7시 이후
                          (아스팔트 온도 체크 필수!)
                        - 겨울: 방한 옷 착용, 짧게 자주
                        - 비오는 날: 발바닥 꼭 닦아주기

                        🎯 산책 매너
                        - 목줄/하네스 필수 착용
                        - 배변봉투 항상 지참
                        - 낯선 개와 인사 시 보호자 확인
                        - 공공장소 에티켓 지키기

                        💡 산책 꿀팁
                        - 새로운 냄새 맡기 허용 (정신적 자극)
                        - 간식으로 올바른 행동 강화
                        - 일정한 시간대 유지
                        - 산책 후 발바닥 체크 (상처, 이물질)

                        🚫 산책 중 주의
                        - 쓰레기, 음식물 먹지 않게
                        - 독성 식물 조심
                        - 과도한 운동은 관절에 무리
                        """,
                3
        );
    }

    private void seedChatQuickAnswer(String buttonLabel, String triggerKeyword, String answerText, int displayOrder) {
        if (chatQuickAnswerRepository.existsByTriggerKeyword(triggerKeyword)) {
            return;
        }
        chatQuickAnswerRepository.save(new ChatQuickAnswer(
                buttonLabel,
                triggerKeyword,
                answerText.strip(),
                displayOrder,
                true
        ));
    }
}
