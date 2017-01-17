/*
 * Copyright 2014 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.guided.dtable.backend.server.indexing;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.search.Query;
import org.drools.workbench.models.datamodel.imports.Import;
import org.drools.workbench.models.guided.dtable.backend.GuidedDTXMLPersistence;
import org.drools.workbench.models.guided.dtable.shared.model.GuidedDecisionTable52;
import org.drools.workbench.screens.guided.dtable.type.GuidedDTableResourceTypeDefinition;
import org.junit.Test;
import org.kie.workbench.common.services.refactoring.backend.server.BaseIndexingTest;
import org.kie.workbench.common.services.refactoring.backend.server.TestIndexer;
import org.kie.workbench.common.services.refactoring.backend.server.query.builder.SingleTermQueryBuilder;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueSharedPartIndexTerm;
import org.kie.workbench.common.services.refactoring.service.PartType;
import org.uberfire.ext.metadata.engine.Index;
import org.uberfire.java.nio.file.Path;

public class IndexGuidedDecisionTableAttributesTest extends BaseIndexingTest<GuidedDTableResourceTypeDefinition> {

    @Test
    public void testIndexGuidedDecisionTableAttributes() throws IOException, InterruptedException {
        //Add test files
        final Path path = basePath.resolve( "dtable1.gdst" );
        final GuidedDecisionTable52 model = GuidedDecisionTableFactory.makeTableWithAttributeCol( "org.drools.workbench.screens.guided.dtable.backend.server.indexing",
                                                                                                  new ArrayList<Import>() {{
                                                                                                      add( new Import( "org.drools.workbench.screens.guided.dtable.backend.server.indexing.classes.Applicant" ) );
                                                                                                  }},
                                                                                                  "dtable1" );
        final String xml = GuidedDTXMLPersistence.getInstance().marshal( model );
        ioService().write( path,
                           xml );

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index

        final Index index = getConfig().getIndexManager().get( org.uberfire.ext.metadata.io.KObjectUtil.toKCluster( basePath.getFileSystem() ) );

        //Decision Table defining a RuleFlow-Group named myRuleFlowGroup. This should match dtable1.gdst
        //This checks whether there is a Rule Attribute "ruleflow-group" and its Value is "myRuleflowGroup"
        {
            final Query query = new SingleTermQueryBuilder( new ValueSharedPartIndexTerm( "myRuleFlowGroup", PartType.RULEFLOW_GROUP ) )
                    .build();
            searchFor(index, query, 1, path);
        }
    }

    @Override
    protected TestIndexer getIndexer() {
        return new TestGuidedDecisionTableFileIndexer();
    }

    @Override
    protected GuidedDTableResourceTypeDefinition getResourceTypeDefinition() {
        return new GuidedDTableResourceTypeDefinition();
    }

    @Override
    protected String getRepositoryName() {
        return this.getClass().getSimpleName();
    }

}