package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.common.base.Pair;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.Lists;
import com.stardog.stark.Value;
import com.stardog.stark.query.*;
import com.stardog.stark.query.impl.SelectQueryResultImpl;
import com.stardog.stark.query.io.QueryResultFormats;
import com.stardog.stark.query.io.QueryResultParsers;
import com.stardog.stark.query.io.QueryResultWriters;
import io.github.kawamuray.wasmtime.Engine;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Instance;
import io.github.kawamuray.wasmtime.Memory;
import io.github.kawamuray.wasmtime.Module;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.Val;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static com.stardog.stark.Values.literal;

public class StardogWasm {

    public static final String WASM_FUNCTION_MALLOC = "malloc";
    public static final String WASM_FUNCTION_FREE = "free";
    public static final String WASM_FUNCTION_MAPPING_DICTIONARY_GET = "mappingDictionaryGet";
    public static final String WASM_FUNCTION_MAPPING_DICTIONARY_ADD = "mappingDictionaryAdd";
    public static final String WASM_FUNCTION_AGGREGATE = "aggregate";
    public static final String WASM_FUNCTION_GET_VALUE = "get_value";
    public static final String WASM_FUNCTION_EVALUATE = "evaluate";
    public static final String WASM_FUNCTION_DOC = "doc";

    public static LoadingCache<URL, Module> loadingCache = CacheBuilder.newBuilder().softValues().removalListener(
                    (RemovalListener<URL, Module>) removal -> removal.getValue().close())
            .build(new CacheLoader<URL, Module>() {
                @Override
                public Module load(URL url) throws IOException {
                    try(final Engine engine = store.engine()) {
                        return Module.fromBinary(engine, getWasm(url));
                    }
                }
            });

    public static Store<Void> store = Store.withoutData();

    private static byte[] getWasm(final URL wasmUrl) throws IOException {

        final URLConnection conn = wasmUrl.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.connect();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(conn.getInputStream(), baos);
        return baos.toByteArray();
    }

    public static ByteArrayOutputStream readResult(final Store store, final Memory memory, final Integer output_pointer) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ByteBuffer memoryBuffer = memory.buffer(store);

        for (Integer i = output_pointer, max = memoryBuffer.limit(); i < max; ++i) {
            final byte[] b = new byte[1];
            memoryBuffer.position(i);
            memoryBuffer.get(b);

            if (b[0] == 0) {
                break;
            }
            baos.write(b[0]);
        }
        return baos;
    }

    public static Value[] readFromWasmMemory(AtomicReference<Instance> instanceRef, String name, int output_pointer) {
        try (final Memory memory = instanceRef.get().getMemory(StardogWasm.store, name).get()) {
            try {
                final SelectQueryResult selectQueryResult = QueryResultParsers.readSelect(new ByteArrayInputStream(StardogWasm.readResult(StardogWasm.store, memory, output_pointer).toByteArray()), QueryResultFormats.JSON);
                final Optional<BindingSet> bs = selectQueryResult.stream().findFirst();

                if (bs.isPresent()) {
                    return bs.get().stream().map(Binding::value).toArray(Value[]::new);
                } else {
                    return new Value[0];
                }
            } catch (IOException e) {
                return new Value[0];
            }
        }
    }

    public static void free(AtomicReference<Instance> instanceRef, Pair<Integer, Integer> pointer) {
        try (final Func freeFunction = instanceRef.get().getFunc(StardogWasm.store, StardogWasm.WASM_FUNCTION_FREE).get();) {
            freeFunction.call(StardogWasm.store, Val.fromI32(pointer.first), Val.fromI32(pointer.second));
        }
    }

    public static Pair<Integer, Integer> writeToWasmMemory(AtomicReference<Instance> instanceRef, String name, Value[] values) throws IOException {

        final List<String> vars = Lists.newArrayListWithCapacity(values.length);
        final BindingSets.Builder bindingSetsBuilder = BindingSets.builder();
        IntStream.range(0, values.length).forEach(i -> bindingSetsBuilder.add(Bindings.of(String.format("value_%d", i), values[i])));
        final List<BindingSet> bindings = Collections.singletonList(bindingSetsBuilder.build());

        final SelectQueryResult queryResult = new SelectQueryResultImpl(vars, bindings);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        QueryResultWriters.write(queryResult, byteArrayOutputStream, QueryResultFormats.JSON);

        final Integer input_pointer;
        try (final Func mallocFunction = instanceRef.get().getFunc(StardogWasm.store, StardogWasm.WASM_FUNCTION_MALLOC).get()) {
            byteArrayOutputStream.write('\0');
            mallocFunction.call(StardogWasm.store, Val.fromI32(byteArrayOutputStream.toByteArray().length))[0].i32();
            input_pointer = mallocFunction.call(StardogWasm.store, Val.fromI32(byteArrayOutputStream.toByteArray().length))[0].i32();
        }

        try(final Memory memory = instanceRef.get().getMemory(StardogWasm.store, name).get()) {
            final ByteBuffer memoryBuffer = memory.buffer(StardogWasm.store);
            final byte[] input = byteArrayOutputStream.toByteArray();
            final int pages = (int) Math.ceil(input.length / 64.0);
            if (pages > memory.size(StardogWasm.store)) {
                memory.grow(StardogWasm.store, pages - memory.size(StardogWasm.store));
            }

            memoryBuffer.position(input_pointer);
            memoryBuffer.put(input);
        }
        return Pair.create(input_pointer,byteArrayOutputStream.toByteArray().length);
    }

    public static Pair<Integer, Integer> writeBindingsToWasmMemory(AtomicReference<Instance> instanceRef, String name, List<BindingSet> bindings, List<String> vars) throws IOException {
        final SelectQueryResult queryResult = new SelectQueryResultImpl(vars, bindings);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        QueryResultWriters.write(queryResult, byteArrayOutputStream, QueryResultFormats.JSON);

        Integer input_pointer;
        try (final Func mallocFunction = instanceRef.get().getFunc(StardogWasm.store, StardogWasm.WASM_FUNCTION_MALLOC).get();) {
            byteArrayOutputStream.write('\0');
            mallocFunction.call(StardogWasm.store, Val.fromI32(byteArrayOutputStream.toByteArray().length))[0].i32();
            input_pointer = mallocFunction.call(StardogWasm.store, Val.fromI32(byteArrayOutputStream.toByteArray().length))[0].i32();
        }

        try(final Memory memory = instanceRef.get().getMemory(StardogWasm.store, name).get()) {
            final ByteBuffer memoryBuffer = memory.buffer(StardogWasm.store);
            final byte[] input = byteArrayOutputStream.toByteArray();
            final int pages = (int) Math.ceil(input.length / 64.0);
            if (pages > memory.size(StardogWasm.store)) {
                memory.grow(StardogWasm.store, pages - memory.size(StardogWasm.store));
            }

            memoryBuffer.position(input_pointer);
            memoryBuffer.put(input);
        }
        return Pair.create(input_pointer,byteArrayOutputStream.toByteArray().length);
    }

    public static Pair<Integer, Integer> writeAggregateArgsToWasmMemory(AtomicReference<Instance> instanceRef, long multiplicity, String name, Value[] values) throws IOException {

        final List<String> vars = Lists.newArrayListWithCapacity(values.length);
        final BindingSets.Builder bindingSetsBuilder = BindingSets.builder();
        vars.add("multiplicity");
        bindingSetsBuilder.add("multiplicity", literal(multiplicity));
        IntStream.range(0, values.length).forEach(i -> {
            vars.add(String.format("value_%d", i));
            bindingSetsBuilder.add(vars.get(i), values[i]);
        });
        final List<BindingSet> bindings = Collections.singletonList(bindingSetsBuilder.build());
        return StardogWasm.writeBindingsToWasmMemory(instanceRef, name, bindings, vars);
    }
}
