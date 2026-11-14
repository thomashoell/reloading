package de.tho.reloading.web;

import de.tho.reloading.entity.Caliber;
import de.tho.reloading.entity.Manufacturer;
import de.tho.reloading.repository.CaliberRepository;
import de.tho.reloading.repository.ManufacturerRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

/** Manufacturers and calibers for select boxes and for the names in lists. */
@Component
public class MasterDataLookup {

    private final ManufacturerRepository manufacturerRepository;
    private final CaliberRepository caliberRepository;

    public MasterDataLookup(ManufacturerRepository manufacturerRepository, CaliberRepository caliberRepository) {
        this.manufacturerRepository = manufacturerRepository;
        this.caliberRepository = caliberRepository;
    }

    /** Sorted by name. */
    public List<Manufacturer> manufacturers() {
        return StreamSupport.stream(manufacturerRepository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(Manufacturer::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Sorted by name. */
    public List<Caliber> calibers() {
        return StreamSupport.stream(caliberRepository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(Caliber::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Map<UUID, String> manufacturerNames() {
        Map<UUID, String> names = new HashMap<>();
        manufacturerRepository.findAll().forEach(manufacturer -> names.put(manufacturer.getId(), manufacturer.getName()));
        return names;
    }

    public Map<UUID, String> caliberNames() {
        Map<UUID, String> names = new HashMap<>();
        caliberRepository.findAll().forEach(caliber -> names.put(caliber.getId(), caliber.getName()));
        return names;
    }
}
