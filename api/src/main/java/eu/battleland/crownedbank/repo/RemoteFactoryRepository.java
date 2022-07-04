package eu.battleland.crownedbank.repo;

import com.google.common.collect.ImmutableSet;
import eu.battleland.crownedbank.abstracted.Repository;
import eu.battleland.crownedbank.remote.Remote;

public class RemoteFactoryRepository
        extends Repository<String, Remote.Factory> {

    /**
     * Register remote factory.
     * @param entry Remote factory.
     */
    @Override
    public void register(Remote.Factory entry) {
        super.register(entry);
    }

    /**
     * @param id Identifier.
     * @return Remote factory.
     */
    @Override
    public Remote.Factory retrieve(String id) {
        return super.retrieve(id);
    }

    /**
     * @return All factories.
     */
    @Override
    public ImmutableSet<Remote.Factory> all() {
        return super.all();
    }
}
