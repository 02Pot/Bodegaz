package com.warehouse.system.Controller;

import com.warehouse.system.DTO.ProfilePicture;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Service.File.ProfilePictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @PostMapping(value = "/upload/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserModel user) throws IOException {

        profilePictureService.upload(user.getId(), file);
        return ResponseEntity.ok(Map.of("url", "/api/users/upload/profile-picture"));
    }

    @GetMapping("/me/profile-picture")
    public ResponseEntity<Resource> getPicture(@AuthenticationPrincipal UserModel user) throws IOException {
        return serve(profilePictureService.get(user.getId()));
    }

    @GetMapping("/{userId}/profile-picture")
    public ResponseEntity<Resource> getByUserId(@PathVariable UUID userId) throws IOException {
        return serve(profilePictureService.get(userId));
    }

    @DeleteMapping("/me/profile-picture")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserModel user) {
        profilePictureService.remove(user.getId());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Resource> serve(ProfilePicture picture) {
        if(picture == null){
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(picture.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(picture.resource());
    }

}
