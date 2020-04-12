/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.drools.verifier.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.drools.verifier.api.Status;
import org.drools.verifier.api.reporting.Issue;
import org.drools.verifier.core.checks.base.JavaCheckRunner;
import org.drools.verifier.core.main.Analyzer;
import org.drools.verifier.core.main.Reporter;
import org.drools.workbench.models.guided.dtable.backend.GuidedDTXMLPersistence;
import org.drools.workbench.models.guided.dtable.shared.model.GuidedDecisionTable52;
import org.drools.workbench.services.verifier.plugin.client.api.DrlInitialize;
import org.drools.workbench.services.verifier.plugin.client.api.FactTypes;
import org.drools.workbench.services.verifier.plugin.client.builders.ModelMetaDataEnhancer;

@Mojo(name = "analyseGDST", defaultPhase = LifecyclePhase.TEST)
public class GDSTAnalysisMojo
        extends AbstractMojo {

    private final static String DATE_FORMAT = "dd-MMM-yyyy";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    public void execute() throws MojoExecutionException {
        getLog().info("Starting Decision Table analysis.");
        visit(project.getBasedir());
    }

    private void visit(final File file) {
        if (file.isDirectory()) {
            for (final File listFile : file.listFiles()) {
                visit(listFile);
            }
        } else if (file.getName().endsWith("gdst")) {
            try {
                getLog().info(analyze(file));
            } catch (Exception e) {
                getLog().error("Failed to read: " + file.getName());
            }
        }
    }

    private String analyze(final File file) throws Exception {

        getLog().info("Analysing: " + file.getAbsolutePath());

        if (file.getName().contains("Large file")) {
            // TODO : Skipping for test reasons
            return "skipping";
        }

        final GuidedDecisionTable52 table52 = GuidedDTXMLPersistence.getInstance().unmarshal(loadResource(file));
        final Analyzer analyzer = new MavenAnalyzerBuilder()
                .with(getReporter())
                .with(new DrlInitialize("UUID",
                                        table52,
                                        new ModelMetaDataEnhancer(table52).getHeaderMetaData(),
                                        new FactTypes(),
                                        DATE_FORMAT))
                .with(new JavaCheckRunner())
                .buildAnalyzer();

        // First run
        analyzer.resetChecks();
        analyzer.analyze();

        return "done";
    }

    private Reporter getReporter() {
        return new Reporter() {
            @Override
            public void sendReport(Set<Issue> issues) {
                for (Issue issue : issues) {
                    switch (issue.getSeverity()) {
                        case ERROR:
                            getLog().error(String.format("%s %s", issue.getSeverity(), issue.getCheckType()));
                            break;
                        case WARNING:
                            getLog().warn(String.format("%s %s", issue.getSeverity(), issue.getCheckType()));
                            break;
                        case NOTE:
                            getLog().info(String.format("%s %s", issue.getSeverity(), issue.getCheckType()));
                            break;
                    }
                }
            }

            @Override
            public void sendStatus(Status status) {

            }
        };
    }

    public static String loadResource(final File file) throws
            Exception {
        final InputStream in = new FileInputStream(file);
        final Reader reader = new InputStreamReader(in);
        final StringBuilder text = new StringBuilder();
        final char[] buf = new char[1024];
        int len = 0;
        while ((len = reader.read(buf)) >= 0) {
            text.append(buf,
                        0,
                        len);
        }
        return text.toString();
    }
}
