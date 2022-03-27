package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.query.SelectQueryResult;
import org.junit.Test;

import java.util.Optional;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertStringLiteral;
import static org.assertj.core.api.Assertions.assertThat;

public class TestCall extends AbstractStardogTest {

    @Test
    public void testToUpper() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf") +
            "prefix f: <file:rust/toupper/target/wasm32-unknown-unknown/release/> " +
            " select ?result where { bind(wf:call(f:toUpper, \"stardog\") AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            final Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            final Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("STARDOG");
            assertThat(aResult).isExhausted();
        }
    }

    @Test
    public void testDictionaryMapperAdd() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf") +
                "prefix f: <file:rust/test-mapping-dictionary-add/target/wasm32-unknown-unknown/release/> " +
                " select ?result where { unnest(wf:call(f:testMappingDictionaryAdd, \"stardog\") AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            final Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            final Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("STARDOG");
            assertThat(aResult).isExhausted();
        }
    }

    @Test
    public void testDictionaryMapperGet() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf") +
                "prefix f: <file:rust/test-mapping-dictionary-get/target/wasm32-unknown-unknown/release/> " +
                "select (wf:call(f:testMappingDictionaryGet, set(?a)) AS ?result) WHERE { values ?a {\"stardog\"} }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            final Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            final Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("STARDOG");
            assertThat(aResult).isExhausted();
        }
    }

    public void testDictionaryMapperGetConstant() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf") +
                "prefix f: <file:rust/test-mapping-dictionary-get/target/wasm32-unknown-unknown/release/> " +
                "select (wf:call(f:testMappingDictionaryGet, ?al) AS ?result) WHERE { bind(set(\"stardog\") as ?al) }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            final Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            final Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("STARDOG");
            assertThat(aResult).isExhausted();
        }
    }


}
