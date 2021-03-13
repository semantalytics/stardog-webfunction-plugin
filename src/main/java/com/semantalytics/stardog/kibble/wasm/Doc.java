package com.semantalytics.stardog.kibble.wasm;

import com.complexible.common.rdf.model.ArrayLiteral;
import com.complexible.stardog.index.dictionary.MappingDictionary;
import com.complexible.stardog.plan.filter.AbstractExpression;
import com.complexible.stardog.plan.filter.Expression;
import com.complexible.stardog.plan.filter.ExpressionVisitor;
import com.complexible.stardog.plan.filter.ValueSolution;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.google.common.cache.*;
import com.google.common.collect.Lists;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.Values;
import com.stardog.stark.query.Binding;
import com.stardog.stark.query.BindingSet;
import com.stardog.stark.query.BindingSets;
import com.stardog.stark.query.SelectQueryResult;
import com.stardog.stark.query.impl.SelectQueryResultImpl;
import com.stardog.stark.query.io.QueryResultFormats;
import com.stardog.stark.query.io.QueryResultParsers;
import com.stardog.stark.query.io.QueryResultWriters;
import org.apache.commons.io.IOUtils;
import org.wasmer.Instance;
import org.wasmer.Memory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class Doc extends AbstractExpression implements UserDefinedFunction {

    /* NOTES
    function to get docs wasm:doc?
    support ipfs:// functions
    caching wasm?
    how to force a reload?
    function versioning? This would be helped with ipfs
    do I need to handle expanding memory????
     */
    private RemovalListener<URL, Instance> removalListener = new RemovalListener<URL, Instance>() {
        @Override
        public void onRemoval(RemovalNotification<URL, Instance> removal) {
            Instance instance = removal.getValue();
            instance.close();
        }
    };

    private LoadingCache<URL, Instance> instanceCache = CacheBuilder.newBuilder()
            .softValues()
            .removalListener(removalListener)
            .build(new CacheLoader<URL, Instance>() {
                @Override
                public Instance load(URL url) throws IOException {
                    return new Instance(getWasm(url));
                }
            });

    public Doc() {
        super(new Expression[0]);
    }

    public Doc(final Doc call) {
        super(call);
    }

    @Override
    public ValueOrError evaluate(final ValueSolution valueSolution) {
        if (getArgs().size() == 1) {
            ValueOrError firstArgValueOrError = getFirstArg().evaluate(valueSolution);
            final URL wasmUrl;
            if (!firstArgValueOrError.isError()) {
                try {
                    wasmUrl = new URL(firstArgValueOrError.value().toString());
                } catch (MalformedURLException e) {
                    return ValueOrError.Error;
                }
            } else {
                return ValueOrError.Error;
            }

            final Instance instance;

            try {
                instance = instanceCache.get(wasmUrl);
            } catch (ExecutionException e) {
                return ValueOrError.Error;
            }

            final Integer output_pointer = (Integer) instance.exports.getFunction("doc").apply()[0];

            Memory memory = instance.exports.getMemory("memory");

            return ValueOrError.General.of(Values.literal(readResult(memory, output_pointer).toString()));

        } else {
            return ValueOrError.Error;
        }
    }

    private byte[] getWasm(final URL wasmUrl) throws IOException {
        final ByteArrayOutputStream baos;

        URLConnection conn = wasmUrl.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.connect();

        baos = new ByteArrayOutputStream();
        IOUtils.copy(conn.getInputStream(), baos);
        return baos.toByteArray();
    }

    private ByteArrayOutputStream readResult(final Memory memory, final Integer output_pointer) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder output = new StringBuilder();
        ByteBuffer memoryBuffer = memory.buffer();

        for (Integer i = output_pointer, max = memoryBuffer.limit(); i < max; ++i) {
            final byte[] b = new byte[1];
            memoryBuffer.position(i);
            memoryBuffer.get(b);

            if (b[0] == 0) {
                break;
            }
            baos.write(b[0]);

            output.appendCodePoint(b[0]);
        }

        return baos;
    }

    @Override
    public String getName() {
        return WasmVocabulary.doc.toString();
    }

    @Override
    public List<String> getNames() {
        return Lists.newArrayList(getName());
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
