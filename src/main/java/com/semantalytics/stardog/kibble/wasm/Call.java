package com.semantalytics.stardog.kibble.wasm;

import com.complexible.common.rdf.model.ArrayLiteral;
import com.complexible.stardog.index.dictionary.MappingDictionary;
import com.complexible.stardog.plan.filter.AbstractExpression;
import com.complexible.stardog.plan.filter.Expression;
import com.complexible.stardog.plan.filter.ExpressionVisitor;
import com.complexible.stardog.plan.filter.ValueSolution;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.google.common.cache.*;
import com.google.common.collect.Lists;
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
import io.ipfs.cid.Cid;
import io.ipfs.multihash.Multihash;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static io.github.kawamuray.wasmtime.WasmValType.I32;
import static io.github.kawamuray.wasmtime.WasmValType.I64;
import static java.util.stream.Collectors.*;

public class Call extends AbstractExpression implements UserDefinedFunction {

    /* NOTES
    function to list functions?
    function to get docs wasm:doc?
    support ipfs:// functions
    caching wasm?
    how to force a reload?
    function versioning? This would be helped with ipfs
    do I need to handle expanding memory????
     */

    private static final long versionID = 1L;

    public Call() {
        super(new Expression[0]);
    }

    public Call(final Call call) {
        super(call);
    }

    @Override
    public ValueOrError evaluate(final ValueSolution valueSolution) {
        if (getArgs().size() > 0) {
            final ValueOrError[] valueOrErrors = getArgs().stream().map(e -> e.evaluate(valueSolution)).toArray(ValueOrError[]::new);
            if (Arrays.stream(valueOrErrors).noneMatch(ValueOrError::isError)) {
                final Value[] values = Arrays.stream(valueOrErrors).map(ValueOrError::value).toArray(Value[]::new);

                final List<String> vars = Lists.newArrayListWithCapacity(values.length);
                List<BindingSet> bindings = IntStream.range(0, values.length).mapToObj(i -> {
                    vars.add(String.format("value[%d]", i));
                    return BindingSets.builder().add(vars.get(i), values[i]).build();
                }).collect(toList());

                //I screwd this up. I need to be adding multiple bindings to the binding set not multiple binding sets

                final SelectQueryResult queryResult = new SelectQueryResultImpl(vars, bindings);
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                try {
                    QueryResultWriters.write(queryResult, byteArrayOutputStream, QueryResultFormats.JSON);
                } catch (IOException e) {
                    return ValueOrError.Error;
                }

                final URL wasmUrl;

                try {
                    wasmUrl = new URL(values[0].toString() + '/' + versionID);
                } catch (MalformedURLException e) {
                    return ValueOrError.Error;
                }

                try (Store store = new Store();
                     Linker linker = new Linker(store);
                     Engine engine = store.engine();
                     Module module = Module.fromBinary(engine, getWasm(wasmUrl))) {
                    AtomicReference<Memory> memRef = new AtomicReference<>();


                  //I'm almost positive I screwed something up here but it should be close. It gets really confusing which direction
                  //things are going

                    Func mappingDictionaryGet = WasmFunctions.wrap(store, I64, I32, I32, I64, (id, bufAddr, bufLen) -> {

                        ByteBuffer buf = memRef.get().buffer();
                        buf.position(bufAddr);
                        final Value value = valueSolution.getDictionary().getValue(id);
                        buf.put(value.toString().getBytes(StandardCharsets.UTF_8));
                        return null;
                    });

                    Func mappingDictionaryAdd = WasmFunctions.wrap(store, I32, I32, I64, (bufAddr, bufLen) -> {
                        ByteBuffer buf = memRef.get().buffer();
                        char[] value = new char[bufLen];
                        buf.asCharBuffer().get(value, bufAddr, bufLen);
                        long myid = valueSolution.getDictionary().add(Values.resource(value.toString()));

                        return Val.fromI64(myid).i64();
                    });

                    Collection<Extern> imports = Arrays.asList(Extern.fromFunc(mappingDictionaryGet), Extern.fromFunc(mappingDictionaryAdd));

                    try (Instance instance = new Instance(store, module, imports)) {
                        Integer input_pointer = instance.getFunc("malloc").get().call(Val.fromI32(byteArrayOutputStream.toByteArray().length))[0].i32();

                        Memory memory = instance.getMemory("memory").get();
                        memRef.set(memory);
                        ByteBuffer memoryBuffer = memory.buffer();
                        byte[] input = byteArrayOutputStream.toByteArray();
                        int pages = (int)Math.ceil(input.length / 64.0);
                        if(pages > memory.size()) {
                            memory.grow(pages - memory.size());
                        }
                        memoryBuffer.position(input_pointer);
                        memoryBuffer.put(input);

                        final Integer output_pointer = instance.getFunc("evaluate").get().call(Val.fromI32(input_pointer))[0].i32();

                        final SelectQueryResult selectQueryResult;
                        try {
                            selectQueryResult = QueryResultParsers.readSelect(new ByteArrayInputStream(readResult(memory, output_pointer).toByteArray()), QueryResultFormats.JSON);
                        } catch (IOException e) {
                            return ValueOrError.Error;
                        }

                        final Optional<BindingSet> bs = selectQueryResult.stream().findFirst();

                        try {
                            if (bs.isPresent()) {
                                if (bs.get().size() > 1) {
                                    final MappingDictionary mappingDictionary = valueSolution.getDictionary();
                                    final long[] ids = bs.get().stream().map(b -> b.get()).mapToLong(v -> mappingDictionary.add(v)).toArray();
                                    return ValueOrError.General.of(new ArrayLiteral(ids));
                                } else if (bs.get().size() == 1) {
                                    final Optional<Binding> firstVar = bs.get().stream().findFirst();
                                    if (firstVar.isPresent()) {
                                        return ValueOrError.General.of(firstVar.get().value());
                                    } else {
                                        return ValueOrError.Error;
                                    }
                                } else {
                                    return ValueOrError.Error;
                                }
                            } else {
                                return ValueOrError.Error;
                            }
                        } finally {
                            instance.getFunc("free").get().call(Val.fromI32(input_pointer), Val.fromI32(byteArrayOutputStream.toByteArray().length));
                        }
                    }
                } catch (IOException e) {
                    return ValueOrError.Error;
                }
            } else {
                return ValueOrError.Error;
            }
        } else {
            return ValueOrError.Error;
        }
    }

    private byte[] getWasm(final URL wasmUrl) throws IOException {
        final ByteArrayOutputStream baos;

        URLConnection conn = wasmUrl.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.connect();

        baos = new ByteArrayOutputStream();
        IOUtils.copy(conn.getInputStream(), baos);
        return baos.toByteArray();
    }

    private ByteArrayOutputStream readResult(final Memory memory, final Integer output_pointer) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder output = new StringBuilder();
        ByteBuffer memoryBuffer = memory.buffer();

        for (Integer i = output_pointer, max = memoryBuffer.limit(); i < max; ++i) {
            final byte[] b = new byte[1];
            memoryBuffer.position(i);
            memoryBuffer.get(b);

            if (b[0] == 0) {
                break;
            }
            baos.write(b[0]);

            output.appendCodePoint(b[0]);
        }

        return baos;
    }

    @Override
    public String getName() {
        return WasmVocabulary.call.toString();
    }

    @Override
    public List<String> getNames() {
        return Lists.newArrayList(getName());
    }

    @Override
    public Call copy() {
        return new Call(this);
    }

    @Override
    public void accept(final ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }
}
