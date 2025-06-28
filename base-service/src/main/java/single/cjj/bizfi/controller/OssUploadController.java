package single.cjj.bizfi.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import single.cjj.bizfi.entity.ApiResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OSS 文件上传接口模拟实现。
 */
@RestController
@RequestMapping("/oss")
public class OssUploadController {

    private static final String UPLOAD_DIR = "uploads";

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ApiResponse.error("文件为空");
        }
        // 确保上传目录存在
        Path uploadPath = Path.of(UPLOAD_DIR);
        Files.createDirectories(uploadPath);

        // 使用随机文件名防止冲突
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String filename = UUID.randomUUID() + ext;
        Path target = uploadPath.resolve(filename);
        // 保存文件
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // 这里模拟生成 OSS 访问地址
        String url = "https://your-bucket.oss-cn-hangzhou.aliyuncs.com/" + filename;
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return ApiResponse.success(result);
    }
}
