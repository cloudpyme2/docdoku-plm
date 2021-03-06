/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2013 DocDoku SARL
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

import com.docdoku.core.product.Geometry;
import com.docdoku.core.product.PartMasterTemplate;
import com.docdoku.core.services.FileNotFoundException;
import com.docdoku.core.services.FileAlreadyExistsException;
import com.docdoku.core.services.CreationException;
import com.docdoku.core.common.BinaryResource;
import com.docdoku.core.document.DocumentIteration;
import com.docdoku.core.document.DocumentMasterTemplate;
import com.docdoku.core.product.PartIteration;
import java.util.*;
import javax.persistence.*;

public class BinaryResourceDAO {

    private EntityManager em;
    private Locale mLocale;

    public BinaryResourceDAO(Locale pLocale, EntityManager pEM) {
        em = pEM;
        mLocale = pLocale;
    }

    public BinaryResourceDAO(EntityManager pEM) {
        em = pEM;
        mLocale = Locale.getDefault();
    }

    public void createBinaryResource(BinaryResource pBinaryResource) throws FileAlreadyExistsException, CreationException {
        try {
            //the EntityExistsException is thrown only when flush occurs    
            em.persist(pBinaryResource);
            em.flush();
        } catch (EntityExistsException pEEEx) {
            throw new FileAlreadyExistsException(mLocale, pBinaryResource);
        } catch (PersistenceException pPEx) {
            //EntityExistsException is case sensitive
            //whereas MySQL is not thus PersistenceException could be
            //thrown instead of EntityExistsException
            throw new CreationException(mLocale);
        }
    }

    public void removeBinaryResource(String pFullName) throws FileNotFoundException {
        BinaryResource file = loadBinaryResource(pFullName);
        em.remove(file);
    }

    public void removeBinaryResource(BinaryResource pBinaryResource) {
        em.remove(pBinaryResource);
    }

    public BinaryResource loadBinaryResource(String pFullName) throws FileNotFoundException {
        BinaryResource file = em.find(BinaryResource.class, pFullName);
        if (file == null) {
            throw new FileNotFoundException(mLocale, pFullName);
        } else {
            return file;
        }
    }

    public PartIteration getPartOwner(BinaryResource pBinaryResource) {
        TypedQuery<PartIteration> query;
        if(pBinaryResource instanceof Geometry){
            query = em.createQuery("SELECT p FROM PartIteration p WHERE :binaryResource MEMBER OF p.geometries", PartIteration.class);
        }else if(pBinaryResource.isNativeCADFile()){
            query = em.createQuery("SELECT p FROM PartIteration p WHERE p.nativeCADFile = :binaryResource", PartIteration.class);
        }else{
            query = em.createQuery("SELECT p FROM PartIteration p WHERE :binaryResource MEMBER OF p.attachedFiles", PartIteration.class);
        }
        try {
            return query.setParameter("binaryResource", pBinaryResource).getSingleResult();
        } catch (NoResultException pNREx) {
            return null;
        }
    }
    
    public DocumentIteration getDocumentOwner(BinaryResource pBinaryResource) {
        TypedQuery<DocumentIteration> query = em.createQuery("SELECT d FROM DocumentIteration d WHERE :binaryResource MEMBER OF d.attachedFiles", DocumentIteration.class);
        try {
            return query.setParameter("binaryResource", pBinaryResource).getSingleResult();
        } catch (NoResultException pNREx) {
            return null;
        }
    }

    public DocumentMasterTemplate getDocumentTemplateOwner(BinaryResource pBinaryResource) {
        TypedQuery<DocumentMasterTemplate> query = em.createQuery("SELECT t FROM DocumentMasterTemplate t WHERE :binaryResource MEMBER OF t.attachedFiles", DocumentMasterTemplate.class);
        try {
            return query.setParameter("binaryResource", pBinaryResource).getSingleResult();
        } catch (NoResultException pNREx) {
            return null;
        }
    }

    public PartMasterTemplate getPartTemplateOwner(BinaryResource pBinaryResource) {
        TypedQuery<PartMasterTemplate> query = em.createQuery("SELECT t FROM PartMasterTemplate t WHERE t.attachedFile = :binaryResource", PartMasterTemplate.class);
        try {
            return query.setParameter("binaryResource", pBinaryResource).getSingleResult();
        } catch (NoResultException pNREx) {
            return null;
        }
    }
}
