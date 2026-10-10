package com.warehouse.system.Controller;

import com.warehouse.system.DTO.ProfilePicture;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Service.File.ProfilePictureService;
import com.warehouse.system.Utils.GetCurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfilePictureController {

    private final ProfilePictureService profilePictureService;
    private final GetCurrentUserId getCurrentUserId;

    @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            Authentication auth) throws IOException {

        profilePictureService.upload(getCurrentUserId.currentUserId(auth), file);
        return ResponseEntity.ok(Map.of("url", "/api/users/me/profile-picture"));
    }

    @GetMapping("/me/profile-picture")
    public ResponseEntity<Resource> getMine(Authentication auth)
            throws IOException {
        return serve(profilePictureService.get(getCurrentUserId.currentUserId(auth)));
    }

    @GetMapping("/{userId}/profile-picture")
    public ResponseEntity<Resource> getByUserId(@PathVariable UUID userId) throws IOException {
        return serve(profilePictureService.get(userId));
    }

    @DeleteMapping("/me/profile-picture")
    public ResponseEntity<Void> delete(Authentication auth) {
        profilePictureService.remove(getCurrentUserId.currentUserId(auth));
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Resource> serve(ProfilePicture picture) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(picture.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(picture.resource());
    }

}
