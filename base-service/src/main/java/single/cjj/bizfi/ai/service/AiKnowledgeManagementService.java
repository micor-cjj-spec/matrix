package single.cjj.bizfi.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import single.cjj.bizfi.ai.dto.AiCitationResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeChunkResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocDetailResponse;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocRequest;
import single.cjj.bizfi.ai.dto.AiKnowledgeDocSummaryResponse;

import java.util.List;

public interface AiKnowledgeManagementService {
    IPage<AiKnowledgeDocSummaryResponse> listDocs(
            int page,
            int size,
            String keyword,
            String category,
            String status,
            String kbId
    );

    AiKnowledgeDocDetailResponse getDoc(String docId);

    AiKnowledgeDocDetailResponse createDoc(AiKnowledgeDocRequest request);

    AiKnowledgeDocDetailResponse updateDoc(String docId, AiKnowledgeDocRequest request);

    boolean deleteDoc(String docId);

    int rebuildChunks(String docId);

    List<AiKnowledgeChunkResponse> listChunks(String docId);

    List<String> listCategories();

    List<AiCitationResponse> retrieve(String question, List<String> kbIds, Integer topK);
}
