package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.common.base.Pair;
import com.complexible.common.rdf.model.ArrayLiteral;
import com.complexible.stardog.index.dictionary.MappingDictionary;
import com.complexible.stardog.plan.filter.AbstractExpression;
import com.complexible.stardog.plan.filter.Expression;
import com.complexible.stardog.plan.filter.ExpressionVisitor;
import com.complexible.stardog.plan.filter.ValueSolution;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.stardog.stark.*;
import io.github.kawamuray.wasmtime.Module;
import io.github.kawamuray.wasmtime.Instance;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Val;
import io.github.kawamuray.wasmtime.Engine;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.Linker;
import io.github.kawamuray.wasmtime.Extern;
import io.github.kawamuray.wasmtime.wasi.WasiCtx;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static com.stardog.stark.Values.*;
import static io.github.kawamuray.wasmtime.WasmValType.I32;
import static io.github.kawamuray.wasmtime.WasmValType.I64;

public class CallSimple extends AbstractExpression implements UserDefinedFunction {

    private static final WebFunctionVocabulary names = WebFunctionVocabulary.call;

    @Override
        public void initialize() {
    }

    public CallSimple() {
        super(new Expression[0]);
    }

    public CallSimple(final CallSimple call) {
        super(call);
    }

    //TODO try to keep object allocation to a minimum
    @Override
    public ValueOrError evaluate(final ValueSolution valueSolution) {
        if (getArgs().size() >= 1) {

            final ValueOrError[] valueOrErrors = getArgs().stream().map(e -> e.evaluate(valueSolution)).toArray(ValueOrError[]::new);
            if (Arrays.stream(valueOrErrors).noneMatch(ValueOrError::isError)) {
                final Value[] values = Arrays.stream(valueOrErrors).map(ValueOrError::value).toArray(Value[]::new);

                try {
                    final URL wasmUrl = StardogWasm.getWasmUrl(values[0]);
                    try(final Instance instance = initWasm(wasmUrl, valueSolution.getDictionary())) {

                        try {
                            final AtomicReference<Instance> instanceRef = new AtomicReference<>(instance);
                            final Pair<Integer, Integer> input = StardogWasm.writeToWasmMemory(instanceRef, "memory", Arrays.stream(values).skip(1).toArray(Value[]::new));

                            try (final Func evaluateFunction = instance.getFunc(StardogWasm.store, StardogWasm.WASM_FUNCTION_EVALUATE).get()) {
                                final Integer output_pointer = evaluateFunction.call(StardogWasm.store, Val.fromI32(input.first))[0].i32();
                                StardogWasm.free(instanceRef, input);
                                final Value output_value = StardogWasm.readFromWasmMemory(instanceRef, "memory", output_pointer)[0];
                                //TODO should get binding value_0 not just assume that its the first and only binding
                                if(output_value instanceof Literal && ((Literal)output_value).datatypeIRI().equals(iri("tag:stardog:api:array"))) {
                                    return ValueOrError.General.of(ArrayLiteral.coerce((Literal)output_value));
                                } else {
                                    return ValueOrError.General.of(output_value);
                                }
                            }
                        } catch (IOException e) {
                            return ValueOrError.Error;
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

    private static Instance initWasm(final URL wasmUrl, MappingDictionary mappingDictionary) throws ExecutionException {
        final AtomicReference<Instance> instanceRef = new AtomicReference<>();
        try(final Engine engine = StardogWasm.store.engine()) {
            final Module module = StardogWasm.loadingCache.get(wasmUrl);

            final Func mappingDictionaryGetFunc = WasmFunctions.wrap(StardogWasm.store, I64, I32, (id) -> {
                final Value value = mappingDictionary.getValue(id);

                Pair<Integer, Integer> buf = null;
                //TODO fix this. possible NPE
                try {
                    buf = StardogWasm.writeToWasmMemory(instanceRef, "memory", new Value[] {value});
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
        return instanceRef.get();
    }

    @Override
    public String getName() {
        return names.getImmutableName();
    }

    @Override
    public List<String> getNames() {
        return names.getNames();
    }

    @Override
    public CallSimple copy() {
        return new CallSimple(this);
    }

    @Override
    public void accept(final ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }
}