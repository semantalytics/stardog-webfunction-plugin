package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.query.SelectQueryResult;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class TestFilter extends AbstractStardogTest {

    final String queryHeader = WebFunctionVocabulary.sparqlPrefix("wf", "0.0.0") +
            " prefix f: <file:src/main/rust/function/target/wasm32-unknown-unknown/release/> ";

    @Test
    public void testIriFunctionNoArgs() {

        final String aQuery = queryHeader +
            " SELECT ?result WHERE { BIND(wf:filter(string:isNumeric, array:of(\"star\", \"dog\", \"1\", \"2\"))) AS ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            Optional<Literal> aPossibleLiteral = aResult.next().literal("result");
            assertThat(aPossibleLiteral).isPresent();
            assertThat(aPossibleLiteral.get().label()).isEqualTo("[ \"1\"^^<http://www.w3.org/2001/XMLSchema#string> \"2\"^^<http://www.w3.org/2001/XMLSchema#string> ]");
        }
    }
}
