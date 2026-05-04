# Politics-Match 아키텍처 가이드

## 📐 전체 시스템 아키텍처

### 데이터 흐름 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────────────┐         ┌──────────────────────┐   │
│  │ DataInitializer       │         │ SchedulerConfig      │   │
│  │ (구동 시 1회)          │         │ @EnableScheduling    │   │
│  └───────┬───────────────┘         └──────────┬───────────┘   │
│          │                                    │                │
│          ├─→ CSV 파일로드                      └─→ RestTemplate │
│          │    (첫 실행 시만)                        Bean 등록   │
│          │                                         │            │
│          └─→ Politician 데이터 로드              │            │
│               (API 기반)                        │            │
│                                               │            │
└───────────────────────────────────────────┼────────────────┘   
                                            │
                 ┌──────────────────────────┴──────────────────┐
                 │                                             │
            ┌────▼────┐                              ┌────────▼────┐
            │ Vote    │                              │ Vote        │
            │ Excel   │                              │ Update      │
            │ Service │                              │ Service     │
            │ (CSV)   │                              │ (Scheduler) │
            └─────────┘                              └────────┬────┘
                 │                                          │
                 │                          ┌───────────────┴──────────────┐
                 │                          │                              │
                 │                    ┌─────▼────────┐          ┌─────────▼──┐
                 │                    │ Daily        │          │ Weekly     │
                 │                    │ Update       │          │ Update     │
                 │                    │ (매일 2시)    │          │ (월 3시)   │
                 │                    └─────┬────────┘          └─────┬──────┘
                 │                          │                        │
                 │                    ┌─────▼────────────────────────▼──┐
                 │                    │ Vote Data Service             │
                 │                    │ - fetchBillIds()              │
                 │                    │ - fetchVoteResults()          │
                 │                    │ - saveVoteResults()           │
                 │                    └────────┬─────────────────────┘
                 │                             │
                 └─────────────────┬───────────┘
                                   │
                         ┌─────────▼──────────┐
                         │  Vote History DB   │
                         │  (VoteHistory 테이블)│
                         └────────────────────┘
```

---

## 🗂 디렉토리 구조

```
src/main/java/com/jgs/politics/
│
├── PoliticsMatchApplication.java ⭐ 메인 클래스
│   └── @EnableScheduling 추가됨
│
├── config/
│   └── DataInitializer.java ⭐ 초기 데이터 로드 (1회)
│       ├── PoliticianDataService 호출
│       └── VoteExcelService 호출
│
├── global/
│   └── config/
│       ├── SchedulerConfig.java ⭐ 스케줄러 설정
│       │   └── RestTemplate Bean
│       └── [other configs]
│
├── domain/
│   ├── politician/
│   │   ├── Politician.java (엔티티)
│   │   ├── PoliticianRepository.java
│   │   ├── PoliticianDataService.java (API → DB)
│   │   ├── PoliticianService.java (비즈니스 로직)
│   │   ├── PoliticianRestController.java
│   │   └── dto/
│   │       └── PoliticianSummaryDTO.java
│   │
│   └── vote/
│       ├── VoteHistory.java (엔티티)
│       ├── VoteHistoryRepository.java
│       ├── VoteExcelService.java (CSV → DB)
│       ├── VoteDataService.java ⭐ API 클라이언트 (새로 정리됨)
│       ├── VoteUpdateService.java ⭐ 스케줄러 (새로 생성됨)
│       ├── PoliticianStatService.java
│       └── [controllers]
│
└── infra/
    └── assembly/
        └── ...
```

---

## 🔄 데이터 흐름 시나리오

### 시나리오 1: 애플리케이션 시작 (초기화)

```
1. Spring Boot 시작
   ↓
2. PoliticsMatchApplication.main() 실행
   └── @EnableScheduling 활성화
   ↓
3. DataInitializer.run() 자동 실행 (CommandLineRunner)
   ├─ 의원 테이블 확인
   │  ├─ 비어있음? → PoliticianDataService.fetchAndSaveAllData()
   │  │            ├─ 국회의원 API 호출
   │  │            ├─ 22대만 필터링
   │  │            └─ DB 저장
   │  └─ 이미 있음? → 스킵
   │
   └─ Vote 테이블 확인
      ├─ 비어있음? → VoteExcelService.importVotesFromCsv()
      │            ├─ CSV 파일 읽기
      │            ├─ 의원 매칭 (Politician 조인)
      │            ├─ 배치 처리 (1000건 단위)
      │            └─ DB 저장 (30만건)
      └─ 이미 있음? → 스킵
   ↓
4. 초기화 완료
   └─ "🚀 애플리케이션 초기화가 성공적으로 완료되었습니다."
```

### 시나리오 2: 매일 새벽 2시 (자동)

```
Spring Scheduler 트리거 (Cron: 0 0 2 * * *)
   ↓
VoteUpdateService.updateDailyVotes()
   ├─ fetchRecentBillIds(currentAge) 호출
   │  ├─ 국회의원 API: TVBPMBILL11 (최근 100건)
   │  └─ 본회의 처리 완료된 의안만 추출
   ↓
87개 의안 발견
   ├─ 각 의안마다:
   │  ├─ 중복 체크 (voteRepository.existsByBillId)
   │  │  ├─ 이미 있음? → 스킵
   │  │  └─ 없음? → 수집
   │  │
   │  ├─ fetchAndSaveVoteResults() 호출
   │  │  ├─ VoteDataService 호출
   │  │  ├─ 표결 API: nojepdqqaweusdfbi
   │  │  ├─ 의원별 찬반 데이터 추출
   │  │  └─ DB 저장 (배치)
   │  │
   │  └─ Thread.sleep(200ms) (API 부하 방지)
   │
   └─ 완료: "✔ [완료] 87건 중 82건의 의안 데이터 수집 완료"
```

### 시나리오 3: 매주 월요일 새벽 3시 (자동)

```
Spring Scheduler 트리거 (Cron: 0 0 3 ? * MON)
   ↓
VoteUpdateService.updateWeeklyVotes()
   ├─ fetchAllBillIds() 호출 (페이지네이션)
   │  ├─ 1페이지 → 2페이지 → 3페이지 ... (반복)
   │  └─ 모든 페이지의 의안 수집
   ↓
약 1,000+ 개 의안 발견
   ├─ 각 의안마다:
   │  ├─ 중복 체크
   │  ├─ 없는 의안만 수집
   │  └─ fetchAndSaveVoteResults() 호출
   │
   └─ 완료: "✔ [주간 완료] 1000건 중 150건의 신규 의안 데이터 수집"
```

---

## 📋 API 호출 구조

### Vote 관련 API

```
1. 의안 목록 조회 API
   URL: https://open.assembly.go.kr/portal/openapi/TVBPMBILL11
   파라미터:
   - KEY: API 키
   - Type: json
   - pIndex: 페이지 번호
   - pSize: 페이지 크기 (보통 100)
   - AGE: 국회 대수 (22)
   
   응답:
   {
     "TVBPMBILL11": [
       {...},
       {
         "row": [
           {"BILL_ID": "...", "PROC_RESULT": "가결", ...},
           ...
         ]
       }
     ]
   }

2. 표결 상세 조회 API
   URL: https://open.assembly.go.kr/portal/openapi/nojepdqqaweusdfbi
   파라미터:
   - KEY: API 키
   - Type: json
   - pIndex: 1 (보통 고정)
   - pSize: 500 (의원 수보다 많게)
   - AGE: 국회 대수 (22)
   - BILL_ID: 의안 ID
   
   응답:
   {
     "nojepdqqaweusdfbi": [
       {...},
       {
         "row": [
           {
             "HG_NM": "의원명",
             "RESULT_VOTE_MOD": "찬성",
             "BILL_ID": "...",
             "BILL_NAME": "의안명",
             "POLY_NM": "정당",
             "MONA_CD": "의원코드"
           },
           ...
         ]
       }
     ]
   }
```

---

## 🔧 설정 항목 상세

### application.properties

```properties
# ========== Vote 업데이트 설정 ==========

# 1. 현재 국회 대수 (숫자)
vote.update.current-age=22

# 2. CSV 파일 경로 (초기 로드 시만 사용)
#    - 첫 실행 시 Vote 테이블이 비어있으면 이 경로에서 CSV 읽음
#    - 그 이후는 스케줄러에서 API 호출
vote.csv.path=C:\\Users\\SSAFY\\Desktop\\데이터_국회의원 본회의 표결정보.csv

# 3. 스케줄러 활성화/비활성화
vote.scheduler.enabled=true  # 개발: false로 변경

# 4. 일일 업데이트 Cron
#    기본값: 0 0 2 * * * (매일 새벽 2시)
#    테스트: 0 */5 * * * * (매 5분마다)
vote.scheduler.daily.cron=0 0 2 * * *

# 5. 주간 업데이트 Cron
#    기본값: 0 0 3 ? * MON (매주 월요일 새벽 3시)
#    테스트: 0 */10 * * * * (매 10분마다)
vote.scheduler.weekly.cron=0 0 3 ? * MON

# 6. API 호출 간격 (밀리초)
#    200ms = 초당 5건 호출 (API 서버 부하 고려)
vote.scheduler.api-delay-ms=200

# ========== 로깅 설정 ==========
logging.level.com.jgs.politics=DEBUG
logging.level.org.springframework.scheduling=INFO
```

---

## 💾 데이터베이스 스키마

### vote_history 테이블

```sql
CREATE TABLE vote_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  age VARCHAR(10),                  -- 국회 대수 (22, 21, ...)
  bill_id VARCHAR(50),              -- 의안 ID
  bill_name VARCHAR(255),           -- 의안명
  vote_date VARCHAR(10),            -- 표결 날짜 (YYYY-MM-DD)
  hg_nm VARCHAR(50),                -- 의원명
  hg_nm_hanja VARCHAR(100),         -- 의원명 (한자)
  poly_nm VARCHAR(50),              -- 정당명
  result_vote VARCHAR(20),          -- 표결 결과 (찬성/반대/기권)
  mona_cd VARCHAR(50),              -- 의원 코드
  
  UNIQUE KEY unique_vote (bill_id, mona_cd),
  INDEX idx_bill_id (bill_id),
  INDEX idx_mona_cd (mona_cd),
  INDEX idx_poly_nm (poly_nm)
);
```

---

## 🧪 테스트 방법

### 1. 초기화 테스트
```bash
# 1. Vote 테이블 삭제
mysql> DELETE FROM vote_history;

# 2. 애플리케이션 시작
mvn spring-boot:run

# 3. 로그 확인
# "🚀 [완료] 총 300000건의 데이터 전수 적재가 성공적으로 끝났습니다!"
```

### 2. 스케줄러 테스트 (빠른 주기)
```properties
# application.properties에서 Cron을 매 1분으로 변경
vote.scheduler.daily.cron=0 * * * * *   # 매 분
vote.scheduler.weekly.cron=0 * * * * *  # 매 분

# 로그 확인
# "▶ [스케줄러 실행] 일일 표결 데이터 업데이트 시작"
```

### 3. API 호출 검증
```java
// 임시 테스트 코드
@Test
public void testVoteDataService() {
    List<String> billIds = voteDataService.fetchBillIds("22", 1, 100);
    assert !billIds.isEmpty();
    
    List<VoteHistory> votes = voteDataService.fetchVoteResults("22", billIds.get(0));
    assert !votes.isEmpty();
}
```

---

## 📊 성능 지표

### 초기 로드 (CSV)
- 데이터량: 30만건
- 소요 시간: 약 5-10분
- 배치 크기: 1000건
- 메모리: 안정적 (EntityManager.clear 사용)

### 일일 업데이트
- 대상: 최근 의안 ~100건
- 소요 시간: 약 30-60초
- API 호출: 100회
- 저장: ~20,000-30,000건

### 주간 업데이트
- 대상: 전체 의안 ~1,000건
- 소요 시간: 약 5-10분
- API 호출: 1,000회
- 저장: 신규 의안만 (~5,000-10,000건)

---

## ⚠️ 문제 해결

### 1. 스케줄러가 실행되지 않음
```
✗ 원인: @EnableScheduling 누락
✓ 해결: PoliticsMatchApplication에 @EnableScheduling 추가

✗ 원인: application.properties에서 비활성화
✓ 해결: vote.scheduler.enabled=true 확인
```

### 2. CSV 파일을 찾을 수 없음
```
✗ 원인: 경로 오류 또는 파일 부재
✓ 해결: application.properties의 vote.csv.path 확인
        파일 인코딩: UTF-8 필수
```

### 3. API 호출 실패
```
✗ 원인: 네트워크 오류 또는 API 키 만료
✓ 해결: application.properties의 na.api.key 확인
        https://open.assembly.go.kr/ 에서 API 키 재발급
```

### 4. 메모리 부족
```
✗ 원인: 배치 처리 미사용
✓ 해결: VoteExcelService.importVotesFromCsv에서
        entityManager.clear() 확인
```

---

**최종 작성**: 2026-04-30
**버전**: 1.0
