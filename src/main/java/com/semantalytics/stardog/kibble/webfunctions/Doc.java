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
import com.complexible.stardog.security.User;
import com.google.common.collect.Lists;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import io.github.kawamuray.wasmtime.Module;
import io.github.kawamuray.wasmtime.Instance;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Engine;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.Linker;
import io.github.kawamuray.wasmtime.Extern;
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

public class Doc extends AbstractExpression implements UserDefinedFunction {

    private static final WebFunctionVocabulary names = WebFunctionVocabulary.doc;

    private static final int pluginVersion = 1;

    @Override
    public void initialize() {
    }

    public Doc() {
        super(new Expression[0]);
    }

    public Doc(final Doc call) {
        super(call);
    }

    @Override
    public ValueOrError evaluate(final ValueSolution valueSolution) {

        if (getArgs().size() >= 1) {

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
                    try(final Instance instance = initWasm(wasmUrl, valueSolution.getDictionary())) {

                        AtomicReference<Instance> instanceRef = new AtomicReference<>();
                        instanceRef.set(instance);

                        try (Func evaluateFunction = instance.getFunc(StardogWasm.store, StardogWasm.WASM_FUNCTION_DOC).get()) {
                            final Integer output_pointer = evaluateFunction.call(StardogWasm.store)[0].i32();
                            final Value output_value = StardogWasm.readFromWasmMemory(instanceRef, "memory", output_pointer)[0];
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

    private Instance initWasm(final URL wasmUrl, MappingDictionary mappingDictionary) throws ExecutionException {
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

    public int pluginVersion() {
        return pluginVersion;
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
    public Doc copy() {
        return new Doc(this);
    }

    @Override
    public void accept(final ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }
}