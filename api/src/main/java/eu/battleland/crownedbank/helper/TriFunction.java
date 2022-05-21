package eu.battleland.crownedbank.helper;

@FunctionalInterface
public interface TriFunction<X, Y, Z, R> {

    R apply(X first, Y second, Z third) throws Exception;

}
