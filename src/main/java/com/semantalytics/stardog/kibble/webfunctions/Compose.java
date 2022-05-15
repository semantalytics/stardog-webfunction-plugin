package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.stardog.plan.filter.*;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.FunctionRegistry;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.stardog.stark.*;
import org.apache.commons.math3.analysis.function.Exp;

import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public final class Compose extends AbstractExpression implements UserDefinedFunction {

    private static final WebFunctionVocabulary names = WebFunctionVocabulary.compose;

    static Map<String, List<Value>> compositionMap = new HashMap<>();

    public Compose() {
        super(new Expression[0]);
    }

    public Compose(final Compose compose) {
        super(compose);
    }

    @Override
    public ValueOrError evaluate(final ValueSolution valueSolution) {

        final BNode compositeFunctionName = Values.bnode();
        if(getArgs().size() >= 2) {
            List<ValueOrError> argsValueOrError = getArgs().stream().map(e -> e.evaluate(valueSolution)).collect(toList());
            if (argsValueOrError.stream().noneMatch(ValueOrError::isError)) {
                if(argsValueOrError.stream().map(ValueOrError::value).allMatch(v -> v instanceof IRI || (v instanceof Literal && EvalUtil.isStringLiteral((Literal)v)) || v instanceof BNode)) {
                    List<Value> values = argsValueOrError.stream().map(ValueOrError::value).collect(toList());
                    compositionMap.put(compositeFunctionName.id(), values);
                    return ValueOrError.General.of(compositeFunctionName);
                } else {
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
