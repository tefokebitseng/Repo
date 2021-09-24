/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.security.AccessItemMode;
import org.dspace.content.security.CrisSecurity;

/**
 * Implementation of {@link AccessItemMode} to configure the item withdrawn
 * modes.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class WithdrawItemMode implements AccessItemMode {

    /**
     * Defines the users enabled to use this withdrawn configuration
     */
    private CrisSecurity security;

    /**
     * Contains the list of groups metadata for CUSTOM security
     */
    private List<String> groups = new ArrayList<String>();

    /**
     * Contains the list of users metadata for CUSTOM security
     */
    private List<String> users = new ArrayList<String>();

    /**
     * Contains the list of items metadata for CUSTOM security
     */
    private List<String> items;

    @Override
    public CrisSecurity getSecurity() {
        return security;
    }

    @Override
    public List<String> getGroupMetadataFields() {
        return groups;
    }

    @Override
    public List<String> getUserMetadataFields() {
        return users;
    }

    @Override
    public List<String> getItemMetadataFields() {
        return items;
    }

    @Override
    public List<String> getGroups() {
        return groups;
    }

    public void setSecurity(CrisSecurity security) {
        this.security = security;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public void setItems(final List<String> items) {
        this.items = items;
    }
}
