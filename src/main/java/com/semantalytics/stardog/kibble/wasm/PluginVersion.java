package com.semantalytics.stardog.kibble.wasm;

import com.complexible.stardog.plan.filter.ExpressionVisitor;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.AbstractFunction;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.google.common.collect.Range;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.Values;

import java.net.MalformedURLException;
import java.net.URL;

import static com.stardog.stark.Values.*;

public class PluginVersion extends AbstractFunction implements UserDefinedFunction {

    public PluginVersion() {
        super(Range.all(), WasmVocabulary.pluginVersion.toString());
    }

    public PluginVersion(final PluginVersion pluginVersion) {
        super(pluginVersion);
    }

    @Override
    protected ValueOrError internalEvaluate(final Value... values) {
        return ValueOrError.General.of(literal(Call.pluginVersion()));
    }


    @Override
    public PluginVersion copy() {
        return new PluginVersion(this);
    }

    @Override
    public void accept(final ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }
}
