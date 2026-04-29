package com.jgs.politics.domain.vote;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PoliticianStatService {

    private final VoteHistoryRepository voteRepository;

    /**
     * 의원의 당론 준수율(Loyalty) 계산
     * 해당 정당의 다수가 찬성할 때 본인도 찬성했는지 비율 측정
     */
    public double calculatePartyLoyalty(String monaCd, String polyNm) {
        List<VoteHistory> myVotes = voteRepository.findByMonaCd(monaCd);
        long totalCount = myVotes.size();
        if (totalCount == 0) return 0;

        long loyaltyCount = 0;
        for (VoteHistory myVote : myVotes) {
            // 해당 의안의 정당 다수 의견 조회 (간략화된 로직)
            String partyOpinion = voteRepository.findMajorResultByBillIdAndPolyNm(myVote.getBillId(), polyNm);
            if (myVote.getResultVote().equals(partyOpinion)) {
                loyaltyCount++;
            }
        }
        return (double) loyaltyCount / totalCount * 100;
    }

    /**
     * 표결 참여율 스탯
     */
    public double calculateParticipationRate(String monaCd, int totalBills) {
        long votedCount = voteRepository.countByMonaCd(monaCd);
        return (double) votedCount / totalBills * 100;
    }
}