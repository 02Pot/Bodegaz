package com.warehouse.system.Service;

import com.warehouse.system.DTO.Request.CreateWarehouseRequest;
import com.warehouse.system.Enums.UserType;
import com.warehouse.system.Model.UserModel;
import com.warehouse.system.Model.Warehouse;
import com.warehouse.system.Model.WarehouseAddress;
import com.warehouse.system.Repository.UserModelRepository;
import com.warehouse.system.Repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final UserModelRepository userModelRepository;

    @Transactional
    @PreAuthorize("hasAuthority('SELLER_ROLE')")
    public Warehouse addWarehouse(CreateWarehouseRequest request, UUID id){
        UserModel user = userModelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        WarehouseAddress address = new WarehouseAddress();

        address.setAddressBlock(request.getAddressBlock());
        address.setCity(request.getCity());
        address.setBarangay(request.getBarangay());
        address.setCountry(request.getCountry());
        address.setZipcode(request.getZipcode());

        Warehouse warehouse = new Warehouse();

        warehouse.setName(request.getName());
        warehouse.setWarehouseCapacityKg(request.getWarehouseCapacityKg());

        warehouse.setUser(user);
        warehouse.setWarehouseAddress(address);

        address.setWarehouse(warehouse);

        return warehouseRepository.save(warehouse);

    }

}
