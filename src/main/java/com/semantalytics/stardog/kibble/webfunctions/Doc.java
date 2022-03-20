package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.common.base.Pair;
import com.complexible.common.rdf.model.ArrayLiteral;
import com.complexible.stardog.plan.filter.AbstractExpression;
import com.complexible.stardog.plan.filter.Expression;
import com.complexible.stardog.plan.filter.ExpressionVisitor;
import com.complexible.stardog.plan.filter.ValueSolution;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.query.Binding;
import com.stardog.stark.query.BindingSet;
import com.stardog.stark.query.BindingSets;
import com.stardog.stark.query.SelectQueryResult;
import com.stardog.stark.query.impl.SelectQueryResultImpl;
import com.stardog.stark.query.io.QueryResultFormats;
import com.stardog.stark.query.io.QueryResultParsers;
import com.stardog.stark.query.io.QueryResultWriters;
import io.github.kawamuray.wasmtime.Module;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.Instance;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Val;
import io.github.kawamuray.wasmtime.Engine;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.Linker;
import io.github.kawamuray.wasmtime.Extern;
import io.github.kawamuray.wasmtime.Memory;
import io.github.kawamuray.wasmtime.wasi.WasiCtx;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static com.stardog.stark.Values.iri;
import static io.github.kawamuray.wasmtime.WasmValType.I32;
import static io.github.kawamuray.wasmtime.WasmValType.I64;
import static org.apache.commons.lang3.StringUtils.substringBetween;

public class Doc extends AbstractExpression implements UserDefinedFunction {

    static LoadingCache<URL, byte[]> loadingCache = Call.loadingCache;

    private Store<Void> store = Store.withoutData();
    private io.github.kawamuray.wasmtime.Module module;

    private static final int pluginVersion = 1;

    @Override
    public void initialize() {
        store = Store.withoutData();
        module = null;
    }

    public Doc() {
        super(new Expression[0]);
    }

    public Doc(final Doc call) {
        super(call);
    }

    @Override
    public ValueOrError evaluate(final ValueSolution valueSolution) {
        if (getArgs().size() == 1) {
            final ValueOrError[] valueOrErrors = getArgs().stream().map(e -> e.evaluate(valueSolution)).toArray(ValueOrError[]::new);
            if (Arrays.stream(valueOrErrors).noneMatch(ValueOrError::isError)) {
                final Value[] values = Arrays.stream(valueOrErrors).map(ValueOrError::value).toArray(Value[]::new);

                try {
                    final URL wasmUrl = new URL(values[0].toString() + '/' + pluginVersion);
                    try(Instance instance = initWasm(loadingCache.get(wasmUrl), valueSolution)) {

                        int input;
                        AtomicReference<Instance> instanceRef = new AtomicReference<>();
                        instanceRef.set(instance);

                        try (Func evaluateFunction = instance.getFunc(store, "doc").get()) {
                            final Integer output_pointer = evaluateFunction.call(store)[0].i32();
                            final Value output_value = readFromWasmMemory(instanceRef, "memory", output_pointer)[0];
                            if(output_value instanceof Literal && ((Literal)output_value).datatypeIRI().equals(iri("tag:stardog:api:array"))) {
                                long[] arrayLiteralValues = Arrays.stream(substringBetween(((Literal) output_value).label(), "[", "]").split(",")).mapToLong(Long::valueOf).toArray();
                                return ValueOrError.General.of(new ArrayLiteral(arrayLiteralValues));
                            } else {
                                return ValueOrError.General.of(output_value);
                            }
                        }
                    }
                } catch (MalformedURLException | ExecutionException e) {
                    return ValueOrError.Error;
                }
            } else {
                return ValueOrError.Error;
            }
        } else {
            return ValueOrError.Error;
        }
    }

    private Instance initWasm(byte[] wasmBytes, ValueSolution valueSolution) {
        AtomicReference<Instance> instanceRef = new AtomicReference<>();
        try(Engine engine = store.engine()) {
            if(module == null) {
                module = Module.fromBinary(engine, wasmBytes);
            }

            Func mappingDictionaryGetFunc = WasmFunctions.wrap(store, I64, I32, (id) -> {
                final Value value = valueSolution.getDictionary().getValue(id.longValue());

                Pair<Integer, Integer> buf = null;
                //TODO fix this. possible NPE
                try {
                    buf = writeToWasmMemory(instanceRef, "memory", Lists.newArrayList(value).toArray(new Value[1]));
                } catch (IOException e) {
                    //TODO ???
                }

                return buf.first;
            });

            Func mappingDictionaryAddFunc = WasmFunctions.wrap(store, I32, I64, (addr) ->
                    valueSolution.getDictionary().add(readFromWasmMemory(instanceRef, "memory", addr.intValue())[0]));

            Func pluginVersionFunc = WasmFunctions.wrap(store, I32, () -> pluginVersion());

            try (Linker linker = new Linker(engine)) {
                linker.define("env", "mappingDictionaryAdd", Extern.fromFunc(mappingDictionaryAddFunc));
                linker.define("env", "mappingDictionaryGet", Extern.fromFunc(mappingDictionaryGetFunc));
                linker.define("env", "pluginVersion", Extern.fromFunc(pluginVersionFunc));
                linker.module(store, "", module);

                WasiCtx.addToLinker(linker);
                instanceRef.set(linker.instantiate(store, module));
            }
        }
        return instanceRef.get();
    }

    private Pair<Integer, Integer> writeToWasmMemory(AtomicReference<Instance> instanceRef, String name, Value[] values) throws IOException {

        final List<String> vars = Lists.newArrayListWithCapacity(values.length);
        BindingSets.Builder bindingSetsBuilder = BindingSets.builder();
        IntStream.range(0, values.length).forEach(i -> {
            vars.add(String.format("value_%d", i));
            bindingSetsBuilder.add(vars.get(i), values[i]);
        });
        List<BindingSet> bindings = Arrays.asList(bindingSetsBuilder.build());

        final SelectQueryResult queryResult = new SelectQueryResultImpl(vars, bindings);
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        QueryResultWriters.write(queryResult, byteArrayOutputStream, QueryResultFormats.JSON);

        Integer input_pointer;
        try (Func mallocFunction = instanceRef.get().getFunc(store, "malloc").get();) {
            byteArrayOutputStream.write('\0');
            mallocFunction.call(store, Val.fromI32(byteArrayOutputStream.toByteArray().length))[0].i32();
            input_pointer = mallocFunction.call(store, Val.fromI32(byteArrayOutputStream.toByteArray().length))[0].i32();
        }

        try(Memory memory = instanceRef.get().getMemory(store, name).get()) {
            ByteBuffer memoryBuffer = memory.buffer(store);
            byte[] input = byteArrayOutputStream.toByteArray();
            int pages = (int) Math.ceil(input.length / 64.0);
            if (pages > memory.size(store)) {
                memory.grow(store, pages - memory.size(store));
            }

            memoryBuffer.position(input_pointer);
            memoryBuffer.put(input);
        }
        return Pair.create(input_pointer,byteArrayOutputStream.toByteArray().length);
    }

    private Value[] readFromWasmMemory(AtomicReference<Instance> instanceRef, String name, int output_pointer) {
        try (Memory memory = instanceRef.get().getMemory(store, name).get()) {
            try {
                SelectQueryResult selectQueryResult = QueryResultParsers.readSelect(new ByteArrayInputStream(readResult(store, memory, output_pointer).toByteArray()), QueryResultFormats.JSON);
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

    private void free(AtomicReference<Instance> instanceRef, Pair<Integer, Integer> pointer) {
        try (Func freeFunction = instanceRef.get().getFunc(store, "free").get();) {
            freeFunction.call(store, Val.fromI32(pointer.first), Val.fromI32(pointer.second));
        }
    }

    private static byte[] getWasm(final URL wasmUrl) throws IOException {
        final ByteArrayOutputStream baos;

        URLConnection conn = wasmUrl.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.connect();

        baos = new ByteArrayOutputStream();
        IOUtils.copy(conn.getInputStream(), baos);
        return baos.toByteArray();
    }

    private ByteArrayOutputStream readResult(final Store store, final Memory memory, final Integer output_pointer) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteBuffer memoryBuffer = memory.buffer(store);

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

    public int pluginVersion() {
        return pluginVersion;
    }

    @Override
    public String getName() {
        return WasmVocabulary.doc.toString();
    }

    @Override
    public List<String> getNames() {
        return Lists.newArrayList(getName());
    }

    @Override
    public Doc copy() {
        return new Doc(this);
    }

    @Override
    public void accept(final ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }
}