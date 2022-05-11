package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.query.BindingSet;
import com.stardog.stark.query.SelectQueryResult;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class TestCompose extends AbstractStardogTest {

    final String queryHeader = WebFunctionVocabulary.sparqlPrefix("wf", "0.0.0") +
            " prefix f: <file:src/main/rust/function/target/wasm32-unknown-unknown/release/> ";

    @Test
    public void wrongTypeForFirstArg() {

        final String aQuery = queryHeader +
            " SELECT ?result WHERE { BIND(wf:call(1) AS ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {
            Assertions.assertThat(aResult.hasNext()).isTrue();
            final BindingSet aBindingSet = aResult.next();
            Assertions.assertThat(aBindingSet.size()).isZero();
            Assertions.assertThat(aResult.hasNext()).isFalse();
        }
    }

    @Test
    public void tooFewArgs() {

        final String aQuery = queryHeader +
                " SELECT ?result WHERE { BIND(wf:call() AS ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {
            Assertions.assertThat(aResult.hasNext()).isTrue();
            final BindingSet aBindingSet = aResult.next();
            Assertions.assertThat(aBindingSet.size()).isZero();
            Assertions.assertThat(aResult.hasNext()).isFalse();
        }
    }

    @Test
    public void compositionOfTwoBuiltins() {

        final String aQuery = queryHeader +
                " SELECT ?result WHERE { BIND(wf:call(wf:compose(\"TOUPPER\", \"PI\")) AS ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            Optional<Literal> aPossibleLiteral = aResult.next().literal("result");
            assertThat(aPossibleLiteral).isPresent();
            assertThat(aPossibleLiteral.get().label()).isEqualTo("3.14...");
        }
    }

    @Test
    public void testIriFunctionNoArgs() {

        final String aQuery = queryHeader +
                " SELECT ?result WHERE { BIND(wf:call(wf:compose(str(wf:echo1x1x1.wasm), str(wf:to_upper.wasm)), \"Hello world\") AS ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            Optional<Literal> aPossibleLiteral = aResult.next().literal("result");
            assertThat(aPossibleLiteral).isPresent();
            assertThat(aPossibleLiteral.get().label()).isEqualTo("HELLO WORLD");
        }
    }

    @Test
    public void testIriFunction() {

        final String aQuery = queryHeader +
                " SELECT ?result WHERE { BIND(wf:call(string:upperCase, \"Hello world\" ) AS ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            Optional<Literal> aPossibleLiteral = aResult.next().literal("result");
            assertThat(aPossibleLiteral).isPresent();
            assertThat(aPossibleLiteral.get().label()).isEqualTo("HELLO WORLD");
        }
    }

    @Test
    public void testStringFunction() {

        final String aQuery = queryHeader +
                " SELECT ?result WHERE { BIND(wf:call(str(fn:upperCase), \"Hello world\" ) AS ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            Optional<Literal> aPossibleLiteral = aResult.next().literal("result");
            assertThat(aPossibleLiteral).isPresent();
            assertThat(aPossibleLiteral.get().label()).isEqualTo("HELLO WORLD");
        }
    }

    @Test
    public void testStringFunctionAsArg() {

        final String aQuery = queryHeader +
                " SELECT ?result WHERE { BIND(string:reverse(wf:call(string:upperCase, \"Hello world\" )) AS ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            Optional<Literal> aPossibleLiteral = aResult.next().literal("result");
            assertThat(aPossibleLiteral).isPresent();
            assertThat(aPossibleLiteral.get().label()).isEqualTo("DLROW OLLEH");
        }
    }
}
