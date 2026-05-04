package com.jgs.politics.domain.bill;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 안건 분석 데이터 조회 API
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis/bills")
@RequiredArgsConstructor
public class BillAnalysisController {

    private final BillAnalysisRepository billAnalysisRepository;

    /**
     * 논란이 많은 상위 10개 안건 조회
     */
    @GetMapping("/controversial")
    public List<BillAnalysis> getControversialBills() {
        List<BillAnalysis> result = billAnalysisRepository.findTop10ByOrderByControversyScoreDesc();
        if (result.isEmpty()) {
            log.warn("논란 있는 안건이 없습니다. 분석 데이터가 생성되지 않았을 수 있습니다.");
        }
        return result;
    }

    /**
     * 모든 안건 분석 조회
     */
    @GetMapping("/all")
    public List<BillAnalysis> getAllBillAnalysis() {
        return billAnalysisRepository.findAllByOrderByControversyScoreDesc();
    }

    /**
     * 특정 안건 분석 조회
     */
    @GetMapping("/{billId}")
    public BillAnalysis getBillAnalysis(@PathVariable String billId) {
        return billAnalysisRepository.findByBillId(billId)
                .orElseThrow(() -> new RuntimeException("안건 분석 데이터를 찾을 수 없습니다: " + billId));
    }
}
