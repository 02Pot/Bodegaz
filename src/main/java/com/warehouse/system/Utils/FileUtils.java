package com.warehouse.system.Utils;

import com.warehouse.system.DTO.StoredUpload;
import com.warehouse.system.Model.Warehouse;
import com.warehouse.system.Model.WarehouseImage;
import com.warehouse.system.Service.File.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FileUtils {

    private final FileStorageService storageService;

    public List<StoredUpload> storeFiles(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) return List.of();

        List<StoredUpload> result = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                var stored = storageService.store(file);
                result.add(new StoredUpload(file, stored));
            }
            return result;
        } catch (IOException | RuntimeException e) {
            deleteQuietly(
                    result.stream()
                            .map(upload -> upload.storedFile().storedName())
                            .toList()
            );

            if (e instanceof IOException ioException) {
                throw new UncheckedIOException(
                        "Failed to store files", ioException
                );
            }

            throw e;
        }
    }

    public List<String> toUrls(List<WarehouseImage> images) {
        return images.stream()
                .map(img -> "/api/warehouse/images/" + img.getId())   // note the trailing slash
                .toList();
    }

    public void deleteQuietly(List<String> names) {
        for (String name : names) {
            try {
                storageService.delete(name);
            } catch (IOException ignored) { }
        }
    }

}
