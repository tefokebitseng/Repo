/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.vufind.metadatamapping;

import java.util.Map;
import javax.annotation.Resource;

import org.dspace.importer.external.metadatamapping.AbstractMetadataFieldMapping;

public class VuFindFieldMapping extends AbstractMetadataFieldMapping {

    /**
     * Defines which metadatum is mapped on which metadatum. Note that while the key must be unique it
     * only matters here for postprocessing of the value. The mapped MetadatumContributor has full control over
     * what metadatafield is generated.
     *
     * @param metadataFieldMap The map containing the link between retrieve metadata and metadata that will be set to
     *                         the item.
     */
    @SuppressWarnings("unchecked")
    @Override
    @Resource(name = "vufindMetadataFieldMap")
    public void setMetadataFieldMap(Map metadataFieldMap) {
        super.setMetadataFieldMap(metadataFieldMap);
    }

}
