package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.query.SelectQueryResult;
import org.junit.Test;

import java.util.Optional;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertStringLiteral;
import static org.assertj.core.api.Assertions.assertThat;

public class TestInCallOutPropertyFunction extends AbstractStardogTest {

    @Test
    public void testToUpperpf() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "0.0.0") +
                "prefix f: <file:src/main/rust/target/wasm32-unknown-unknown/release/> " +
                " select ?result where {" +
                "(\"file:src/test/rust/target/wasm32-unknown-unknown/release/to_upper.wasm\" \"stardog\") wf:IN_call_OUT ?result" +
                "}";

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
    public void testToUpperpfInCallOut() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "0.0.0") +
                "prefix f: <file:src/main/rust/target/wasm32-unknown-unknown/release/> " +
                " select ?result where {" +
                "(\"file:src/test/rust/target/wasm32-unknown-unknown/release/to_upper.wasm\" \"stardog\") wf:IN_call_OUT ?result" +
                "}";

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
    public void testToUpperPfVar() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "0.0.0") +
                "prefix f: <file:src/main/rust/target/wasm32-unknown-unknown/release/> " +
                " select ?result where {" +
                "BIND(\"stardog\" as ?str)" +
                "(\"file:src/test/rust/target/wasm32-unknown-unknown/release/to_upper.wasm\" ?str) wf:IN_call_OUT ?result" +
                "}";

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
    public void testToUpperPfMultiple() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "0.0.0") +
                "prefix f: <file:src/main/rust/target/wasm32-unknown-unknown/release/> " +
                " select ?result where {" +
                "(\"file:src/test/rust/target/wasm32-unknown-unknown/release/to_upper.wasm\" ?str) wf:IN_call_OUT ?result" +
                " values ?str { \"what language do you think this is\" \"como estas\" \"bonjour\"}}";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("WHAT LANGUAGE DO YOU THINK THIS IS");

            assertThat(aResult).hasNext();
            aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("COMO ESTAS");

            assertThat(aResult).hasNext();
            aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("BONJOUR");

            assertThat(aResult).isExhausted();
        }
    }
}
