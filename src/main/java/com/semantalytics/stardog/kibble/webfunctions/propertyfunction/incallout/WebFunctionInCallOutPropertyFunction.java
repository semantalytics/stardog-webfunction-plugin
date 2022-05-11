package com.semantalytics.stardog.kibble.webfunctions.propertyfunction.incallout;

import com.complexible.stardog.index.statistics.Cardinality;
import com.complexible.stardog.plan.*;
import com.complexible.stardog.plan.eval.ExecutionContext;
import com.complexible.stardog.plan.eval.TranslateException;
import com.complexible.stardog.plan.eval.operator.*;
import com.complexible.stardog.plan.util.QueryTermRenderer;
import com.google.common.base.Preconditions;
import com.semantalytics.stardog.kibble.webfunctions.WebFunctionVocabulary;
import com.stardog.stark.IRI;
import com.stardog.stark.Values;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class WebFunctionInCallOutPropertyFunction implements PropertyFunction {

    private static final WebFunctionVocabulary names = WebFunctionVocabulary.IN_call_OUT;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<IRI> getURIs() {
                             return names.getNames().stream().map(Values::iri).collect(toList());
                                                                                                 }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebFunctionInCallOutPropertyFunctionPlanNodeBuilder newBuilder() {
                                                 return new WebFunctionInCallOutPropertyFunctionPlanNodeBuilder();
                                                                                         }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operator translate(final ExecutionContext theExecutionContext,
                              final PropertyFunctionPlanNode thePropertyFunctionPlanNode,
                              final Operator theOperator) throws TranslateException {

        if (thePropertyFunctionPlanNode instanceof WebFunctionInCallOutPropertyFunctionPlanNode) {
            return new WebFunctionInCallOutPropertyFunctionOperator(theExecutionContext,
                                          (WebFunctionInCallOutPropertyFunctionPlanNode) thePropertyFunctionPlanNode,
                                           theOperator);
        } else {
            throw new TranslateException("Invalid node type, cannot translate");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void estimate(final PropertyFunctionPlanNode theNode,
                         Costs.CostingContext theContext) throws PlanException {
        Preconditions.checkArgument(theNode instanceof WebFunctionInCallOutPropertyFunctionPlanNode);

        //TODO should we add a method to wasm to provide an estimate???

        theNode.setCardinality(Cardinality.UNKNOWN);

        // assume a flat cost of 1 per iteration + the cost of our child
        theNode.setCost(theNode.getCardinality().value() + theNode.getArg().getCost());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String explain(final PropertyFunctionPlanNode theNode, final QueryTermRenderer theTermRenderer) {
        Preconditions.checkArgument(theNode instanceof WebFunctionInCallOutPropertyFunctionPlanNode);
        Preconditions.checkNotNull(theTermRenderer);
        //TODO should this be added to the webassembly as well?

        final WebFunctionInCallOutPropertyFunctionPlanNode aNode = (WebFunctionInCallOutPropertyFunctionPlanNode) theNode;

        return String.format("Call WebFunction {}", aNode.getWasm());
    }
}
