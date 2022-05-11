package com.semantalytics.stardog.kibble.webfunctions;

import com.semantalytics.stardog.kibble.AbstractStardogTest;
import com.stardog.stark.Literal;
import com.stardog.stark.query.SelectQueryResult;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.Test;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class TestGet extends AbstractStardogTest {

    @Test
    public void testWrongTypeThirdArg() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "snapshot") +
                "select ?result where { unnest(wf:get(<http://google.com>) as ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            List<Integer> results = IteratorUtils.toList(aResult).stream().map(b -> b.get("result")).filter(Literal.class::isInstance).map(Literal.class::cast).map(Literal::intValue).collect(toList());
            assertThat(results).containsExactly(1, 2, 5);
        }
    }


    @Test
    public void testWrongTypeSecondArg() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "snapshot") +
                "select ?result where { unnest(wf:get(<http://google.com>) as ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            List<Integer> results = IteratorUtils.toList(aResult).stream().map(b -> b.get("result")).filter(Literal.class::isInstance).map(Literal.class::cast).map(Literal::intValue).collect(toList());
            assertThat(results).containsExactly(1, 2, 5);
        }
    }


    @Test
    public void testWrongTypeFirstArg() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "snapshot") +
                "select ?result where { unnest(wf:get(<http://google.com>) as ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            List<Integer> results = IteratorUtils.toList(aResult).stream().map(b -> b.get("result")).filter(Literal.class::isInstance).map(Literal.class::cast).map(Literal::intValue).collect(toList());
            assertThat(results).containsExactly(1, 2, 5);
        }
    }


    @Test
    public void testTooFewArgs() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "snapshot") +
                "select ?result where { unnest(wf:get(<http://google.com>) as ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            List<Integer> results = IteratorUtils.toList(aResult).stream().map(b -> b.get("result")).filter(Literal.class::isInstance).map(Literal.class::cast).map(Literal::intValue).collect(toList());
            assertThat(results).containsExactly(1, 2, 5);
        }
    }


    @Test
    public void testTooManyArgs() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "snapshot") +
                "select ?result where { unnest(wf:get(<http://google.com>) as ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            List<Integer> results = IteratorUtils.toList(aResult).stream().map(b -> b.get("result")).filter(Literal.class::isInstance).map(Literal.class::cast).map(Literal::intValue).collect(toList());
            assertThat(results).containsExactly(1, 2, 5);
        }
    }


    @Test
    public void testHttp() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "snapshot") +
                "select ?result where { unnest(wf:get(<http://google.com>) as ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            List<Integer> results = IteratorUtils.toList(aResult).stream().map(b -> b.get("result")).filter(Literal.class::isInstance).map(Literal.class::cast).map(Literal::intValue).collect(toList());
            assertThat(results).containsExactly(1, 2, 5);
        }
    }

    @Test
    public void testFile() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "snapshot") +
                "select ?result where { unnest(wf:get(<http://google.com>) as ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            List<Integer> results = IteratorUtils.toList(aResult).stream().map(b -> b.get("result")).filter(Literal.class::isInstance).map(Literal.class::cast).map(Literal::intValue).collect(toList());
            assertThat(results).containsExactly(1, 2, 5);
        }
    }

    @Test
    public void testIpfs() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "snapshot") +
                "select ?result where { unnest(wf:get(<http://google.com>) as ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            List<Integer> results = IteratorUtils.toList(aResult).stream().map(b -> b.get("result")).filter(Literal.class::isInstance).map(Literal.class::cast).map(Literal::intValue).collect(toList());
            assertThat(results).containsExactly(1, 2, 5);
        }
    }

    @Test
    public void testIpns() {

        final String aQuery = WebFunctionVocabulary.sparqlPrefix("wf", "snapshot") +
                "select ?result where { unnest(wf:get(<http://google.com>) as ?result) }";

        try(final SelectQueryResult aResult = connection.select(aQuery).execute()) {

            assertThat(aResult).hasNext().withFailMessage("Should have a result");
            List<Integer> results = IteratorUtils.toList(aResult).stream().map(b -> b.get("result")).filter(Literal.class::isInstance).map(Literal.class::cast).map(Literal::intValue).collect(toList());
            assertThat(results).containsExactly(1, 2, 5);
        }
    }
}
