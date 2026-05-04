# Politics-Match 개발 로드맵

## 📍 현재 상태
- ✅ DB 구조 완성 (의원 + Vote 데이터)
- ✅ 데이터 수집 자동화 (CSV + API 스케줄러)
- ❌ 데이터 분석 및 점수 계산
- ❌ 사용자 입력 시스템
- ❌ 매칭 알고리즘
- ❌ 시각화

---

## 🎯 최종 목표

### 목표 1: 정치적 선호 기반 의원 매칭
> 사용자가 정치적 선호도를 입력 → 가장 잘 매치되는 의원 추천

**필요 기능:**
- 사용자 정치적 성향 입력 (설문/퀵 입력)
- 의원별 정치적 스펙트럼 계산
- 매칭 점수 계산 (코사인 유사도 등)
- 상위 매칭 의원 추천

### 목표 2: 의원 정치적 성향 시각화
> 의원의 찬성/반대 입장을 시각적으로 표현

**필요 기능:**
- 안건 카테고리별 입장 분포
- 의원별 정당별 차이 분석
- 정치 스펙트럼 그래프

---

## 🛣 단계별 구현 계획

### Phase 1: 데이터 분석 (1-2주)

#### 1.1 Vote 데이터 기반 의원 프로필 생성
```
의원별 정치적 스펙트럼 계산:
- 찬성/반대/기권 비율
- 안건 카테고리별 입장 (경제, 환경, 외교 등)
- 정당과의 일관성 정도
```

**구현 위치:**
```
src/main/java/com/jgs/politics/domain/vote/
├── PoliticianProfileService.java (새로 생성)
│   ├── calculatePoliticianProfile() - 의원 프로필 생성
│   ├── getVoteStatistics() - 찬반 통계
│   └── getBillCategoryStats() - 카테고리별 통계
│
└── BillCategoryService.java (새로 생성)
    ├── classifyBill() - 안건 분류
    └── getCategoryStatistics() - 카테고리별 통계
```

#### 1.2 안건 카테고리화
```
안건을 다음 카테고리로 분류:
- 경제/재정
- 환경/기후
- 보건/의료
- 교육
- 외교/국방
- 사회/복지
- 기타
```

**구현:**
```java
// Bill 엔티티에 카테고리 추가
@Entity
public class Bill {
    private String billId;
    private String billName;
    private String category;      // ← 추가
    private String description;   // ← 추가
}

// 키워드 기반 자동 분류
public String classifyBill(String billName) {
    if (billName.contains("세금") || billName.contains("예산")) return "ECONOMY";
    if (billName.contains("환경") || billName.contains("탄소")) return "ENVIRONMENT";
    // ... 등등
}
```

**DB 변경:**
```sql
CREATE TABLE bill (
    id BIGINT PRIMARY KEY,
    bill_id VARCHAR(50) UNIQUE,
    bill_name VARCHAR(255),
    category VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP
);

CREATE TABLE vote_statistic (
    id BIGINT PRIMARY KEY,
    mona_cd VARCHAR(50),
    agree_count INT,
    disagree_count INT,
    abstain_count INT,
    category VARCHAR(50),
    created_at TIMESTAMP
);
```

#### 1.3 의원 프로필 테이블 추가
```sql
CREATE TABLE politician_profile (
    mona_cd VARCHAR(50) PRIMARY KEY,
    political_score FLOAT,          -- -100 ~ 100 (진보 ← → 보수)
    consistency_score FLOAT,        -- 정당 일관성 (%)
    economic_score FLOAT,
    environment_score FLOAT,
    health_score FLOAT,
    education_score FLOAT,
    foreign_score FLOAT,
    social_score FLOAT,
    total_votes INT,
    last_updated TIMESTAMP,
    
    FOREIGN KEY (mona_cd) REFERENCES politician(mona_cd)
);
```

---

### Phase 2: 사용자 입력 시스템 (1주)

#### 2.1 사용자 정치 성향 입력 방식 선택

**옵션 A: 빠른 설문 (5-10개 질문)**
```java
@Entity
public class UserProfile {
    @Id
    private Long userId;
    private Float politicalScore;      // -100 ~ 100
    private Float economicScore;
    private Float environmentScore;
    private Float healthScore;
    private Float educationScore;
    private Float foreignScore;
    private Float socialScore;
}

public class UserProfileDTO {
    // 간단한 5개 질문
    // Q1. 경제정책: 좌측(1) ← → 우측(5)
    // Q2. 환경정책: 좌측(1) ← → 우측(5)
    // ...
}
```

**옵션 B: 상세 설문 (20-30개 질문)**
```java
@Entity
public class UserAnswer {
    @Id
    private Long id;
    private Long userId;
    private Long questionId;
    private Integer answerValue;  // 1-5
}

@Entity
public class Question {
    @Id
    private Long id;
    private String questionText;
    private String category;
    private Integer order;
}
```

#### 2.2 사용자 프로필 API 엔드포인트

```java
@RestController
@RequestMapping("/api/users/profile")
public class UserProfileController {
    
    @PostMapping("/create")
    public ResponseEntity<UserProfile> createProfile(
            @RequestBody UserProfileDTO dto) {
        // 빠른 설문으로 프로필 생성
    }
    
    @PostMapping("/survey")
    public ResponseEntity<UserProfile> createFromSurvey(
            @RequestBody List<UserAnswer> answers) {
        // 상세 설문으로 프로필 생성
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfile> getProfile(
            @PathVariable Long userId) {
    }
}
```

#### 2.3 프론트엔드 (HTML/Thymeleaf)

```html
<!-- src/main/resources/templates/user-survey.html -->
<form id="surveyForm">
    <h2>당신의 정치 성향을 알려주세요</h2>
    
    <!-- Q1. 경제정책 -->
    <div class="question">
        <label>Q1. 경제정책: 누진 세제 (좌측) ← → 감세 정책 (우측)</label>
        <input type="range" name="economic" min="1" max="5" value="3">
    </div>
    
    <!-- Q2. 환경정책 -->
    <div class="question">
        <label>Q2. 환경: 강한 규제 (좌측) ← → 산업 우선 (우측)</label>
        <input type="range" name="environment" min="1" max="5" value="3">
    </div>
    
    <!-- ... 등등 ... -->
    
    <button type="submit">매칭 시작</button>
</form>
```

---

### Phase 3: 매칭 알고리즘 (1주)

#### 3.1 유사도 계산 (코사인 유사도)

```java
@Service
public class MatchingService {
    
    /**
     * 사용자와 의원의 유사도 계산 (코사인 유사도)
     * 범위: 0 ~ 1 (1에 가까울수록 유사)
     */
    public double calculateSimilarity(UserProfile user, PoliticianProfile politician) {
        double[] userVector = {
            user.getPoliticalScore(),
            user.getEconomicScore(),
            user.getEnvironmentScore(),
            user.getHealthScore(),
            user.getEducationScore(),
            user.getForeignScore(),
            user.getSocialScore()
        };
        
        double[] politicianVector = {
            politician.getPoliticalScore(),
            politician.getEconomicScore(),
            politician.getEnvironmentScore(),
            politician.getHealthScore(),
            politician.getEducationScore(),
            politician.getForeignScore(),
            politician.getSocialScore()
        };
        
        return cosineSimilarity(userVector, politicianVector);
    }
    
    private double cosineSimilarity(double[] a, double[] b) {
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

#### 3.2 매칭 결과 API

```java
@RestController
@RequestMapping("/api/matching")
public class MatchingController {
    
    @PostMapping("/find-similar")
    public ResponseEntity<List<MatchResult>> findSimilarPoliticians(
            @RequestBody UserProfile userProfile,
            @RequestParam(defaultValue = "10") int topN) {
        
        // 상위 N명의 매칭 의원 반환
        List<MatchResult> results = matchingService.findTopMatches(
            userProfile, topN
        );
        
        return ResponseEntity.ok(results);
    }
}

@Data
public class MatchResult {
    private String monaCd;
    private String hgNm;
    private String polyNm;
    private double similarityScore;    // 0 ~ 100 (%)
    private String photoUrl;
    private String regionName;
}
```

---

### Phase 4: 시각화 (1.5주)

#### 4.1 의원 정치 스펙트럼 시각화

```html
<!-- src/main/resources/templates/politician-detail.html -->
<div id="politicianProfile">
    <h2>{{ politician.hgNm }} - {{ politician.polyNm }}</h2>
    
    <!-- 정치 스펙트럼 (1D) -->
    <div class="spectrum">
        <span>진보</span>
        <div class="spectrum-bar">
            <div class="spectrum-value" 
                 style="left: {{ (politicalScore + 100) / 2 }}%">
                {{ politicalScore }}
            </div>
        </div>
        <span>보수</span>
    </div>
    
    <!-- 정책별 입장 (Radar Chart) -->
    <canvas id="radarChart"></canvas>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script>
        const ctx = document.getElementById('radarChart').getContext('2d');
        const radarChart = new Chart(ctx, {
            type: 'radar',
            data: {
                labels: ['경제', '환경', '보건', '교육', '외교', '사회'],
                datasets: [{
                    label: '{{ politician.hgNm }}',
                    data: [
                        {{ economicScore }},
                        {{ environmentScore }},
                        {{ healthScore }},
                        {{ educationScore }},
                        {{ foreignScore }},
                        {{ socialScore }}
                    ],
                    borderColor: '#4472C4',
                    backgroundColor: 'rgba(68, 114, 196, 0.2)'
                }]
            }
        });
    </script>
    
    <!-- 정당 비교 -->
    <div class="party-comparison">
        <h3>정당 평균과의 비교</h3>
        <div class="comparison-bars">
            <div class="bar">
                <span>{{ politician.hgNm }}</span>
                <div style="width: {{ yourScore }}%"></div>
            </div>
            <div class="bar">
                <span>{{ politician.polyNm }}</span>
                <div style="width: {{ partyAverage }}%"></div>
            </div>
        </div>
    </div>
</div>
```

#### 4.2 매칭 결과 시각화

```html
<!-- src/main/resources/templates/matching-results.html -->
<div id="matchingResults">
    <h2>당신과 매치된 의원들</h2>
    
    <!-- 상위 3명 -->
    <div class="top-matches">
        <div class="match-card rank-1">
            <div class="rank">1위</div>
            <img src="{{ topMatches[0].photoUrl }}" alt="{{ topMatches[0].hgNm }}">
            <h3>{{ topMatches[0].hgNm }}</h3>
            <p>{{ topMatches[0].polyNm }} | {{ topMatches[0].regionName }}</p>
            <div class="score">{{ topMatches[0].similarityScore }}% 일치</div>
        </div>
        <!-- 2위, 3위도 유사하게 -->
    </div>
    
    <!-- 상세 매칭 리스트 (Table) -->
    <table id="matchTable">
        <thead>
            <tr>
                <th>순위</th>
                <th>의원명</th>
                <th>정당</th>
                <th>지역</th>
                <th>일치도</th>
                <th>상세보기</th>
            </tr>
        </thead>
        <tbody>
            <tr v-for="(match, index) in matchResults" :key="match.monaCd">
                <td>{{ index + 1 }}</td>
                <td>{{ match.hgNm }}</td>
                <td>{{ match.polyNm }}</td>
                <td>{{ match.regionName }}</td>
                <td>
                    <div class="progress-bar">
                        <div class="progress" 
                             :style="{ width: match.similarityScore + '%' }">
                        </div>
                    </div>
                    {{ match.similarityScore }}%
                </td>
                <td>
                    <a :href="`/politician/detail/${match.monaCd}`">보기</a>
                </td>
            </tr>
        </tbody>
    </table>
</div>
```

#### 4.3 안건별 의원 입장 시각화

```html
<!-- src/main/resources/templates/bill-analysis.html -->
<div id="billAnalysis">
    <h2>안건별 의원 입장</h2>
    
    <!-- 특정 안건 선택 -->
    <select id="billSelect">
        <option value="">안건을 선택하세요</option>
        <option v-for="bill in bills" :key="bill.billId" :value="bill.billId">
            {{ bill.billName }}
        </option>
    </select>
    
    <!-- 진행 바 그래프 -->
    <div class="bill-results">
        <div class="result-group">
            <div class="result-label">찬성</div>
            <div class="result-bar">
                <div class="agree" :style="{ width: agreePercentage + '%' }">
                    {{ agreeCount }}명
                </div>
            </div>
        </div>
        
        <div class="result-group">
            <div class="result-label">반대</div>
            <div class="result-bar">
                <div class="disagree" :style="{ width: disagreePercentage + '%' }">
                    {{ disagreeCount }}명
                </div>
            </div>
        </div>
        
        <div class="result-group">
            <div class="result-label">기권</div>
            <div class="result-bar">
                <div class="abstain" :style="{ width: abstainPercentage + '%' }">
                    {{ abstainCount }}명
                </div>
            </div>
        </div>
    </div>
    
    <!-- 정당별 입장 분석 -->
    <div class="party-analysis">
        <h3>정당별 입장</h3>
        <div v-for="party in parties" :key="party" class="party-row">
            <span>{{ party }}</span>
            <div class="party-votes">
                <span class="agree">찬성: {{ partyVotes[party].agree }}</span>
                <span class="disagree">반대: {{ partyVotes[party].disagree }}</span>
                <span class="abstain">기권: {{ partyVotes[party].abstain }}</span>
            </div>
        </div>
    </div>
</div>
```

---

## 📊 구현 우선순위

### 1순위 (필수)
- [ ] Phase 1: PoliticianProfileService (의원 프로필 계산)
- [ ] Phase 2: 빠른 설문 (5개 질문)
- [ ] Phase 3: 기본 매칭 알고리즘

### 2순위 (중요)
- [ ] Phase 4: 상위 매칭 결과 시각화
- [ ] Phase 1: 안건 카테고리화
- [ ] 의원 상세 정보 페이지

### 3순위 (추가)
- [ ] Phase 2: 상세 설문 (20-30개)
- [ ] 정당 비교 분석
- [ ] 시간대별 성향 변화 추적

---

## 💾 DB 스키마 추가

```sql
-- 의원 프로필 테이블
CREATE TABLE politician_profile (
    mona_cd VARCHAR(50) PRIMARY KEY,
    political_score FLOAT,
    economic_score FLOAT,
    environment_score FLOAT,
    health_score FLOAT,
    education_score FLOAT,
    foreign_score FLOAT,
    social_score FLOAT,
    consistency_score FLOAT,
    total_votes INT,
    last_updated TIMESTAMP,
    FOREIGN KEY (mona_cd) REFERENCES politician(mona_cd)
);

-- 사용자 프로필 테이블
CREATE TABLE user_profile (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    political_score FLOAT,
    economic_score FLOAT,
    environment_score FLOAT,
    health_score FLOAT,
    education_score FLOAT,
    foreign_score FLOAT,
    social_score FLOAT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- 안건 정보 테이블
CREATE TABLE bill (
    bill_id VARCHAR(50) PRIMARY KEY,
    bill_name VARCHAR(255),
    category VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP
);

-- 투표 통계 테이블
CREATE TABLE vote_statistic (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mona_cd VARCHAR(50),
    category VARCHAR(50),
    agree_count INT,
    disagree_count INT,
    abstain_count INT,
    last_updated TIMESTAMP,
    FOREIGN KEY (mona_cd) REFERENCES politician(mona_cd)
);
```

---

## 🚀 다음 액션 아이템

### 이번 주
- [ ] PoliticianProfileService 구현
- [ ] Vote 데이터 기반 의원 스코어 계산
- [ ] 사용자 설문 페이지 기본 구현

### 다음 주
- [ ] 매칭 알고리즘 구현
- [ ] 매칭 결과 API 개발
- [ ] 시각화 (Chart.js 활용)

### 그 다음
- [ ] 안건 카테고리화 정교화
- [ ] 추천 알고리즘 고도화
- [ ] 캐싱 및 성능 최적화

---

**작성일**: 2026-04-30
**현재 Progress**: 25% (데이터 수집 완료)
