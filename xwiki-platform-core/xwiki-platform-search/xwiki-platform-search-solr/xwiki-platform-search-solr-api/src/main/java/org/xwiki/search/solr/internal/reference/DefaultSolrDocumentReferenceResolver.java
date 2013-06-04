/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.search.solr.internal.reference;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.search.solr.internal.api.SolrIndexerException;

/**
 * Dispatch to the proper {@link SolrDocumentReferenceResolver}.
 * 
 * @version $Id$
 * @since 5.1M2
 */
@Component
@Singleton
public class DefaultSolrDocumentReferenceResolver implements SolrDocumentReferenceResolver
{
    /**
     * Used to find the {@link SolrDocumentReferenceResolver}.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public List<EntityReference> getReferences(EntityReference reference) throws SolrIndexerException
    {
        EntityType type = reference.getType();

        SolrDocumentReferenceResolver resolver;
        try {
            resolver =
                this.componentManager.getInstance(SolrDocumentReferenceResolver.class, type.name().toLowerCase());
        } catch (ComponentLookupException e) {
            throw new SolrIndexerException("Failed to get SolrDocumentReferenceResolver corresponding to entity type ["
                + type + "]", e);
        }

        return resolver.getReferences(reference);
    }
}
