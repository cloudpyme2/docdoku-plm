/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2015 DocDoku SARL
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
package com.docdoku.server.dao;

import com.docdoku.core.document.DocumentMasterTemplate;
import com.docdoku.core.document.DocumentMasterTemplateKey;
import com.docdoku.core.exceptions.CreationException;
import com.docdoku.core.exceptions.DocumentMasterTemplateAlreadyExistsException;
import com.docdoku.core.exceptions.DocumentMasterTemplateNotFoundException;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DocumentMasterTemplateDAO {

    private static final Logger LOGGER = Logger.getLogger(DocumentMasterTemplateDAO.class.getName());

    private EntityManager em;
    private Locale mLocale;

    public DocumentMasterTemplateDAO(Locale pLocale, EntityManager pEM) {
        em = pEM;
        mLocale = pLocale;
    }

    public DocumentMasterTemplateDAO(EntityManager pEM) {
        em = pEM;
        mLocale = Locale.getDefault();
    }

    public void updateDocMTemplate(DocumentMasterTemplate pTemplate) {
        em.merge(pTemplate);
    }

    public DocumentMasterTemplate removeDocMTemplate(DocumentMasterTemplateKey pKey) throws DocumentMasterTemplateNotFoundException {
        DocumentMasterTemplate template = loadDocMTemplate(pKey);
        em.remove(template);
        return template;
    }

    public List<DocumentMasterTemplate> findAllDocMTemplates(String pWorkspaceId) {
        DocumentMasterTemplate[] templates;
        Query query = em.createQuery("SELECT DISTINCT t FROM DocumentMasterTemplate t WHERE t.workspaceId = :workspaceId");
        return query.setParameter("workspaceId", pWorkspaceId).getResultList();
    }

    public DocumentMasterTemplate loadDocMTemplate(DocumentMasterTemplateKey pKey)
            throws DocumentMasterTemplateNotFoundException {
        DocumentMasterTemplate template = em.find(DocumentMasterTemplate.class, pKey);
        if (template == null) {
            throw new DocumentMasterTemplateNotFoundException(mLocale, pKey.getId());
        } else {
            return template;
        }
    }

    public void createDocMTemplate(DocumentMasterTemplate pTemplate) throws DocumentMasterTemplateAlreadyExistsException, CreationException {
        try {
            //the EntityExistsException is thrown only when flush occurs
            em.persist(pTemplate);
            em.flush();
        } catch (EntityExistsException pEEEx) {
            LOGGER.log(Level.FINER,null,pEEEx);
            throw new DocumentMasterTemplateAlreadyExistsException(mLocale, pTemplate);
        } catch (PersistenceException pPEx) {
            //EntityExistsException is case sensitive
            //whereas MySQL is not thus PersistenceException could be
            //thrown instead of EntityExistsException
            LOGGER.log(Level.WARNING,null,pPEx);
            throw new CreationException(mLocale);
        }
    }
}
