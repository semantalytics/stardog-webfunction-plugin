package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.stardog.AbstractStardogModule;
import com.complexible.stardog.plan.eval.service.Service;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public final class WebFunctionServiceModule extends AbstractStardogModule {
    @Override
    protected void configure() {

        Multibinder.newSetBinder(binder(), Service.class)
                .addBinding()
                .to(WebFunctionService.class)
                .in(Singleton.class);
    }
}