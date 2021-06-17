/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model.factory.impl;

import static java.lang.String.format;

import java.util.List;

import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.app.orcid.model.factory.OrcidCommonObjectFactory;
import org.dspace.app.orcid.model.factory.OrcidProfileSectionFactory;
import org.dspace.app.orcid.service.MetadataSignatureGenerator;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class for that handle commons behaviors of all the available orcid
 * profile section factories.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public abstract class AbstractOrcidProfileSectionFactory implements OrcidProfileSectionFactory {

    protected final OrcidProfileSectionType sectionType;

    protected final OrcidProfileSyncPreference preference;

    @Autowired
    protected ItemService itemService;

    @Autowired
    protected OrcidCommonObjectFactory orcidCommonObjectFactory;

    @Autowired
    protected MetadataSignatureGenerator metadataSignatureGenerator;

    public AbstractOrcidProfileSectionFactory(OrcidProfileSectionType sectionType,
        OrcidProfileSyncPreference preference) {
        this.sectionType = sectionType;
        this.preference = preference;

        if (!getSupportedTypes().contains(sectionType)) {
            throw new IllegalArgumentException(format("The ORCID configuration does not support "
                + "the section type %s. Supported types are %s", sectionType, getSupportedTypes()));
        }
    }

    protected abstract List<OrcidProfileSectionType> getSupportedTypes();

    @Override
    public OrcidProfileSectionType getProfileSectionType() {
        return sectionType;
    }

    @Override
    public OrcidProfileSyncPreference getSynchronizationPreference() {
        return preference;
    }

    public ItemService getItemService() {
        return itemService;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public OrcidCommonObjectFactory getOrcidCommonObjectFactory() {
        return orcidCommonObjectFactory;
    }

    public void setOrcidCommonObjectFactory(OrcidCommonObjectFactory orcidCommonObjectFactory) {
        this.orcidCommonObjectFactory = orcidCommonObjectFactory;
    }

    protected List<MetadataValue> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

}
