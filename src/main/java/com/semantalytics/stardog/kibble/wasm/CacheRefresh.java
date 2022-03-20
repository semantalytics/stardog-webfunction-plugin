package com.semantalytics.stardog.kibble.wasm;

import com.complexible.stardog.plan.filter.ExpressionVisitor;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.AbstractFunction;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.google.common.collect.Range;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;

import java.net.MalformedURLException;
import java.net.URL;

public class CacheRefresh extends AbstractFunction implements UserDefinedFunction {

    public CacheRefresh() {
        super(Range.all(), WasmVocabulary.cacheRefresh.toString());
    }

    public CacheRefresh(final CacheRefresh clearCache) {
        super(clearCache);
    }

    @Override
    protected ValueOrError internalEvaluate(final Value... values) {
        if(values.length == 0) {
            Call.loadingCache.asMap().keySet().stream().forEach(Call.loadingCache::refresh);
        } else {
            for (Value value : values) {
                try {
                    final URL wasmUrl;
                    if (assertIRI(value)) {
                        wasmUrl = new URL(value.toString() + '/' + Call.pluginVersion());
                    } else if (assertLiteral(value)) {
                        wasmUrl = new URL(((Literal) value).label() + '/' + Call.pluginVersion());
                    } else {
                        wasmUrl = null;
                    }
                    Call.loadingCache.refresh(wasmUrl);
                } catch (MalformedURLException e) {
                    return ValueOrError.Error;
                }
            }
        }
        return ValueOrError.General.of(null);
    }

    @Override
    public CacheRefresh copy() {
        return new CacheRefresh(this);
    }

    @Override
    public void accept(final ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }
}

