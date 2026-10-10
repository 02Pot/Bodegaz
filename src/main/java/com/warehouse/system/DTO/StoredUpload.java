package com.warehouse.system.DTO;

import com.warehouse.system.Service.File.FileStorageService;
import org.springframework.web.multipart.MultipartFile;

public record StoredUpload(
        MultipartFile originalFile,
        FileStorageService.StoredFile storedFile
) {}