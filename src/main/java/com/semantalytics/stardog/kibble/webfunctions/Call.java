package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.common.base.Pair;
import com.complexible.common.rdf.model.ArrayLiteral;
import com.complexible.stardog.plan.filter.*;
import com.complexible.stardog.plan.filter.expr.Constant;
import com.complexible.stardog.plan.filter.expr.ConstantImpl;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.FunctionRegistry;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.google.api.client.util.Lists;
import com.stardog.stark.*;
import com.stardog.stark.query.BindingSet;
import com.stardog.stark.query.SelectQueryResult;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Instance;
import io.github.kawamuray.wasmtime.Val;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertLiteral;
import static com.stardog.stark.Values.iri;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public final class Call extends AbstractExpression implements UserDefinedFunction {

    private static final WebFunctionVocabulary names = WebFunctionVocabulary.call;

    public Call() {
        super(new Expression[0]);
    }

    private Call(final Call call) {
        super(call);
    }

    @Override
    public Call copy() {
        return new Call(this);
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
                final String functionName;

                if (assertLiteral(firstArgValueOrError.value())) {
                    functionName = ((Literal) firstArgValueOrError.value()).label();
                } else if (firstArgValueOrError.value() instanceof IRI) {
                    functionName = firstArgValueOrError.value().toString();
                } else if (firstArgValueOrError.value() instanceof BNode) {
                    functionName = ((BNode) firstArgValueOrError.value()).id();
                } else {
                    return ValueOrError.Error;
                }

                final List<Expression> functionArgs = getArgs().stream().skip(1).collect(toList());

                Expression function = null;
                try {
                    if (Compose.compositionMap.containsKey(functionName)) {
                        List<Expression> compositeFunctions = Compose.compositionMap.get(functionName).stream().map(Expressions::constant).collect(toList());
                        Collections.reverse(compositeFunctions);
                        List<Expression> initial = Stream.concat(Stream.of(compositeFunctions.get(0)), functionArgs.stream()).collect(toList());
                        function = compositeFunctions.stream().skip(1).reduce(FunctionRegistry.Instance.get(WebFunctionVocabulary.call.getImmutableName(), initial, null), (e1, e2) ->
                                FunctionRegistry.Instance.get(WebFunctionVocabulary.call.getImmutableName(), Arrays.asList(e2, e1), null));


                    } else if (Partial.partialMap.containsKey(functionName)) {
                        final List<Expression> partialArgs = Partial.partialMap.get(functionName).stream().map(Expressions::constant).collect(toList());
                        ListIterator<Expression> f = functionArgs.listIterator();
                        partialArgs.replaceAll(e -> {
                            if(WebFunctionVocabulary.var.getNames().contains(e.evaluate(valueSolution).value().toString()))  {
                                if(f.hasNext()) {
                                    return f.next();
                                } else {
                                    return e;
                                }
                            } else {
                                return e;
                            }
                        });
                        f.forEachRemaining(partialArgs::add);

                        function = FunctionRegistry.Instance.get(WebFunctionVocabulary.call.getImmutableName(), partialArgs, null);
                    } else {
                        try {
                            function = FunctionRegistry.Instance.get(functionName, functionArgs, null);
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
                                                final SelectQueryResult selectQueryResult = StardogWasm.readFromWasmMemorySelectQueryResult(instanceRef, "memory", output_pointer);
                                                //TODO this should be closed
                                                if (selectQueryResult.hasNext()) {
                                                    BindingSet bs = selectQueryResult.next();
                                                    if(bs.size() == 1) {
                                                        if (bs.get("value_0") instanceof Literal && ((Literal) bs.get("value_0")).datatypeIRI().equals(iri("tag:stardog:api:array"))) {
                                                            return ValueOrError.General.of(ArrayLiteral.coerce((Literal) bs.get("value_0")));
                                                        } else {
                                                            return ValueOrError.General.of(bs.get("value_0"));
                                                        }
                                                    } else {
                                                        //TODO needs to be ordered
                                                        return ValueOrError.General.of(new ArrayLiteral(bs.stream().mapToLong(b -> valueSolution.getDictionary().add(b.value())).toArray()));
                                                    }
                                                } else {
                                                    return ValueOrError.Error;
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