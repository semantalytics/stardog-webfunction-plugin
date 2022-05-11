package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.stardog.db.ConnectableConnection;
import com.complexible.stardog.index.statistics.Accuracy;
import com.complexible.stardog.index.statistics.Cardinality;
import com.complexible.stardog.plan.Costs;
import com.complexible.stardog.plan.PlanNode;
import com.complexible.stardog.plan.QueryTerm;
import com.complexible.stardog.plan.eval.service.*;
import com.complexible.stardog.plan.filter.Expressions;
import com.google.common.base.Preconditions;
import com.stardog.stark.IRI;
import com.stardog.stark.Value;

import java.util.List;
import java.util.Map;

import static com.stardog.stark.Values.iri;

final class WebFunctionService extends SingleQueryService {

    //TODO move to WebFunctionVocabulary

    @Override
    public boolean canEvaluate(final IRI theIRI) {
        return WebFunctionVocabulary.service.getNames().contains(theIRI.toString());
    }

    /*
    example

    prefix str: <wf+http://wf.semantaltyics.com/stardog/function/string/>

    SERVICE <tag:semantalytics:webfunction> {
        []          wf:call f:toUpper;
                    wf:args ("star" "dog);
                    wf:result ?result
    }

    SERVICE <tag:semantalytics:webfunction> { [ wf:call f:toUpper; wf:args ("star" "dog); wf:results ?result ] .}
     */


    @Override
    public ServiceQuery createQuery(final IRI theIRI, final PlanNode body) {
        return createWebFunctionQuery(body);
    }

    public static PlanNodeBodyServiceQuery createWebFunctionQuery(PlanNode theBody) {
        Map<QueryTerm, ServiceParameters> subjToParams = ServiceParameterUtils.build(theBody);
        Preconditions.checkArgument(subjToParams.size() == 1, "Parameters must correspond to a single subject");
        ServiceParameters params = subjToParams.values().iterator().next();
        (new WebFunctionService.WebFunctionServiceParamValidator(params)).validate();
        Value webFunctionValue = params.first(iri(WebFunctionVocabulary.call.getMutableName()))
                .filter(Expressions::isConstant)
                .map((t) -> t.getValue().value())
                .orElse(null);
        //TODO function is constanct might want to relax that later
        return createServiceQuery(theBody, webFunctionValue, params);
    }

    private static PlanNodeBodyServiceQuery createServiceQuery(PlanNode body, Value webFunctionValue, ServiceParameters params) {
        List<QueryTerm> args = params.get(iri(WebFunctionVocabulary.args.getMutableName()));
        List<QueryTerm> results = params.get(iri(WebFunctionVocabulary.result.getMutableName()));
        return new WebFunctionServiceQuery(body, webFunctionValue, args, results);
    }

    private static class WebFunctionServiceParamValidator extends Validator {
        WebFunctionServiceParamValidator(ServiceParameters theParameters) {
            super(theParameters);
        }

        @Override
        public void validate() {
            //TODO implement this
        }
    }
}