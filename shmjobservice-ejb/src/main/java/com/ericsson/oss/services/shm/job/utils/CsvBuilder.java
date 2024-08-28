/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.shm.job.utils;

import java.util.List;

public abstract class CsvBuilder {

    private static final String delimiter = "\t";
    private static final String NA = "NA";

    public static String getTitles() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%s%s", "JobName", delimiter));
        stringBuilder.append(String.format("%s%s", "JobType", delimiter));
        stringBuilder.append(String.format("%s%s", "LogLevel", delimiter));
        stringBuilder.append(String.format("%s%s", "NodeName", delimiter));
        stringBuilder.append(String.format("%s%s", "NodeType", delimiter));
        stringBuilder.append(String.format("%s%s", "ActivityName", delimiter));
        stringBuilder.append(String.format("%s%s", "EntryTime", delimiter));
        stringBuilder.append(String.format("%s%s", "Message", delimiter));
        stringBuilder.append("\r\n");
        return stringBuilder.toString();
    }

    public static String constructCsv(final List<String[]> logs) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (final String[] actualLog : logs) {
            for (String s : actualLog) {
                if (s != null) {
                    if (s.contains(delimiter)) {
                        s.replace("delimiter_Tab", "    ");
                    } else if (s.isEmpty()) {
                        s = NA;
                    }
                }
                stringBuilder.append(String.format("%s%s", s, delimiter));
            }
            stringBuilder.append("\r\n");
        }
        return stringBuilder.toString();
    }
}
