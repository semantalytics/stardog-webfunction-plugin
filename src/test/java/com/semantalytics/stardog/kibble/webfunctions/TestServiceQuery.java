package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.Value;
import com.stardog.stark.query.SelectQueryResult;
import org.junit.Test;

import java.util.Optional;

import static com.complexible.stardog.plan.filter.functions.AbstractFunction.assertStringLiteral;
import static org.assertj.core.api.Assertions.assertThat;

public class TestServiceQuery extends AbstractStardogTest {

    final String queryHeader = WebFunctionVocabulary.sparqlPrefix("wf", "latest") +
            " prefix f: <file:src/test/rust/target/wasm32-unknown-unknown/release/> " +
            " prefix wfs: <tag:semantalytics:stardog:webfunction:0.0.0:> ";


    @Test
    public void testServiceQuery() {

        final String aQuery = queryHeader +
                " select ?result where { SERVICE wfs:service {" +
                "  [] wf:call \"file:src/test/rust/target/wasm32-unknown-unknown/release/to_upper.wasm\"; " +
                "     wf:args \"stardog\";" +
                "     wf:result ?result } }";

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
    public void testServiceQueryFunctionNameFromBind() {

        final String aQuery = queryHeader +
                " select ?result where { " +
                " BIND(str(f:to_upper.wasm) as ?func) " +
                " SERVICE wfs:service {" +
                "  [] wf:call ?func; " +
                "     wf:args \"stardog\";" +
                "     wf:result ?result } }";

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
    public void missingCallPredicateShouldFail() {

        final String aQuery = queryHeader +
                " SELECT ?result WHERE { SERVICE wfs:service {" +
                " [ wf:args ?args; wf:result ?result ] } VALUES ?args {\"stardog\"}}";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("STARDOG");

            assertThat(aResult).isExhausted();
        }
    }

    @Test
    public void missingResultsPredicateShouldFail() {

        final String aQuery = queryHeader +
                " SELECT ?result WHERE { SERVICE wfs:service {" +
                "  [ wf:call ?func; wf:args ?args; ] } VALUES ?args {\"stardog\"}}";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("STARDOG");

            assertThat(aResult).isExhausted();
        }
    }

    @Test
    public void constantResultsShouldFail() {

        final String aQuery = queryHeader +
                " SELECT ?result WHERE { SERVICE wfs:service {" +
                "  [ wf:call ?func; wf:args ?args; wf:results \"results\" ] } VALUES ?args {\"stardog\"}}";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("STARDOG");

            assertThat(aResult).isExhausted();
        }
    }


    @Test
    public void testServiceOneVarInput() {

        final String aQuery = queryHeader +
                " select ?result where { SERVICE wfs:service {" +
                "  [] wf:call \"file:src/test/rust/target/wasm32-unknown-unknown/release/to_upper.wasm\"; " +
                "     wf:args ?args;" +
                "     wf:result ?result } values ?args {\"stardog\"}}";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("STARDOG");

            assertThat(aResult).isExhausted();
        }
    }

    @Test
    public void testServiceQueryVarInput() {

        final String aQuery = queryHeader +
                " select ?result where { SERVICE wfs:service {" +
                "  [] wf:call \"file:src/test/rust/target/wasm32-unknown-unknown/release/to_upper.wasm\"; " +
                "     wf:args ?args;" +
                "     wf:result ?result } values ?args {\"star\" \"dog\"}}";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("STAR");

            assertThat(aResult).hasNext();
            aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("DOG");

            assertThat(aResult).isExhausted();
        }
    }
}
