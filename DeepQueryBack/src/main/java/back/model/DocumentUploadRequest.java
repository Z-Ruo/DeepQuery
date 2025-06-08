package back.model;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class DocumentUploadRequest {
    private String collectionName; // 所选集合名称
    private MultipartFile file; // 上传的文件
}
