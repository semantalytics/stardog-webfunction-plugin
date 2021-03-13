package com.semantalytics.stardog.kibble.wasm;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.query.SelectQueryResult;
import org.junit.Test;

import java.util.Optional;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertStringLiteral;
import static org.assertj.core.api.Assertions.assertThat;

public class TestDoc extends AbstractStardogTest {


    @Test
    public void test() {
    
        final String aQuery = WasmVocabulary.sparqlPrefix("wasm") +
                    //"select ?result where { bind(wasm:call(<https://github.com/wasmerio/wasmer-java/raw/master/examples/greet.wasm>, \"greet\", \"Stardog\") AS ?result) }";
                "prefix tricks: <https://github.com/semantalytics/stardog-extensions/blob/wasmer/src/main/resources/> " +
                     " select ?result where { bind(wasm:call(<https://github.com/semantalytics/stardog-extensions/blob/wasmer/src/main/resources/speak?raw=true>, \"woof\") AS ?result) }";

            try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

                assertThat(aResult).hasNext();
                final Optional<Value> aPossibleValue = aResult.next().value("result");
                assertThat(aPossibleValue).isPresent();
                final Value aValue = aPossibleValue.get();
                assertThat(assertStringLiteral(aValue));
                final Literal aLiteral = ((Literal)aValue);
                assertThat(Literal.intValue(aLiteral)).isEqualTo(12);
                assertThat(aResult).isExhausted();
            }
    }

    @Test
    public void testToUpper() {

        final String aQuery = WasmVocabulary.sparqlPrefix("wasm") +
                //"select ?result where { bind(wasm:call(<https://github.com/wasmerio/wasmer-java/raw/master/examples/greet.wasm>, \"greet\", \"Stardog\") AS ?result) }";
                "prefix tricks: <https://github.com/semantalytics/stardog-extensions/blob/wasmer/src/main/resources/> " +
                " select ?result where { bind(wasm:doc(<file:///tmp/jaro>) AS ?result) }";

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
    public void testFunctionInProjection() {

        final String aQuery = WasmVocabulary.sparqlPrefix("wasm") +
                //"select ?result where { bind(wasm:call(<https://github.com/wasmerio/wasmer-java/raw/master/examples/greet.wasm>, \"greet\", \"Stardog\") AS ?result) }";
                " prefix ex: <https://github.com/semantalytics/stardog-wasm/raw/main/wasm/> " +
                " select (wasm:call(ex:toUpper, ?value) as ?result) where { bind(\"stardog\" AS ?value) } limit 1";

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
