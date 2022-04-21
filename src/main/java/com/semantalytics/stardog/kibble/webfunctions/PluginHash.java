package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.stardog.plan.filter.ExpressionVisitor;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.AbstractFunction;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.stardog.stark.Value;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSource;

import static com.stardog.stark.Values.literal;

public class PluginHash extends AbstractFunction implements UserDefinedFunction {

    private static final WebFunctionVocabulary names = WebFunctionVocabulary.pluginHash;

    public static final String hash;

    static {
        hash = computePluginHash();
    }

    private static String computePluginHash() {
        CodeSource codeSource = PluginHash.class.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            try (InputStream is = codeSource.getLocation().openStream()) {
                try {
                    return DigestUtils.sha256Hex(is);
                } catch(IOException e){
                    return DigestUtils.sha256Hex(Version.PLUGIN_VERSION);
                }
            } catch (IOException e) {
                return DigestUtils.sha256Hex(Version.PLUGIN_VERSION);
            }
        } else {
            return DigestUtils.sha256Hex(Version.PLUGIN_VERSION);
        }
    }

    public PluginHash() {
        super(0, names.getNames().toArray(new String[0]));
    }

    public PluginHash(final PluginHash pluginVersion) {
        super(pluginVersion);
    }

    @Override
    protected ValueOrError internalEvaluate(final Value... values) {
        return ValueOrError.General.of(literal(hash));
    }


    @Override
    public PluginHash copy() {
        return new PluginHash(this);
    }

    @Override
    public void accept(final ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }

    @Override
    public String toString() {
        return names.name();
    }

}

