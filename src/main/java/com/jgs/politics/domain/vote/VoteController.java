package com.jgs.politics.domain.vote;

import java.util.List;
import java.util.Map;
import java.util.TreeMap; // 정렬을 위해 추가
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jgs.politics.domain.vote.dto.BillVoteSummaryResponse;
import com.jgs.politics.domain.vote.dto.IndividualVoteDTO;
import com.jgs.politics.domain.vote.dto.MemberIdentityDTO;
import com.jgs.politics.domain.vote.dto.MemberVoteHistoryResponse;
import com.jgs.politics.domain.vote.dto.VoteResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteHistoryRepository voteRepository;

    /**
     * 전체 투표 목록 조회 (페이징)
     */
    @GetMapping
    public Page<VoteResponse> getVotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("voteDate").descending());
        
        return voteRepository.findAll(pageRequest).map(vote -> 
            VoteResponse.builder()
                .age(vote.getAge())
                .hgNm(vote.getHgNm())
                .polyNm(vote.getPolyNm())
                .billName(vote.getBillName())
                .resultVote(vote.getResultVote())
                .voteDate(vote.getVoteDate())
                .build()
        );
    }
    
    /**
     * 1. 안건별 요약 보기 (특정 법안의 찬/반 명단 및 통계)
     */
    @GetMapping("/bill/{billId}")
    public BillVoteSummaryResponse getBillVoteSummary(@PathVariable String billId) {
        List<VoteHistory> votes = voteRepository.findByBillId(billId);
        
        if (votes.isEmpty()) return null;

        // 1. 우선 VoteHistory 리스트를 MemberIdentityDTO 리스트로 먼저 변환 (타입 추론 명확)
        // 여기서 v.getHgNm() 등에 빨간줄이 간다면 VoteHistory 엔티티의 @Getter를 확인해야 합니다.
        List<MemberIdentityDTO> identityList = votes.stream()
                .map(v -> MemberIdentityDTO.builder()
                        .hgNm(v.getHgNm())
                        .monaCd(v.getMonaCd())
                        .polyNm(v.getPolyNm())
                        .build())
                .collect(Collectors.toList());

        // 2. 변환된 DTO 리스트를 결과(찬성/반대 등)별로 그룹화
        // 이 단계에서 VoteHistory가 아닌 MemberIdentityDTO를 사용하므로 제네릭 에러가 발생하지 않습니다.
        Map<String, List<MemberIdentityDTO>> groupedMembers = votes.stream()
                .collect(Collectors.groupingBy(
                    VoteHistory::getResultVote,
                    TreeMap::new,
                    Collectors.toList()
                )).entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().stream()
                        .map(v -> MemberIdentityDTO.builder()
                            .hgNm(v.getHgNm())
                            .monaCd(v.getMonaCd())
                            .polyNm(v.getPolyNm())
                            .build())
                        .collect(Collectors.toList()),
                    (oldValue, newValue) -> oldValue,
                    TreeMap::new
                ));

        // --- 더 간단한 방법 (추천) ---
        // 위 코드가 복잡하다면 아래와 같이 "임시 맵"을 먼저 만들고 가공하세요.
        Map<String, List<VoteHistory>> rawGrouped = votes.stream()
                .collect(Collectors.groupingBy(VoteHistory::getResultVote, TreeMap::new, Collectors.toList()));

        Map<String, List<MemberIdentityDTO>> finalGroupedMembers = new TreeMap<>();
        rawGrouped.forEach((result, voteList) -> {
            List<MemberIdentityDTO> dtos = voteList.stream()
                .map(v -> MemberIdentityDTO.builder()
                    .hgNm(v.getHgNm())
                    .monaCd(v.getMonaCd())
                    .polyNm(v.getPolyNm())
                    .build())
                .collect(Collectors.toList());
            finalGroupedMembers.put(result, dtos);
        });

        Map<String, Long> voteCounts = votes.stream()
                .collect(Collectors.groupingBy(VoteHistory::getResultVote, TreeMap::new, Collectors.counting()));

        return BillVoteSummaryResponse.builder()
                .billId(billId)
                .billName(votes.get(0).getBillName())
                .totalVoters(votes.size())
                .counts(voteCounts)
                .memberIdentities(finalGroupedMembers)
                .build();
    }

    /**
     * 2. 의원별 투표 이력 보기 (특정 의원의 과거 행보)
     */
    @GetMapping("/member/{monaCd}")
    public MemberVoteHistoryResponse getMemberVoteHistory(
            @PathVariable String monaCd,
            @RequestParam(defaultValue = "0") int page) {
        
        PageRequest pageRequest = PageRequest.of(page, 20, Sort.by("voteDate").descending());
        Page<VoteHistory> votePage = voteRepository.findByMonaCd(monaCd, pageRequest);

        if (votePage.isEmpty()) return null;

        VoteHistory first = votePage.getContent().get(0);

        List<IndividualVoteDTO> voteList = votePage.getContent().stream()
                .map(vote -> IndividualVoteDTO.builder()
                        .billId(vote.getBillId())
                        .billName(vote.getBillName())
                        .resultVote(vote.getResultVote())
                        .voteDate(vote.getVoteDate())
                        .build())
                .collect(Collectors.toList());

        return MemberVoteHistoryResponse.builder()
                .hgNm(first.getHgNm())
                .polyNm(first.getPolyNm())
                .monaCd(monaCd)
                .votes(voteList)
                .build();
    }
}