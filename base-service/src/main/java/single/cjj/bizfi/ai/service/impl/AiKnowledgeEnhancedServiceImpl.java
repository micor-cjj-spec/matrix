package single.cjj.bizfi.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.entity.BizfiAiKnowledgeChunk;
import single.cjj.bizfi.ai.mapper.BizfiAiKnowledgeChunkMapper;
import single.cjj.bizfi.ai.service.AiKnowledgeService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service("aiKnowledgeEnhancedService")
public class AiKnowledgeEnhancedServiceImpl implements AiKnowledgeService {

    private static final int DEFAULT_TOP_K = 5;

    @Autowired
    private BizfiAiKnowledgeChunkMapper knowledgeChunkMapper;

    @Override
    public List<AiCitationResponse> retrieve(String question, List<String> kbIds) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }
        List<String> keywords = extractKeywords(question);
        if (keywords.isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<BizfiAiKnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        for (String keyword : keywords) {
            wrapper.or(w -> w.like(BizfiAiKnowledgeChunk::getFkeywords, keyword)
                    .or().like(BizfiAiKnowledgeChunk::getFcontent, keyword));
        }
        List<BizfiAiKnowledgeChunk> chunks = knowledgeChunkMapper.selectList(wrapper);
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        return chunks.stream()
                .map(chunk -> new ScoredChunk(chunk, score(chunk, keywords)))
                .filter(item -> item.score > 0)
                .sorted(Comparator.comparingInt(ScoredChunk::score).reversed()
                        .thenComparing(item -> item.chunk.getFseq(), Comparator.nullsLast(Integer::compareTo)))
                .limit(DEFAULT_TOP_K)
                .map(item -> new AiCitationResponse(
                        item.chunk.getFdocid(),
                        item.chunk.getFdocid(),
                        item.chunk.getFchunkid(),
                        buildSnippet(item.chunk.getFcontent())
                ))
                .collect(Collectors.toList());
    }

    private List<String> extractKeywords(String question) {
        String normalized = question.replaceAll("[，。！？、,.!?;；:\\s]+", " ").trim();
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        String[] parts = normalized.split(" ");
        List<String> keywords = new ArrayList<>();
        for (String part : parts) {
            String keyword = part.trim().toLowerCase(Locale.ROOT);
            if (keyword.length() >= 2) {
                keywords.add(keyword);
            }
        }
        return keywords.stream().distinct().toList();
    }

    private int score(BizfiAiKnowledgeChunk chunk, List<String> keywords) {
        int score = 0;
        String content = chunk.getFcontent() == null ? "" : chunk.getFcontent().toLowerCase(Locale.ROOT);
        String kw = chunk.getFkeywords() == null ? "" : chunk.getFkeywords().toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (kw.contains(keyword)) {
                score += 5;
            }
            if (content.contains(keyword)) {
                score += 2;
            }
        }
        return score;
    }

    private String buildSnippet(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String text = content.trim();
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }

    private record ScoredChunk(BizfiAiKnowledgeChunk chunk, int score) {
    }
}
