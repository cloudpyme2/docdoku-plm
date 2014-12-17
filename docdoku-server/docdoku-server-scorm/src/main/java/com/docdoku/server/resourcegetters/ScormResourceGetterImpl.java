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
package com.docdoku.server.resourcegetters;

import com.docdoku.core.common.BinaryResource;
import com.docdoku.core.document.DocumentIteration;
import com.docdoku.core.exceptions.StorageException;
import com.docdoku.core.services.IDataManagerLocal;
import com.docdoku.server.viewers.utils.ScormUtil;

import javax.ejb.EJB;
import java.io.InputStream;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScormResourceGetterImpl implements DocumentResourceGetter {

    @EJB
    private IDataManagerLocal dataManager;


    @Override
    public boolean canGetConvertedResource(String outputFormat, BinaryResource binaryResource) {
        return false;
    }

    @Override
    public InputStream getConvertedResource(String outputFormat, BinaryResource binaryResource, DocumentIteration docI, Locale locale) throws Exception {
        return null;
    }

    @Override
    public boolean canGetSubResourceVirtualPath(BinaryResource binaryResource) {
        try {
            return dataManager.exists(binaryResource, ScormUtil.getScormSubResourceVirtualPath(ScormUtil.IMS_MANIFEST));
        } catch (StorageException e) {
            Logger.getLogger(ScormResourceGetterImpl.class.getName()).log(Level.INFO, null, e);
            return false;
        }
    }

    @Override
    public String getSubResourceVirtualPath(BinaryResource binaryResource, String subResourceUri) {
        return ScormUtil.getScormSubResourceVirtualPath(subResourceUri);
    }

}
