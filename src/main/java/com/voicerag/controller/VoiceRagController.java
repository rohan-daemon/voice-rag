package com.voicerag.controller;

import com.voicerag.model.SearchResult;
import com.voicerag.model.TranscriptionStatus;
import com.voicerag.service.IndexService;
import com.voicerag.service.SearchService;
import com.voicerag.service.TranscriptionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class VoiceRagController {
    private final TranscriptionService transcriptionService;
    private final IndexService indexService;
    private final SearchService searchService;

    public VoiceRagController(TranscriptionService transcriptionService, IndexService indexService, SearchService searchService) {
        this.transcriptionService = transcriptionService;
        this.indexService = indexService;
        this.searchService = searchService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("indexed", indexService.isIndexed());
        return "index";
    }

    @PostMapping("/transcribe")
    public String transcribe(Model model) {
        List<TranscriptionStatus> results = transcriptionService.transcribeAll();
        model.addAttribute("transcriptionResults", results);
        model.addAttribute("indexed", indexService.isIndexed());
        return "index";
    }

    @PostMapping("/index")
    public String buildIndex(Model model) {
        String result = indexService.buildIndex();
        model.addAttribute("indexResult", result);
        model.addAttribute("indexed", indexService.isIndexed());
        return "index";
    }

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String query,
                        @RequestParam(defaultValue = "5") int maxResults,
                        Model model) {
        model.addAttribute("indexed", indexService.isIndexed());
        model.addAttribute("query", query);

        if (!query.isBlank()) {
            List<SearchResult> results = searchService.search(query, maxResults);
            model.addAttribute("searchResults", results);
        }

        return "index";
    }
}
