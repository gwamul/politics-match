package com.jgs.politics.domain.politician.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AnalysisViewController {

    @GetMapping("/analysis/political-map")
    public String politicalMapPage() {
        return "analysis/political-map";
    }
}
