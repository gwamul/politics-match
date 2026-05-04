package com.jgs.politics.domain.vote;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "vote_history")
@Getter
@NoArgsConstructor
public class VoteHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String monaCd;      // 국회의원코드 (MONA_CD)
    private String hgNm;        // 의원명 (HG_NM)
    private String hgNmHanja;  // [추가] 의원명(한자)
    private String billId;      // 의안ID (BILL_ID)
    private String billName;    // 의안명 (BILL_NAME)
    private String voteDate;    // 의결일자 (VOTE_DATE)
    private String resultVote;  // 표결결과 (RESULT_VOTE_MOD) - 찬성/반대/기권
    private String polyNm;      // 정당 (POLY_NM)
    private String age;         // 대수 (AGE) - 22대 필터링용

    @Builder
    public VoteHistory(String monaCd, String hgNm, String hgNmHanja, String billId, String billName,
                       String voteDate, String resultVote, String polyNm, String age) {
        this.monaCd = monaCd;
        this.hgNm = hgNm;
        this.hgNmHanja = hgNmHanja;
        this.billId = billId;
        this.billName = billName;
        this.voteDate = voteDate;
        this.resultVote = resultVote;
        this.polyNm = polyNm;
        this.age = age;
    }
}