package com.semantalytics;

import com.complexible.stardog.docs.nlp.impl.DictionaryLinker;
import com.google.common.collect.ImmutableMultimap;
import com.stardog.stark.IRI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;

public class LinkerDump {

    public static final void main(String... args) throws IOException, NoSuchFieldException, IllegalAccessException {
        DictionaryLinker.Linker linkerSerializer = DictionaryLinker.Linker.from(new File(args[0]));

        Field privateField = DictionaryLinker.Linker.class.getDeclaredField("mLinks");
        privateField.setAccessible(true);
        ImmutableMultimap<String, IRI> mLinks = (ImmutableMultimap<String, com.stardog.stark.IRI>)privateField.get(linkerSerializer);
        FileWriter fileWriter = new FileWriter(args[0] + ".rustout");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        mLinks.forEach((s, iri) -> {
            printWriter.printf("    (r#\"%s\"#,r#\"%s\"#),\n", s, iri);
        });
    }
}
