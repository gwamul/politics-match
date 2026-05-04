package com.jgs.politics.domain.vote.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jgs.politics.domain.vote.VoteHistory;

@Repository
public interface VoteHistoryRepository extends JpaRepository<VoteHistory, Long> {

    // 1. 특정 의원의 모든 표결 기록 조회
    List<VoteHistory> findByMonaCd(String monaCd);

    // 2. 특정 의안(Bill)에 대한 특정 의원의 표결 결과 확인
    Optional<VoteHistory> findByBillIdAndMonaCd(String billId, String monaCd);

    // 3. 특정 의원의 전체 표결 참여 횟수 (성실도 계산용)
    long countByMonaCd(String monaCd);

    /**
     * 4. 특정 의안에 대한 정당별 '다수 의견' 조회 (당론 추출 핵심 쿼리)
     * 예: "A의안에 대해 국민의힘 의원들이 가장 많이 던진 표는?" -> "찬성"
     */
    @Query(value = "SELECT result_vote FROM (" +
            "  SELECT result_vote, COUNT(*) as cnt " +
            "  FROM vote_history " +
            "  WHERE bill_id = :billId AND poly_nm = :polyNm " +
            "  GROUP BY result_vote " +
            "  ORDER BY cnt DESC " +
            ") LIMIT 1", nativeQuery = true)
    String findMajorResultByBillIdAndPolyNm(@Param("billId") String billId, @Param("polyNm") String polyNm);

    // 5. 특정 키워드가 포함된 법안들에 대한 표결 기록 (분야별 성향 분석용)
    // 예: "환경"이 포함된 법안에 이 의원이 어떻게 투표했는가
    List<VoteHistory> findByMonaCdAndBillNameContaining(String monaCd, String keyword);

	boolean existsByBillId(String billId);

    Optional<VoteHistory> findTopByOrderByIdDesc();

    List<VoteHistory> findByIdGreaterThanOrderByIdAsc(Long id);
	
	
	//최근 투표 순, 이름순 페이징 조회
	Page<VoteHistory> findAll(Pageable pageable);

	List<VoteHistory> findByBillId(String billId);

	Page<VoteHistory> findByMonaCd(String monaCd, PageRequest pageRequest);
	
	Page<VoteHistory> findByMonaCd(String monaCd, Pageable pageable);
    
	

	
}