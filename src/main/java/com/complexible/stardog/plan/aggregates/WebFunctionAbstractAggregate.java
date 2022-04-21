package com.complexible.stardog.plan.aggregates;

import com.complexible.stardog.plan.filter.Expression;
import com.complexible.stardog.plan.filter.functions.UserDefinedFunction;

import java.util.List;

public abstract class WebFunctionAbstractAggregate extends AbstractAggregate implements UserDefinedFunction {

    public WebFunctionAbstractAggregate(String... theURIs) {
        super(theURIs);
    }

    public WebFunctionAbstractAggregate(boolean theDistinct, List<Expression> theArgs, String... theURIs) {
        super(theDistinct, theArgs, theURIs);
    }

    public WebFunctionAbstractAggregate(AbstractAggregate theAggregate) {
        super(theAggregate);
    }
}