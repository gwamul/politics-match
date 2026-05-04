package com.jgs.politics.domain.politician.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jgs.politics.domain.politician.PoliticianProfile;

import java.util.Optional;
import java.util.List;

@Repository
public interface PoliticianProfileRepository extends JpaRepository<PoliticianProfile, String> {
    
    Optional<PoliticianProfile> findByMonaCd(String monaCd);
    
    List<PoliticianProfile> findByPolyNm(String polyNm);
    
    List<PoliticianProfile> findAllByOrderByApprovalRateDesc();
}
