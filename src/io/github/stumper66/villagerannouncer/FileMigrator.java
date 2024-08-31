/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package io.github.stumper66.villagerannouncer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Migrates older yml versions to the latest available
 *
 * @author stumper66
 * @since 1.1
 */
public class FileMigrator {

    private static int getFieldDepth(@NotNull final String line) {
        int whiteSpace = 0;

        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != ' ') {
                break;
            }
            whiteSpace++;
        }
        return whiteSpace == 0 ? 0 : whiteSpace / 2;
    }

    private static class FieldInfo {

        String simpleValue;
        List<String> valueList;
        final int depth;
        boolean hasValue;

        FieldInfo(final String value, final int depth) {
            this.simpleValue = value;
            this.depth = depth;
        }

        FieldInfo(final String value, final int depth, final boolean isListValue) {
            if (isListValue) {
                addListValue(value);
            } else {
                this.simpleValue = value;
            }
            this.depth = depth;
        }

        boolean isList() {
            return valueList != null;
        }

        void addListValue(final String value) {
            if (valueList == null) {
                valueList = new LinkedList<>();
            }
            valueList.add(value);
        }

        public String toString() {
            if (this.isList()) {
                if (this.valueList == null || this.valueList.isEmpty()) {
                    return super.toString();
                } else {
                    return String.join(",", this.valueList);
                }
            }

            if (this.simpleValue == null) {
                return super.toString();
            } else {
                return this.simpleValue;
            }
        }
    }

    private static class KeySectionInfo {

        KeySectionInfo() {
            this.lines = new LinkedList<>();
        }

        int lineNumber;
        @NotNull
        final List<String> lines;
        int sectionNumber;
        int sectionStartingLine;
    }

    private static String getKeyFromList(final @NotNull List<String> list,
                                         final String currentKey) {
        if (list.isEmpty()) {
            return currentKey;
        }

        String result = String.join(".", list);
        if (currentKey != null) {
            result += "." + currentKey;
        }

        return result;
    }

    @NotNull private static Map<String, KeySectionInfo> buildKeySections(
            @NotNull final List<String> contents) {

        final Map<String, KeySectionInfo> sections = new TreeMap<>();
        KeySectionInfo keySection = null;
        String keyName = null;
        int sectionNumber = 0;
        int sectionStartingLine = 0;
        boolean foundNonComment = false;

        for (int i = 0; i < contents.size(); i++) {
            final String origline = contents.get(i);

            final int depth = getFieldDepth(origline);
            final String line = origline.replace("\t", "").trim();

            if (line.startsWith("# ||  Section")) {
                final int foundSectionNumber = extractSectionNumber(line);
                if (foundSectionNumber > 0) {
                    sectionNumber = foundSectionNumber;
                }
                sectionStartingLine = 0;
            }

            if (sectionStartingLine == 0 && sectionNumber > 0 && line.startsWith("# ||||")) {
                sectionStartingLine = i + 2;
                foundNonComment = false;
            }

            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }

            if (!foundNonComment) {
                if (sectionStartingLine > 0) {
                    sectionStartingLine = i;
                }
                foundNonComment = true;
            }

            if (depth == 0) {
                if (keySection != null) {
                    sections.put(keyName, keySection);
                }

                keySection = new KeySectionInfo();
                keySection.lineNumber = i;
                keySection.sectionNumber = sectionNumber;
                keySection.sectionStartingLine = sectionStartingLine;
                keyName = line;
            } else if (keySection != null) {
                keySection.lines.add(origline);
            }
        }

        if (keySection != null) {
            sections.put(keyName, keySection);
        }

        return sections;
    }

    private static int extractSectionNumber(final String input) {
        final Pattern p = Pattern.compile("# \\|\\|\\s{2}Section (\\d{2})");
        final Matcher m = p.matcher(input);
        if (m.find() && m.groupCount() == 1) {
            String temp = m.group(1);
            if (temp.length() > 1 && temp.charAt(0) == '0') {
                temp = temp.substring(1);
            }

            if (isInteger(temp)) {
                return Integer.parseInt(temp);
            }
        }

        return 0;
    }

    private static boolean isInteger(@Nullable final String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        try {
            Integer.parseInt(str);
            return true;
        } catch (final NumberFormatException ex) {
            return false;
        }
    }

    static void copyYmlValues(final File from, @NotNull final File to) {
        final String regexPattern = "^[^':]*:.*";
        final List<String> processedKeys = new LinkedList<>();

        try {
            final List<String> oldConfigLines = Files.readAllLines(from.toPath(),
                    StandardCharsets.UTF_8);
            final List<String> newConfigLines = Files.readAllLines(to.toPath(),
                    StandardCharsets.UTF_8);

            final SortedMap<String, FieldInfo> oldConfigMap = getMapFromConfig(oldConfigLines);
            final SortedMap<String, FileMigrator.FieldInfo> newConfigMap = getMapFromConfig(
                    newConfigLines);
            final List<String> currentKey = new LinkedList<>();
            int keysMatched = 0;
            int valuesUpdated = 0;
            int valuesMatched = 0;

            for (int currentLine = 0; currentLine < newConfigLines.size(); currentLine++) {
                String line = newConfigLines.get(currentLine);
                final int depth = getFieldDepth(line);
                if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }

                if (line.matches(regexPattern)) {
                    final int firstColon = line.indexOf(':');
                    final boolean hasValues = line.length() > firstColon + 1;
                    String key = line.substring(0, firstColon).replace("\t", "").trim();
                    final String keyOnly = key;
                    String oldKey = key;

                    if (depth == 0) {
                        currentKey.clear();
                    } else if (currentKey.size() > depth) {
                        while (currentKey.size() > depth) {
                            currentKey.remove(currentKey.size() - 1);
                        }
                        key = getKeyFromList(currentKey, key);
                    } else {
                        key = getKeyFromList(currentKey, key);
                    }

                    if (!hasValues) {
                        currentKey.add(keyOnly);

                        if (oldConfigMap.containsKey(oldKey) && newConfigMap.containsKey(key)) {
                            final FileMigrator.FieldInfo fiOld = oldConfigMap.get(oldKey);
                            final FileMigrator.FieldInfo fiNew = newConfigMap.get(key);
                            final String padding = getPadding((depth + 1) * 2);

                            // arrays go here:
                            if (fiOld.isList()) {
                                // add any values present in old list that might not be present in new
                                for (final String oldValue : fiOld.valueList) {
                                    if (!fiNew.isList() || !fiNew.valueList.contains(
                                            oldValue)) {
                                        final String newline =
                                                padding + "- " + oldValue; // + "\r\n" + line;
                                        newConfigLines.add(currentLine + 1, newline);
                                        Log.inf(
                                                "&fFile Loader: &8(Migration) &7Added array value: &b"
                                                        + oldValue);
                                    }
                                }
                            } else {
                                // non-array values go here.  Loop thru and find any subkeys under here
                                final int numOfPeriods = countPeriods(key);
                                for (final Map.Entry<String, FieldInfo> entry : oldConfigMap.entrySet()) {
                                    final String enumeratedKey = entry.getKey();
                                    final int numOfPeriods_Enumerated = countPeriods(
                                            enumeratedKey);
                                    if (enumeratedKey.startsWith(key)
                                            && numOfPeriods_Enumerated == numOfPeriods + 1
                                            && !newConfigMap.containsKey(enumeratedKey)) {
                                        final FileMigrator.FieldInfo fi = entry.getValue();
                                        if (!fi.isList() && fi.simpleValue != null) {
                                            final String newline =
                                                    padding + getEndingKey(enumeratedKey) + ": "
                                                            + fi.simpleValue;
                                            newConfigLines.add(currentLine + 1, newline);
                                            Log.inf(
                                                    "&fFile Loader: &8(Migration) &7Adding key: &b"
                                                            + enumeratedKey + "&7, value: &r"
                                                            + fi.simpleValue + "&7.");

                                            processedKeys.add(key);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (oldConfigMap.containsKey(oldKey)) {
                        keysMatched++;
                        final String value = line.substring(firstColon + 1).trim();
                        final FileMigrator.FieldInfo fi = oldConfigMap.get(oldKey);
                        final String migratedValue = fi.simpleValue;

                        if (key.toLowerCase().startsWith("file-version")) {
                            continue;
                        }

                        final String parentKey = getParentKey(key);
                        if (fi.hasValue && parentKey != null && !processedKeys.contains(
                                parentKey)) {
                            // here's where we add values from the old config not present in the new
                            for (final Map.Entry<String, FieldInfo> entry : oldConfigMap.entrySet()) {
                                final String oldValue = entry.getKey();
                                if (!oldValue.startsWith(parentKey)) {
                                    continue;
                                }
                                if (newConfigMap.containsKey(oldValue)) {
                                    continue;
                                }
                                if (!isEntitySameSubkey(parentKey, oldValue)) {
                                    continue;
                                }

                                final FileMigrator.FieldInfo fiOld = entry.getValue();
                                if (fiOld.isList()) {
                                    continue;
                                }
                                final String padding = getPadding(depth * 2);
                                final String newline =
                                        padding + getEndingKey(oldValue) + ": " + fiOld.simpleValue;
                                newConfigLines.add(currentLine + 1, newline);
                                Log.inf(
                                        "&fFile Loader: &8(Migration) &7Adding key: &b"
                                                + oldValue + "&7, value: &r" + fiOld.simpleValue
                                                + "&7.");
                            }
                            processedKeys.add(parentKey);
                        }

                        if (!value.equals(migratedValue)) {
                            if (migratedValue != null) {
                                valuesUpdated++;
                                Log.inf(
                                        "&fFile Loader: &8(Migration) &7Current key: &b" + key
                                                + "&7, replacing: &r" + value + "&7, with: &r"
                                                + migratedValue + "&7.");

                                line = line.replace(value, migratedValue);
                                newConfigLines.set(currentLine, line);
                            }
                        } else {
                            valuesMatched++;
                        }
                    }
                } else if (line.trim().startsWith("-")) {
                    final String key = getKeyFromList(currentKey, null);
                    final String value = line.trim().substring(1).trim();

                    // we have an array value present in the new config but not the old, so it must've been removed
                    if (oldConfigMap.containsKey(key) && oldConfigMap.get(key).isList()
                            && !oldConfigMap.get(key).valueList.contains(value)) {
                        newConfigLines.remove(currentLine);
                        currentLine--;
                        Log.inf(
                                "&fFile Loader: &8(Migration) &7Current key: &b" + key
                                        + "&7, removing value: &r" + value + "&7.");
                    }
                }
            } // loop to next line

            Files.write(to.toPath(), newConfigLines, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING);
            Log.inf(
                    "&fFile Loader: &8(Migration) &7Migrated &b" + to.getName() + "&7 successfully.");
            Log.inf(String.format(
                    "&fFile Loader: &8(Migration) &7Keys matched: &b%s&7, values matched: &b%s&7, values updated: &b%s&7.",
                    keysMatched, valuesMatched, valuesUpdated));
        } catch (final Exception e) {
            Log.war("File Loader: (Migration) Failed to migrate " + to.getName()
                    + "! Stack trace:");
            e.printStackTrace();
        }
    }

    private static int countPeriods(@NotNull final String text) {
        int count = 0;

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '.') {
                count++;
            }
        }

        return count;
    }

    @NotNull private static String getPadding(final int space) {
        return " ".repeat(space);
    }

    private static boolean isEntitySameSubkey(@NotNull final String key1,
                                              @NotNull final String key2) {
        final int lastPeriod = key2.lastIndexOf('.');
        final String checkKey = lastPeriod > 0 ? key2.substring(0, lastPeriod) : key2;

        return (key1.equalsIgnoreCase(checkKey));
    }

    @NotNull private static String getEndingKey(@NotNull final String input) {
        final int lastPeriod = input.lastIndexOf('.');
        if (lastPeriod < 0) {
            return input;
        }

        return input.substring(lastPeriod + 1);
    }

    @Nullable private static String getParentKey(@NotNull final String input) {
        final int lastPeriod = input.lastIndexOf('.');
        if (lastPeriod < 0) {
            return null;
        }

        return input.substring(0, lastPeriod);
    }

    private static int getFirstNonCommentLine(@NotNull final List<String> input) {
        for (int lineNum = 0; lineNum < input.size(); lineNum++) {
            final String line = input.get(lineNum).replace("\t", "").trim();
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }
            return lineNum;
        }

        return -1;
    }

    @NotNull private static SortedMap<String, FileMigrator.FieldInfo> getMapFromConfig(
            @NotNull final List<String> input) {
        final SortedMap<String, FileMigrator.FieldInfo> configMap = new TreeMap<>();
        final List<String> currentKey = new LinkedList<>();
        final String regexPattern = "^[^':]*:.*";

        for (String line : input) {

            final int depth = getFieldDepth(line);
            line = line.replace("\t", "").trim();
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }

            //if (line.contains(":")) {
            if (line.matches(regexPattern)) {
                final int firstColon = line.indexOf(':');
                final boolean hasValues = line.length() > firstColon + 1;
                String key = line.substring(0, firstColon).replace("\t", "").trim();
                final String origKey = key;

                if (origKey.startsWith("-")) {
                    if (currentKey.size() > depth) {
                        while (currentKey.size() > depth) {
                            currentKey.remove(currentKey.size() - 1);
                        }
                    }
                    final String temp = origKey.substring(1).trim();
                    String tempKey;
                    for (int i = 0; i < 100; i++) {
                        tempKey = String.format("%s[%s]", temp, i);
                        final String checkKey = getKeyFromList(currentKey, tempKey);
                        if (!configMap.containsKey(checkKey)) {
                            currentKey.add(tempKey);
                            configMap.put(checkKey, null);
                            break;
                        }
                    }
                    continue;
                }

                if (depth == 0) {
                    currentKey.clear();
                } else {
                    if (currentKey.size() > depth) {
                        while (currentKey.size() > depth) {
                            currentKey.remove(currentKey.size() - 1);
                        }
                    }
                    key = getKeyFromList(currentKey, key);
                }

                if (!hasValues) {
                    currentKey.add(origKey);
                    if (!configMap.containsKey(key)) {
                        configMap.put(key, new FileMigrator.FieldInfo(null, depth));
                    }
                } else {
                    final String value = line.substring(firstColon + 1).trim();
                    final FileMigrator.FieldInfo fi = new FileMigrator.FieldInfo(value, depth);
                    fi.hasValue = true;
                    configMap.put(key, fi);
                }
            } else if (line.startsWith("-")) {
                final String key = getKeyFromList(currentKey, null);
                final String value = line.trim().substring(1).trim();
                if (configMap.containsKey(key)) {
                    final FileMigrator.FieldInfo fi = configMap.get(key);
                    fi.addListValue(value);
                } else {
                    configMap.put(key, new FileMigrator.FieldInfo(value, depth, true));
                }
            }
        }

        return configMap;
    }
}