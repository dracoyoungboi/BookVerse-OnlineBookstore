package com.bookverse.BookVerse.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileUploadService {

    // Đường dẫn thư mục để lưu ảnh
    private static final String UPLOAD_DIR = "src/main/resources/static/user/img/product/";
    // Đường dẫn URL để truy cập ảnh (từ static folder)
    private static final String URL_PREFIX = "/user/img/product/";

    /**
     * Upload file ảnh và trả về đường dẫn URL
     * @param file File ảnh cần upload
     * @return Đường dẫn URL của ảnh (ví dụ: /user/img/product/abc123.jpg)
     * @throws IOException Nếu có lỗi khi lưu file
     */
    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Tạo tên file unique để tránh trùng lặp
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // Tạo đường dẫn đầy đủ để lưu file
        Path uploadPath = Paths.get(UPLOAD_DIR);
        
        // Tạo thư mục nếu chưa tồn tại
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Lưu file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Trả về đường dẫn URL
        return URL_PREFIX + uniqueFilename;
    }

    /**
     * Xóa file ảnh cũ nếu tồn tại
     * @param imageUrl Đường dẫn URL của ảnh cần xóa (ví dụ: /user/img/product/abc123.jpg)
     */
    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            // Chỉ xóa file nếu nằm trong thư mục product (để tránh xóa nhầm)
            if (imageUrl.startsWith(URL_PREFIX)) {
                String filename = imageUrl.substring(URL_PREFIX.length());
                Path filePath = Paths.get(UPLOAD_DIR, filename);
                
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
            }
        } catch (IOException e) {
            // Log lỗi nhưng không throw exception để không làm gián đoạn flow
            System.err.println("Error deleting file: " + imageUrl + " - " + e.getMessage());
        }
    }

    /**
     * Kiểm tra xem file có phải là ảnh không
     * @param file File cần kiểm tra
     * @return true nếu là ảnh, false nếu không
     */
    public boolean isImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }
}


