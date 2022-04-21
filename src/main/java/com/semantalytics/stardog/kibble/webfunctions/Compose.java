package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.stardog.plan.filter.ExpressionVisitor;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.AbstractFunction;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Range;
import com.stardog.stark.*;

import java.util.*;
import java.util.Map;

public final class Compose extends AbstractFunction implements UserDefinedFunction {

    private static final WebFunctionVocabulary names = WebFunctionVocabulary.compose;

    static Map<String, Queue<String>> compositionMap = new HashMap<>();

    public Compose() {
        super(Range.atLeast(2), names.getNames().toArray(new String[0]));
    }

    public Compose(final Compose compose) {
        super(compose);
    }

    @Override
    protected ValueOrError internalEvaluate(Value... values) {

        final BNode compositeFunctionName = Values.bnode();
        final ArrayDeque<String> functionComposition = new ArrayDeque<>();

        for(final Value value : values) {
            final String functionName;
            if (assertLiteral(values[0])) {
                functionName = ((Literal) value).label();
            } else if (values[0] instanceof IRI) {
                functionName = value.toString();
            } else {
                return ValueOrError.Error;
            }
            functionComposition.push(functionName);
        }

        compositionMap.put(compositeFunctionName.id(), functionComposition);

        return ValueOrError.General.of(compositeFunctionName);
    }

    @Override
    public Compose copy() {
        return new Compose(this);
    }

    @Override
    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    @Override
    public void initialize() {
        compositionMap = new HashMap<>();
    }
}
