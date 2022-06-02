package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.stardog.plan.eval.ExecutionException;
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
    public void testTemp() {

        final String aQuery =
"                prefix wf: <http://semantalytics.com/2021/03/ns/stardog/webfunction/0.0.0/> " +
        " prefix f: <file:src/test/rust/target/wasm32-unknown-unknown/release/> " +
        "                prefix wfs: <tag:semantalytics:stardog:webfunction:0.0.0:> " +
                "select ?str ?lang ?iso_639_1 ?iso_639_3 ?confidence where {" +
       "{ select ?langs ?str WHERE { bind(wf:call(str(f:array_of.wasm), \"en\", \"es\", \"fr\") as ?langs) { VALUES ?str { \"hello world\" \"buenos dias\" \"bonjour\"} } } } " +
"            service wfs:service { " +
"                    [] wf:call \"http://wf.semantalytics.com/ipns/k51qzi5uqu5dlx0ttqevj64d3twk31y7hsgnofkqkjaiv11k98lj2rx60kjgv5/stardog/function/string/lang/detectConfidence/1.0.3-SNAPSHOT\"; " +
"            wf:args (?str ?langs); " +
"            wf:result (?lang ?iso_639_1 ?iso_639_3 ?confidence) . " +
"        } " +
"        } ";


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
    public void testEmptyResult() {

        final String aQuery = queryHeader +
                " select ?result where { SERVICE wfs:service {" +
                "  [] wf:call \"file:src/test/rust/target/wasm32-unknown-unknown/release/empty.wasm\"; " +
                "     wf:result ?result } }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).isExhausted();
        }
    }

    @Test
    public void testServiceQueryWrapingBNode() {

        final String aQuery = queryHeader +
                " select ?result where { SERVICE wfs:service {" +
                "    [ wf:call \"file:src/test/rust/target/wasm32-unknown-unknown/release/to_upper.wasm\"; " +
                "      wf:args \"stardog\";" +
                "      wf:result ?result " +
                "    ]" +
                "  }" +
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

    @Test(expected = ExecutionException.class)
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

    @Test(expected = ExecutionException.class)
    public void missingResultsPredicateShouldFail() {

        final String aQuery = queryHeader +
                " SELECT ?result WHERE { SERVICE wfs:service {" +
                "  [ wf:call ?func; wf:args ?args; ] } VALUES ?args {\"stardog\"}}";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

        }
    }

    @Test(expected = ExecutionException.class)
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
    public void testServiceTwoArgs() {

        final String aQuery = queryHeader +
                " select ?result where { SERVICE wfs:service {" +
                "  [] wf:call \"file:src/test/rust/target/wasm32-unknown-unknown/release/concat.wasm\"; " +
                "     wf:args (\"star\" \"dog\"); " +
                "     wf:result ?result } }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("stardog");

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
    public void testServiceMappingDictionarySet() {

        final String aQuery = queryHeader +
                " select ?result where { SERVICE wfs:service {" +
                "  [] wf:call \"file:src/test/rust/target/wasm32-unknown-unknown/release/array_of.wasm\"; " +
                "     wf:args (\"star\" \"dog\");" +
                "     wf:result ?result1 . " +
                "  } " +
                " UNNEST(?result1 as ?result) " +
                " }";

        try (final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext();
            Optional<Value> aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            Value aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            Literal aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("star");

            assertThat(aResult).hasNext();
            aPossibleValue = aResult.next().value("result");
            assertThat(aPossibleValue).isPresent();
            aValue = aPossibleValue.get();
            assertThat(assertStringLiteral(aValue));
            aLiteral = ((Literal)aValue);
            assertThat(aLiteral.label()).isEqualTo("dog");

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
