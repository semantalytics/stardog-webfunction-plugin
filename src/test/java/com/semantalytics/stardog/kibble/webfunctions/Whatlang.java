package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.query.SelectQueryResult;
import org.junit.Test;

import java.util.Optional;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertStringLiteral;
import static org.assertj.core.api.Assertions.assertThat;

public class Whatlang extends AbstractStardogTest {

    @Test
    public void testLanguageDetect() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf") +
                "prefix f: <file:src/main/rust/function_string_lang/whatlang/target/wasm32-unknown-unknown/release/> " +
                " select ?result where { bind(wf:call(str(f:whatlang.wasm), \"what language do you think this is?\") AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            final Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            final Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.toString()).isEqualTo("\"what language do you think this is?\"@en");
            assertThat(aResult).isExhausted();
        }
    }
}
