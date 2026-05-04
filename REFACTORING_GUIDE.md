# Politics-Match 프로젝트 리팩토링 완료 문서

## 📋 개요
politics-match 프로젝트의 데이터 수집 및 업데이트 구조를 리팩토링했습니다.
- **초기 데이터 로드**: CSV 파일 기반 (일회성)
- **주기적 업데이트**: Spring Scheduler 기반 (자동)

---

## 🏗 리팩토링 전후 비교

### 이전 구조
```
PoliticsMatchApplication (메인 클래스)
└── DataInitializer (일회성 초기화)
    ├── PoliticianDataService (API 호출)
    ├── VoteExcelService (CSV 파일 로드)
    └── VoteDataService (사용되지 않음 - 주석처리)
```

**문제점:**
- ❌ 주기적 업데이트 메커니즘 없음
- ❌ API 호출 방식이 미사용
- ❌ CSV 파일에만 의존
- ❌ 초기화와 업데이트 로직이 분리 안 됨

---

### 현재 구조 (리팩토링 후)
```
PoliticsMatchApplication (메인 클래스)
├── @EnableScheduling 추가
│
├── DataInitializer (일회성 초기화)
│   ├── 의원 정보 첫 로드 (1회)
│   └── Vote 데이터 CSV 초기 로드 (1회)
│
├── SchedulerConfig (스케줄러 설정)
│   └── RestTemplate Bean 등록
│
└── 스케줄러 작업들
    ├── VoteUpdateService (자동 업데이트)
    │   ├── updateDailyVotes() - 매일 새벽 2시
    │   └── updateWeeklyVotes() - 매주 월요일 새벽 3시
    │
    └── VoteDataService (API 클라이언트)
        ├── fetchBillIds() - 의안 목록 조회
        ├── fetchVoteResults() - 표결 결과 조회
        └── saveVoteResults() - DB 저장
```

**개선사항:**
- ✅ 주기적 자동 업데이트 (매일/매주)
- ✅ API 활용 (CSV + API 병행)
- ✅ 중복 체크 로직 추가
- ✅ 구조 분리 및 책임 명확화
- ✅ 에러 핸들링 강화
- ✅ 설정 외부화 (properties)

---

## 📁 생성/수정된 파일

### 1. 신규 생성
```
src/main/java/com/jgs/politics/global/config/SchedulerConfig.java
src/main/java/com/jgs/politics/domain/vote/VoteUpdateService.java
```

### 2. 수정된 파일
```
src/main/java/com/jgs/politics/PoliticsMatchApplication.java
src/main/java/com/jgs/politics/config/DataInitializer.java
src/main/java/com/jgs/politics/domain/vote/VoteDataService.java
src/main/java/com/jgs/politics/domain/vote/VoteUpdateService.java
src/main/resources/application.properties
```

---

## ⚙️ 주요 설정 (application.properties)

### Vote 업데이트 설정
```properties
# 현재 국회 대수
vote.update.current-age=22

# CSV 파일 경로 (초기 로드 시만 사용)
vote.csv.path=C:\\Users\\SSAFY\\Desktop\\데이터_국회의원 본회의 표결정보.csv

# 일일 업데이트 시간 (매일 새벽 2시)
vote.scheduler.daily.cron=0 0 2 * * *

# 주간 업데이트 시간 (매주 월요일 새벽 3시)
vote.scheduler.weekly.cron=0 0 3 ? * MON

# API 호출 간격 (200ms = 서버 부하 방지)
vote.scheduler.api-delay-ms=200
```

### Cron 표현식 설명
```
분(0-59) 시(0-23) 일(1-31) 월(1-12) 요일(0-6, 0=일요일)

매일 새벽 2시:        0 0 2 * * *
매주 월요일 새벽 3시:  0 0 3 ? * MON
```

---

## 🔄 데이터 흐름

### 1단계: 애플리케이션 시작
```
Spring Boot 시작
  ↓
DataInitializer.run() 실행 (CommandLineRunner)
  ├── 의원 테이블 비어있는지 확인
  │   └── YES: PoliticianDataService.fetchAndSaveAllData()
  │
  └── Vote 테이블 비어있는지 확인
      └── YES: VoteExcelService.importVotesFromCsv()
  
로그: "🚀 [완료] 애플리케이션 초기화가 성공적으로 완료되었습니다."
```

### 2단계: 주기적 자동 업데이트
```
매일 새벽 2시 (일일 업데이트)
  ↓
VoteUpdateService.updateDailyVotes()
  ├── fetchRecentBillIds() - 최근 의안 100건 조회
  └── 각 의안마다:
      ├── 중복 체크 (이미 수집됨?)
      ├── fetchAndSaveVoteResults() - 표결 데이터 수집 및 저장
      └── Thread.sleep(200ms) - API 서버 부하 방지

매주 월요일 새벽 3시 (주간 전수 조사)
  ↓
VoteUpdateService.updateWeeklyVotes()
  ├── fetchAllBillIds() - 전체 의안 조회 (페이지네이션)
  └── 각 의안마다:
      ├── 중복 체크
      ├── fetchAndSaveVoteResults() - 표결 데이터 수집 및 저장
      └── Thread.sleep(200ms)
```

---

## 📊 클래스별 책임

### PoliticsMatchApplication
- ✅ 애플리케이션 진입점
- ✅ @EnableScheduling 활성화

### DataInitializer
- ✅ 초기 데이터 로드만 담당
- ✅ 의원 기본 정보 첫 로드
- ✅ Vote 데이터 CSV 초기 로드
- ❌ 주기적 업데이트는 담당 안 함

### SchedulerConfig
- ✅ Spring Scheduler 설정
- ✅ RestTemplate Bean 등록

### VoteUpdateService
- ✅ 매일/매주 자동 스케줄 작업
- ✅ 의안 목록 조회
- ✅ 표결 데이터 수집 및 저장
- ✅ 중복 체크 및 에러 처리

### VoteDataService
- ✅ API 호출 로직 분리 (재사용성 향상)
- ✅ fetchBillIds() - 의안 목록 조회
- ✅ fetchVoteResults() - 표결 결과 조회
- ✅ saveVoteResults() - DB 저장

### VoteExcelService
- ✅ CSV 파일 파싱
- ✅ 의원 매칭 (monaCd)
- ✅ 배치 처리 (메모리 관리)

---

## 🚀 사용 방법

### 1. 정상 운영 모드
```bash
# 애플리케이션 시작
mvn spring-boot:run

# 자동으로:
# 1. 첫 실행 시: 초기 데이터 로드 (의원 + Vote CSV)
# 2. 매일 새벽 2시: 최근 의안 자동 수집
# 3. 매주 월요일 새벽 3시: 전체 의안 전수 조사
```

### 2. 강제 초기화 (선택사항)
```bash
# Vote 테이블 전체 삭제 후 다시 시작하면
# DataInitializer에서 CSV 파일 재로드
```

### 3. 스케줄러 비활성화 (개발/테스트)
```properties
# application.properties
vote.scheduler.enabled=false
```

---

## 📋 로그 예시

### 애플리케이션 시작
```
▶ [System] 애플리케이션 초기화 프로세스를 시작합니다.
1단계: 의원 기본 정보 DB가 비어있습니다. 초기 로드 중...
✔ 의원 기본 정보 로드 완료.
2단계: Vote 데이터 DB가 비어있습니다. CSV 파일에서 로드 중...
▶ [System] 의원 매칭용 캐시 로드 완료 (267명)
>>> [진행중] 1000건 적재 완료...
🚀 [완료] 총 300000건의 데이터 전수 적재가 성공적으로 끝났습니다!
🚀 [완료] 애플리케이션 초기화가 성공적으로 완료되었습니다.
   - 주기적 Vote 업데이트: 매일 새벽 2시 (일일) + 매주 월요일 새벽 3시 (주간)
```

### 매일 새벽 2시 (자동 실행)
```
▶ [스케줄러 실행] 일일 표결 데이터 업데이트 시작 (시각: 2026-04-30 02:00:00)
>>> 총 87개의 의안 발견
>>> 의안: BILL_ID_1 - 304건 저장 완료
>>> 의안: BILL_ID_2 - 287건 저장 완료
...
✔ [완료] 87건 중 82건의 의안 데이터 수집 완료
```

---

## ⚠️ 주의사항

### 1. CSV 파일 경로
```properties
# Windows 경로 (현재)
vote.csv.path=C:\\Users\\SSAFY\\Desktop\\데이터_국회의원 본회의 표결정보.csv

# Linux 경로로 변경 필요 시
vote.csv.path=/home/user/data/vote_data.csv
```

### 2. API 서버 부하
- 200ms 간격으로 API 호출 (설정 가능)
- 과도한 호출 시 응답 거부될 수 있음

### 3. 데이터베이스 성능
- 대량 데이터 삽입 시 배치 처리 (1000건 단위)
- 일시적 성능 저하 가능

### 4. 시스템 시간
- 스케줄러는 서버 시스템 시간 기준
- 시간대 설정 확인 필요 (Asia/Seoul)

---

## 🔧 향후 개선 사항

1. **VoteUpdateService를 설정값 기반으로 활성화/비활성화**
   ```java
   @Conditional(SchedulerEnabledCondition.class)
   ```

2. **동적 Cron 표현식 변경**
   ```java
   ScheduledTaskRegistrar.addTriggerTask()
   ```

3. **스케줄 실행 이력 저장**
   ```java
   ScheduleLog 엔티티 추가
   ```

4. **모니터링 및 알림**
   ```java
   실패 시 이메일/Slack 알림
   ```

5. **성능 최적화**
   ```java
   배치 처리 크기 동적 조정
   캐싱 레이어 추가 (Redis)
   ```

---

## 📞 문의 및 지원

- 에러 발생 시: 로그 파일 확인
- 스케줄러 미실행 시: @EnableScheduling 확인
- API 오류 시: 네트워크 연결 및 API KEY 확인

---

**마지막 수정**: 2026-04-30
**버전**: 1.0 (리팩토링 완료)
