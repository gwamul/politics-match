package com.jgs.politics.domain.politician;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PoliticianRepository extends JpaRepository<Politician, String> {
    // 기본적인 save, findAll, findById 등은 상속받아 바로 사용 가능합니다.
	
	
	// 1. 정당별 통계 (정당명, 인원수)
    @Query("SELECT p.polyNm, COUNT(p) FROM Politician p GROUP BY p.polyNm")
    List<Object[]> countByParty();

    // 2. 지역별 통계 (시/도, 인원수)
    @Query("SELECT p.cityName, COUNT(p) FROM Politician p GROUP BY p.cityName")
    List<Object[]> countByCity();

    // 3. 선수별 통계 (초선/재선.., 인원수)
    @Query("SELECT p.reeleGbnNm, COUNT(p) FROM Politician p GROUP BY p.reeleGbnNm")
    List<Object[]> countByElectionCount();
    
    // 4. 성별 통계
    @Query("SELECT p.sexGbnNm, COUNT(p) FROM Politician p GROUP BY p.sexGbnNm")
    List<Object[]> countByGender();
}