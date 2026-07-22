package single.cjj.workflow.attachment;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

public interface WorkflowStorageProvider {

    String providerKey();

    StoredObject put(String objectKey, InputStream inputStream, long maximumBytes) throws IOException;

    Resource load(String objectKey) throws IOException;

    void delete(String objectKey) throws IOException;

    record StoredObject(long size, String sha256) {
    }
}
