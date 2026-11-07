package com.warehouse.system.Controller;

import com.warehouse.system.DTO.Request.WarehouseRequest;
import com.warehouse.system.DTO.Request.WarehouseUpdateRequest;
import com.warehouse.system.DTO.Response.ScrollResponse;
import com.warehouse.system.DTO.Response.SellerWarehouseResponse;
import com.warehouse.system.DTO.Response.WarehouseResponse;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Model.WarehouseImage;
import com.warehouse.system.Service.File.FileStorageService;
import com.warehouse.system.Service.Warehouse.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final Clock clock;
    private final FileStorageService storageService;

    @GetMapping("/all")
    public ResponseEntity<ScrollResponse<WarehouseResponse>> getAll(
            @RequestParam(required = false)UUID cursor,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserModel user
    ){
        return ResponseEntity.ok(warehouseService.getAllWarehouse(cursor,size,user.getId()));
    }

    @GetMapping("/{warehouseId}")
    public ResponseEntity<WarehouseResponse> getById(
            @PathVariable UUID warehouseId,
            @AuthenticationPrincipal UserModel user
    ){
        return ResponseEntity.ok(warehouseService.getByWarehouseId(user.getId(),warehouseId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<SellerWarehouseResponse>> getMyWarehouse(@AuthenticationPrincipal UserModel user){
        return ResponseEntity.ok(warehouseService.getMyWarehouse(user.getId()));
    }

    @PostMapping(value = "/add",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WarehouseResponse> addWarehouse(
            @RequestPart("data") @Valid WarehouseRequest request,
            @RequestPart(value = "images",required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserModel user
    ) throws IOException {
        return ResponseEntity.ok(warehouseService.addWarehouse(request,images,user.getId()));
    }

    @PatchMapping(value = "/{warehouseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WarehouseResponse> updateWarehouse(
            @PathVariable UUID warehouseId,
            @RequestPart(value = "data",required = false) WarehouseUpdateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> images,
            @RequestParam(value = "removeImageIds", required = false) List<UUID> removeImageIds,
            @AuthenticationPrincipal UserModel user
    ) throws IOException {
        return ResponseEntity.ok(warehouseService.updateWarehouse(
                warehouseId,
                request,
                images,
                removeImageIds,
                user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarehouse(
            @AuthenticationPrincipal UserModel user,
            @PathVariable UUID id
    ){
        return ResponseEntity.ok(warehouseService.delete(user.getId(),id));
    }

    @GetMapping("/trending")
    public ResponseEntity<Page<WarehouseResponse>> getTrendingWarehouse(
            @AuthenticationPrincipal UserModel user,
            @RequestParam int size
    ){
        return ResponseEntity.ok(warehouseService.getByTrending(user.getId(),size));
    }

    @GetMapping("/newest")
    public ResponseEntity<Page<WarehouseResponse>> getNewestWarehouse(
            @AuthenticationPrincipal UserModel user,
            @RequestParam int size
    ){
        return ResponseEntity.ok(warehouseService.getByNewest(user.getId(),size));
    }

    //UPLOAD IMAGE
    @PostMapping(value = "/{warehouseId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ImageResponse>> upload(
            @PathVariable UUID warehouseId,
            @RequestParam("files") List<MultipartFile> files) throws IOException {

        List<ImageResponse> body = warehouseService.upload(warehouseId, files).stream()
                .map(ImageResponse::from).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    //GET IMAGES from warehouse ID
    @GetMapping("/{warehouseId}/images")
    public List<ImageResponse> list(@PathVariable UUID warehouseId) {
        return warehouseService.list(warehouseId).stream().map(ImageResponse::from).toList();
    }

    //GET IMAGES from imageId
    @GetMapping("/images/{imageId}")
    public ResponseEntity<Resource> view(@PathVariable UUID imageId) throws IOException {
        return serve(imageId, false);
    }

    //DOWNLOAD IMAGE uses image id
    @GetMapping("/images/{imageId}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID imageId) throws IOException {
        return serve(imageId, true);
    }

    //DELETE IMAGE uses image id
    @DeleteMapping("/images/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable UUID imageId) throws IOException {
        warehouseService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Resource> serve(UUID imageId, boolean asAttachment) throws IOException {
        WarehouseImage img = warehouseService.get(imageId);
        Resource resource = storageService.load(img.getStoredFileName());

        ContentDisposition disposition = (asAttachment
                ? ContentDisposition.attachment()
                : ContentDisposition.inline())
                .filename(img.getOriginalFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    public record ImageResponse(UUID id, String fileName, String contentType, long sizeBytes, String url) {
        static ImageResponse from(WarehouseImage i) {
            return new ImageResponse(i.getId(), i.getOriginalFileName(),
                    i.getContentType(), i.getSizeBytes(), "/api/images/" + i.getId());
        }
    }

}
