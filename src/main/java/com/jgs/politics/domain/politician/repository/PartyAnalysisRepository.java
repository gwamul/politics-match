package com.jgs.politics.domain.politician.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jgs.politics.domain.politician.PartyAnalysis;

import java.util.Optional;
import java.util.List;

@Repository
public interface PartyAnalysisRepository extends JpaRepository<PartyAnalysis, Long> {
    
    Optional<PartyAnalysis> findByPartyName(String partyName);
    
    List<PartyAnalysis> findAllByOrderByCohesionDesc();
}
