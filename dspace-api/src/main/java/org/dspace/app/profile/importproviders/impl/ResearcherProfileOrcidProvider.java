/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.profile.importproviders.impl;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.dspace.app.profile.importproviders.ResearcherProfileProvider;
import org.dspace.app.profile.importproviders.model.ConfiguredResearcherProfileProvider;
import org.dspace.app.profile.importproviders.model.ResearcherProfileSource;
import org.dspace.content.MetadataValue;
import org.dspace.eperson.EPerson;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.impl.OrcidV3AuthorDataProvider;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class ResearcherProfileOrcidProvider implements ResearcherProfileProvider {

    @Autowired
    private OrcidV3AuthorDataProvider orcidV3AuthorDataProvider;

    public Optional<ConfiguredResearcherProfileProvider> configureProvider(EPerson eperson, List<URI> uriList) {
        Optional<MetadataValue> metadataIdentifier = getMetadataIdentifier(eperson);
        if (metadataIdentifier.isPresent()) {
            ConfiguredResearcherProfileProvider configured = new ConfiguredResearcherProfileProvider(
                    new ResearcherProfileSource(metadataIdentifier.get().getValue()), this);
            return Optional.of(configured);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(ResearcherProfileSource source) {
        return orcidV3AuthorDataProvider.getExternalDataObject(source.getId());
    }

    private Optional<MetadataValue> getMetadataIdentifier(EPerson eperson) {
        return eperson.getMetadata().stream().filter(metadata -> {
            return "perucris".equals(metadata.getMetadataField().getMetadataSchema().getName()) &&
                    "eperson".equals(metadata.getMetadataField().getElement()) &&
                    "orcid".equals(metadata.getMetadataField().getQualifier());
        }).findFirst();
    }

    public void setOrcidV3AuthorDataProvider(OrcidV3AuthorDataProvider orcidV3AuthorDataProvider) {
        this.orcidV3AuthorDataProvider = orcidV3AuthorDataProvider;
    }

}
