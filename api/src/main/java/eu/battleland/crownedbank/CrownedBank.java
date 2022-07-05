package eu.battleland.crownedbank;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import eu.battleland.crownedbank.model.Currency;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CrownedBank {

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private static CrownedBankAPI api;

    @Getter
    @Setter
    private static Logger logger;

    @Getter
    @Setter
    private static int remoteTimeoutMillis = 5000;

    @Getter
    @Setter
    private static int wealthyCheckAccountLimit = 25;
    @Getter
    @Setter
    private static long wealthyCheckMillis = 5 * 60 * 1000;

    @Getter
    @Setter
    private static boolean identityNameMajor = false;


    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(new RecordTypeAdapterFactory())
            .registerTypeAdapter(Map.class, (JsonSerializer<Map<Currency, Float>>) (src, typeOfSrc, context) -> {
                final var object = new JsonObject();
                src.forEach((currency, value) -> {
                    object.add(currency.identifier(), new JsonPrimitive(value));
                });
                return object;
            })
            .registerTypeAdapter(Map.class, (JsonDeserializer<Map<Currency, Float>>) (json, typeOfT, context) -> {
                final var map = new HashMap<Currency, Float>();
                final var object = json.getAsJsonObject();
                object.entrySet().forEach((entry) -> {
                    final Currency currency = getApi().getCurrencyRepository().retrieve(entry.getKey());
                    if (currency == null)
                        throw new IllegalStateException("Unregistered currency: " + entry.getKey());

                    map.put(currency, entry.getValue().getAsFloat());
                });
                return map;
            })
            .create();

    // https://github.com/google/gson/issues/1794
    public static class RecordTypeAdapterFactory implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) type.getRawType();
            if (!clazz.isRecord()) {
                return null;
            }
            TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    delegate.write(out, value);
                }

                @Override
                public T read(JsonReader reader) throws IOException {
                    if (reader.peek() == JsonToken.NULL) {
                        reader.nextNull();
                        return null;
                    } else {
                        var recordComponents = clazz.getRecordComponents();
                        var typeMap = new HashMap<String, TypeToken<?>>();
                        for (int i = 0; i < recordComponents.length; i++) {
                            typeMap.put(recordComponents[i].getName(), TypeToken.get(recordComponents[i].getGenericType()));
                        }
                        var argsMap = new HashMap<String, Object>();

                        reader.beginObject();
                        while (reader.hasNext()) {
                            String name = reader.nextName();
                            argsMap.put(name, gson.getAdapter(typeMap.get(name)).read(reader));
                        }
                        reader.endObject();

                        var argTypes = new Class<?>[recordComponents.length];
                        var args = new Object[recordComponents.length];
                        for (int i = 0; i < recordComponents.length; i++) {
                            argTypes[i] = recordComponents[i].getType();
                            args[i] = argsMap.get(recordComponents[i].getName());
                        }
                        Constructor<T> constructor;
                        try {
                            constructor = clazz.getDeclaredConstructor(argTypes);
                            constructor.setAccessible(true);
                            return constructor.newInstance(args);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };
        }
    }
}
