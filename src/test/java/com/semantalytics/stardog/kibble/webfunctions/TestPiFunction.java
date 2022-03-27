package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.query.BindingSet;
import com.stardog.stark.query.SelectQueryResult;
import org.junit.Test;

import java.util.Optional;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertStringLiteral;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class TestPiFunction extends AbstractStardogTest {

    @Test
    public void testPi() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf") +
                " prefix f: <file:rust/pi/target/wasm32-unknown-unknown/release/> " +
                " select ?result where { bind(wf:call(f:pi) AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            final Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            final Literal aLiteral = ((Literal)aValue);
            assertThat(Literal.floatValue(aLiteral)).isEqualTo(3.1415926535f, within(0.00001f));
            assertThat(aResult).isExhausted();
        }
    }

    @Test
    public void compareWithNativePi() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf") +
                " prefix f: <file:rust/pi/target/wasm32-unknown-unknown/release/> " +
                " select ?result (PI() as ?nativeResult) where { bind(wf:call(f:pi) AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            final BindingSet bindingSet = aResult.next();
            final Optional<Value> aPossibleValue = bindingSet.value("result");
            assertThat(aPossibleValue).isPresent();
            final Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            final Literal aLiteral = ((Literal)aValue);
            final Optional<Value> aPossibleValueNative = bindingSet.value("nativeResult");
            assertThat(aPossibleValueNative).isPresent();
            final Value aValueNative = aPossibleValue.get();
            assertThat(assertStringLiteral(aValueNative));
            final Literal aLiteralNative = ((Literal)aValueNative);
            assertThat(Literal.floatValue(aLiteral)).isEqualTo(Literal.floatValue(aLiteralNative), within(0.00001f));
            assertThat(aResult).isExhausted();
        }
    }
}
