package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.query.SelectQueryResult;
import org.junit.Test;

import java.util.Optional;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertStringLiteral;
import static org.assertj.core.api.Assertions.assertThat;

public class TestPluginVersion extends AbstractStardogTest {

    @Test
    public void testPluginVersion() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "snapshot") +
                " select ?result where { bind(wf:pluginVersion() AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            final Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            final Literal aLiteral = ((Literal) aValue);
            assertThat((aLiteral.label())).isEqualTo(PluginHash.hash);
            assertThat(aResult).isExhausted();
        }
    }
}