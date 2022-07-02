package eu.battleland.crownedbank;

import com.google.common.collect.ImmutableSet;
import eu.battleland.crownedbank.abstracted.Repository;
import eu.battleland.crownedbank.remote.Remote;
import lombok.Getter;
import lombok.Setter;

/**
 * Remote repository.
 */
public class RemoteRepository
    extends Repository<Remote> {

    @Getter @Setter
    private static Remote defaultRemote;

    /**
     * Register remote.
     * @param entry Remote.
     */
    @Override
    public void register(Remote entry) {
        super.register(entry);
    }

    /**
     * Retrieve remote.
     * @param id Identifier.
     * @return Remote instance.
     */
    @Override
    public Remote retrieve(String id) {
        return super.retrieve(id);
    }

    /**
     * @return All Remotes
     */
    @Override
    public ImmutableSet<Remote> all() {
        return super.all();
    }
}
