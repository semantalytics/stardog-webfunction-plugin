package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.common.base.Pair;
import com.complexible.common.rdf.model.ArrayLiteral;
import com.complexible.stardog.plan.filter.AbstractExpression;
import com.complexible.stardog.plan.filter.Expression;
import com.complexible.stardog.plan.filter.ExpressionVisitor;
import com.complexible.stardog.plan.filter.ValueSolution;
import com.complexible.stardog.plan.filter.expr.ConstantImpl;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.FunctionRegistry;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.google.api.client.util.Lists;
import com.stardog.stark.*;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Instance;
import io.github.kawamuray.wasmtime.Val;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertLiteral;
import static com.stardog.stark.Values.iri;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public final class CallX extends AbstractExpression implements UserDefinedFunction {

    private static final WebFunctionVocabulary names = WebFunctionVocabulary.callx;

    public CallX() {
        super(new Expression[0]);
    }

    private CallX(final CallX call) {
        super(call);
    }

    @Override
    public CallX copy() {
        return new CallX(this);
    }

    @Override
    public void accept(final ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    @Override
    public ValueOrError evaluate(final ValueSolution valueSolution) {
        if (getArgs().size() >= 1) {
            final ValueOrError firstArgValueOrError = getFirstArg().evaluate(valueSolution);
            if (!firstArgValueOrError.isError()) {
                final String functionIri;

                //TODO how do I know if i should execute as webfunction or interal? Let native take precidence?
                if (assertLiteral(firstArgValueOrError.value())) {
                    functionIri = ((Literal) firstArgValueOrError.value()).label();
                } else if (firstArgValueOrError.value() instanceof IRI) {
                    functionIri = firstArgValueOrError.value().toString();
                } else if (firstArgValueOrError.value() instanceof BNode) {
                    functionIri = ((BNode) firstArgValueOrError.value()).id();
                } else {
                    return ValueOrError.Error;
                }

                final List<Expression> functionArgs = getArgs().stream().skip(1).collect(toList());

                Expression function = null;
                try {
                    if (Compose.compositionMap.containsKey(functionIri)) {
                        for (final String compositeFunction : Compose.compositionMap.get(functionIri)) {
                            if (function == null) {
                                try {
                                    function = FunctionRegistry.Instance.get(compositeFunction, functionArgs, null);
                                } catch (UnsupportedOperationException e) {
                                    function = FunctionRegistry.Instance.get(this.getName(), functionArgs, null);
                                }
                            } else {
                                try {
                                    function = FunctionRegistry.Instance.get(compositeFunction, singletonList(function), null);
                                } catch (UnsupportedOperationException e) {
                                    final List<Expression> args = Lists.newArrayList();
                                    args.add(new ConstantImpl(Values.literal(compositeFunction)));
                                    args.add(function);
                                    function = FunctionRegistry.Instance.get(this.getName(), args, null);
                                }
                        }
                        }
                    } else if (Partial.partialMap.containsKey(functionIri)) {
                        final List<Expression> partialArgs = Partial.partialMap.get(functionIri);
                        final List<ValueOrError> partialArgsValueOrError = partialArgs.stream().map(e -> e.evaluate(valueSolution)).collect(toList());
                        if (partialArgsValueOrError.stream().noneMatch(ValueOrError::isError)) {
                            final List<String> partialArgsValueString = partialArgsValueOrError.stream().map(ValueOrError::value).map(Value::toString).collect(toList());
                            functionArgs.stream().forEach(e -> {
                                if (partialArgsValueString.indexOf(WebFunctionVocabulary.var.toString()) != -1) {
                                    partialArgs.set(partialArgsValueString.indexOf(WebFunctionVocabulary.var.toString()), e);
                                } else {
                                    partialArgs.add(e);
                                }
                            });
                            try {
                                function = FunctionRegistry.Instance.get(partialArgs.get(0).toString(), partialArgs.stream().skip(1).collect(toList()), null);
                            } catch (UnsupportedOperationException e) {
                                function = FunctionRegistry.Instance.get(this.getName(), partialArgs, null);
                            }
                        } else {
                            return ValueOrError.Error;
                        }
                    } else {
                        try {
                            function = FunctionRegistry.Instance.get(functionIri, functionArgs, null);
                        } catch(UnsupportedOperationException e) {
                            final ValueOrError[] valueOrErrors = getArgs().stream().map(exp -> exp.evaluate(valueSolution)).toArray(ValueOrError[]::new);
                            if (Arrays.stream(valueOrErrors).noneMatch(ValueOrError::isError)) {
                                final Value[] values = Arrays.stream(valueOrErrors).map(ValueOrError::value).toArray(Value[]::new);

                                try {
                                    final URL wasmUrl = StardogWasm.getWasmUrl(values[0]);
                                    try(final Instance instance = StardogWasm.initWasm(wasmUrl, valueSolution.getDictionary())) {

                                        try {
                                            final AtomicReference<Instance> instanceRef = new AtomicReference<>(instance);
                                            final Pair<Integer, Integer> input = StardogWasm.writeToWasmMemory(instanceRef, "memory", Arrays.stream(values).skip(1).toArray(Value[]::new));

                                            try (final Func evaluateFunction = instance.getFunc(StardogWasm.store, StardogWasm.WASM_FUNCTION_EVALUATE).get()) {
                                                final Integer output_pointer = evaluateFunction.call(StardogWasm.store, Val.fromI32(input.first))[0].i32();
                                                StardogWasm.free(instanceRef, input);
                                                final Value output_value = StardogWasm.readFromWasmMemory(instanceRef, "memory", output_pointer)[0];
                                                if(output_value instanceof Literal && ((Literal)output_value).datatypeIRI().equals(iri("tag:stardog:api:array"))) {
                                                    return ValueOrError.General.of(ArrayLiteral.coerce((Literal)output_value));
                                                } else {
                                                    return ValueOrError.General.of(output_value);
                                                }
                                            }
                                        } catch (IOException e1) {
                                            return ValueOrError.Error;
                                        }
                                    }
                                } catch (MalformedURLException | ExecutionException e1) {
                                    return ValueOrError.Error;
                                }
                            } else {
                                return ValueOrError.Error;
                            }
                        }
                    }
                    return function.evaluate(valueSolution);
                } catch (UnsupportedOperationException e) {
                    return ValueOrError.Error;
                }
            } else {
                return ValueOrError.Error;
            }
        } else {
            return ValueOrError.Error;
        }
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
    public String toString() {
        return names.name();
    }
}