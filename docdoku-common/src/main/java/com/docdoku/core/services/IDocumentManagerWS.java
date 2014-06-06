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

import com.docdoku.core.common.BinaryResource;
import com.docdoku.core.common.User;
import com.docdoku.core.document.*;
import com.docdoku.core.exceptions.*;
import com.docdoku.core.meta.InstanceAttribute;
import com.docdoku.core.meta.InstanceAttributeTemplate;
import com.docdoku.core.meta.TagKey;
import com.docdoku.core.query.DocumentSearchQuery;
import com.docdoku.core.security.ACLUserEntry;
import com.docdoku.core.security.ACLUserGroupEntry;
import com.docdoku.core.sharing.SharedDocument;
import com.docdoku.core.sharing.SharedEntityKey;
import com.docdoku.core.workflow.Task;
import com.docdoku.core.workflow.TaskKey;

import javax.jws.WebService;
import java.util.Date;
import java.util.Map;

/**
 *
 * @author Florent Garin
 */
@WebService
public interface IDocumentManagerWS {

    String generateId(String pWorkspaceId, String pDocMTemplateId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, DocumentMasterTemplateNotFoundException;

    User[] getUsers(String pWorkspaceId) throws WorkspaceNotFoundException, AccessRightException, AccountNotFoundException, UserNotFoundException, UserNotActiveException;

    User[] getReachableUsers() throws AccountNotFoundException;

    DocumentRevision getDocumentRevision(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, DocumentRevisionNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, AccessRightException;

    DocumentRevision[] searchDocumentRevisions(DocumentSearchQuery pQuery) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, ESServerException;

    DocumentMasterTemplate getDocumentMasterTemplate(DocumentMasterTemplateKey pKey) throws WorkspaceNotFoundException, DocumentMasterTemplateNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMasterTemplate createDocumentMasterTemplate(String workspaceId, String id, String documentType, String mask, InstanceAttributeTemplate[] attributeTemplates, boolean idGenerated, boolean attributesLocked) throws WorkspaceNotFoundException, AccessRightException, DocumentMasterTemplateAlreadyExistsException, UserNotFoundException, NotAllowedException, CreationException;

    DocumentMasterTemplate[] getDocumentMasterTemplates(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentRevision[] findDocumentRevisionsByFolder(String pCompletePath) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentRevision[] findDocumentRevisionsByTag(TagKey pKey) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentRevision[] getCheckedOutDocumentRevisions(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    String[] getFolders(String pCompletePath) throws WorkspaceNotFoundException, FolderNotFoundException, UserNotFoundException, UserNotActiveException;

    String[] getTags(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    Task[] getTasks(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentRevision approveTaskOnDocument(String pWorkspaceId, TaskKey pTaskKey, String pComment, String pSignature) throws WorkspaceNotFoundException, TaskNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, WorkflowNotFoundException;

    DocumentRevision rejectTaskOnDocument(String pWorkspaceId, TaskKey pTaskKey, String pComment, String pSignature) throws WorkspaceNotFoundException, TaskNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, WorkflowNotFoundException;

    DocumentRevision checkInDocument(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException, ESServerException;

    DocumentRevision checkOutDocument(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, AccessRightException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException;

    DocumentRevision undoCheckOutDocument(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, DocumentRevisionNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, AccessRightException;

    void deleteDocumentRevision(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException, ESServerException;

    DocumentRevision moveDocumentRevision(String pParentFolder, DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, DocumentRevisionNotFoundException, NotAllowedException, AccessRightException, FolderNotFoundException, UserNotFoundException, UserNotActiveException;

    Folder createFolder(String parentFolder, String folder) throws WorkspaceNotFoundException, NotAllowedException, AccessRightException, FolderNotFoundException, FolderAlreadyExistsException, UserNotFoundException, CreationException;

    DocumentRevisionKey[] deleteFolder(String pCompletePath) throws WorkspaceNotFoundException, NotAllowedException, AccessRightException, UserNotFoundException, FolderNotFoundException, ESServerException;

    DocumentRevisionKey[] moveFolder(String pCompletePath, String pDestParentFolder, String pDestFolder) throws WorkspaceNotFoundException, NotAllowedException, AccessRightException, UserNotFoundException, FolderNotFoundException, CreationException, FolderAlreadyExistsException, ESServerException;

    DocumentRevision updateDocument(DocumentIterationKey key, String revisionNote, InstanceAttribute[] attributes, DocumentIterationKey[] linkKeys) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException;

    DocumentRevision[] createDocumentRevision(DocumentRevisionKey pOriginalDocRPK, String pTitle, String pDescription, String pWorkflowModelId, ACLUserEntry[] aclUserEntries, ACLUserGroupEntry[] aclUserGroupEntries, Map<String,String> roleMappings) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, NotAllowedException, DocumentRevisionAlreadyExistsException, CreationException, WorkflowModelNotFoundException, RoleNotFoundException, DocumentRevisionNotFoundException, FileAlreadyExistsException;

    DocumentRevision removeFileFromDocument(String pFullName) throws WorkspaceNotFoundException, DocumentRevisionNotFoundException, NotAllowedException, AccessRightException, FileNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentMasterTemplate removeFileFromTemplate(String pFullName) throws WorkspaceNotFoundException, DocumentMasterTemplateNotFoundException, AccessRightException, FileNotFoundException, UserNotFoundException, UserNotActiveException;

    void deleteDocumentMasterTemplate(DocumentMasterTemplateKey key) throws  WorkspaceNotFoundException, AccessRightException, DocumentMasterTemplateNotFoundException, UserNotFoundException;

    void deleteTag(TagKey key) throws WorkspaceNotFoundException, AccessRightException, TagNotFoundException, UserNotFoundException;

    void createTag(String workspaceId, String label) throws WorkspaceNotFoundException, AccessRightException, CreationException, TagAlreadyExistsException, UserNotFoundException;

    DocumentRevision saveTags(DocumentRevisionKey pDocRPK, String[] pTags) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException, ESServerException;

    DocumentRevisionKey[] getIterationChangeEventSubscriptions(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentRevisionKey[] getStateChangeEventSubscriptions(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, ESServerException;

    void subscribeToIterationChangeEvent(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException;

    void subscribeToStateChangeEvent(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException;

    void unsubscribeToIterationChangeEvent(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException, DocumentRevisionNotFoundException;

    DocumentMasterTemplate updateDocumentMasterTemplate(DocumentMasterTemplateKey key, String documentType, String mask, InstanceAttributeTemplate[] attributeTemplates, boolean idGenerated, boolean attributesLocked) throws WorkspaceNotFoundException, AccessRightException, DocumentMasterTemplateNotFoundException, UserNotFoundException;

    void unsubscribeToStateChangeEvent(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException, DocumentRevisionNotFoundException;

    boolean isUserStateChangeEventSubscribedForGivenDocument(String pWorkspaceId, DocumentRevision docR) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;

    boolean isUserIterationChangeEventSubscribedForGivenDocument(String pWorkspaceId, DocumentRevision docR) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;

    DocumentRevision[] getDocumentRevisionsWithAssignedTasksForGivenUser(String pWorkspaceId, String assignedUserLogin) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentRevision[] getDocumentRevisionsWithOpenedTasksForGivenUser(String pWorkspaceId, String assignedUserLogin) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    DocumentRevision[] getDocumentRevisionsWithReference(String pWorkspaceId, String reference, int maxResults) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;

    int getTotalNumberOfDocuments(String pWorkspaceId) throws WorkspaceNotFoundException, AccessRightException, AccountNotFoundException;

    long getDiskUsageForDocumentsInWorkspace(String pWorkspaceId) throws WorkspaceNotFoundException, AccessRightException, AccountNotFoundException;

    long getDiskUsageForDocumentTemplatesInWorkspace(String pWorkspaceId) throws WorkspaceNotFoundException, AccessRightException, AccountNotFoundException;

    DocumentRevision createDocumentMaster(String pParentFolder, String pDocMId, String pTitle, String pDescription, String pDocMTemplateId, String pWorkflowModelId, ACLUserEntry[] pACLUserEntries, ACLUserGroupEntry[] pACLUserGroupEntries, Map<String, String> roleMappings) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, NotAllowedException, FolderNotFoundException, DocumentMasterTemplateNotFoundException, FileAlreadyExistsException, CreationException, DocumentRevisionAlreadyExistsException, RoleNotFoundException, WorkflowModelNotFoundException, DocumentMasterAlreadyExistsException;

    DocumentRevision[] getAllCheckedOutDocumentRevisions(String pWorkspaceId) throws WorkspaceNotFoundException, AccessRightException, AccountNotFoundException;

    SharedDocument createSharedDocument(DocumentRevisionKey pDocRPK, String pPassword, Date pExpireDate) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, DocumentRevisionNotFoundException, UserNotActiveException, NotAllowedException;

    void deleteSharedDocument(SharedEntityKey sharedEntityKey) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, SharedEntityNotFoundException;

    DocumentIteration findDocumentIterationByBinaryResource(BinaryResource pBinaryResource) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, DocumentRevisionNotFoundException;

    void updateDocumentACL(DocumentRevisionKey docKey, Map<String,String> userEntries, Map<String,String> userGroupEntries) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, DocumentRevisionNotFoundException, AccessRightException;

    void removeACLFromDocumentRevision(DocumentRevisionKey documentRevisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, DocumentRevisionNotFoundException, AccessRightException;

    DocumentRevision[] getAllDocumentsInWorkspace(String workspaceId, int start, int pMaxResults) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;

    int getDocumentsInWorkspaceCount(String workspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;

    User savePersonalInfo(String workspaceId, String name, String email, String language) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;
}
