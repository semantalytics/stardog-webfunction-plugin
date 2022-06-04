package com.semantalytics.stardog.kibble.webfunctions.propertyfunction.outcallin;

import com.complexible.stardog.index.statistics.Cardinality;
import com.complexible.stardog.plan.*;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.semantalytics.stardog.kibble.webfunctions.WebFunctionVocabulary;
import com.stardog.stark.IRI;

import java.util.List;

import static com.stardog.stark.Values.iri;
import static java.util.stream.Collectors.toList;

public final class WebFunctionOutCallInPropertyFunctionPlanNode extends AbstractPropertyFunctionPlanNode {

    private static final WebFunctionVocabulary names = WebFunctionVocabulary.OUT_call_IN;

    public WebFunctionOutCallInPropertyFunctionPlanNode(final PlanNode theArg,
                                                        final List<QueryTerm> theSubjects,
                                                        final List<QueryTerm> theObjects,
                                                        final QueryTerm theContext,
                                                        final QueryDataset.Scope theScope,
                                                        final double theCost,
                                                        final Cardinality theCardinality,
                                                        final ImmutableSet<Integer> theSubjVars,
                                                        final ImmutableSet<Integer> thePredVars,
                                                        final ImmutableSet<Integer> theObjVars,
                                                        final ImmutableSet<Integer> theContextVars,
                                                        final ImmutableSet<Integer> theAssuredVars,
                                                        final ImmutableSet<Integer> theAllVars) {
        super(theArg,
                theSubjects,
                theObjects,
                theContext,
                theScope,
                theCost,
                theCardinality,
                theSubjVars,
                thePredVars,
                theObjVars,
                theContextVars,
                theAssuredVars,
                theAllVars);
    }

    public ValueOrError getWasm() {
        return getObjects().get(0).getValue(); }


    public List<QueryTerm> getInput() {
        return getObjects().stream().collect(toList());
    }

    public List<QueryTerm> getResultVars() {
        return getSubjects();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImmutableList<QueryTerm> getInputs() {
        return getObjects();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IRI getURI() {
        return iri(names.getImmutableName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebFunctionOutCallInPropertyFunctionPlanNode copy() {
        return new WebFunctionOutCallInPropertyFunctionPlanNode(getArg().copy(),
                                               getSubjects(),
                                               getObjects(),
                                               getContext(),
                                               getScope(),
                                               getCost(),
                                               getCardinality(),
                                               getSubjectVars(),
                                               getPredicateVars(),
                                               getObjectVars(),
                                               getContextVars(),
                                               getAssuredVars(),
                                               getAllVars());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PropertyFunctionNodeBuilder createBuilder() {
        return new WebFunctionOutCallInPropertyFunctionPlanNodeBuilder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean canEquals(final Object theObj) {
        return theObj instanceof WebFunctionOutCallInPropertyFunctionPlanNode;
    }
}