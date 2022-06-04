package com.semantalytics.stardog.kibble.webfunctions.propertyfunction.incallout;

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

public final class WebFunctionInCallOutPropertyFunctionPlanNode extends AbstractPropertyFunctionPlanNode {

    private static final WebFunctionVocabulary names = WebFunctionVocabulary.IN_call_OUT;

    public WebFunctionInCallOutPropertyFunctionPlanNode(final PlanNode theArg,
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
        return getSubjects().get(0).getValue(); }


    public List<QueryTerm> getInput() {
        return getSubjects().stream().collect(toList());
    }

    public List<QueryTerm> getResultVars() {
        return getObjects();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImmutableList<QueryTerm> getInputs() {
        return getSubjects();
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
    public WebFunctionInCallOutPropertyFunctionPlanNode copy() {
        return new WebFunctionInCallOutPropertyFunctionPlanNode(getArg().copy(),
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
        return new WebFunctionInCallOutPropertyFunctionPlanNodeBuilder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean canEquals(final Object theObj) {
        return theObj instanceof WebFunctionInCallOutPropertyFunctionPlanNode;
    }
}