package com.semantalytics;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException {
        URL url = new URL("ipfs://bafybeie5nqv6kd3qnfjupgvz34woh3oksc3iau6abmyajn7qvtf6d2ho34/readme");
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        System.out.println(IOUtils.toString(in, "UTF-8"));
    }
}
