/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.drools.workbench.screens.scenariosimulation.client.editor.strategies;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.drools.scenariosimulation.api.model.ScenarioSimulationModel;
import org.drools.workbench.screens.scenariosimulation.client.models.ScenarioGridModel;
import org.drools.workbench.screens.scenariosimulation.client.rightpanel.TestToolsView;
import org.drools.workbench.screens.scenariosimulation.model.ScenarioSimulationModelContent;
import org.uberfire.backend.vfs.ObservablePath;

import static org.drools.workbench.screens.scenariosimulation.client.utils.ConstantHolder.LOCALDATE_CANONICAL_NAME;
import static org.drools.workbench.screens.scenariosimulation.client.utils.ConstantHolder.LOCALDATE_SIMPLE_NAME;

/**
 * The <b>Strategy</b> to use to manage/modify/save data inside the editor.
 * Every actual implementation should provide methods to manage a specific kind of data source (ex. RULE, DMN)
 */
public interface DataManagementStrategy {

    Map<String, SimpleClassEntry> SIMPLE_CLASSES_MAP = Collections.unmodifiableMap(Stream.of(
            new AbstractMap.SimpleEntry<>(Boolean.class.getSimpleName(), new SimpleClassEntry(Boolean.class)),
            new AbstractMap.SimpleEntry<>(Double.class.getSimpleName(), new SimpleClassEntry(Double.class)),
            new AbstractMap.SimpleEntry<>(Integer.class.getSimpleName(), new SimpleClassEntry(Integer.class)),
            new AbstractMap.SimpleEntry<>(Number.class.getSimpleName(), new SimpleClassEntry(Number.class)),
            new AbstractMap.SimpleEntry<>(String.class.getSimpleName(), new SimpleClassEntry(String.class)),
            // LocalDate is not supported by GWT
            new AbstractMap.SimpleEntry<>(LOCALDATE_SIMPLE_NAME, new SimpleClassEntry(LOCALDATE_SIMPLE_NAME, LOCALDATE_CANONICAL_NAME))).
            collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));

    void populateTestTools(final TestToolsView.Presenter testToolsPresenter, final ScenarioGridModel scenarioGridModel);

    void manageScenarioSimulationModelContent(ObservablePath currentPath, ScenarioSimulationModelContent toManage);

    void setModel(ScenarioSimulationModel model);

    /**
     * Returns <code>true</code> if the given value is a <b>data</b> type (e.g. a <b>FactType</b> for DMO)
     * @param value
     * @return
     */
    boolean isADataType(String value);
}