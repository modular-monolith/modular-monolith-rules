package io.modulith.rules.config;

import io.modulith.rules.api.CommunicationType;
import io.modulith.rules.api.ModuleDefinition;
import io.modulith.rules.api.ModulithRuleSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for loading {@link ModulithRuleSet} definitions from YAML configuration files.
 *
 * <p>The loader supports reading configuration from the classpath, the filesystem, or an
 * in-memory string. All overloads delegate to a lightweight hand-written YAML parser that
 * handles the subset of YAML used by modulith-rules configuration files.
 *
 * <p>Example YAML structure:
 * <pre>{@code
 * root-package: com.example
 * modules:
 *   ordering:
 *     base-package: com.example.ordering
 *     api-packages:
 *       - .api.
 *     internal-packages:
 *       - .internal.
 *     allowed-dependencies:
 *       - payments
 *     communication:
 *       payments: SYNCHRONOUS
 *       notifications: ASYNCHRONOUS
 *   catalog:
 *     allowed-dependencies:
 *       - shared
 * }</pre>
 *
 * <p>If {@code base-package} is omitted for a module, it is derived as
 * {@code rootPackage + "." + moduleName}.
 */
public final class ModulithConfigLoader {

    private static final String DEFAULT_CONFIG_FILE = "modulith-rules.yml";

    private ModulithConfigLoader() {
    }

    /**
     * Loads the default configuration file {@code modulith-rules.yml} from the classpath.
     *
     * @return the parsed {@link ModulithRuleSet}
     * @throws IllegalArgumentException if the default file is not found on the classpath
     * @throws IllegalStateException if the file is missing required fields or cannot be read
     */
    public static ModulithRuleSet loadFromClasspath() {
        return loadFromClasspath(DEFAULT_CONFIG_FILE);
    }

    /**
     * Loads a named configuration file from the classpath.
     *
     * @param fileName the name of the file to load
     * @return the parsed {@link ModulithRuleSet}
     * @throws IllegalArgumentException if the file is not found on the classpath
     * @throws IllegalStateException if the file is missing required fields or cannot be read
     */
    public static ModulithRuleSet loadFromClasspath(String fileName) {
        InputStream stream = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(fileName);
        if (stream == null) {
            throw new IllegalArgumentException(
                "Configuration file '" + fileName + "' not found on classpath");
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return parse(reader);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to read configuration file from classpath: " + fileName, e);
        }
    }

    /**
     * Loads a configuration file from the given file path.
     *
     * @param filePath the path to the configuration file
     * @return the parsed {@link ModulithRuleSet}
     * @throws IllegalStateException if the file cannot be read or is missing required fields
     */
    public static ModulithRuleSet loadFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(
                new FileReader(filePath, StandardCharsets.UTF_8))) {
            return parse(reader);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to read configuration file: " + filePath, e);
        }
    }

    /**
     * Loads configuration from a YAML string. Useful for testing or inline configuration.
     *
     * @param yamlContent the YAML content to parse
     * @return the parsed {@link ModulithRuleSet}
     * @throws IllegalStateException if the content is missing required fields or cannot be parsed
     */
    public static ModulithRuleSet loadFromString(String yamlContent) {
        try (BufferedReader reader = new BufferedReader(new StringReader(yamlContent))) {
            return parse(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse YAML content", e);
        }
    }

    private static ModulithRuleSet parse(BufferedReader reader) throws IOException {
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isBlank() && !line.trim().startsWith("#")) {
                lines.add(line);
            }
        }

        String rootPackage = null;
        List<String> moduleOrder = new ArrayList<>();
        Map<String, String> basePackages = new LinkedHashMap<>();
        Map<String, List<String>> apiPackages = new LinkedHashMap<>();
        Map<String, List<String>> internalPackages = new LinkedHashMap<>();
        Map<String, List<String>> allowedDependencies = new LinkedHashMap<>();
        Map<String, Map<String, CommunicationType>> communicationContracts = new LinkedHashMap<>();

        int i = 0;
        while (i < lines.size()) {
            String current = lines.get(i);
            int indent = getIndent(current);
            String trimmed = current.trim();

            if (indent == 0 && trimmed.startsWith("root-package:")) {
                rootPackage = extractValue(trimmed);
                i++;
            } else if (indent == 0 && trimmed.equals("modules:")) {
                i++;
                while (i < lines.size() && getIndent(lines.get(i)) >= 2) {
                    String modLine = lines.get(i);
                    if (getIndent(modLine) == 2 && modLine.trim().endsWith(":")) {
                        String moduleName = modLine.trim();
                        moduleName = moduleName.substring(0, moduleName.length() - 1).trim();
                        moduleOrder.add(moduleName);
                        i++;

                        while (i < lines.size() && getIndent(lines.get(i)) >= 4) {
                            String propLine = lines.get(i);
                            String propTrimmed = propLine.trim();

                            if (propTrimmed.startsWith("base-package:")) {
                                basePackages.put(moduleName, extractValue(propTrimmed));
                                i++;
                            } else if (propTrimmed.equals("api-packages:")) {
                                i++;
                                List<String> items = parseListItems(lines, i);
                                apiPackages.put(moduleName, items);
                                i += items.size();
                            } else if (propTrimmed.equals("internal-packages:")) {
                                i++;
                                List<String> items = parseListItems(lines, i);
                                internalPackages.put(moduleName, items);
                                i += items.size();
                            } else if (propTrimmed.equals("allowed-dependencies:")) {
                                i++;
                                List<String> items = parseListItems(lines, i);
                                allowedDependencies.put(moduleName, items);
                                i += items.size();
                            } else if (propTrimmed.equals("communication:")) {
                                i++;
                                Map<String, CommunicationType> contracts = new LinkedHashMap<>();
                                while (i < lines.size() && getIndent(lines.get(i)) >= 6) {
                                    String commLine = lines.get(i).trim();
                                    int colonIdx = commLine.indexOf(':');
                                    if (colonIdx > 0) {
                                        String targetModule = commLine.substring(0, colonIdx).trim();
                                        String commType = commLine.substring(colonIdx + 1).trim();
                                        contracts.put(targetModule,
                                            CommunicationType.valueOf(commType.toUpperCase()));
                                    }
                                    i++;
                                }
                                communicationContracts.put(moduleName, contracts);
                            } else {
                                i++;
                            }
                        }
                    } else {
                        i++;
                    }
                }
            } else {
                i++;
            }
        }

        if (rootPackage == null) {
            throw new IllegalStateException("root-package is required in the configuration file");
        }

        ModulithRuleSet.Builder builder = ModulithRuleSet.forRootPackage(rootPackage);
        for (String moduleName : moduleOrder) {
            String basePackage = basePackages.getOrDefault(
                moduleName, rootPackage + "." + moduleName);
            ModuleDefinition.Builder moduleBuilder = ModuleDefinition.builder(moduleName)
                .basePackage(basePackage);

            List<String> api = apiPackages.getOrDefault(moduleName, Collections.emptyList());
            if (!api.isEmpty()) {
                moduleBuilder.apiPackages(api.toArray(new String[0]));
            }

            List<String> internal = internalPackages.getOrDefault(moduleName, Collections.emptyList());
            if (!internal.isEmpty()) {
                moduleBuilder.internalPackages(internal.toArray(new String[0]));
            }

            List<String> deps = allowedDependencies.getOrDefault(moduleName, Collections.emptyList());
            if (!deps.isEmpty()) {
                moduleBuilder.allowedDependencies(deps.toArray(new String[0]));
            }

            Map<String, CommunicationType> contracts =
                communicationContracts.getOrDefault(moduleName, Collections.emptyMap());
            for (Map.Entry<String, CommunicationType> entry : contracts.entrySet()) {
                moduleBuilder.communicatesWith(entry.getKey(), entry.getValue());
            }

            builder.module(moduleBuilder.build());
        }

        return builder.build();
    }

    private static String extractValue(String line) {
        int colonIdx = line.indexOf(':');
        if (colonIdx < 0 || colonIdx == line.length() - 1) {
            return "";
        }
        return line.substring(colonIdx + 1).trim();
    }

    private static int getIndent(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    private static List<String> parseListItems(List<String> lines, int startIndex) {
        List<String> items = new ArrayList<>();
        int i = startIndex;
        while (i < lines.size()) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("- ")) {
                items.add(trimmed.substring(2).trim());
                i++;
            } else {
                break;
            }
        }
        return items;
    }
}
