package com.semantalytics.stardog.kibble.webfunctions;

import com.stardog.stark.IRI;

import static com.stardog.stark.Values.iri;

public class WebFunctionVocabulary {

	public static final String NS = "http://semantalytics.com/2021/03/ns/stardog/webfunction/";

	public static final IRI call = iri(NS + "call");
    public static final IRI doc = iri(NS + "doc");
    public static final IRI cacheClear = iri(NS + "cacheClear");
    public static final IRI cacheList = iri(NS + "cacheList");
    public static final IRI cacheLoad = iri(NS + "cacheLoad");
    public static final IRI cacheRefresh = iri(NS + "cacheRefresh");
    public static final IRI pluginVersion = iri(NS + "pluginVersion");
    public static final IRI agg = iri(NS + "agg");
    public static final IRI get = iri(NS + "get");
    public static final IRI upgrade = iri(NS + "upgrade");

    public static String sparqlPrefix(final String prefixName) {
        return "PREFIX " + prefixName + ": <" + NS + "> ";
    }
}
