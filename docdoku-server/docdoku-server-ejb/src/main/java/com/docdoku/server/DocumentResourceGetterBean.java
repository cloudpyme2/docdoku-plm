/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2014 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.docdoku.server;

import com.docdoku.core.common.BinaryResource;
import com.docdoku.core.common.User;
import com.docdoku.core.document.DocumentIteration;
import com.docdoku.core.security.UserGroupMapping;
import com.docdoku.core.services.IDocumentManagerLocal;
import com.docdoku.core.services.IDocumentResourceGetterManagerLocal;
import com.docdoku.core.services.IUserManagerLocal;
import com.docdoku.server.resourcegetters.DocumentResourceGetter;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.InputStream;
import java.util.Locale;


/**
 * Resource Getter
 */
@Stateless(name="DocumentResourceGetterBean")
public class DocumentResourceGetterBean implements IDocumentResourceGetterManagerLocal {

    @Inject
    @Any
    private Instance<DocumentResourceGetter> documentResourceGetters;
    @EJB
    private IDocumentManagerLocal documentService;
    @EJB
    private IUserManagerLocal userManager;

    @Override
    public InputStream getConvertedResource(String outputFormat, BinaryResource binaryResource) throws Exception {
        DocumentIteration docI;
        Locale locale;
        if(userManager.isCallerInRole(UserGroupMapping.REGULAR_USER_ROLE_ID)) {
            User user = userManager.whoAmI(binaryResource.getWorkspaceId());
            locale = new Locale(user.getLanguage());
        }else{
            locale = Locale.getDefault();
        }

        docI = documentService.findDocumentIterationByBinaryResource(binaryResource);


        DocumentResourceGetter selectedDocumentResourceGetter = null;
        for (DocumentResourceGetter documentResourceGetter : documentResourceGetters) {
            if (documentResourceGetter.canGetConvertedResource(outputFormat, binaryResource)) {
                selectedDocumentResourceGetter = documentResourceGetter;
                break;
            }
        }
        if (selectedDocumentResourceGetter != null) {
            return selectedDocumentResourceGetter.getConvertedResource(outputFormat, binaryResource,docI,locale);
        }

        return null;
    }

    @Override
    public String getSubResourceVirtualPath(BinaryResource binaryResource, String subResourceUri) {
        DocumentResourceGetter selectedDocumentResourceGetter = null;
        for (DocumentResourceGetter documentResourceGetter : documentResourceGetters) {
            if (documentResourceGetter.canGetSubResourceVirtualPath(binaryResource)) {
                selectedDocumentResourceGetter = documentResourceGetter;
                break;
            }
        }
        if (selectedDocumentResourceGetter != null) {
            return selectedDocumentResourceGetter.getSubResourceVirtualPath(binaryResource, subResourceUri);
        }
        return "";
    }

}
