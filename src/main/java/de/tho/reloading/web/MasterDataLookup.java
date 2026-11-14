package de.tho.reloading.web;

import de.tho.reloading.entity.Caliber;
import de.tho.reloading.entity.Manufacturer;
import de.tho.reloading.repository.CaliberRepository;
import de.tho.reloading.repository.ManufacturerRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
        return manufacturerRepository.findAll().stream()
                .sorted(Comparator.comparing(Manufacturer::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Sorted by name. */
    public List<Caliber> calibers() {
        return caliberRepository.findAll().stream()
                .sorted(Comparator.comparing(Caliber::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** The manufacturers with these ids, sorted by name; for filters that only offer what a list contains. */
    public List<Manufacturer> manufacturers(Collection<UUID> ids) {
        Set<UUID> wanted = new HashSet<>(ids);
        return manufacturers().stream().filter(manufacturer -> wanted.contains(manufacturer.getId())).toList();
    }

    /** The calibers with these ids, sorted by name. */
    public List<Caliber> calibers(Collection<UUID> ids) {
        Set<UUID> wanted = new HashSet<>(ids);
        return calibers().stream().filter(caliber -> wanted.contains(caliber.getId())).toList();
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
