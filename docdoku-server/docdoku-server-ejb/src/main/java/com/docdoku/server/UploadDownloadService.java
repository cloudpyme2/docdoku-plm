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
package com.docdoku.server;

import com.docdoku.core.services.*;
import com.docdoku.core.document.DocumentIterationKey;
import com.docdoku.core.document.DocumentMasterTemplateKey;
import com.docdoku.core.product.PartIterationKey;
import com.docdoku.core.product.PartMasterKey;
import com.docdoku.core.product.PartRevisionKey;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.ws.soap.MTOM;

/**
 *
 * @author Florent Garin
 */
@MTOM
@Local(IUploadDownloadWS.class)
@Stateless(name = "UploadDownloadService")
@WebService(serviceName = "UploadDownloadService", endpointInterface = "com.docdoku.core.services.IUploadDownloadWS")
public class UploadDownloadService implements IUploadDownloadWS {

    @EJB
    private IDocumentManagerLocal documentService;

    @EJB
    private IProductManagerLocal productService;

    @EJB
    private IConverterManagerLocal converterService;
    
    @RolesAllowed("users")
    @XmlMimeType("application/octet-stream")
    @Override
    public DataHandler downloadFromDocument(String workspaceId, String docMId, String docMVersion, int iteration, String fileName) throws NotAllowedException, FileNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
        String fullName = workspaceId + "/documents/" + docMId + "/" + docMVersion + "/" + iteration + "/" + fileName;
        File dataFile = documentService.getDataFile(fullName);

        return new DataHandler(new FileDataSource(dataFile));
    }

    @RolesAllowed("users")
    @XmlMimeType("application/octet-stream")
    @Override
    public DataHandler downloadNativeFromPart(String workspaceId, String partMNumber, String partRVersion, int iteration, String fileName) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, FileNotFoundException, NotAllowedException {
        String fullName = workspaceId + "/parts/" + partMNumber + "/" + partRVersion + "/" + iteration + "/nativecad/" + fileName;
        File dataFile = productService.getDataFile(fullName);

        return new DataHandler(new FileDataSource(dataFile));
    }

    @RolesAllowed("users")
    @XmlMimeType("application/octet-stream")
    @Override
    public DataHandler downloadFromPart(String workspaceId, String partMNumber, String partRVersion, int iteration, String fileName) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, FileNotFoundException, NotAllowedException {
        String fullName = workspaceId + "/parts/" + partMNumber + "/" + partRVersion + "/" + iteration + "/" + fileName;
        File dataFile = productService.getDataFile(fullName);

        return new DataHandler(new FileDataSource(dataFile));
    }
    
    @RolesAllowed("users")
    @XmlMimeType("application/octet-stream")
    @Override
    public DataHandler downloadFromTemplate(String workspaceId, String templateID, String fileName) throws NotAllowedException, FileNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
        String fullName = workspaceId + "/templates/" + templateID + "/" + fileName;
        File dataFile = documentService.getDataFile(fullName);

        return new DataHandler(new FileDataSource(dataFile));
    }

    @RolesAllowed("users")
    @Override
    public void uploadToDocument(String workspaceId, String docMId, String docMVersion, int iteration, String fileName,
            @XmlMimeType("application/octet-stream") DataHandler data) throws IOException, CreationException, WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException {
        DocumentIterationKey docPK = null;
        File vaultFile = null;

        docPK = new DocumentIterationKey(workspaceId, docMId, docMVersion, iteration);

        vaultFile = documentService.saveFileInDocument(docPK, fileName, 0);

        vaultFile.getParentFile().mkdirs();
        vaultFile.createNewFile();
        OutputStream outStream = new BufferedOutputStream(new FileOutputStream(vaultFile));
        data.writeTo(outStream);
        outStream.close();
        //StreamingDataHandler dh = (StreamingDataHandler) data;
        //dh.moveTo(vaultFile);
        //dh.close();
        documentService.saveFileInDocument(docPK, fileName, vaultFile.length());
    }
    
    @RolesAllowed("users")
    @Override
    public void uploadGeometryToPart(String workspaceId, String partMNumber, String partRVersion, int iteration, String fileName, int quality,
            @XmlMimeType("application/octet-stream") DataHandler data) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, NotAllowedException, PartRevisionNotFoundException, FileAlreadyExistsException, CreationException, IOException {
        PartIterationKey partIPK = null;
        File vaultFile = null;

        partIPK = new PartIterationKey(new PartRevisionKey(new PartMasterKey(workspaceId, partMNumber), partRVersion), iteration);
        vaultFile = productService.saveGeometryInPartIteration(partIPK, fileName, quality, 0);

        vaultFile.getParentFile().mkdirs();
        vaultFile.createNewFile();
        OutputStream outStream = new BufferedOutputStream(new FileOutputStream(vaultFile));
        data.writeTo(outStream);
        outStream.close();
        productService.saveGeometryInPartIteration(partIPK, fileName, quality, vaultFile.length());
    }


    @RolesAllowed("users")
    @Override
    public void uploadNativeCADToPart(String workspaceId, String partMNumber, String partRVersion, int iteration, String fileName,
            @XmlMimeType("application/octet-stream") DataHandler data) throws Exception {
        PartIterationKey partIPK = null;
        File vaultFile = null;

        partIPK = new PartIterationKey(workspaceId, partMNumber, partRVersion, iteration);
        vaultFile = productService.saveNativeCADInPartIteration(partIPK, fileName, 0);

        vaultFile.getParentFile().mkdirs();
        vaultFile.createNewFile();
        OutputStream outStream = new BufferedOutputStream(new FileOutputStream(vaultFile));
        data.writeTo(outStream);
        outStream.close();
        productService.saveNativeCADInPartIteration(partIPK, fileName, vaultFile.length());
        converterService.convertCADFileToJSON(partIPK, vaultFile);
    }

    @RolesAllowed("users")
    @Override
    public void uploadToPart(String workspaceId, String partMNumber, String partRVersion, int iteration, String fileName,
            @XmlMimeType("application/octet-stream") DataHandler data) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, NotAllowedException, PartRevisionNotFoundException, FileAlreadyExistsException, CreationException, IOException {
        PartIterationKey partIPK = null;
        File vaultFile = null;

        partIPK = new PartIterationKey(new PartRevisionKey(new PartMasterKey(workspaceId, partMNumber), partRVersion), iteration);
        vaultFile = productService.saveFileInPartIteration(partIPK, fileName, 0);

        vaultFile.getParentFile().mkdirs();
        vaultFile.createNewFile();
        OutputStream outStream = new BufferedOutputStream(new FileOutputStream(vaultFile));
        data.writeTo(outStream);
        outStream.close();
        productService.saveFileInPartIteration(partIPK, fileName, vaultFile.length());
    }    

    @RolesAllowed("users")
    @Override
    public void uploadToTemplate(String workspaceId, String templateID, String fileName,
            @XmlMimeType("application/octet-stream") DataHandler data) throws IOException, CreationException, WorkspaceNotFoundException, NotAllowedException, DocumentMasterTemplateNotFoundException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException {
        DocumentMasterTemplateKey templatePK = null;
        File vaultFile = null;

        templatePK = new DocumentMasterTemplateKey(workspaceId, templateID);
        vaultFile = documentService.saveFileInTemplate(templatePK, fileName, 0);
        vaultFile.getParentFile().mkdirs();
        vaultFile.createNewFile();
        OutputStream outStream = new BufferedOutputStream(new FileOutputStream(vaultFile));
        data.writeTo(outStream);
        outStream.close();

        //StreamingDataHandler dh = (StreamingDataHandler) data;
        //dh.moveTo(vaultFile);
        //dh.close();
        documentService.saveFileInTemplate(templatePK, fileName, vaultFile.length());
    }
}
