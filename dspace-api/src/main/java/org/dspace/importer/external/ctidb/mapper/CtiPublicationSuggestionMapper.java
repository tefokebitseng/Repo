/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.mapper;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.importer.external.ctidb.model.CtiProduccionBibliografica;

/**
 * Map CtiProduccionBibliografica to MetadataValueDTOs suitable for the Suggestion Model.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class CtiPublicationSuggestionMapper extends AbstractCtiMapper {

    public List<MetadataValueDTO> mapCtiPublicationSuggestion(CtiProduccionBibliografica entity) {

        List<MetadataValueDTO> metadata = new ArrayList<MetadataValueDTO>();

        metadata.add(getCtiIdentifier(entity));

        metadata.add(new MetadataValueDTO("dc", "title", null, null, entity.getTitulo().trim()));

        if (entity.getFechaProduccion() != null) {
            metadata.add(new MetadataValueDTO("dc", "date", "issued", null, mapDate(entity.getFechaProduccion())));
        }

        return metadata;

    }

}