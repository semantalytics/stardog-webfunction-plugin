package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.query.SelectQueryResult;
import org.junit.Test;

import java.util.Optional;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertStringLiteral;
import static org.assertj.core.api.Assertions.assertThat;

public class TestArrayOf extends AbstractStardogTest {

    @Test
    public void testArrayOf() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf") +
                "prefix array: <file:src/main/rust/function_array/target/wasm32-unknown-unknown/release/> " +
                " select ?result where { unnest(wf:call(str(array:of.wasm), \"star\", \"dog\") AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue1 = aResult.next().value("result");
            assertThat(aPossibleValue1).isPresent();
            final Value aValue1 = aPossibleValue1.get();
            assertThat(assertStringLiteral(aValue1));
            final Literal aLiteral = ((Literal)aValue1);
            assertThat(aLiteral.label()).isEqualTo("star");

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue2 = aResult.next().value("result");
            assertThat(aPossibleValue2).isPresent();
            final Value aValue2 = aPossibleValue2.get();
            assertThat(assertStringLiteral(aValue2));
            final Literal aLiteral2 = ((Literal)aValue2);
            assertThat(aLiteral2.label()).isEqualTo("dog");

            assertThat(aResult).isExhausted();
        }
    }
}
