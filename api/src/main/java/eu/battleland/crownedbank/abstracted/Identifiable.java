package eu.battleland.crownedbank.abstracted;

import lombok.NonNull;

@FunctionalInterface
public interface Identifiable<I> {

    @NonNull I identifier();

}
