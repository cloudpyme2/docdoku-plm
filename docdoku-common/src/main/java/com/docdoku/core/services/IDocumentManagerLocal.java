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
package com.docdoku.core.services;

import com.docdoku.core.document.SearchQuery;
import com.docdoku.core.document.DocumentMasterTemplate;
import com.docdoku.core.meta.InstanceAttributeTemplate;
import com.docdoku.core.document.DocumentMasterKey;
import com.docdoku.core.document.TagKey;
import com.docdoku.core.document.Folder;
import com.docdoku.core.meta.InstanceAttribute;
import com.docdoku.core.security.ACLUserEntry;
import com.docdoku.core.security.ACLUserGroupEntry;
import com.docdoku.core.document.DocumentIterationKey;
import com.docdoku.core.document.DocumentMaster;
import com.docdoku.core.workflow.TaskKey;
import com.docdoku.core.common.User;
import com.docdoku.core.document.DocumentMasterTemplateKey;
import com.docdoku.core.workflow.Task;

import javax.annotation.security.RolesAllowed;
import java.io.File;

/**
 *
 * @author Florent Garin
 */
public interface IDocumentManagerLocal {

    String generateId(String pWorkspaceId, String pDocMTemplateId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, DocumentMasterTemplateNotFoundException;

    User[] getUsers(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMaster getDocumentMaster(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, DocumentMasterNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException;

    DocumentMaster[] searchDocumentMasters(SearchQuery pQuery) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMasterTemplate getDocumentMasterTemplate(DocumentMasterTemplateKey pKey) throws WorkspaceNotFoundException, DocumentMasterTemplateNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMasterTemplate[] getDocumentMasterTemplates(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMaster[] findDocumentMastersByFolder(String pCompletePath) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMaster[] findDocumentMastersByTag(TagKey pKey) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMaster[] getCheckedOutDocumentMasters(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    String[] getFolders(String pCompletePath) throws WorkspaceNotFoundException, FolderNotFoundException, UserNotFoundException, UserNotActiveException;

    String[] getTags(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    Task[] getTasks(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMaster approve(String pWorkspaceId, TaskKey pTaskKey, String pComment) throws WorkspaceNotFoundException, TaskNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException;

    DocumentMaster reject(String pWorkspaceId, TaskKey pTaskKey, String pComment) throws WorkspaceNotFoundException, TaskNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException;

    File saveFileInTemplate(DocumentMasterTemplateKey pDocMTemplateKey, String pName, long pSize) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterTemplateNotFoundException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException;

    File saveFileInDocument(DocumentIterationKey pDocPK, String pName, long pSize) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException;

    File getDataFile(String pFullName) throws WorkspaceNotFoundException, NotAllowedException, FileNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMaster checkInDocument(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException;

    DocumentMaster checkOutDocument(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, AccessRightException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException;

    DocumentMaster undoCheckOutDocument(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, DocumentMasterNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException;

    void deleteDocumentMaster(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException;

    DocumentMaster moveDocumentMaster(String pParentFolder, DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, DocumentMasterNotFoundException, NotAllowedException, AccessRightException, FolderNotFoundException, UserNotFoundException, UserNotActiveException;

    Folder createFolder(String pParentFolder, String pFolder) throws WorkspaceNotFoundException, NotAllowedException, AccessRightException, FolderNotFoundException, FolderAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException;

    DocumentMasterKey[] deleteFolder(String pCompletePath) throws WorkspaceNotFoundException, NotAllowedException, AccessRightException, UserNotFoundException, UserNotActiveException, FolderNotFoundException;

    DocumentMasterKey[] moveFolder(String pCompletePath, String pDestParentFolder, String pDestFolder) throws WorkspaceNotFoundException, NotAllowedException, AccessRightException, UserNotFoundException, FolderNotFoundException, CreationException, FolderAlreadyExistsException;
    
    DocumentMaster updateDocument(DocumentIterationKey pKey, String pRevisionNote, InstanceAttribute[] pAttributes, DocumentIterationKey[] pLinkKeys) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException;

    DocumentMasterTemplate updateDocumentMasterTemplate(DocumentMasterTemplateKey pKey, String pDocumentType, String pMask, InstanceAttributeTemplate[] pAttributeTemplates, boolean idGenerated) throws WorkspaceNotFoundException, WorkspaceNotFoundException, AccessRightException, DocumentMasterTemplateNotFoundException, UserNotFoundException, UserNotActiveException;

    User whoAmI(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMaster createDocumentMaster(String pParentFolder, String pDocMId, String pTitle, String pDescription, String pDocMTemplateId, String pWorkflowModelId, ACLUserEntry[] aclUserEntries, ACLUserGroupEntry[] aclUserGroupEntries) throws WorkspaceNotFoundException, WorkflowModelNotFoundException, NotAllowedException, DocumentMasterTemplateNotFoundException, AccessRightException, DocumentMasterAlreadyExistsException, FolderNotFoundException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException;

    DocumentMaster[] createVersion(DocumentMasterKey pOriginalDocMPK, String pTitle, String pDescription, String pWorkflowModelId, ACLUserEntry[] aclUserEntries, ACLUserGroupEntry[] aclUserGroupEntries) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, WorkflowModelNotFoundException, AccessRightException, DocumentMasterAlreadyExistsException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException;

    DocumentMaster removeFileFromDocument(String pFullName) throws WorkspaceNotFoundException, DocumentMasterNotFoundException, NotAllowedException, AccessRightException, FileNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMasterTemplate removeFileFromTemplate(String pFullName) throws WorkspaceNotFoundException, DocumentMasterTemplateNotFoundException, AccessRightException, FileNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMasterTemplate createDocumentMasterTemplate(String pWorkspaceId, String pId, String pDocumentType, String pMask, InstanceAttributeTemplate[] pAttributeTemplates, boolean idGenerated) throws WorkspaceNotFoundException, AccessRightException, DocumentMasterTemplateAlreadyExistsException, UserNotFoundException, UserNotActiveException, NotAllowedException, CreationException;

    void deleteDocumentMasterTemplate(DocumentMasterTemplateKey pKey) throws WorkspaceNotFoundException, AccessRightException, DocumentMasterTemplateNotFoundException, UserNotFoundException, UserNotActiveException;

    void deleteTag(TagKey pKey) throws WorkspaceNotFoundException, AccessRightException, TagNotFoundException, UserNotFoundException, UserNotActiveException;

    void createTag(String pWorkspaceId, String pLabel) throws WorkspaceNotFoundException, AccessRightException, CreationException, TagAlreadyExistsException, UserNotFoundException, UserNotActiveException;

    DocumentMaster saveTags(DocumentMasterKey pDocMPK, String[] pTags) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException;

    DocumentMasterKey[] getIterationChangeEventSubscriptions(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMasterKey[] getStateChangeEventSubscriptions(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    void subscribeToIterationChangeEvent(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, UserNotFoundException, UserNotActiveException;

    void subscribeToStateChangeEvent(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterNotFoundException, UserNotFoundException, UserNotActiveException;

    void unsubscribeToIterationChangeEvent(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    void unsubscribeToStateChangeEvent(DocumentMasterKey pDocMPK) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    boolean isUserStateChangeEventSubscribedForGivenDocument(String pWorkspaceId, DocumentMaster docM) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException ;

    boolean isUserIterationChangeEventSubscribedForGivenDocument(String pWorkspaceId, DocumentMaster docM) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException ;

    DocumentMaster[] getDocumentMastersWithAssignedTasksForGivenUser(String pWorkspaceId, String assignedUserLogin) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMaster[] getDocumentMastersWithReference(String pWorkspaceId, String reference, int maxResults) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    int getDocumentsCountInWorkspace(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;

    Long getDiskUsageForDocumentsInWorkspace(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;

    Long getDiskUsageForDocumentTemplatesInWorkspace(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;
}
