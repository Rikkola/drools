package org.drools.workbench.screens.guided.dtable.client.widget.table;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.ait.lienzo.client.core.event.NodeMouseMoveEvent;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.types.Transform;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.drools.workbench.models.guided.dtable.shared.model.ActionCol52;
import org.drools.workbench.models.guided.dtable.shared.model.AttributeCol52;
import org.drools.workbench.models.guided.dtable.shared.model.BaseColumn;
import org.drools.workbench.models.guided.dtable.shared.model.CompositeColumn;
import org.drools.workbench.models.guided.dtable.shared.model.MetadataCol52;
import org.drools.workbench.screens.guided.dtable.client.resources.GuidedDecisionTableResources;
import org.drools.workbench.screens.guided.dtable.client.resources.i18n.GuidedDecisionTableConstants;
import org.drools.workbench.screens.guided.dtable.client.widget.table.accordion.GuidedDecisionTableAccordion;
import org.drools.workbench.screens.guided.dtable.client.widget.table.accordion.GuidedDecisionTableAccordionItem;
import org.drools.workbench.screens.guided.dtable.client.widget.table.columns.control.AttributeColumnConfigRow;
import org.drools.workbench.screens.guided.dtable.client.widget.table.columns.control.ColumnLabelWidget;
import org.drools.workbench.screens.guided.dtable.client.widget.table.columns.control.ColumnManagementView;
import org.drools.workbench.screens.guided.dtable.client.widget.table.columns.control.DeleteColumnManagementAnchorWidget;
import org.drools.workbench.screens.guided.dtable.client.widget.table.utilities.ColumnUtilities;
import org.drools.workbench.screens.guided.dtable.client.wizard.column.pages.common.DecisionTableColumnViewUtils;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.CheckBox;
import org.gwtbootstrap3.client.ui.Icon;
import org.gwtbootstrap3.client.ui.constants.IconSize;
import org.jboss.errai.common.client.ui.ElementWrapperWidget;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.kie.workbench.common.widgets.client.ruleselector.RuleSelector;
import org.uberfire.ext.wires.core.grids.client.model.Bounds;
import org.uberfire.ext.wires.core.grids.client.model.GridColumn;
import org.uberfire.ext.wires.core.grids.client.widget.grid.GridWidget;
import org.uberfire.ext.wires.core.grids.client.widget.layer.GridLayer;
import org.uberfire.ext.wires.core.grids.client.widget.layer.impl.DefaultGridLayer;
import org.uberfire.ext.wires.core.grids.client.widget.layer.impl.GridLienzoPanel;
import org.uberfire.ext.wires.core.grids.client.widget.layer.pinning.GridPinnedModeManager;
import org.uberfire.ext.wires.core.grids.client.widget.layer.pinning.TransformMediator;
import org.uberfire.ext.wires.core.grids.client.widget.layer.pinning.impl.RestrictedMousePanMediator;

public class GuidedDecisionTableModellerViewImpl extends Composite implements GuidedDecisionTableModellerView {

    private static final double VP_SCALE = 1.0;

    private static GuidedDecisionTableModellerViewImplUiBinder uiBinder = GWT.create(GuidedDecisionTableModellerViewImplUiBinder.class);

    private final RuleSelector ruleSelector = new RuleSelector();

    private final GuidedDecisionTableModellerBoundsHelper boundsHelper = new GuidedDecisionTableModellerBoundsHelper();

    @UiField
    FlowPanel accordionContainer;

    @UiField
    Button addColumn;

    @UiField
    Button editColumns;

    @UiField
    Icon pinnedModeIndicator;

    @UiField(provided = true)
    GridLienzoPanel gridPanel = new GridLienzoPanel() {

        @Override
        public void onResize() {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    updatePanelSize();
                    refreshScrollPosition();

                    final TransformMediator restriction = mousePanMediator.getTransformMediator();
                    final Transform transform = restriction.adjust(gridLayer.getViewport().getTransform(),
                                                                   gridLayer.getVisibleBounds());
                    gridLayer.getViewport().setTransform(transform);
                    gridLayer.draw();
                }
            });
        }
    };

    @Inject
    private GuidedDecisionTableAccordion guidedDecisionTableAccordion;

    private VerticalPanel attributeConfigWidget = makeDefaultPanel();

    private VerticalPanel metaDataConfigWidget = makeDefaultPanel();

    private VerticalPanel conditionsConfigWidget = makeDefaultPanel();

    private VerticalPanel actionsConfigWidget = makeDefaultPanel();

    private GuidedDecisionTableAccordion accordion;

    private TransformMediator defaultTransformMediator;

    private GuidedDecisionTableModellerView.Presenter presenter;

    private final DefaultGridLayer gridLayer = defaultGridLayer();

    private final RestrictedMousePanMediator mousePanMediator = restrictedMousePanMediator();

    @Inject
    private ColumnManagementView actionsPanel;

    @Inject
    private ColumnManagementView conditionsPanel;

    @Inject
    private ManagedInstance<AttributeColumnConfigRow> attributeColumnConfigRows;

    @Inject
    private ManagedInstance<DeleteColumnManagementAnchorWidget> deleteColumnManagementAnchorWidgets;

    public GuidedDecisionTableModellerViewImpl() {
        initWidget(uiBinder.createAndBindUi(this));
        pinnedModeIndicator.setSize(IconSize.LARGE);
    }

    DefaultGridLayer defaultGridLayer() {
        return new DefaultGridLayer() {
            @Override
            public void enterPinnedMode(final GridWidget gridWidget,
                                        final Command onStartCommand) {
                super.enterPinnedMode(gridWidget,
                                      new Command() {
                                          @Override
                                          public void execute() {
                                              onStartCommand.execute();
                                              presenter.onViewPinned(true);
                                          }
                                      });
            }

            @Override
            public void exitPinnedMode(final Command onCompleteCommand) {
                super.exitPinnedMode(new Command() {
                    @Override
                    public void execute() {
                        onCompleteCommand.execute();
                        presenter.onViewPinned(false);
                    }
                });
            }

            @Override
            public TransformMediator getDefaultTransformMediator() {
                return defaultTransformMediator;
            }
        };
    }

    RestrictedMousePanMediator restrictedMousePanMediator() {
        return new RestrictedMousePanMediator(gridLayer) {
            @Override
            protected void onMouseMove(final NodeMouseMoveEvent event) {
                super.onMouseMove(event);
                presenter.updateRadar();
            }
        };
    }

    protected void initWidget(final Widget widget) {
        super.initWidget(widget);
    }

    @Override
    public void init(final GuidedDecisionTableModellerView.Presenter presenter) {
        this.presenter = presenter;

        getActionsPanel().init(presenter);
        getConditionsPanel().init(presenter);
        setupAccordion(presenter);
    }

    @PostConstruct
    public void setup() {
        setupSubMenu();
        setupGridPanel();
    }

    void setupGridPanel() {
        //Lienzo stuff - Set default scale
        final Transform transform = newTransform().scale(VP_SCALE);
        gridPanel.getViewport().setTransform(transform);

        //Lienzo stuff - Add mouse pan support
        defaultTransformMediator = new BoundaryTransformMediator(GuidedDecisionTableModellerViewImpl.this);
        mousePanMediator.setTransformMediator(defaultTransformMediator);
        gridPanel.getViewport().getMediators().push(mousePanMediator);
        mousePanMediator.setBatchDraw(true);

        gridPanel.setBounds(getBounds());
        gridPanel.getScrollPanel().addDomHandler(scrollEvent -> getPresenter().updateRadar(),
                                                 ScrollEvent.getType());

        //Wire-up widgets
        gridPanel.add(gridLayer);

        //Set ID on GridLienzoPanel for Selenium tests.
        gridPanel.getElement().setId("dtable_container_" + Document.get().createUniqueId());
    }

    void setupSubMenu() {
        disableColumnOperationsMenu();
        getAddColumn().addClickHandler((e) -> addColumn());
        getEditColumns().addClickHandler((e) -> editColumns());
    }

    void addColumn() {
        if (getPresenter().isColumnCreationEnabledToActiveDecisionTable()) {
            getPresenter().openNewGuidedDecisionTableColumnWizard();
        }
    }

    void editColumns() {
        if (getPresenter().isColumnCreationEnabledToActiveDecisionTable()) {
            toggleClassName(getAccordionContainer(), GuidedDecisionTableResources.INSTANCE.css().openedAccordion());
            toggleClassName(getEditColumns(), "active");
        }
    }

    void toggleClassName(final Widget widget,
                         final String className) {
        widget.getElement().toggleClassName(className);
    }

    @Override
    public void onResize() {
        gridPanel.onResize();
        getPresenter().updateRadar();
    }

    @Override
    public HandlerRegistration addKeyDownHandler(final KeyDownHandler handler) {
        return gridPanel.addKeyDownHandler(handler);
    }

    @Override
    public void enableColumnOperationsMenu() {
        getAddColumn().setEnabled(true);
        getEditColumns().setEnabled(true);
    }

    @Override
    public void disableColumnOperationsMenu() {
        getAddColumn().setEnabled(false);
        getEditColumns().setEnabled(false);
    }

    @Override
    public HandlerRegistration addContextMenuHandler(final ContextMenuHandler handler) {
        return gridPanel.addDomHandler(handler,
                                       ContextMenuEvent.getType());
    }

    @Override
    public HandlerRegistration addMouseDownHandler(final MouseDownHandler handler) {
        return rootPanel().addDomHandler(handler,
                                         MouseDownEvent.getType());
    }

    RootPanel rootPanel() {
        return RootPanel.get();
    }

    Widget ruleInheritanceWidget() {
        final FlowPanel result = makeFlowPanel();

        result.setStyleName(GuidedDecisionTableResources.INSTANCE.css().ruleInheritance());
        result.add(ruleInheritanceLabel());
        result.add(ruleSelector());

        return result;
    }

    FlowPanel makeFlowPanel() {
        return new FlowPanel();
    }

    Widget ruleSelector() {
        getRuleSelector().setEnabled(false);

        getRuleSelector().addValueChangeHandler(e -> {
            presenter.getActiveDecisionTable().setParentRuleName(e.getValue());
        });

        return getRuleSelector();
    }

    Label ruleInheritanceLabel() {
        final Label label = new Label(GuidedDecisionTableConstants.INSTANCE.AllTheRulesInherit());

        label.setStyleName(GuidedDecisionTableResources.INSTANCE.css().ruleInheritanceLabel());

        return label;
    }

    @Override
    public void clear() {
        gridLayer.removeAll();
    }

    @Override
    public void addDecisionTable(final GuidedDecisionTableView gridWidget) {
        //Ensure the first Decision Table is visible
        if (gridLayer.getGridWidgets().isEmpty()) {
            final Point2D translation = getTranslation(gridWidget);
            final Transform t = gridLayer.getViewport().getTransform();
            t.translate(translation.getX(),
                        translation.getY());
        }
        gridLayer.add(gridWidget);
        gridLayer.batch();
    }

    private Point2D getTranslation(final GuidedDecisionTableView gridWidget) {
        final double boundsPadding = GuidedDecisionTableModellerBoundsHelper.BOUNDS_PADDING;
        final Transform t = gridLayer.getViewport().getTransform();
        final double requiredTranslateX = boundsPadding - gridWidget.getX();
        final double requiredTranslateY = boundsPadding - gridWidget.getY();
        final double actualTranslateX = t.getTranslateX();
        final double actualTranslateY = t.getTranslateY();
        final double dx = requiredTranslateX - actualTranslateX;
        final double dy = requiredTranslateY - actualTranslateY;
        return new Point2D(dx,
                           dy);
    }

    @Override
    public void removeDecisionTable(final GuidedDecisionTableView gridWidget,
                                    final Command afterRemovalCommand) {
        if (gridWidget == null) {
            return;
        }
        final Command remove = () -> {
            gridLayer.remove(gridWidget);

            afterRemovalCommand.execute();
            disableColumnOperationsMenu();

            gridLayer.batch();
        };
        if (gridLayer.isGridPinned()) {
            final GridPinnedModeManager.PinnedContext context = gridLayer.getPinnedContext();
            if (gridWidget.equals(context.getGridWidget())) {
                gridLayer.exitPinnedMode(remove);
            }
        } else {
            remove.execute();
        }
    }

    @Override
    public void setEnableColumnCreation(final boolean enabled) {
        addColumn.setEnabled(enabled);
    }

    @Override
    public void refreshRuleInheritance(final String selectedParentRuleName,
                                       final Collection<String> availableParentRuleNames) {
        ruleSelector.setRuleName(selectedParentRuleName);
        ruleSelector.setRuleNames(availableParentRuleNames);
    }

    @Override
    public void refreshScrollPosition() {
        gridPanel.refreshScrollPosition();
    }

    private VerticalPanel makeDefaultPanel() {
        return new VerticalPanel() {{
            add(blankSlate());
        }};
    }

    Label blankSlate() {
        final String disabledLabelStyle = "text-muted";
        final String noColumns = GuidedDecisionTableConstants.INSTANCE.NoColumnsAvailable();

        return new Label() {{
            setText(noColumns);
            setStyleName(disabledLabelStyle);
        }};
    }

    @Override
    public void refreshAttributeWidget(final List<AttributeCol52> attributeColumns) {
        getAttributeConfigWidget().clear();

        if (attributeColumns == null || attributeColumns.isEmpty()) {
            getAccordion().getItem(GuidedDecisionTableAccordionItem.Type.ATTRIBUTE).setOpen(false);
            getAttributeConfigWidget().add(blankSlate());
            return;
        }

        for (AttributeCol52 attributeColumn : attributeColumns) {
            AttributeColumnConfigRow attributeColumnConfigRow = getAttributeColumnConfigRows().get();
            attributeColumnConfigRow.init(attributeColumn,
                                          getPresenter());
            getAttributeConfigWidget().add(attributeColumnConfigRow.getView());
        }
    }

    @Override
    public void refreshMetaDataWidget(final List<MetadataCol52> metaDataColumns) {
        metaDataConfigWidget.clear();

        if (metaDataColumns == null || metaDataColumns.isEmpty()) {
            accordion.getItem(GuidedDecisionTableAccordionItem.Type.METADATA).setOpen(false);
            metaDataConfigWidget.add(blankSlate());
            return;
        }

        final boolean isEditable = presenter.isActiveDecisionTableEditable();
        for (MetadataCol52 metaDataColumn : metaDataColumns) {
            HorizontalPanel hp = new HorizontalPanel();
            hp.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

            final ColumnLabelWidget label = makeColumnLabel(metaDataColumn);
            hp.add(label);

            final MetadataCol52 originalColumn = metaDataColumn;
            final CheckBox chkHideColumn = new CheckBox(new StringBuilder(GuidedDecisionTableConstants.INSTANCE.HideThisColumn()).append(GuidedDecisionTableConstants.COLON).toString());
            chkHideColumn.setValue(metaDataColumn.isHideColumn());
            chkHideColumn.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(final ClickEvent event) {
                    final MetadataCol52 editedColumn = originalColumn.cloneColumn();
                    editedColumn.setHideColumn(chkHideColumn.getValue());
                    presenter.getActiveDecisionTable().updateColumn(originalColumn,
                                                                    editedColumn);
                }
            });

            hp.add(chkHideColumn);

            if (isEditable) {
                final DeleteColumnManagementAnchorWidget deleteWidget = deleteColumnManagementAnchorWidgets.get();
                deleteWidget.init(metaDataColumn.getMetadata(),
                                  () -> presenter.getActiveDecisionTable().deleteColumn(metaDataColumn));
                hp.add(deleteWidget);
            }

            metaDataConfigWidget.add(hp);
        }
    }

    private ColumnLabelWidget makeColumnLabel(final MetadataCol52 metaDataColumn) {
        final ColumnLabelWidget label = new ColumnLabelWidget(metaDataColumn.getMetadata());
        ColumnUtilities.setColumnLabelStyleWhenHidden(label,
                                                      metaDataColumn.isHideColumn());
        return label;
    }

    @Override
    public void refreshConditionsWidget(final List<CompositeColumn<? extends BaseColumn>> conditionColumns) {
        getConditionsConfigWidget().clear();

        if (conditionColumns == null || conditionColumns.isEmpty()) {
            getAccordion().getItem(GuidedDecisionTableAccordionItem.Type.CONDITION).setOpen(false);
            getConditionsConfigWidget().add(blankSlate());
            return;
        }

        getConditionsConfigWidget().add(getConditionsPanel());

        final Map<String, List<BaseColumn>> columnGroups =
                conditionColumns.stream().collect(
                        Collectors.groupingBy(
                                DecisionTableColumnViewUtils::getColumnManagementGroupTitle,
                                Collectors.toList()
                        )
                );
        getConditionsPanel().renderColumns(columnGroups);
    }

    @Override
    public void refreshActionsWidget(final List<ActionCol52> actionColumns) {
        getActionsConfigWidget().clear();

        if (actionColumns == null || actionColumns.isEmpty()) {
            getAccordion().getItem(GuidedDecisionTableAccordionItem.Type.ACTION).setOpen(false);
            getActionsConfigWidget().add(blankSlate());
            return;
        }

        //Each Action is a row in a vertical panel
        getActionsConfigWidget().add(getActionsPanel());

        //Add Actions to panel
        final Map<String, List<BaseColumn>> columnGroups =
                actionColumns.stream().collect(
                        Collectors.groupingBy(
                                DecisionTableColumnViewUtils::getColumnManagementGroupTitle,
                                Collectors.toList()
                        )
                );
        getActionsPanel().renderColumns(columnGroups);
    }

    @Override
    public void refreshColumnsNote(final boolean hasColumnDefinitions) {
        getGuidedDecisionTableAccordion().setColumnsNoteInfoHidden(hasColumnDefinitions);
    }

    @Override
    public void setZoom(final int zoom) {
        //Set zoom preserving translation
        final Transform transform = newTransform();
        final double tx = gridPanel.getViewport().getTransform().getTranslateX();
        final double ty = gridPanel.getViewport().getTransform().getTranslateY();
        transform.translate(tx,
                            ty);
        transform.scale(zoom / 100.0);

        //Ensure the change in zoom keeps the view in bounds. IGridLayer's visibleBounds depends
        //on the Viewport Transformation; so set it to the "proposed" transformation before checking.
        gridPanel.getViewport().setTransform(transform);
        final TransformMediator restriction = mousePanMediator.getTransformMediator();
        final Transform newTransform = restriction.adjust(transform,
                                                          gridLayer.getVisibleBounds());
        gridPanel.getViewport().setTransform(newTransform);
        gridPanel.getViewport().batch();
        gridPanel.refreshScrollPosition();
    }

    @Override
    public void onInsertColumn() {
        addColumn();
    }

    @Override
    public GridLayer getGridLayerView() {
        return gridLayer;
    }

    @Override
    public GridLienzoPanel getGridPanel() {
        return gridPanel;
    }

    @Override
    public Bounds getBounds() {
        if (presenter == null) {
            return boundsHelper.getBounds(Collections.emptySet());
        } else {
            return boundsHelper.getBounds(presenter.getAvailableDecisionTables());
        }
    }

    @Override
    public void select(final GridWidget selectedGridWidget) {
        getRuleSelector().setEnabled(true);
        getGridLayer().select(selectedGridWidget);
    }

    @Override
    public void selectLinkedColumn(final GridColumn<?> link) {
        gridLayer.selectLinkedColumn(link);
    }

    @Override
    public Set<GridWidget> getGridWidgets() {
        return gridLayer.getGridWidgets();
    }

    @Override
    public void setPinnedModeIndicatorVisibility(final boolean visibility) {
        pinnedModeIndicator.setVisible(visibility);
    }

    GuidedDecisionTableAccordion getGuidedDecisionTableAccordion() {
        return guidedDecisionTableAccordion;
    }

    void setupAccordion(final Presenter presenter) {
        accordion = makeAccordion(presenter);

        final Widget widget = asWidget(accordion);

        getAccordionContainer().add(widget);
        getAccordionContainer().add(ruleInheritanceWidget());
    }

    FlowPanel getAccordionContainer() {
        return accordionContainer;
    }

    Widget asWidget(final GuidedDecisionTableAccordion accordion) {
        final GuidedDecisionTableAccordion.View accordionView = accordion.getView();

        return ElementWrapperWidget.getWidget(accordionView.getElement());
    }

    GuidedDecisionTableAccordion makeAccordion(final Presenter presenter) {
        final GuidedDecisionTableAccordion accordion = getGuidedDecisionTableAccordion();

        accordion.addItem(GuidedDecisionTableAccordionItem.Type.ATTRIBUTE,
                          getAttributeConfigWidget());
        accordion.addItem(GuidedDecisionTableAccordionItem.Type.METADATA,
                          getMetaDataConfigWidget());
        accordion.addItem(GuidedDecisionTableAccordionItem.Type.CONDITION,
                          getConditionsConfigWidget());
        accordion.addItem(GuidedDecisionTableAccordionItem.Type.ACTION,
                          getActionsConfigWidget());

        return accordion;
    }

    VerticalPanel getAttributeConfigWidget() {
        return attributeConfigWidget;
    }

    VerticalPanel getMetaDataConfigWidget() {
        return metaDataConfigWidget;
    }

    VerticalPanel getConditionsConfigWidget() {
        return conditionsConfigWidget;
    }

    VerticalPanel getActionsConfigWidget() {
        return actionsConfigWidget;
    }

    RuleSelector getRuleSelector() {
        return ruleSelector;
    }

    Button getAddColumn() {
        return addColumn;
    }

    Button getEditColumns() {
        return editColumns;
    }

    Presenter getPresenter() {
        return presenter;
    }

    GuidedDecisionTableAccordion getAccordion() {
        return accordion;
    }

    DefaultGridLayer getGridLayer() {
        return gridLayer;
    }

    Transform newTransform() {
        return new Transform();
    }

    ColumnManagementView getActionsPanel() {
        return actionsPanel;
    }

    ColumnManagementView getConditionsPanel() {
        return conditionsPanel;
    }

    ManagedInstance<AttributeColumnConfigRow> getAttributeColumnConfigRows() {
        return attributeColumnConfigRows;
    }

    interface GuidedDecisionTableModellerViewImplUiBinder extends UiBinder<Widget, GuidedDecisionTableModellerViewImpl> {

    }
}