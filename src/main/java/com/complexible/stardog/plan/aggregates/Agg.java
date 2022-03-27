package com.complexible.stardog.plan.aggregates;

import com.complexible.common.base.Pair;
import com.complexible.common.rdf.model.ArrayLiteral;
import com.complexible.stardog.index.dictionary.MappingDictionary;
import com.complexible.stardog.plan.filter.Expression;
import com.complexible.stardog.plan.filter.ValueSolution;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.google.common.base.Preconditions;
import com.semantalytics.stardog.kibble.webfunctions.StardogWasm;
import com.semantalytics.stardog.kibble.webfunctions.WebFunctionVocabulary;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import io.github.kawamuray.wasmtime.*;
import io.github.kawamuray.wasmtime.wasi.WasiCtx;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static com.stardog.stark.Values.iri;
import static io.github.kawamuray.wasmtime.WasmValType.I32;
import static io.github.kawamuray.wasmtime.WasmValType.I64;
import static org.apache.commons.lang3.StringUtils.substringBetween;

public class Agg extends AbstractAggregate implements UserDefinedFunction {

    private static final int pluginVersion = 1;
    private Instance instance;

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void reset() {
        instance.close();
        instance = null;
    }

    public Agg() {
        super(WebFunctionVocabulary.agg.toString());
    }

    public Agg(final Agg agg) {
        super(agg);
        this.instance = agg.instance;
    }

    @Override
    protected ValueOrError compute(final ValueSolution valueSolution) {

        final ValueOrError[] valueOrErrors = getArgs().stream().map(e -> e.evaluate(valueSolution)).toArray(ValueOrError[]::new);
        if (Arrays.stream(valueOrErrors).noneMatch(ValueOrError::isError)) {
            final Value[] values = Arrays.stream(valueOrErrors).map(ValueOrError::value).toArray(Value[]::new);

            try {
                final URL wasmUrl;
                if(values[0] instanceof Literal) {
                    wasmUrl = new URL(((Literal) values[0]).label());
                } else {
                    wasmUrl = new URL(values[0].toString() + '/' + pluginVersion);
                }
                final Instance instance = initWasm(wasmUrl, valueSolution.getDictionary());

                    int input;
                    try {
                        final AtomicReference<Instance> instanceRef = new AtomicReference<>(instance);
                        final Pair<Integer, Integer> input_pointer = StardogWasm.writeAggregateArgsToWasmMemory(instanceRef, valueSolution.getMultiplicity(), "memory", values);
                        input = StardogWasm.writeToWasmMemory(instanceRef, "memory", values).first;
                        StardogWasm.free(instanceRef, input_pointer);

                        try (final Func evaluateFunction = instance.getFunc(StardogWasm.store, StardogWasm.WASM_FUNCTION_AGGREGATE).get()) {
                            final Integer output_pointer = evaluateFunction.call(StardogWasm.store, Val.fromI32(input))[0].i32();
                            final Value output_value = StardogWasm.readFromWasmMemory(instanceRef, "memory", output_pointer)[0];
                            if(output_value instanceof Literal && ((Literal)output_value).datatypeIRI().equals(iri("tag:stardog:api:array"))) {
                                final long[] arrayLiteralValues = Arrays.stream(substringBetween(((Literal) output_value).label(), "[", "]").split(",")).mapToLong(Long::valueOf).toArray();
                                return ValueOrError.General.of(new ArrayLiteral(arrayLiteralValues));
                            } else {
                                return ValueOrError.General.of(output_value);
                            }
                        }
                    } catch (IOException e) {
                        return ValueOrError.Error;
                    }


            } catch (MalformedURLException | ExecutionException e) {
                return ValueOrError.Error;
            }
        } else {
            return ValueOrError.Error;
        }
    }

    @Override
    public void setArgs(final List<Expression> theArgs) {
        Preconditions.checkArgument(theArgs.size() >= 1, "Agg web function takes only at least one argument, 0 found");
        super.setArgs(theArgs);
    }

    @Override
    protected ValueOrError _getValue() {
        try (final Func evaluateFunction = instance.getFunc(StardogWasm.store, StardogWasm.WASM_FUNCTION_GET_VALUE).get()) {
            final Integer output_pointer = evaluateFunction.call(StardogWasm.store)[0].i32();
            final Value output_value = StardogWasm.readFromWasmMemory(new AtomicReference<Instance>(instance), "memory", output_pointer)[0];
            if (output_value instanceof Literal && ((Literal) output_value).datatypeIRI().equals(iri("tag:stardog:api:array"))) {
                final long[] arrayLiteralValues = Arrays.stream(substringBetween(((Literal) output_value).label(), "[", "]").split(",")).mapToLong(Long::valueOf).toArray();
                return ValueOrError.General.of(new ArrayLiteral(arrayLiteralValues));
            } else {
                return ValueOrError.General.of(output_value);
            }
        } finally {
            if(instance != null) {
                instance.close();
                instance = null;
            }
        }
    }

    @Override
    protected ValueOrError aggregate(final long theMultiplicity, final Value theValue, final Value... theOtherValues) {
        // overriding compute
        return null;
    }

    @Override
    public Aggregate copy() {
        return new Agg(this);
    }

    private Instance initWasm(final URL wasmUrl, MappingDictionary mappingDictionary) throws ExecutionException {
        if(instance == null) {
            final AtomicReference<Instance> instanceRef = new AtomicReference<>();
            try (final Engine engine = StardogWasm.store.engine()) {
                final Module module = StardogWasm.loadingCache.get(wasmUrl);

                final Func mappingDictionaryGetFunc = WasmFunctions.wrap(StardogWasm.store, I64, I32, (id) -> {
                    final Value value = mappingDictionary.getValue(id);

                    Pair<Integer, Integer> buf = null;
                    //TODO fix this. possible NPE
                    try {
                        buf = StardogWasm.writeToWasmMemory(instanceRef, "memory", new Value[]{value});
                    } catch (IOException e) {
                        //TODO ???
                    }

                    return buf.first;
                });

                final Func mappingDictionaryAddFunc = WasmFunctions.wrap(StardogWasm.store, I32, I64, (addr) ->
                        mappingDictionary.add(StardogWasm.readFromWasmMemory(instanceRef, "memory", addr)[0]));

                try (Linker linker = new Linker(engine)) {
                    linker.define("env", StardogWasm.WASM_FUNCTION_MAPPING_DICTIONARY_ADD, Extern.fromFunc(mappingDictionaryAddFunc));
                    linker.define("env", StardogWasm.WASM_FUNCTION_MAPPING_DICTIONARY_GET, Extern.fromFunc(mappingDictionaryGetFunc));
                    linker.module(StardogWasm.store, "", module);

                    WasiCtx.addToLinker(linker);
                    instanceRef.set(linker.instantiate(StardogWasm.store, module));
                }
            }
            this.instance = instanceRef.get();
            return instanceRef.get();
        } else {
            return instance;
        }
    }

    public static int pluginVersion() {
        return pluginVersion;
    }
}