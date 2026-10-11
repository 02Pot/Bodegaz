package com.warehouse.system.Controller;

import com.warehouse.system.DTO.Response.StorageTypeResponse;
import com.warehouse.system.Repository.StorageTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/storage/types")
@RequiredArgsConstructor
public class StorageTypeController {

    private final StorageTypeRepository repo;

    @GetMapping
    public List<StorageTypeResponse> list() {
        return repo.findAll().stream().map(StorageTypeResponse::from).toList();
    }

}
