package com.semantalytics.stardog.kibble.wasm;

import com.stardog.stark.IRI;

import static com.stardog.stark.Values.iri;

public class WasmVocabulary {

	public static final String NS = "http://semantalytics.com/2021/03/ns/stardog/kibble/wasm/";

	public static final IRI call = iri(NS + "call");

    public static String sparqlPrefix(final String prefixName) {
        return "PREFIX " + prefixName + ": <" + NS + "> ";
    }
}
