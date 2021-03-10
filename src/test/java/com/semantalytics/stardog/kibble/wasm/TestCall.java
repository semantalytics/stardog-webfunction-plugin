package com.semantalytics.stardog.kibble.wasm;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.query.BindingSet;
import com.stardog.stark.query.SelectQueryResult;
import org.junit.Test;

import java.util.Optional;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertStringLiteral;
import static org.assertj.core.api.Assertions.assertThat;

public class TestCall extends AbstractStardogTest {


    @Test
    public void test() {
    
        final String aQuery = WasmVocabulary.sparqlPrefix("wasm") +
        //            "select ?result where { bind(wasm:call(<https://github.com/wasmerio/wasmer-java/raw/master/tests/resources/simple.wasm>, \"sum\", 5, 7) AS ?result) }";
        "select ?result where { bind(wasm:call(tricks:woof.wasm, \"sum\", 5, 7) AS ?result) }";

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

}
