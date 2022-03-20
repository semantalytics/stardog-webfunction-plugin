package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.query.SelectQueryResult;
import org.junit.Test;

import java.util.Optional;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertStringLiteral;
import static org.assertj.core.api.Assertions.assertThat;

public class TestLevenshteinFunction extends AbstractStardogTest {

    @Test
    public void testLevenshtein() {

        final String aQuery = WasmVocabulary.sparqlPrefix("wasm") +
                "prefix tricks: <https://github.com/semantalytics/stardog-extensions/blob/wasmer/src/main/resources/> " +
                " select ?result where { bind(wasm:call(<file:////home/zcw100/git/stardog-wasm/rust/levenshtein/target/wasm32-unknown-unknown/release>, \"kitten\", \"sitting\") AS ?result) }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            final Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            final Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            final Literal aLiteral = ((Literal)aValue);
            assertThat(Literal.intValue(aLiteral)).isEqualTo(3);
            assertThat(aResult).isExhausted();
        }
    }

}
