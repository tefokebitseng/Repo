/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.ctidb.provider;

import org.dspace.external.model.ExternalDataObject;
import org.dspace.importer.external.ctidb.suggestion.AbstractCtiSuggestionLoader;
import org.dspace.importer.external.ctidb.suggestion.CtiPatentSuggestionLoader;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is the implementation of the ExternalDataProvider interface that
 * will deal with the CtiPatent External Data lookup
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 */
public class CtiPatentDataProvider extends AbstractCtiDataProvider {

//    private static final Logger log = LogManager.getLogger(CtiPublicationDataProvider.class);

    @Autowired
    private CtiPatentSuggestionLoader suggestionLoader;

    @Override
    public AbstractCtiSuggestionLoader getCtiSuggestionLoader() {
        return suggestionLoader;
    }

    @Override
    protected ExternalDataObject getExternalDataObjectImpl(String ctiId) {
        return getCtiDatabaseImportFacade().getCtiPatent(ctiId);
    }

}