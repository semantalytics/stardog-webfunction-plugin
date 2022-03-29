package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.query.SelectQueryResult;
import org.junit.Test;

import java.util.Optional;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertStringLiteral;
import static org.assertj.core.api.Assertions.assertThat;

public class TestSum extends AbstractStardogTest {

    @Test
    public void testSum() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf") +
        " select (wf:agg(\"file:rust/aggregate/sum/target/wasm32-unknown-unknown/release/sum.wasm\", ?a) AS ?result)  WHERE { VALUES ?a { 1 2 3 1}} ";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            final Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            final Literal aLiteral = ((Literal)aValue);
            assertThat(Literal.longValue(aLiteral)).isEqualTo(7);
            assertThat(aResult).isExhausted();
        }
    }

    @Test
    public void testSumIpns() {

    final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf") +
            " prefix f: <ipns://wf.semantalytics.com/stardog/aggregate/> " +
            " select (wf:agg(f:sum, ?a) AS ?result)  WHERE { VALUES ?a { 1 2 3 1 }} ";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

        assertThat(aResult).hasNext();
        final Optional<Value> aPossibleValue = aResult.next().value("result");
        assertThat(aPossibleValue).isPresent();
        final Value aValue = aPossibleValue.get();
        assertThat(assertStringLiteral(aValue));
        final Literal aLiteral = ((Literal)aValue);
        assertThat(Literal.longValue(aLiteral)).isEqualTo(7);
        assertThat(aResult).isExhausted();
    }
}


}
