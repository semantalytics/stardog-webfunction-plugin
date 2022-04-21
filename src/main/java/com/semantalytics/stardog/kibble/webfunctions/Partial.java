package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.stardog.plan.filter.AbstractExpression;
import com.complexible.stardog.plan.filter.Expression;
import com.complexible.stardog.plan.filter.ExpressionVisitor;
import com.complexible.stardog.plan.filter.ValueSolution;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.google.common.collect.Lists;
import com.stardog.stark.BNode;
import com.stardog.stark.Values;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public final class Partial extends AbstractExpression implements UserDefinedFunction {

    private static final WebFunctionVocabulary names = WebFunctionVocabulary.partial;

    static Map<String, List<Expression>> partialMap = new HashMap<>();

    public Partial(final Partial partial) {
        super(partial);
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
    public ValueOrError evaluate(final ValueSolution valueSolution) {

        final BNode partialFunctionName = Values.bnode();

        partialMap.put(Values.bnode().id(), getArgs().stream().collect(toList()));

        return ValueOrError.General.of(partialFunctionName);
    }

    @Override
    public Partial copy() {
        return new Partial(this);
    }

    @Override
    public void accept(ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    @Override
    public void initialize() {
        partialMap = new HashMap<>();
    }
}
