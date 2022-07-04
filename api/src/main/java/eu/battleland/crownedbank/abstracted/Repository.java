package eu.battleland.crownedbank.abstracted;

import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository of identifiable entries.
 *
 * @param <T> Type of Entry
 */
public abstract class Repository<I, T extends Identifiable<I>> {

    private final Map<I, T> entries
            = new ConcurrentHashMap<>();

    /**
     * Register entry.
     *
     * @param entry Entry.
     */
    public void register(final T entry) {
        this.entries.put(entry.identifier(), entry);
    }

    /**
     * Retrieve registered entry.
     *
     * @param id Identifier.
     * @return Entry.
     */
    public T retrieve(final I id) {
        return this.entries.get(id);
    }

    /**
     * @return All entries.
     */
    public ImmutableSet<T> all() {
        return ImmutableSet.<T>builderWithExpectedSize(this.entries.size()).addAll(entries.values()).build();
    }

}
