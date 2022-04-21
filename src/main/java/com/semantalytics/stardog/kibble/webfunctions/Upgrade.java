package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.common.util.ServiceLoaders;
import com.complexible.stardog.StardogVersion;
import com.complexible.stardog.plan.filter.ExpressionVisitor;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.filter.functions.AbstractFunction;
import com.complexible.stardog.plan.filter.functions.FunctionRegistry;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;
import com.complexible.stardog.protocols.http.server.StardogHttpServiceLoader;
import com.google.common.collect.Range;
import com.google.common.io.Resources;
import com.stardog.stark.Value;
import com.stardog.stark.impl.StringLiteral;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;

import static com.stardog.stark.Values.*;

public class Upgrade extends AbstractFunction implements UserDefinedFunction {

    public Upgrade() {
        super(Range.atMost(1), WebFunctionVocabulary.upgrade.toString());
    }

    public Upgrade(final Upgrade upgrade) {
        super(upgrade);
    }

    @Override
    protected ValueOrError internalEvaluate(final Value... values) {
        final URL newJarUrl;
        final String newJarName;
        if(values.length == 1 && assertStringLiteral(values[0])) {
            if(((StringLiteral)values[0]).label().matches("^\\d+.\\d+.\\d$")) {
                final String requestedPluginVersion = ((StringLiteral)values[0]).label();
                newJarName = "semantalytics-stardog-webfunction-" + requestedPluginVersion + "-sd" + StardogVersion.VERSION;
            } else {
                return ValueOrError.General.of(literal(values[0] + " is not a valid version"));
            }
        } else {
            URL latest = null;
            try {
                latest = new URL("ipns://wf.semantalytics.com/stardog/plugin/latest");
            } catch (MalformedURLException e) {
                return ValueOrError.General.of(literal(e.getMessage()));
            }
            try {
                newJarName = Resources.toString(latest, StandardCharsets.UTF_8 );
            } catch (IOException e) {
                return ValueOrError.General.of(literal("Unable to retrieve latest plugin name from " + latest));
            }
        }

        try {
            newJarUrl = new URL("ipns://wf.semantalytics.com/stardog/plugin/" + newJarName);
        } catch (MalformedURLException e) {
            return ValueOrError.General.of(literal("Unable to retrieve plugin from ipns://wf.semantalytics.com/stardog/plugin/" + newJarName));
        }

        File tempJar = null;
        try {
            tempJar = File.createTempFile("stardog-webfunction-plugin-upgrade", null);
        } catch (IOException e) {
            return ValueOrError.General.of(literal("Unable to create temp file"));
        }

        try {
            Files.copy(newJarUrl.openStream(), tempJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return ValueOrError.General.of(literal("Unable to retrieve plugin from " + newJarUrl));
        }

        CodeSource codeSource = this.getClass().getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            URL oldJar = codeSource.getLocation();

            try {
                Files.delete(Paths.get(oldJar.toURI()));
            } catch (IOException e) {
                return ValueOrError.General.of(literal("Unable to delete old plugin " + oldJar));
            } catch (URISyntaxException e) {
                return ValueOrError.General.of(literal(e.getMessage()));
            }
            try {
                Files.move(tempJar.toPath(), Paths.get(oldJar.toURI()).getParent().resolve(newJarName));
            } catch (IOException e) {
                return ValueOrError.General.of(literal("Unable move jar into Stardog class path and old plugin has been removed."));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            try {
                return ValueOrError.General.of(literal("Successfully upgraded " + Paths.get(oldJar.toURI()).getFileName() + " => " + newJarName ));
            } catch (URISyntaxException e) {
                return ValueOrError.Error;
            }
        } else {
            return ValueOrError.General.of(literal("Unable to locate current plugin. Please manually upgrade"));
        }
    }

    @Override
    public Upgrade copy() {
        return new Upgrade(this);
    }

    @Override
    public void accept(final ExpressionVisitor expressionVisitor) {
        expressionVisitor.visit(this);
    }
}
