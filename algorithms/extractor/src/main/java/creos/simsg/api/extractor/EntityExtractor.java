package creos.simsg.api.extractor;

import creos.simsg.api.model.Entity;
import creos.simsg.api.model.Fuse;
import creos.simsg.api.model.Substation;
import creos.simsg.api.navigation.bfs.BFSFuse;

import java.util.*;

/**
 * Extracts entities in a <a href="https://en.wikipedia.org/wiki/Breadth-first_search">BFS</a> order.
 */
public class EntityExtractor implements Extractor<Entity> {
    public static final EntityExtractor INSTANCE = new EntityExtractor();

    private EntityExtractor(){}

    @Override
    public void extractAndSave(Substation substation) {
        Set<Entity> allEntities = new HashSet<>();
        BFSFuse.INSTANCE.navigate(substation, (Fuse fuse) -> allEntities.add(fuse.getOwner()));
        substation.getGrid()
                .save(ExtracterUtils.getKeyCertain(Entity.class, substation), new ArrayList<>(allEntities));
    }

    @Override
    public List<Entity> getExtracted(Substation substation) {
        String key = ExtracterUtils.getKeyCertain(Entity.class, substation);
        Optional<Object> optEntities = substation.getGrid().retrieve(key);
        if(optEntities.isEmpty()) {
            extractAndSave(substation);
            optEntities = substation.getGrid().retrieve(key);
        }
        return (List<Entity>) optEntities.get();
    }
}
