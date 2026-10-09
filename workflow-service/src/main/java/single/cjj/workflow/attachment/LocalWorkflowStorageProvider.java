package single.cjj.workflow.attachment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class LocalWorkflowStorageProvider implements WorkflowStorageProvider {

    private final Path root;

    public LocalWorkflowStorageProvider(
            @Value("${workflow.attachment.local-root:./data/workflow-attachments}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public String providerKey() {
        return "LOCAL";
    }

    @Override
    public StoredObject put(String objectKey, InputStream inputStream, long maximumBytes) throws IOException {
        Path target = resolve(objectKey);
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        MessageDigest digest = sha256Digest();
        long total = 0;
        try (OutputStream output = Files.newOutputStream(temporary)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                total += read;
                if (total > maximumBytes) {
                    throw new IOException("上传文件超过允许大小");
                }
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
        } catch (IOException ex) {
            Files.deleteIfExists(temporary);
            throw ex;
        }
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return new StoredObject(total, HexFormat.of().formatHex(digest.digest()));
    }

    @Override
    public Resource load(String objectKey) throws IOException {
        Path path = resolve(objectKey);
        if (!Files.isRegularFile(path)) {
            throw new IOException("文件不存在");
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String objectKey) throws IOException {
        Files.deleteIfExists(resolve(objectKey));
    }

    private Path resolve(String objectKey) throws IOException {
        Path path = root.resolve(objectKey).normalize();
        if (!path.startsWith(root)) {
            throw new IOException("非法存储路径");
        }
        return path;
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM 不支持 SHA-256", ex);
        }
    }
}
