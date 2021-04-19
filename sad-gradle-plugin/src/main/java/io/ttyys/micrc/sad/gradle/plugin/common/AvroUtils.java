package io.ttyys.micrc.sad.gradle.plugin.common;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility method for working with Avro objects.
 */
public class AvroUtils {
    /**
     * The namespace separator.
     */
    private static final String NAMESPACE_SEPARATOR = ".";

    /**
     * The extension separator.
     */
    private static final String EXTENSION_SEPARATOR = ".";

    /**
     * The Unix separator.
     */
    private static final String UNIX_SEPARATOR = "/";

    /**
     * Not intended for instantiation.
     */
    private AvroUtils() { }

    /**
     * Assembles a file path based on the namespace and name of the provided {@link Schema}.
     *
     * @param schema the schema for which to assemble a path
     * @return a file path
     */
    public static String assemblePath(Schema schema) {
        return assemblePath(schema.getNamespace(), schema.getName(), Constants.SCHEMA_EXTENSION);
    }

    /**
     * Assembles a file path based on the namespace and name of the provided {@link Protocol}.
     *
     * @param protocol the protocol for which to assemble a path
     * @return a file path
     */
    public static String assemblePath(Protocol protocol) {
        return assemblePath(protocol.getNamespace(), protocol.getName(), Constants.PROTOCOL_EXTENSION);
    }

    /**
     * Assembles a file path based on the provided arguments.
     *
     * @param namespace the namespace for the path; may be null
     * @param name the name for the path; will result in an exception if null or empty
     * @param extension the extension for the path
     * @return the assembled path
     */
    private static String assemblePath(String namespace, String name, String extension) {
        Strings.requireNotEmpty(name, "Path cannot be assembled for nameless objects");
        List<String> parts = new ArrayList<>();
        if (Strings.isNotEmpty(namespace)) {
            parts.add(namespace.replaceAll(Pattern.quote(NAMESPACE_SEPARATOR), UNIX_SEPARATOR));
        }
        parts.add(name + EXTENSION_SEPARATOR + extension);
        return String.join(UNIX_SEPARATOR, parts);
    }

}
