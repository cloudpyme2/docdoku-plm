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
package com.docdoku.server.rest;

import com.docdoku.core.document.DocumentMasterTemplate;
import com.docdoku.core.document.DocumentMasterTemplateKey;
import com.docdoku.core.exceptions.*;
import com.docdoku.core.exceptions.NotAllowedException;
import com.docdoku.core.meta.InstanceAttributeTemplate;
import com.docdoku.core.security.UserGroupMapping;
import com.docdoku.core.services.IDocumentTemplateManagerLocal;
import com.docdoku.server.rest.dto.DocumentMasterTemplateDTO;
import com.docdoku.server.rest.dto.DocumentTemplateCreationDTO;
import com.docdoku.server.rest.dto.InstanceAttributeTemplateDTO;
import com.docdoku.server.rest.dto.TemplateGeneratedIdDTO;
import org.dozer.DozerBeanMapperSingletonWrapper;
import org.dozer.Mapper;

import javax.annotation.PostConstruct;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Yassine Belouad
 */
@Stateless
@DeclareRoles(UserGroupMapping.REGULAR_USER_ROLE_ID)
@RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
public class DocumentTemplateResource {

    @EJB
    private IDocumentTemplateManagerLocal documentTemplateService;
    private Mapper mapper;

    public DocumentTemplateResource() {
    }

    @PostConstruct
    public void init() {
        mapper = DozerBeanMapperSingletonWrapper.getInstance();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public DocumentMasterTemplateDTO[] getDocumentMasterTemplates(@PathParam("workspaceId") String workspaceId)
            throws EntityNotFoundException, UserNotActiveException {

        DocumentMasterTemplate[] docMsTemplates = documentTemplateService.getDocumentMasterTemplates(workspaceId);
        DocumentMasterTemplateDTO[] dtos = new DocumentMasterTemplateDTO[docMsTemplates.length];

        for (int i = 0; i < docMsTemplates.length; i++) {
            dtos[i] = mapper.map(docMsTemplates[i], DocumentMasterTemplateDTO.class);
        }

        return dtos;
    }

    @GET
    @Path("{templateId}")
    @Produces(MediaType.APPLICATION_JSON)
    public DocumentMasterTemplateDTO getDocumentMasterTemplates(@PathParam("workspaceId") String workspaceId, @PathParam("templateId") String templateId)
            throws EntityNotFoundException, UserNotActiveException {

        DocumentMasterTemplate docMsTemplate = documentTemplateService.getDocumentMasterTemplate(new DocumentMasterTemplateKey(workspaceId, templateId));
        return mapper.map(docMsTemplate, DocumentMasterTemplateDTO.class);
    }
    
    @GET
    @Path("{templateId}/generate_id")
    @Produces(MediaType.APPLICATION_JSON)
    public TemplateGeneratedIdDTO generateDocMsId(@PathParam("workspaceId") String workspaceId, @PathParam("templateId") String templateId)
            throws EntityNotFoundException, UserNotActiveException {

        String generateId = documentTemplateService.generateId(workspaceId, templateId);
        return new TemplateGeneratedIdDTO(generateId);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DocumentMasterTemplateDTO createDocumentMasterTemplate(@PathParam("workspaceId") String workspaceId, DocumentTemplateCreationDTO templateCreationDTO)
            throws EntityNotFoundException, EntityAlreadyExistsException, AccessRightException, NotAllowedException, CreationException {

        String id =templateCreationDTO.getReference();
        String documentType = templateCreationDTO.getDocumentType();
        String mask = templateCreationDTO.getMask();
        boolean idGenerated = templateCreationDTO.isIdGenerated();
        boolean attributesLocked = templateCreationDTO.isAttributesLocked();
        String workflowModelId = templateCreationDTO.getWorkflowModelId();
        boolean workflowLocked = templateCreationDTO.isWorkflowLocked();

        Set<InstanceAttributeTemplateDTO> attributeTemplates = templateCreationDTO.getAttributeTemplates();
        List<InstanceAttributeTemplateDTO> attributeTemplatesList = new ArrayList<>(attributeTemplates);
        InstanceAttributeTemplateDTO[] attributeTemplatesDtos = new InstanceAttributeTemplateDTO[attributeTemplatesList.size()];

        for (int i = 0; i < attributeTemplatesDtos.length; i++) {
            attributeTemplatesDtos[i] = attributeTemplatesList.get(i);
        }

        DocumentMasterTemplate template = documentTemplateService.createDocumentMasterTemplate(workspaceId, id, documentType, mask, createInstanceAttributeTemplateFromDto(attributeTemplatesDtos), idGenerated, attributesLocked,workflowModelId,workflowLocked);
        return mapper.map(template, DocumentMasterTemplateDTO.class);
    }
    
    @PUT
    @Path("{templateId}") 
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DocumentMasterTemplateDTO updateDocMsTemplate(@PathParam("workspaceId") String workspaceId,@PathParam("templateId") String templateId, DocumentMasterTemplateDTO docMsTemplateDTO)
            throws EntityNotFoundException, AccessRightException {

        String documentType = docMsTemplateDTO.getDocumentType();
        String mask = docMsTemplateDTO.getMask();
        boolean idGenerated = docMsTemplateDTO.isIdGenerated();
        boolean attributesLocked = docMsTemplateDTO.isAttributesLocked();
        String workflowModelId = docMsTemplateDTO.getWorkflowModelId();
        boolean workflowLocked = docMsTemplateDTO.isWorkflowLocked();

        Set<InstanceAttributeTemplateDTO> attributeTemplates = docMsTemplateDTO.getAttributeTemplates();
        List<InstanceAttributeTemplateDTO> attributeTemplatesList = new ArrayList<>(attributeTemplates);
        InstanceAttributeTemplateDTO[] attributeTemplatesDtos = new InstanceAttributeTemplateDTO[attributeTemplatesList.size()];

        for (int i = 0; i < attributeTemplatesDtos.length; i++) {
            attributeTemplatesDtos[i] = attributeTemplatesList.get(i);
        }

        DocumentMasterTemplate template = documentTemplateService.updateDocumentMasterTemplate(new DocumentMasterTemplateKey(workspaceId, templateId), documentType, mask, createInstanceAttributeTemplateFromDto(attributeTemplatesDtos), idGenerated, attributesLocked,workflowModelId,workflowLocked);
        return mapper.map(template, DocumentMasterTemplateDTO.class);
    }

    @DELETE
    @Path("{templateId}")
    public Response deleteDocumentMasterTemplate(@PathParam("workspaceId") String workspaceId, @PathParam("templateId") String templateId)
            throws EntityNotFoundException, AccessRightException {

        documentTemplateService.deleteDocumentMasterTemplate(new DocumentMasterTemplateKey(workspaceId, templateId));
        return Response.ok().build();
    }

    @DELETE
    @Path("{templateId}/files/{fileName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeAttachedFile(@PathParam("workspaceId") String workspaceId, @PathParam("templateId") String templateId, @PathParam("fileName") String fileName)
            throws EntityNotFoundException, AccessRightException, UserNotActiveException {

        String fileFullName = workspaceId + "/document-templates/" + templateId + "/" + fileName;

        documentTemplateService.removeFileFromTemplate(fileFullName);
        return Response.ok().build();
    }

    private InstanceAttributeTemplate[] createInstanceAttributeTemplateFromDto(InstanceAttributeTemplateDTO[] dtos) {
        InstanceAttributeTemplate[] data = new InstanceAttributeTemplate[dtos.length];

        for (int i = 0; i < dtos.length; i++) {
            data[i] = createInstanceAttributeTemplateObject(dtos[i]);
        }

        return data;
    }

    private InstanceAttributeTemplate createInstanceAttributeTemplateObject(InstanceAttributeTemplateDTO instanceAttributeTemplateDTO) {
        InstanceAttributeTemplate data = new InstanceAttributeTemplate();
        data.setName(instanceAttributeTemplateDTO.getName());
        data.setAttributeType(InstanceAttributeTemplate.AttributeType.valueOf(instanceAttributeTemplateDTO.getAttributeType().name()));
        data.setMandatory(instanceAttributeTemplateDTO.isMandatory());
        return data;
    }
}