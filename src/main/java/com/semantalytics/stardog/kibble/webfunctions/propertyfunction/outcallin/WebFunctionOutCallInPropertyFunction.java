package com.semantalytics.stardog.kibble.webfunctions.propertyfunction.outcallin;

import com.complexible.stardog.index.statistics.Accuracy;
import com.complexible.stardog.index.statistics.Cardinality;
import com.complexible.stardog.plan.*;
import com.complexible.stardog.plan.eval.ExecutionContext;
import com.complexible.stardog.plan.eval.TranslateException;
import com.complexible.stardog.plan.eval.operator.*;
import com.complexible.stardog.plan.filter.expr.ValueOrError;
import com.complexible.stardog.plan.util.QueryTermRenderer;
import com.google.common.base.Preconditions;
import com.semantalytics.stardog.kibble.webfunctions.StardogWasmInstance;
import com.semantalytics.stardog.kibble.webfunctions.WebFunctionVocabulary;
import com.stardog.stark.IRI;
import com.stardog.stark.Values;

import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.*;

public class WebFunctionOutCallInPropertyFunction implements PropertyFunction {

        private static final WebFunctionVocabulary names = WebFunctionVocabulary.OUT_call_IN;

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
        public WebFunctionOutCallInPropertyFunctionPlanNodeBuilder newBuilder() {
            return new WebFunctionOutCallInPropertyFunctionPlanNodeBuilder();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Operator translate(final ExecutionContext theExecutionContext,
                                  final PropertyFunctionPlanNode thePropertyFunctionPlanNode,
                                  final Operator theOperator) throws TranslateException {

            if (thePropertyFunctionPlanNode instanceof WebFunctionOutCallInPropertyFunctionPlanNode) {
                return new WebFunctionOutCallInPropertyFunctionOperator(theExecutionContext,
                                                       (WebFunctionOutCallInPropertyFunctionPlanNode) thePropertyFunctionPlanNode,
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
            Preconditions.checkArgument(theNode instanceof WebFunctionOutCallInPropertyFunctionPlanNode);

            List<ValueOrError> argsValueOrError = ((WebFunctionOutCallInPropertyFunctionPlanNode) theNode).getInput().stream().map(queryTerm -> {
                if(queryTerm.isVariable()) {
                    return ValueOrError.General.of(Values.iri(WebFunctionVocabulary.var.getImmutableName()));
                } else {
                    return queryTerm.getValue();
                }
            }).collect(toList());
            if(argsValueOrError.stream().anyMatch(ValueOrError::isError)) {
                throw new PlanException("Unable to generate cardinalty estimates");
            }

            try {
                StardogWasmInstance instance = StardogWasmInstance.from(((WebFunctionOutCallInPropertyFunctionPlanNode) theNode).getWasm().value());
                Cardinality cardinality = instance.getCardinality(theNode.getArg().getCardinality(), argsValueOrError.stream().map(ValueOrError::value).collect(toList()));

                theNode.setCardinality(Cardinality.of(cardinality.value(), Accuracy.takeLessAccurate(cardinality.accuracy(), theNode.getArg().getCardinality().accuracy())));
            } catch(ExecutionException | MalformedURLException e) {
                throw new PlanException(e);
            }

            // assume a flat cost of 1 per iteration + the cost of our child
            theNode.setCost(theNode.getCardinality().value() + theNode.getArg().getCost());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String explain(final PropertyFunctionPlanNode theNode, final QueryTermRenderer theTermRenderer) {
            Preconditions.checkArgument(theNode instanceof WebFunctionOutCallInPropertyFunctionPlanNode);
            Preconditions.checkNotNull(theTermRenderer);
            //TODO should this be added to the webassembly as well?

            final WebFunctionOutCallInPropertyFunctionPlanNode aNode = (WebFunctionOutCallInPropertyFunctionPlanNode) theNode;

            return String.format("Call WebFunction %s", aNode.getWasm());
        }

}
