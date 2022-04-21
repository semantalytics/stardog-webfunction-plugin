package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.common.base.Streams;
import com.google.common.collect.Lists;
import com.vdurmont.semver4j.Semver;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public enum WebFunctionVocabulary {
	call,
    callx,
    doc,
    cacheClear,
    cacheList,
    cacheLoad,
    cacheRefresh,
    pluginVersion,
    pluginHash,
    agg,
    get,
    upgrade,
    IN_call_OUT,
    OUT_call_IN,
    compose,
    filter,
    map,
    memoize,
    partial,
    reduce,
    var;

    public static final String nsTemplate = "http://semantalytics.com/2021/03/ns/stardog/webfunction/%s/";
    public static final String template = "http://semantalytics.com/2021/03/ns/stardog/webfunction/%s/%s";


    public static String sparqlPrefix(final String prefixName, final String version) {
        return String.format("PREFIX %s: <%s>", prefixName, String.format(nsTemplate, version));
    }

    public Optional<String> getMutableName() {
        if(Streams.stream(ServiceLoader.load(PluginVersionService.class).iterator()).map(PluginVersionService::pluginVersion).map(Semver::new).max(Semver::compareTo).get().isEqualTo(Version.PLUGIN_VERSION)) {
            return Optional.of(String.format(template, "latest", name()));
        } else {
            return Optional.empty();
        }
    }

    public String getImmutableName() {
        return String.format(template, Version.PLUGIN_VERSION, name());
    }

    public List<String> getNames() {
        final List names = Lists.newArrayList(getImmutableName());
        if(getMutableName().isPresent()) {
            names.add(getMutableName().get());
        }
        return names;
    }
}