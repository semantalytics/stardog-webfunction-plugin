package com.semantalytics.stardog.kibble.webfunctions;

import com.complexible.stardog.metadata.ConfigProperty;
import com.complexible.stardog.metadata.MetaPropertyProvider;

import static com.complexible.stardog.metadata.MetaProperty.config;

public class WebFunctionOption implements MetaPropertyProvider {

    private static final String PROPERTY_IPFS_GATEWAY = "ipfs.gateway";
    private static final String PROPERTY_IPNS_GATEWAY = "ipns.gateway";
    public static final ConfigProperty<String> IPFS_GATEWAY = config(PROPERTY_IPFS_GATEWAY, "http://wf.semantalytics.com/ipfs").server().creatable().readable().writableWhileOnline().build();
    public static final ConfigProperty<String> IPNS_GATEWAY = config(PROPERTY_IPNS_GATEWAY, "http://wf.semantalytics.com/ipns").server().creatable().readable().writableWhileOnline().build();
}
