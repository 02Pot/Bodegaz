package com.warehouse.system.Service.File;

import com.warehouse.system.DTO.ProfilePicture;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Repository.UserModelRepository;
import com.warehouse.system.Utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.warehouse.system.Service.File.FileStorageService.StoredFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfilePictureService {

    private final UserModelRepository userModelRepository;
    private final FileStorageService fileStorageService;
    private final FileUtils fileUtils;

    public String upload(UUID userId, MultipartFile file) throws IOException {
        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String oldName = user.getProfilePicture();
        StoredFile stored = fileStorageService.store(file);

        try {
            user.setProfilePicture(stored.storedName());
            userModelRepository.save(user);
        } catch (RuntimeException e) {
            fileStorageService.delete(stored.storedName());
            throw e;
        }

        deleteQuietly(oldName);
        return stored.storedName();
    }

    public ProfilePicture get(UUID userId) throws IOException {
        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String name = user.getProfilePicture();
        if (name == null || name.isBlank()) {
            throw new FileNotFoundException("User has no profile picture");
        }
        return new ProfilePicture(fileStorageService.load(name), contentTypeFor(name));
    }

    public void remove(UUID userId) {
        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String oldName = user.getProfilePicture();
        user.setProfilePicture(null);
        userModelRepository.save(user);
        deleteQuietly(oldName);
    }

    private void deleteQuietly(String name) {
        if (name == null || name.isBlank()) return;
        try {
            fileStorageService.delete(name);
        } catch (IOException e) {
            System.out.println("Could not delete old profile picture " + name + " " + e);
        }
    }

    private String contentTypeFor(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }


}
