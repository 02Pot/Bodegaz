package com.warehouse.system.Utils;

import com.warehouse.system.Model.StorageType;
import com.warehouse.system.Repository.StorageTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StorageTypeSeeders implements ApplicationRunner {

    private final StorageTypeRepository repo;

    @Override
    @Transactional
    public void run(ApplicationArguments args){
        seed("Small Locker",  1.0, 1.0, 1.0,   200);
        seed("Medium Locker", 2.0, 1.5, 2.0,   500);
        seed("Pallet Rack",   1.2, 1.0, 2.0,  1000);
        seed("Cold Room",     4.0, 3.0, 2.5,  3000);
        seed("Large Bay",     6.0, 4.0, 3.0,  5000);
    }

    private void seed(String name, double l, double b, double h, double capacity) {
        if (repo.existsByName(name)) return;
        StorageType t = new StorageType();
        t.setName(name);
        t.setLengthMeters(l);
        t.setBreadthMeters(b);
        t.setHeightMeters(h);
        t.setCapacityWeight(capacity);
        repo.save(t);
    }

}
