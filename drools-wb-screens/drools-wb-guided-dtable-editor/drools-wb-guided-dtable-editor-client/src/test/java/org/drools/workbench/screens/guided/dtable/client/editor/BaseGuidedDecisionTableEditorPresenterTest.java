/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.drools.workbench.screens.guided.dtable.client.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.drools.workbench.models.guided.dtable.shared.model.GuidedDecisionTable52;
import org.drools.workbench.screens.guided.dtable.client.type.GuidedDTableResourceType;
import org.drools.workbench.screens.guided.dtable.client.widget.table.GuidedDecisionTableView;
import org.drools.workbench.screens.guided.dtable.client.widget.table.events.cdi.DecisionTableSelectedEvent;
import org.drools.workbench.screens.guided.dtable.model.GuidedDecisionTableEditorContent;
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.guvnor.common.services.shared.validation.model.ValidationMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.uberfire.backend.vfs.ObservablePath;
import org.uberfire.backend.vfs.Path;
import org.uberfire.ext.editor.commons.client.menu.MenuItems;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.workbench.events.NotificationEvent;
import org.uberfire.workbench.model.menu.Menus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class BaseGuidedDecisionTableEditorPresenterTest extends BaseGuidedDecisionTablePresenterTest<BaseGuidedDecisionTableEditorPresenter> {

    private GuidedDTableResourceType resourceType = new GuidedDTableResourceType();

    @Override
    protected GuidedDecisionTableEditorPresenter getPresenter() {
        return new GuidedDecisionTableEditorPresenter(view,
                                                      dtServiceCaller,
                                                      notification,
                                                      decisionTableSelectedEvent,
                                                      validationPopup,
                                                      resourceType,
                                                      editMenuBuilder,
                                                      viewMenuBuilder,
                                                      insertMenuBuilder,
                                                      radarMenuBuilder,
                                                      modeller,
                                                      beanManager,
                                                      placeManager);
    }

    @Test
    public void checkInit() {
        verify(viewMenuBuilder,
               times(1)).setModeller(eq(modeller));
        verify(insertMenuBuilder,
               times(1)).setModeller(eq(modeller));
        verify(radarMenuBuilder,
               times(1)).setModeller(eq(modeller));
        verify(view,
               times(1)).setModellerView(eq(modellerView));
    }

    @Test
    public void checkOnStartup() {
        final ObservablePath path = mock(ObservablePath.class);
        final PlaceRequest placeRequest = mock(PlaceRequest.class);
        final GuidedDecisionTableEditorContent content = makeDecisionTableContent();
        final GuidedDecisionTableView.Presenter dtPresenter = makeDecisionTable(path,
                                                                                path,
                                                                                placeRequest,
                                                                                content);

        presenter.onStartup(path,
                            placeRequest);

        assertEquals(path,
                     presenter.editorPath);
        assertEquals(placeRequest,
                     presenter.editorPlaceRequest);

        verify(view,
               times(1)).showLoading();
        verify(presenter,
               times(1)).loadDocument(eq(path),
                                      eq(placeRequest));
        verify(dtService,
               times(1)).loadContent(eq(path));
        verify(modeller,
               times(1)).addDecisionTable(eq(path),
                                          eq(placeRequest),
                                          eq(content),
                                          any(Boolean.class),
                                          eq(null),
                                          eq(null));
        verify(presenter,
               times(1)).registerDocument(eq(dtPresenter));
        verify(decisionTableSelectedEvent,
               times(1)).fire(dtSelectedEventCaptor.capture());
        verify(view,
               times(1)).hideBusyIndicator();

        final DecisionTableSelectedEvent dtSelectedEvent = dtSelectedEventCaptor.getValue();
        assertNotNull(dtSelectedEvent);
        assertTrue(dtSelectedEvent.getPresenter().isPresent());
        assertEquals(dtPresenter,
                     dtSelectedEvent.getPresenter().get());
    }

    @Test
    public void checkDecisionTableSelectedEventFiredWhenEditorReceivesFocusWithActiveDecisionTable() {
        final GuidedDecisionTableView.Presenter activeDtable = mock(GuidedDecisionTableView.Presenter.class);
        when(modeller.getActiveDecisionTable()).thenReturn(activeDtable);

        presenter.onFocus();

        verify(activeDtable,
               times(1)).initialiseAnalysis();

        verify(decisionTableSelectedEvent,
               times(1)).fire(dtSelectedEventCaptor.capture());

        final DecisionTableSelectedEvent event = dtSelectedEventCaptor.getValue();
        assertNotNull(event);
        assertTrue(event.getPresenter().isPresent());
        assertEquals(activeDtable,
                     event.getPresenter().get());
    }

    @Test
    public void checkDecisionTableSelectedEventNotFiredWhenEditorReceivesFocusWithoutActiveDecisionTable() {
        presenter.onFocus();

        verify(decisionTableSelectedEvent,
               never()).fire(any(DecisionTableSelectedEvent.class));
    }

    @Test
    public void checkMayCloseWithNoDecisionTable() {
        assertTrue(presenter.mayClose());
    }

    @Test
    public void checkMayCloseWithCleanDecisionTable() {
        final ObservablePath path = mock(ObservablePath.class);
        final PlaceRequest placeRequest = mock(PlaceRequest.class);
        final GuidedDecisionTableEditorContent content = makeDecisionTableContent();
        final GuidedDecisionTableView.Presenter dtPresenter = makeDecisionTable(path,
                                                                                path,
                                                                                placeRequest,
                                                                                content);
        when(dtPresenter.getOriginalHashCode()).thenReturn(0);
        when(modeller.getAvailableDecisionTables()).thenReturn(new HashSet<GuidedDecisionTableView.Presenter>() {{
            add(dtPresenter);
        }});

        assertTrue(presenter.mayClose());
    }

    @Test
    public void checkMayCloseWithDirtyDecisionTable() {
        final ObservablePath path = mock(ObservablePath.class);
        final PlaceRequest placeRequest = mock(PlaceRequest.class);
        final GuidedDecisionTableEditorContent content = makeDecisionTableContent();
        final GuidedDecisionTableView.Presenter dtPresenter = makeDecisionTable(path,
                                                                                path,
                                                                                placeRequest,
                                                                                content);
        when(dtPresenter.getOriginalHashCode()).thenReturn(10);
        when(modeller.getAvailableDecisionTables()).thenReturn(new HashSet<GuidedDecisionTableView.Presenter>() {{
            add(dtPresenter);
        }});

        assertFalse(presenter.mayClose());
    }

    @Test
    public void checkOnClose() {
        presenter.onClose();

        verify(modeller,
               times(1)).onClose();
    }

    @Test
    public void checkOnDecisionTableSelectedWhenAvailableSelected() {
        final ObservablePath path = mock(ObservablePath.class);
        final PlaceRequest placeRequest = mock(PlaceRequest.class);
        final GuidedDecisionTableEditorContent content = makeDecisionTableContent();
        final GuidedDecisionTableView.Presenter dtPresenter = makeDecisionTable(path,
                                                                                path,
                                                                                placeRequest,
                                                                                content);
        final DecisionTableSelectedEvent event = new DecisionTableSelectedEvent(dtPresenter);

        when(modeller.isDecisionTableAvailable(any(GuidedDecisionTableView.Presenter.class))).thenReturn(true);
        when(presenter.getActiveDocument()).thenReturn(dtPresenter);

        presenter.onDecisionTableSelected(event);

        verify(presenter,
               never()).activateDocument(any(GuidedDecisionTableView.Presenter.class));
        assertTrue(getMenuState(presenter.getMenus(),
                                MenuItems.VALIDATE));
    }

    private boolean getMenuState(final Menus menus,
                                 final MenuItems menuItem) {
        return menus.getItems().stream().filter(m -> m.getIdentifier() != null).filter(m -> m.getCaption().toLowerCase().equals(menuItem.name().toLowerCase())).findFirst().get().isEnabled();
    }

    @Test
    public void checkOnDecisionTableSelectedWhenAvailableNotSelected() {
        final ObservablePath path = mock(ObservablePath.class);
        final PlaceRequest placeRequest = mock(PlaceRequest.class);
        final GuidedDecisionTableEditorContent content = makeDecisionTableContent();
        final GuidedDecisionTableView.Presenter dtPresenter = makeDecisionTable(path,
                                                                                path,
                                                                                placeRequest,
                                                                                content);
        final DecisionTableSelectedEvent event = new DecisionTableSelectedEvent(dtPresenter);

        when(modeller.isDecisionTableAvailable(any(GuidedDecisionTableView.Presenter.class))).thenReturn(true);
        when(presenter.getActiveDocument()).thenReturn(null);

        presenter.onStartup(path,
                            placeRequest);

        presenter.onDecisionTableSelected(event);

        verify(presenter,
               times(1)).activateDocument(any(GuidedDecisionTableView.Presenter.class));
        verify(radarMenuItem,
               atLeast(1)).setEnabled(eq(true));
        assertTrue(getMenuState(presenter.getMenus(),
                                MenuItems.VALIDATE));
    }

    @Test
    public void checkOnDecisionTableSelectedWhenNotAvailable() {
        final ObservablePath path = mock(ObservablePath.class);
        final PlaceRequest placeRequest = mock(PlaceRequest.class);
        final GuidedDecisionTableEditorContent content = makeDecisionTableContent();
        final GuidedDecisionTableView.Presenter dtPresenter = makeDecisionTable(path,
                                                                                path,
                                                                                placeRequest,
                                                                                content);
        final DecisionTableSelectedEvent event = new DecisionTableSelectedEvent(dtPresenter);

        when(modeller.isDecisionTableAvailable(any(GuidedDecisionTableView.Presenter.class))).thenReturn(false);

        presenter.onDecisionTableSelected(event);

        verify(presenter,
               never()).activateDocument(any(GuidedDecisionTableView.Presenter.class));
        assertTrue(getMenuState(presenter.getMenus(),
                                MenuItems.VALIDATE));
    }

    @Test
    public void checkOnDecisionTableSelectedEventNoTableSelected() {
        final DecisionTableSelectedEvent event = DecisionTableSelectedEvent.NONE;

        presenter.onDecisionTableSelected(event);

        verify(presenter,
               never()).activateDocument(any(GuidedDecisionTableView.Presenter.class));
        assertFalse(getMenuState(presenter.getMenus(),
                                 MenuItems.VALIDATE));
    }

    @Test
    public void checkRefreshDocument() {
        final ObservablePath path = mock(ObservablePath.class);
        final PlaceRequest placeRequest = mock(PlaceRequest.class);
        final GuidedDecisionTableEditorContent content = makeDecisionTableContent();
        final GuidedDecisionTableView.Presenter dtPresenter = makeDecisionTable(path,
                                                                                path,
                                                                                placeRequest,
                                                                                content);

        presenter.onStartup(path,
                            placeRequest);

        verify(view,
               times(1)).showLoading();
        verify(dtService,
               times(1)).loadContent(eq(path));
        verify(decisionTableSelectedEvent,
               times(1)).fire(dtSelectedEventCaptor.capture());
        verify(view,
               times(1)).hideBusyIndicator();

        final DecisionTableSelectedEvent dtSelectedEvent = dtSelectedEventCaptor.getValue();
        assertNotNull(dtSelectedEvent);
        assertTrue(dtSelectedEvent.getPresenter().isPresent());
        assertEquals(dtPresenter,
                     dtSelectedEvent.getPresenter().get());

        when(dtPresenter.getCurrentPath()).thenReturn(path);

        presenter.refreshDocument(dtPresenter);

        verify(view,
               times(2)).showLoading();
        verify(dtService,
               times(2)).loadContent(eq(path));
        verify(modeller,
               times(1)).refreshDecisionTable(eq(dtPresenter),
                                              eq(path),
                                              eq(placeRequest),
                                              eq(content),
                                              any(Boolean.class));
        verify(presenter,
               times(1)).activateDocument(eq(dtPresenter));
        verify(view,
               times(2)).hideBusyIndicator();
    }

    @Test
    public void checkRemoveDocument() {
        final ObservablePath path = mock(ObservablePath.class);
        final PlaceRequest placeRequest = mock(PlaceRequest.class);
        final GuidedDecisionTableEditorContent content = makeDecisionTableContent();
        final GuidedDecisionTableView.Presenter dtPresenter = makeDecisionTable(path,
                                                                                path,
                                                                                placeRequest,
                                                                                content);

        presenter.onStartup(path,
                            placeRequest);

        presenter.removeDocument(dtPresenter);

        verify(modeller,
               times(1)).removeDecisionTable(eq(dtPresenter));
        verify(presenter,
               times(1)).deregisterDocument(eq(dtPresenter));
        verify(presenter,
               times(1)).openOtherDecisionTable();
        verify(dtPresenter,
               times(1)).onClose();
    }

    @Test
    public void checkOpenOtherDecisionTableIsLastDecisionTable() {
        when(modeller.getAvailableDecisionTables()).thenReturn(Collections.emptySet());

        presenter.openOtherDecisionTable();

        verify(presenter,
               never()).activateDocument(any(GuidedDecisionTableView.Presenter.class));
        verify(placeManager,
               never()).forceClosePlace(any(String.class));
        verify(placeManager,
               never()).forceClosePlace(any(PlaceRequest.class));
        verify(decisionTableSelectedEvent,
               times(1)).fire(dtSelectedEventCaptor.capture());

        final DecisionTableSelectedEvent dtSelectedEvent = dtSelectedEventCaptor.getValue();
        assertNotNull(dtSelectedEvent);
        assertFalse(dtSelectedEvent.getPresenter().isPresent());
    }

    @Test
    public void checkOpenOtherDecisionTableIsNotLastDecisionTable() {
        final GuidedDecisionTableView.Presenter remainingDtPresenter = mock(GuidedDecisionTableView.Presenter.class);

        when(modeller.getAvailableDecisionTables()).thenReturn(new HashSet<GuidedDecisionTableView.Presenter>() {{
            add(remainingDtPresenter);
        }});
        doNothing().when(presenter).activateDocument(any(GuidedDecisionTableView.Presenter.class));

        presenter.openOtherDecisionTable();

        verify(placeManager,
               never()).forceClosePlace(any(String.class));
        verify(placeManager,
               never()).forceClosePlace(any(PlaceRequest.class));
        verify(decisionTableSelectedEvent,
               times(2)).fire(dtSelectedEventCaptor.capture());

        final List<DecisionTableSelectedEvent> dtSelectedEvents = dtSelectedEventCaptor.getAllValues();
        assertNotNull(dtSelectedEvents);
        assertEquals(2,
                     dtSelectedEvents.size());
        assertFalse(dtSelectedEvents.get(0).getPresenter().isPresent());
        assertTrue(dtSelectedEvents.get(1).getPresenter().isPresent());
        assertEquals(dtSelectedEvents.get(1).getPresenter().get(),
                     remainingDtPresenter);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void checkOnValidateWithErrors() {
        final ObservablePath path = mock(ObservablePath.class);
        final PlaceRequest placeRequest = mock(PlaceRequest.class);
        final GuidedDecisionTableEditorContent content = makeDecisionTableContent();
        final GuidedDecisionTableView.Presenter dtPresenter = makeDecisionTable(path,
                                                                                path,
                                                                                placeRequest,
                                                                                content);
        final List<ValidationMessage> validationMessages = new ArrayList<ValidationMessage>() {{
            add(new ValidationMessage());
        }};

        when(dtService.validate(any(Path.class),
                                any(GuidedDecisionTable52.class))).thenReturn(validationMessages);
        doNothing().when(presenter).showValidationPopup(any(List.class));

        presenter.onValidate(dtPresenter);

        final ArgumentCaptor<GuidedDecisionTable52> modelCaptor = ArgumentCaptor.forClass(GuidedDecisionTable52.class);

        verify(dtService,
               times(1)).validate(eq(path),
                                  modelCaptor.capture());
        assertNotNull(modelCaptor.getValue());
        assertEquals(dtPresenter.getModel(),
                     modelCaptor.getValue());
        verify(notification,
               never()).fire(any(NotificationEvent.class));
        verify(presenter,
               times(1)).showValidationPopup(eq(validationMessages));
    }

    @Test
    public void checkOnValidateWithoutErrors() {
        final ObservablePath path = mock(ObservablePath.class);
        final PlaceRequest placeRequest = mock(PlaceRequest.class);
        final GuidedDecisionTableEditorContent content = makeDecisionTableContent();
        final GuidedDecisionTableView.Presenter dtPresenter = makeDecisionTable(path,
                                                                                path,
                                                                                placeRequest,
                                                                                content);

        when(dtService.validate(any(Path.class),
                                any(GuidedDecisionTable52.class))).thenReturn(Collections.emptyList());

        presenter.onValidate(dtPresenter);

        final ArgumentCaptor<GuidedDecisionTable52> modelCaptor = ArgumentCaptor.forClass(GuidedDecisionTable52.class);

        verify(dtService,
               times(1)).validate(eq(path),
                                  modelCaptor.capture());
        assertNotNull(modelCaptor.getValue());
        assertEquals(dtPresenter.getModel(),
                     modelCaptor.getValue());
        verify(notification,
               times(1)).fire(any(NotificationEvent.class));
    }

    @Test
    public void checkOnSave() {
        final String commitMessage = "message";
        final ObservablePath path = mock(ObservablePath.class);
        final PlaceRequest placeRequest = mock(PlaceRequest.class);
        final GuidedDecisionTableEditorContent content = makeDecisionTableContent();
        final GuidedDecisionTableView.Presenter dtPresenter = makeDecisionTable(path,
                                                                                path,
                                                                                placeRequest,
                                                                                content);

        presenter.onSave(dtPresenter,
                         commitMessage);

        final ArgumentCaptor<GuidedDecisionTable52> modelCaptor = ArgumentCaptor.forClass(GuidedDecisionTable52.class);
        final ArgumentCaptor<Metadata> metadataCaptor = ArgumentCaptor.forClass(Metadata.class);

        verify(dtService,
               times(1)).saveAndUpdateGraphEntries(eq(path),
                                                   modelCaptor.capture(),
                                                   metadataCaptor.capture(),
                                                   eq(commitMessage));
        assertNotNull(modelCaptor.getValue());
        assertEquals(dtPresenter.getModel(),
                     modelCaptor.getValue());
        assertNotNull(metadataCaptor.getValue());
        assertEquals(dtPresenter.getOverview().getMetadata(),
                     metadataCaptor.getValue());
    }

    @Test
    public void checkOnSourceTabSelected() {
        final String source = "source";
        final ObservablePath path = mock(ObservablePath.class);
        final PlaceRequest placeRequest = mock(PlaceRequest.class);
        final GuidedDecisionTableEditorContent content = makeDecisionTableContent();
        final GuidedDecisionTableView.Presenter dtPresenter = makeDecisionTable(path,
                                                                                path,
                                                                                placeRequest,
                                                                                content);

        when(dtService.toSource(eq(path),
                                any(GuidedDecisionTable52.class))).thenReturn(source);

        presenter.onSourceTabSelected(dtPresenter);

        final ArgumentCaptor<GuidedDecisionTable52> modelCaptor = ArgumentCaptor.forClass(GuidedDecisionTable52.class);

        verify(dtService,
               times(1)).toSource(eq(path),
                                  modelCaptor.capture());
        assertNotNull(modelCaptor.getValue());
        assertEquals(dtPresenter.getModel(),
                     modelCaptor.getValue());

        verify(presenter,
               times(1)).updateSource(eq(source));
    }
}