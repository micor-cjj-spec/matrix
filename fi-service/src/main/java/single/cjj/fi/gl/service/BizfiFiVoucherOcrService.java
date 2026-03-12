package single.cjj.fi.gl.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface BizfiFiVoucherOcrService {
    Map<String, Object> parseFile(MultipartFile file);
}
