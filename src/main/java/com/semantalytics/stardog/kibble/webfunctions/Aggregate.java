package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.common.base.Pair;
import com.complexible.common.rdf.model.ArrayLiteral;
import com.complexible.stardog.index.dictionary.MappingDictionary;
import com.complexible.stardog.plan.aggregates.WebFunctionAbstractAggregate;
import com.complexible.stardog.plan.filter.Expression;
import com.complexible.stardog.plan.filter.ValueSolution;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.google.common.base.Preconditions;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import io.github.kawamuray.wasmtime.Engine;
import io.github.kawamuray.wasmtime.Extern;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Instance;
import io.github.kawamuray.wasmtime.Linker;
import io.github.kawamuray.wasmtime.Module;
import io.github.kawamuray.wasmtime.Val;
import io.github.kawamuray.wasmtime.WasmFunctions;
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

public class Aggregate extends WebFunctionAbstractAggregate implements UserDefinedFunction {

    private Instance instance;
    private static final WebFunctionVocabulary names = WebFunctionVocabulary.agg;

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void reset() {
        if(instance != null) {
            instance.close();
        }
        instance = null;
    }

    public Aggregate() {
        super(names.getNames().toArray(new String[0]));
    }

    public Aggregate(final Aggregate aggregate) {
        super(aggregate);
    }

    @Override
    protected ValueOrError compute(final ValueSolution valueSolution) {

        final ValueOrError[] valueOrErrors = getArgs().stream().map(e -> e.evaluate(valueSolution)).toArray(ValueOrError[]::new);
        if (Arrays.stream(valueOrErrors).noneMatch(ValueOrError::isError)) {
            final Value[] values = Arrays.stream(valueOrErrors).map(ValueOrError::value).toArray(Value[]::new);

            try {
                final URL wasmUrl = StardogWasm.getWasmUrl(values[0]);
                if(instance == null) {
                    instance = StardogWasm.initWasm(wasmUrl, valueSolution.getDictionary());
                }
                    try {
                        final AtomicReference<Instance> instanceRef = new AtomicReference<>(instance);
                        final Pair<Integer, Integer> input = StardogWasm.writeToWasmMemoryWithMultiplicity(instanceRef, "memory", Arrays.stream(values).skip(1).toArray(Value[]::new), valueSolution.getMultiplicity());

                        try (final Func evaluateFunction = instance.getFunc(StardogWasm.store, StardogWasm.WASM_FUNCTION_AGGREGATE).get()) {
                            final Integer output_pointer = evaluateFunction.call(StardogWasm.store, Val.fromI32(input.first))[0].i32();
                            StardogWasm.free(instanceRef, input);
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
        if (instance != null) {
            try (final Func evaluateFunction = instance.getFunc(StardogWasm.store, StardogWasm.WASM_FUNCTION_GET_VALUE).get()) {
                final Integer output_pointer = evaluateFunction.call(StardogWasm.store)[0].i32();
                final Value output_value = StardogWasm.readFromWasmMemory(new AtomicReference<Instance>(instance), "memory", output_pointer)[0];
                if (output_value instanceof Literal && ((Literal) output_value).datatypeIRI().equals(iri("tag:stardog:api:array"))) {
                    return ValueOrError.General.of(ArrayLiteral.coerce((Literal)output_value));
                } else {
                    return ValueOrError.General.of(output_value);
                }
            } finally {
                if (instance != null) {
                    instance.close();
                    instance = null;
                }
            }
        } else {
            return ValueOrError.Error;
        }
    }

    @Override
    protected ValueOrError aggregate(final long theMultiplicity, final Value theValue, final Value... theOtherValues) {
        // overriding compute
        return null;
    }

    @Override
    public com.complexible.stardog.plan.aggregates.Aggregate copy() {
        return new Aggregate(this);
    }
}