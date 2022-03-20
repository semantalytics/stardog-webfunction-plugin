package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.query.SelectQueryResult;
import org.junit.Test;

import java.util.Optional;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertStringLiteral;
import static org.assertj.core.api.Assertions.assertThat;

public class TestCache extends AbstractStardogTest {

    @Test
    public void testCacheList() {
        final String cacheClearQuery = WasmVocabulary.sparqlPrefix("wf") +
                " select ?result where { unnest(wf:cacheClear() AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(cacheClearQuery).execute()) {
            aResult.stream().count();
        }

        final String aQuery = WasmVocabulary.sparqlPrefix("wf") +
                "prefix f: <file:rust/toupper/target/wasm32-unknown-unknown/release/> " +
                " select ?result where { bind(wf:call(f:toUpper, \"stardog\") AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {
            aResult.stream().count();
        }

        final String listCacheQuery = WasmVocabulary.sparqlPrefix("wf") +
                " select ?result where { unnest(wf:cacheList() AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(listCacheQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            final Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            final Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("file:rust/toupper/target/wasm32-unknown-unknown/release/toUpper/1");
            assertThat(aResult).isExhausted();
        }
    }

    @Test
    public void testCacheClear() {

        final String aQuery = WasmVocabulary.sparqlPrefix("wf") +
                "prefix f: <file:rust/toupper/target/wasm32-unknown-unknown/release/> " +
                " select ?result where { bind(wf:call(f:toUpper, \"stardog\") AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {
            aResult.stream().count();
        }

        final String listCacheQuery = WasmVocabulary.sparqlPrefix("wf") +
                " select ?result where { unnest(wf:cacheClear() AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(listCacheQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isEmpty();
        }
    }

    @Test
    public void testCacheLoadFromUrl() {
        final String cacheClearQuery = WasmVocabulary.sparqlPrefix("wf") +
                " select ?result where { unnest(wf:cacheClear() AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(cacheClearQuery).execute()) {
            aResult.stream().count();
        }

        final String aQuery = WasmVocabulary.sparqlPrefix("wf") +
                "prefix f: <file:rust/toupper/target/wasm32-unknown-unknown/release/> " +
                " select ?result where { bind(wf:cacheLoad(f:toUpper) AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {
            aResult.stream().count();
        }

        final String listCacheQuery = WasmVocabulary.sparqlPrefix("wf") +
                " select ?result where { unnest(wf:cacheList() AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(listCacheQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            final Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            final Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("file:rust/toupper/target/wasm32-unknown-unknown/release/toUpper/1");
            assertThat(aResult).isExhausted();
        }
    }

    @Test
    public void testCacheLoadFromLiteral() {
        final String cacheClearQuery = WasmVocabulary.sparqlPrefix("wf") +
                " select ?result where { unnest(wf:cacheClear() AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(cacheClearQuery).execute()) {
            aResult.stream().count();
        }

        final String aQuery = WasmVocabulary.sparqlPrefix("wf") +
                "prefix f: <file:rust/toupper/target/wasm32-unknown-unknown/release/> " +
                " select ?result where { bind(wf:cacheLoad(str(f:toUpper)) AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {
            aResult.stream().count();
        }

        final String listCacheQuery = WasmVocabulary.sparqlPrefix("wf") +
                " select ?result where { unnest(wf:cacheList() AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(listCacheQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            final Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            final Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("file:rust/toupper/target/wasm32-unknown-unknown/release/toUpper/1");
            assertThat(aResult).isExhausted();
        }
    }
}
