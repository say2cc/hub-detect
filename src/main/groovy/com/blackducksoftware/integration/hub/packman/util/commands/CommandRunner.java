/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.hub.packman.util.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class CommandRunner {
    private final Logger logger;

    private final File workingDirectory;

    public CommandRunner(final Logger logger, final File workingDirectory) {
        this.logger = logger;
        this.workingDirectory = workingDirectory;
    }

    public CommandOutput executeQuietly(final Command command) {
        return execute(command, true);
    }

    public CommandOutput execute(final Command command) {
        return execute(command, false);
    }

    public CommandOutput execute(final Command command, final boolean runQuietly) {
        return executeExactly(runQuietly, command.getExecutable(), command.getArgs());
    }

    private CommandOutput executeExactly(final boolean runQuietly, final Executable executable, final String... args) {
        // We have to wrap Arrays.asList() because the supplied list does not support adding at an index
        final List<String> arguments = new ArrayList<>(Arrays.asList(args));
        if (executable != null) {
            arguments.add(0, executable.getFound());
        }

        final ProcessBuilder processBuilder = new ProcessBuilder(arguments);
        processBuilder.directory(workingDirectory);

        logger.debug(String.format("Running command >%s", StringUtils.join(" ", arguments)));
        try {
            Process process;
            process = processBuilder.start();
            final String infoOutput = printStream(process.getInputStream(), runQuietly, false);
            final String errorOutput = printStream(process.getErrorStream(), runQuietly, true);
            return new CommandOutput(infoOutput, errorOutput);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String printStream(final InputStream inputStream, final boolean runQuietly, final boolean error) {
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        final StringBuilder stringBuilder = new StringBuilder();

        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + "\n");
                if (!runQuietly || logger.isTraceEnabled()) {
                    if (error && StringUtils.isNotBlank(line)) {
                        logger.error(line);
                    } else {
                        logger.info(line);
                    }
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return stringBuilder.toString();
    }

}