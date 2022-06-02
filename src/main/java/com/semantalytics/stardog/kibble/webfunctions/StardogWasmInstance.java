package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.common.rdf.model.ArrayLiteral;
import com.complexible.stardog.StardogException;
import com.complexible.stardog.index.dictionary.MappingDictionary;
import com.complexible.stardog.index.statistics.Accuracy;
import com.complexible.stardog.index.statistics.Cardinality;
import com.complexible.stardog.plan.PlanException;
import com.complexible.stardog.plan.QueryTerm;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.Values;
import com.stardog.stark.query.Binding;
import com.stardog.stark.query.BindingSet;
import com.stardog.stark.query.BindingSets;
import com.stardog.stark.query.SelectQueryResult;
import com.stardog.stark.query.impl.SelectQueryResultImpl;
import com.stardog.stark.query.io.QueryResultFormats;
import com.stardog.stark.query.io.QueryResultParsers;
import com.stardog.stark.query.io.QueryResultWriters;
import io.github.kawamuray.wasmtime.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static com.stardog.stark.Values.iri;
import static io.github.kawamuray.wasmtime.WasmValType.I32;
import static io.github.kawamuray.wasmtime.WasmValType.I64;
import static java.util.stream.Collectors.toList;

public class StardogWasmInstance implements Closeable {

    public static final String WASM_FUNCTION_MALLOC = "malloc";
    public static final String WASM_FUNCTION_FREE = "free";
    public static final String WASM_FUNCTION_MAPPING_DICTIONARY_GET = "mapping_dictionary_get";
    public static final String WASM_FUNCTION_MAPPING_DICTIONARY_ADD = "mapping_dictionary_add";
    public static final String WASM_FUNCTION_AGGREGATE = "aggregate";
    public static final String WASM_FUNCTION_GET_VALUE = "get_value";
    public static final String WASM_FUNCTION_EVALUATE = "evaluate";
    public static final String WASM_FUNCTION_CARDINALITY_ESTIMATE = "cardinality_estimate";
    public static final String WASM_FUNCTION_DOC = "doc";

    public static final long WASM_PAGE_SIZE = 64 * FileUtils.ONE_KB;

    static LoadingCache<URL, byte[]> loadingCache = CacheBuilder
            .newBuilder()
            .softValues()
            .build(new CacheLoader<URL, byte[]>() {
                @Override
                public byte[] load(URL url) throws IOException {
                    return getWasm(url);
                }
            });

    private static byte[] getWasm(final URL wasmUrl) throws IOException {

        final URLConnection conn = wasmUrl.openConnection();
        //TODO This should be configurable
        conn.setConnectTimeout(240000);
        conn.setReadTimeout(240000);
        conn.connect();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(conn.getInputStream(), baos);
        return baos.toByteArray();
    }

    private Store<Void> store = Store.withoutData();

    private final AtomicReference<MappingDictionary> mappingDictionaryRef = new AtomicReference<>();
    private Instance instance;
    private boolean closed = false;
    private boolean mappingDictionaryIsSet;

    public boolean isMappingDictionaryIsSet() {
        return mappingDictionaryIsSet;
    }

    public MappingDictionary getMappingDictionary() {
        return mappingDictionaryRef.get();
    }

    public static URL getWasmUrl(final Value value) throws MalformedURLException {
        if (value instanceof Literal) {
            return new URL(((Literal) value).label());
        } else {
            return new URL(value.toString() + '/' + Version.PLUGIN_VERSION);
        }
    }

    public static class WasmMemoryRef {
        public int addr;
        public int len;
        //TODO not sure if these shoudl be int or long

        public static WasmMemoryRef from(final int addr, final int len) {
            return new WasmMemoryRef(addr, len);
        }

        public WasmMemoryRef(final int addr, final int len) {
            this.addr = addr;
            this.len = len;
        }
    }

    private final Func mappingDictionaryGetFunc = WasmFunctions.wrap(store, I64, I32, (id) -> {
        final Value value = this.mappingDictionaryRef.get().getValue(id);

        WasmMemoryRef buf = null;
        //TODO fix this. possible NPE
        try {
            buf = this.writeToWasmMemory("memory", new Value[]{value});
        } catch (IOException e) {
            //TODO ???
        }

        return buf.addr;
    });

    private final Func mappingDictionaryAddFunc = WasmFunctions.wrap(store, I32, I64, (addr) ->
       Arrays.stream(readFromWasmMemory("memory", addr)).map(mappingDictionaryRef.get()::add).findFirst().orElse(-1L));

    public static StardogWasmInstance from(final Value wasmURL) throws ExecutionException, MalformedURLException {
        return new StardogWasmInstance(getWasmUrl(wasmURL));
    }

    public static StardogWasmInstance from(final Value wasmURL, final MappingDictionary mappingDictionary) throws ExecutionException, MalformedURLException {
        return new StardogWasmInstance(getWasmUrl(wasmURL), mappingDictionary);
    }

    public StardogWasmInstance(final URL wasmURL, final MappingDictionary mappingDictionary) throws ExecutionException {
        this(wasmURL);
        this.setMappingDictionary(mappingDictionary);
    }

    public StardogWasmInstance(final URL wasmUrl) throws ExecutionException {
        final AtomicReference<Instance> instanceRef = new AtomicReference<>();
        try (final Engine engine = store.engine()) {
            try (final Module module = Module.fromBinary(engine, loadingCache.get(wasmUrl))) {
                try (Linker linker = new Linker(engine)) {
                    linker.define("env", WASM_FUNCTION_MAPPING_DICTIONARY_ADD, Extern.fromFunc(mappingDictionaryAddFunc));
                    linker.define("env", WASM_FUNCTION_MAPPING_DICTIONARY_GET, Extern.fromFunc(mappingDictionaryGetFunc));
                    linker.module(store, "", module);

                    instanceRef.set(linker.instantiate(store, module));
                }
            }
        }
        this.instance = instanceRef.get();
    }

    private Value[] readFromWasmMemory(final String name, final int output_pointer) {
        try (final Memory memory = instance.getMemory(store, name).get()) {
            try (final SelectQueryResult selectQueryResult = QueryResultParsers.readSelect(new ByteArrayInputStream(readResult(memory, output_pointer).toByteArray()), QueryResultFormats.JSON)) {
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

    private WasmMemoryRef writeToWasmMemory(final String name, final Value[] values) throws IOException {

        final List<String> vars = Lists.newArrayListWithCapacity(values.length);
        final BindingSets.Builder bindingSetsBuilder = BindingSets.builder();
        IntStream.range(0, values.length).forEach(i -> {
            vars.add(String.format("value_%d", i));
            bindingSetsBuilder.add(String.format("value_%d", i), values[i]);
        });
        final List<BindingSet> bindings = Collections.singletonList(bindingSetsBuilder.build());

        final SelectQueryResult queryResult = new SelectQueryResultImpl(vars, bindings);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        QueryResultWriters.write(queryResult, byteArrayOutputStream, QueryResultFormats.JSON);

        final Integer input_pointer;
        try (final Func mallocFunction = instance.getFunc(store, WASM_FUNCTION_MALLOC).get()) {
            byteArrayOutputStream.write('\0');
            mallocFunction.call(store, Val.fromI32(byteArrayOutputStream.toByteArray().length))[0].i32();
            input_pointer = mallocFunction.call(store, Val.fromI32(byteArrayOutputStream.toByteArray().length))[0].i32();
        }

        try (final Memory memory = instance.getMemory(store, name).get()) {
            final ByteBuffer memoryBuffer = memory.buffer(store);
            final byte[] input = byteArrayOutputStream.toByteArray();
            final int pages = (int) (input.length / WASM_PAGE_SIZE + 1);
            if (pages > memory.size(store)) {
                memory.grow(store, pages - memory.size(store));
            }

            memoryBuffer.position(input_pointer);
            memoryBuffer.put(input);
        }
        return WasmMemoryRef.from(input_pointer, byteArrayOutputStream.toByteArray().length);
    }

    public void setMappingDictionary(final MappingDictionary mappingDictionary) {
        this.mappingDictionaryIsSet = true;
        this.mappingDictionaryRef.set(mappingDictionary);
    }

    private WasmMemoryRef writeToWasmMemoryWithCardinality(final String name, final Cardinality cardinality, final Value[] values) throws IOException {

        final List<String> vars = Lists.newArrayListWithCapacity(values.length);
        final BindingSets.Builder bindingSetsBuilder = BindingSets.builder();
        IntStream.range(0, values.length).forEach(i -> {
            vars.add(String.format("value_%d", i));
            bindingSetsBuilder.add(String.format("value_%d", i), values[i]);
        });
        vars.add("cardinality");
        bindingSetsBuilder.add("cardinality", Values.literal(cardinality.value()));
        final List<BindingSet> bindings = Collections.singletonList(bindingSetsBuilder.build());

        final SelectQueryResult queryResult = new SelectQueryResultImpl(vars, bindings);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        QueryResultWriters.write(queryResult, byteArrayOutputStream, QueryResultFormats.JSON);

        final Integer input_pointer;
        try (final Func mallocFunction = instance.getFunc(store, WASM_FUNCTION_MALLOC).get()) {
            byteArrayOutputStream.write('\0');
            mallocFunction.call(store, Val.fromI32(byteArrayOutputStream.toByteArray().length))[0].i32();
            input_pointer = mallocFunction.call(store, Val.fromI32(byteArrayOutputStream.toByteArray().length))[0].i32();
        }

        try (final Memory memory = instance.getMemory(store, name).get()) {
            final ByteBuffer memoryBuffer = memory.buffer(store);
            final byte[] input = byteArrayOutputStream.toByteArray();
            final int pages = (int) (input.length / WASM_PAGE_SIZE + 1);
            if (pages > memory.size(store)) {
                memory.grow(store, pages - memory.size(store));
            }

            memoryBuffer.position(input_pointer);
            memoryBuffer.put(input);
        }
        return WasmMemoryRef.from(input_pointer, byteArrayOutputStream.toByteArray().length);
    }


    private WasmMemoryRef writeToWasmMemoryWithMultiplicity(final String name, final Value[] values, final long multiplicity) throws IOException {

        final List<String> vars = Lists.newArrayListWithCapacity(values.length);
        final BindingSets.Builder bindingSetsBuilder = BindingSets.builder();
        IntStream.range(0, values.length).forEach(i -> {
            vars.add(String.format("value_%d", i));
            bindingSetsBuilder.add(String.format("value_%d", i), values[i]);
        });
        vars.add("multiplicity");
        bindingSetsBuilder.add("multiplicity", Values.literal(multiplicity));
        final List<BindingSet> bindings = Collections.singletonList(bindingSetsBuilder.build());

        final SelectQueryResult queryResult = new SelectQueryResultImpl(vars, bindings);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        QueryResultWriters.write(queryResult, byteArrayOutputStream, QueryResultFormats.JSON);

        final Integer input_pointer;
        try (final Func mallocFunction = instance.getFunc(store, WASM_FUNCTION_MALLOC).get()) {
            byteArrayOutputStream.write('\0');
            mallocFunction.call(store, Val.fromI32(byteArrayOutputStream.toByteArray().length))[0].i32();
            input_pointer = mallocFunction.call(store, Val.fromI32(byteArrayOutputStream.toByteArray().length))[0].i32();
        }

        try (final Memory memory = instance.getMemory(store, name).get()) {
            final ByteBuffer memoryBuffer = memory.buffer(store);
            final byte[] input = byteArrayOutputStream.toByteArray();
            final int pages = (int) (input.length / WASM_PAGE_SIZE + 1);
            if (pages > memory.size(store)) {
                memory.grow(store, pages - memory.size(store));
            }

            memoryBuffer.position(input_pointer);
            memoryBuffer.put(input);
        }
        return WasmMemoryRef.from(input_pointer, byteArrayOutputStream.toByteArray().length);
    }

    private SelectQueryResult readFromWasmMemorySelectQueryResult(final String name, final int output_pointer) {
        try (final Memory memory = instance.getMemory(store, name).get()) {
            return QueryResultParsers.readSelect(new ByteArrayInputStream(readResult(memory, output_pointer).toByteArray()), QueryResultFormats.JSON);
        } catch (IOException e) {
            throw new StardogException(e);
        }
    }

    public void free(final WasmMemoryRef wasmMemoryRef) {
        try (final Func freeFunction = instance.getFunc(store, WASM_FUNCTION_FREE).get();) {
            freeFunction.call(store, Val.fromI32(wasmMemoryRef.addr), Val.fromI32(wasmMemoryRef.len));
        }
    }

    public boolean isClosed() {
        return closed;
    }

    public void close() {
        instance.close();
        store.close();
        instance = null;
        store = null;
        closed = true;
    }

    public Cardinality getCardinality(final Cardinality inputCardinality, List<Value> args) {
        try {
            final WasmMemoryRef input = this.writeToWasmMemoryWithCardinality("memory", inputCardinality, args.toArray(new Value[0]));

            try (final Func evaluateFunction = instance.getFunc(store, WASM_FUNCTION_CARDINALITY_ESTIMATE).get()) {
                final Integer output_pointer = evaluateFunction.call(store, Val.fromI32(input.addr))[0].i32();
                free(input);
                try (final SelectQueryResult selectQueryResult = readFromWasmMemorySelectQueryResult("memory", output_pointer)) {
                    if (selectQueryResult.hasNext()) {
                        final BindingSet bs = selectQueryResult.next();
                        return Cardinality.of(Literal.longValue((Literal) bs.get("cardinality")), Accuracy.valueOf(((Literal) bs.get("accuracy")).label()));
                    } else {
                        throw new PlanException("Unable to retrieve cardinality estimate");
                    }
                }
            }
        } catch (IOException e1) {
            throw new PlanException(e1);
        }
    }

    public SelectQueryResult evaluate(final Value... values) throws IOException {

        final WasmMemoryRef input = writeToWasmMemory("memory", values);

        try (final Func evaluateFunction = instance.getFunc(store, WASM_FUNCTION_EVALUATE).get()) {
            final Integer output_pointer = evaluateFunction.call(store, Val.fromI32(input.addr))[0].i32();
            free(input);
            return readFromWasmMemorySelectQueryResult("memory", output_pointer);
        }
    }

    public SelectQueryResult doc() throws IOException {

        try (Func evaluateFunction = instance.getFunc(store, WASM_FUNCTION_DOC).get()) {
            final Integer output_pointer = evaluateFunction.call(store)[0].i32();
            return readFromWasmMemorySelectQueryResult("memory", output_pointer);
        }
    }

    public SelectQueryResult compute(final Value[] values, long multiplicity) throws IOException {
        final WasmMemoryRef input = writeToWasmMemoryWithMultiplicity("memory", Arrays.stream(values).toArray(Value[]::new), multiplicity);

        try (final Func evaluateFunction = instance.getFunc(store, WASM_FUNCTION_AGGREGATE).get()) {
            final Integer output_pointer = evaluateFunction.call(store, Val.fromI32(input.addr))[0].i32();
            free(input);
            return readFromWasmMemorySelectQueryResult("memory", output_pointer);
        }
    }

    public SelectQueryResult aggregateGetValue() {
        try (final Func evaluateFunction = instance.getFunc(store, WASM_FUNCTION_GET_VALUE).get()) {
            final Integer output_pointer = evaluateFunction.call(store)[0].i32();
            return readFromWasmMemorySelectQueryResult("memory", output_pointer);
        }
    }

    private ByteArrayOutputStream readResult(final Memory memory, final Integer output_pointer) {
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

    public ValueOrError selectQueryResultToValueOrError(final SelectQueryResult selectQueryResult) {
        final List<BindingSet> bindingSets = Streams.stream(selectQueryResult).collect(toList());

        if(bindingSets.size() == 0 || bindingSets.size() == 1 && bindingSets.get(0).size() == 0) {
            return ValueOrError.Error;
        }

        if(bindingSets.size() == 1 && bindingSets.get(0).size() == 1) {
            if (bindingSets.get(0).get("value_0") instanceof Literal && ((Literal) bindingSets.get(0).get("value_0")).datatypeIRI().equals(iri("tag:stardog:api:array"))) {
                return ValueOrError.General.of(ArrayLiteral.coerce((Literal) bindingSets.get(0).get("value_0")));
            } else {
                return ValueOrError.General.of(bindingSets.get(0).get("value_0"));
            }
        } else {
            return ValueOrError.General.of(bindingSetsToArrayLiteral(bindingSets));
        }
    }

    private ArrayLiteral bindingSetsToArrayLiteral(final List<BindingSet> bindingSets) {
        return new ArrayLiteral(
                bindingSets.stream().map(bs -> new ArrayLiteral(bs.stream().map(b -> {
                    if (b.get() instanceof Literal && ((Literal) b.get()).datatypeIRI().equals(iri("tag:stardog:api:array"))) {
                        return ArrayLiteral.coerce((Literal) b.get());
                    } else {
                        return b.get();
                    }
                }).mapToLong(mappingDictionaryRef.get()::add).toArray())).mapToLong(mappingDictionaryRef.get()::add).toArray());
    }
}