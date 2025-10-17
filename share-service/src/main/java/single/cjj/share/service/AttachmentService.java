package single.cjj.share.service;

import org.springframework.stereotype.Service;
import single.cjj.share.model.Attachment;
import single.cjj.share.model.AttachmentPresignRequest;
import single.cjj.share.model.AttachmentPresignResponse;
import single.cjj.share.model.AttachmentType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AttachmentService {
    private final Map<String, Attachment> storage = new ConcurrentHashMap<>();

    public AttachmentPresignResponse generatePresignedUrl(AttachmentPresignRequest request) {
        String id = "file_" + UUID.randomUUID();
        String uploadUrl = "https://storage.example.com/upload/" + id + "?expires=" + Instant.now().plusSeconds(900).toEpochMilli();
        Attachment attachment = new Attachment(id, request.getFileName(), AttachmentType.FILE, null, request.getSize(), request.getContentType());
        storage.put(id, attachment);
        return new AttachmentPresignResponse(uploadUrl, attachment);
    }

    public Attachment findAttachment(String id) {
        return storage.get(id);
    }

    public boolean deleteAttachment(String id) {
        return storage.remove(id) != null;
    }
}
