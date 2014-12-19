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

import com.docdoku.core.change.ChangeItem;
import com.docdoku.core.common.*;
import com.docdoku.core.document.*;
import com.docdoku.core.exceptions.*;
import com.docdoku.core.gcm.GCMAccount;
import com.docdoku.core.meta.InstanceAttribute;
import com.docdoku.core.meta.InstanceAttributeTemplate;
import com.docdoku.core.meta.Tag;
import com.docdoku.core.meta.TagKey;
import com.docdoku.core.query.DocumentSearchQuery;
import com.docdoku.core.security.ACL;
import com.docdoku.core.security.ACLUserEntry;
import com.docdoku.core.security.ACLUserGroupEntry;
import com.docdoku.core.security.UserGroupMapping;
import com.docdoku.core.services.*;
import com.docdoku.core.sharing.SharedDocument;
import com.docdoku.core.sharing.SharedEntityKey;
import com.docdoku.core.util.NamingConvention;
import com.docdoku.core.util.Tools;
import com.docdoku.core.workflow.*;
import com.docdoku.server.dao.*;
import com.docdoku.server.esindexer.ESIndexer;
import com.docdoku.server.esindexer.ESSearcher;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@DeclareRoles({UserGroupMapping.REGULAR_USER_ROLE_ID,UserGroupMapping.ADMIN_ROLE_ID,UserGroupMapping.GUEST_PROXY_ROLE_ID})
@Local(IDocumentManagerLocal.class)
@Stateless(name = "DocumentManagerBean")
@WebService(endpointInterface = "com.docdoku.core.services.IDocumentManagerWS")
public class DocumentManagerBean implements IDocumentManagerWS, IDocumentManagerLocal {

    @PersistenceContext
    private EntityManager em;

    @Resource
    private SessionContext ctx;

    @EJB
    private IUserManagerLocal userManager;
    @EJB
    private IMailerLocal mailer;
    @EJB
    private IGCMSenderLocal gcmNotifier;
    @EJB
    private ESIndexer esIndexer;
    @EJB
    private ESSearcher esSearcher;
    @EJB
    private IDataManagerLocal dataManager;

    private static final Logger LOGGER = Logger.getLogger(DocumentManagerBean.class.getName());

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public BinaryResource saveFileInTemplate(DocumentMasterTemplateKey pDocMTemplateKey, String pName, long pSize) throws WorkspaceNotFoundException, NotAllowedException, DocumentMasterTemplateNotFoundException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException, AccessRightException {
        User user = userManager.checkWorkspaceWriteAccess(pDocMTemplateKey.getWorkspaceId());
        Locale locale = new Locale(user.getLanguage());
        checkNameFileValidity(pName, locale);

        DocumentMasterTemplateDAO templateDAO = new DocumentMasterTemplateDAO(em);
        DocumentMasterTemplate template = templateDAO.loadDocMTemplate(pDocMTemplateKey);
        BinaryResource binaryResource = null;
        String fullName = template.getWorkspaceId() + "/document-templates/" + template.getId() + "/" + pName;

        for (BinaryResource bin : template.getAttachedFiles()) {
            if (bin.getFullName().equals(fullName)) {
                binaryResource = bin;
                break;
            }
        }

        if (binaryResource == null) {
            binaryResource = new BinaryResource(fullName, pSize, new Date());
            new BinaryResourceDAO(em).createBinaryResource(binaryResource);
            template.addFile(binaryResource);
        } else {
            binaryResource.setContentLength(pSize);
            binaryResource.setLastModified(new Date());
        }
        return binaryResource;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public BinaryResource saveFileInDocument(DocumentIterationKey pDocPK, String pName, long pSize) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException, AccessRightException {
        User user = checkDocumentRevisionWriteAccess(new DocumentRevisionKey(pDocPK.getWorkspaceId(), pDocPK.getDocumentMasterId(), pDocPK.getDocumentRevisionVersion()));
        Locale locale = new Locale(user.getLanguage());
        checkNameFileValidity(pName, locale);

        DocumentRevisionDAO docRDAO = new DocumentRevisionDAO(em);
        DocumentRevision docR = docRDAO.loadDocR(new DocumentRevisionKey(pDocPK.getWorkspaceId(), pDocPK.getDocumentMasterId(), pDocPK.getDocumentRevisionVersion()));
        DocumentIteration document = docR.getIteration(pDocPK.getIteration());

        if (isCheckoutByUser(user, docR) && docR.getLastIteration().equals(document)) {
            BinaryResource binaryResource = null;
            String fullName = docR.getWorkspaceId() + "/documents/" + docR.getId() + "/" + docR.getVersion() + "/" + document.getIteration() + "/" + pName;

            for (BinaryResource bin : document.getAttachedFiles()) {
                if (bin.getFullName().equals(fullName)) {
                    binaryResource = bin;
                    break;
                }
            }
            if (binaryResource == null) {
                binaryResource = new BinaryResource(fullName, pSize, new Date());
                new BinaryResourceDAO(em).createBinaryResource(binaryResource);
                document.addFile(binaryResource);
            } else {
                binaryResource.setContentLength(pSize);
                binaryResource.setLastModified(new Date());
            }
            return binaryResource;
        } else {
            throw new NotAllowedException(Locale.getDefault(), "NotAllowedException4");
        }
    }

    @LogDocument
    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID,UserGroupMapping.GUEST_PROXY_ROLE_ID})
    @Override
    public BinaryResource getBinaryResource(String pFullName) throws WorkspaceNotFoundException, NotAllowedException, FileNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException {

        if(ctx.isCallerInRole(UserGroupMapping.GUEST_PROXY_ROLE_ID)){
            //Todo check if document is public
            return new BinaryResourceDAO(em).loadBinaryResource(pFullName);
        }

        User user = userManager.checkWorkspaceReadAccess(BinaryResource.parseWorkspaceId(pFullName));
        Locale userLocale = new Locale(user.getLanguage());
        BinaryResourceDAO binDAO = new BinaryResourceDAO(userLocale, em);
        BinaryResource binaryResource = binDAO.loadBinaryResource(pFullName);

        DocumentIteration document = binDAO.getDocumentOwner(binaryResource);
        if (document != null) {
            DocumentRevision docR = document.getDocumentRevision();

            if(isACLGrantReadAccess(user,docR)){
                if((isInAnotherUserHomeFolder(user,docR) || isCheckoutByAnotherUser(user,docR)) && docR.getLastIteration().equals(document)) {
                    throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException34");
                } else {
                    return binaryResource;
                }
            }else{
                throw new AccessRightException(userLocale,user);
            }
        } else {
            throw new FileNotFoundException(userLocale, pFullName);
        }
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID,UserGroupMapping.GUEST_PROXY_ROLE_ID})
    @Override
    public BinaryResource getTemplateBinaryResource(String pFullName) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, FileNotFoundException {
        User user = userManager.checkWorkspaceReadAccess(BinaryResource.parseWorkspaceId(pFullName));
        Locale userLocale = new Locale(user.getLanguage());
        BinaryResourceDAO binDAO = new BinaryResourceDAO(userLocale, em);
        return binDAO.loadBinaryResource(pFullName);
    }



    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public String [] getFolders(String pCompletePath) throws WorkspaceNotFoundException, FolderNotFoundException, UserNotFoundException, UserNotActiveException {
        User user = userManager.checkWorkspaceReadAccess(Folder.parseWorkspaceId(pCompletePath));
        Folder[] subFolders = new FolderDAO(new Locale(user.getLanguage()), em).getSubFolders(pCompletePath);
        String[] shortNames = new String[subFolders.length];
        int i = 0;
        for (Folder f : subFolders) {
            shortNames[i++] = f.getShortName();
        }
        return shortNames;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision[] findDocumentRevisionsByFolder(String pCompletePath) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
        String workspaceId = Folder.parseWorkspaceId(pCompletePath);
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        List<DocumentRevision> docRs = new DocumentRevisionDAO(new Locale(user.getLanguage()), em).findDocRsByFolder(pCompletePath);
        ListIterator<DocumentRevision> ite = docRs.listIterator();
        while (ite.hasNext()) {
            DocumentRevision docR = ite.next();
            if (!hasDocumentRevisionReadAccess(user,docR)) {
                ite.remove();
            }else if (isCheckoutByAnotherUser(user,docR)) {
                em.detach(docR);
                docR.removeLastIteration();
            }
        }
        return docRs.toArray(new DocumentRevision[docRs.size()]);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision[] findDocumentRevisionsByTag(TagKey pKey) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
        String workspaceId = pKey.getWorkspaceId();
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        List<DocumentRevision> docRs = new DocumentRevisionDAO(new Locale(user.getLanguage()), em).findDocRsByTag(new Tag(user.getWorkspace(), pKey.getLabel()));
        ListIterator<DocumentRevision> ite = docRs.listIterator();
        while (ite.hasNext()) {
            DocumentRevision docR = ite.next();
            if (!hasDocumentRevisionReadAccess(user,docR)){
                ite.remove();
            }else if (isCheckoutByAnotherUser(user,docR)) {
                em.detach(docR);
                docR.removeLastIteration();
            }
        }
        return docRs.toArray(new DocumentRevision[docRs.size()]);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID,UserGroupMapping.GUEST_PROXY_ROLE_ID})
    @Override
    public DocumentRevision getDocumentRevision(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, DocumentRevisionNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, AccessRightException{
        if(ctx.isCallerInRole(UserGroupMapping.GUEST_PROXY_ROLE_ID)){
            DocumentRevision documentRevision = new DocumentRevisionDAO(em).loadDocR(pDocRPK);
            if(documentRevision.isCheckedOut()){
                em.detach(documentRevision);
                documentRevision.removeLastIteration();
            }
            return documentRevision;
        }

        User user = userManager.checkWorkspaceReadAccess(pDocRPK.getWorkspaceId());
        DocumentRevision docR = new DocumentRevisionDAO(new Locale(user.getLanguage()), em).loadDocR(pDocRPK);
        String owner = docR.getLocation().getOwner();
        if ((owner != null) && (!owner.equals(user.getLogin()))) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException5");
        }

        if((docR.getACL() != null && docR.getACL().hasReadAccess(user)) || docR.getACL() == null){

            if ((docR.isCheckedOut()) && (!docR.getCheckOutUser().equals(user))) {
                em.detach(docR);
                docR.removeLastIteration();
            }
            return docR;

        }else{
            throw new AccessRightException(new Locale(user.getLanguage()),user);
        }

    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID,UserGroupMapping.GUEST_PROXY_ROLE_ID})
    @Override
    public DocumentIteration findDocumentIterationByBinaryResource(BinaryResource pBinaryResource) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
        if(ctx.isCallerInRole(UserGroupMapping.GUEST_PROXY_ROLE_ID)){
            return  new DocumentRevisionDAO(em).findDocumentIterationByBinaryResource(pBinaryResource);
        }

        User user = userManager.checkWorkspaceReadAccess(pBinaryResource.getWorkspaceId());
        DocumentRevisionDAO documentMasterDAO = new DocumentRevisionDAO(new Locale(user.getLanguage()),em);
        return  documentMasterDAO.findDocumentIterationByBinaryResource(pBinaryResource);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void updateDocumentACL(String pWorkspaceId, DocumentRevisionKey docKey, Map<String,String> pACLUserEntries, Map<String,String> pACLUserGroupEntries) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, DocumentRevisionNotFoundException, AccessRightException {
        User user = checkDocumentRevisionWriteAccess(docKey);

        DocumentRevisionDAO documentRevisionDAO = new DocumentRevisionDAO(new Locale(user.getLanguage()), em);
        DocumentRevision docR = documentRevisionDAO.loadDocR(docKey);
        if (user.isAdministrator() || isAuthor(user,docR)) {

            if (docR.getACL() == null) {
                ACL acl = new ACL();
                if (pACLUserEntries != null) {
                    for (Map.Entry<String, String> entry : pACLUserEntries.entrySet()) {
                        acl.addEntry(em.getReference(User.class,new UserKey(pWorkspaceId,entry.getKey())),ACL.Permission.valueOf(entry.getValue()));
                    }
                }

                if (pACLUserGroupEntries != null) {
                    for (Map.Entry<String, String> entry : pACLUserGroupEntries.entrySet()) {
                        acl.addEntry(em.getReference(UserGroup.class,new UserGroupKey(pWorkspaceId,entry.getKey())),ACL.Permission.valueOf(entry.getValue()));
                    }
                }

                new ACLDAO(em).createACL(acl);
                docR.setACL(acl);

            }else{
                if (pACLUserEntries != null) {
                    for (ACLUserEntry entry : docR.getACL().getUserEntries().values()) {
                        ACL.Permission newPermission = ACL.Permission.valueOf(pACLUserEntries.get(entry.getPrincipalLogin()));
                        if(newPermission != null){
                            entry.setPermission(newPermission);
                        }
                    }
                }

                if (pACLUserGroupEntries != null) {
                    for (ACLUserGroupEntry entry : docR.getACL().getGroupEntries().values()) {
                        ACL.Permission newPermission = ACL.Permission.valueOf(pACLUserGroupEntries.get(entry.getPrincipalId()));
                        if(newPermission != null){
                            entry.setPermission(newPermission);
                        }
                    }
                }
            }

        }else {
                throw new AccessRightException(new Locale(user.getLanguage()), user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void removeACLFromDocumentRevision(DocumentRevisionKey documentRevisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, DocumentRevisionNotFoundException, AccessRightException {
        User user = userManager.checkWorkspaceReadAccess(documentRevisionKey.getDocumentMaster().getWorkspace());

        Locale locale = new Locale(user.getLanguage());
        DocumentRevision docR = new DocumentRevisionDAO(locale, em).getDocRRef(documentRevisionKey);

        if (user.isAdministrator() || isAuthor(user,docR)) {
            ACL acl = docR.getACL();
            if (acl != null) {
                new ACLDAO(em).removeACLEntries(acl);
                docR.setACL(null);
            }
        }else{
            throw new AccessRightException(locale, user);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision[] getAllDocumentsInWorkspace(String workspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        List<DocumentRevision> docRs = new DocumentRevisionDAO(new Locale(user.getLanguage()), em).getDocumentRevisionsFiltered(user, workspaceId);
        ListIterator<DocumentRevision> ite = docRs.listIterator();

        while (ite.hasNext()) {
            DocumentRevision docR = ite.next();
            if ((docR.isCheckedOut()) && (!docR.getCheckOutUser().equals(user))) {
                em.detach(docR);
                docR.removeLastIteration();
            }
        }
        return docRs.toArray(new DocumentRevision[docRs.size()]);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision[] getAllDocumentsInWorkspace(String workspaceId, int start, int pMaxResults) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        List<DocumentRevision> docRs = new DocumentRevisionDAO(new Locale(user.getLanguage()), em).getDocumentRevisionsFiltered(user, workspaceId, start, pMaxResults);
        ListIterator<DocumentRevision> ite = docRs.listIterator();

        while (ite.hasNext()) {
            DocumentRevision docR = ite.next();

            if ((docR.isCheckedOut()) && (!docR.getCheckOutUser().equals(user))) {
                em.detach(docR);
                docR.removeLastIteration();
            }
        }
        return docRs.toArray(new DocumentRevision[docRs.size()]);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public int getDocumentsInWorkspaceCount(String workspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
        User user = userManager.checkWorkspaceReadAccess(workspaceId);
        return new DocumentRevisionDAO(new Locale(user.getLanguage()), em).getDocumentRevisionsCountFiltered(user, workspaceId);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID,UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public int getTotalNumberOfDocuments(String pWorkspaceId) throws WorkspaceNotFoundException, AccessRightException, AccountNotFoundException {
        Account account = userManager.checkAdmin(pWorkspaceId);
        DocumentRevisionDAO documentRevisionDAO = new DocumentRevisionDAO(new Locale(account.getLanguage()),em);
        return documentRevisionDAO.getTotalNumberOfDocuments(pWorkspaceId);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision[] getCheckedOutDocumentRevisions(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException{
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<DocumentRevision> docRs = new DocumentRevisionDAO(new Locale(user.getLanguage()), em).findCheckedOutDocRs(user);
        return docRs.toArray(new DocumentRevision[docRs.size()]);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Task[] getTasks(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        return new TaskDAO(new Locale(user.getLanguage()), em).findTasks(user);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevisionKey[] getIterationChangeEventSubscriptions(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        return new SubscriptionDAO(em).getIterationChangeEventSubscriptions(user);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public DocumentRevisionKey[] getStateChangeEventSubscriptions(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        return new SubscriptionDAO(em).getStateChangeEventSubscriptions(user);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public boolean isUserStateChangeEventSubscribedForGivenDocument(String pWorkspaceId, DocumentRevision docR) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        return new SubscriptionDAO(em).isUserStateChangeEventSubscribedForGivenDocument(user, docR);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public boolean isUserIterationChangeEventSubscribedForGivenDocument(String pWorkspaceId, DocumentRevision docR) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        return  new SubscriptionDAO(em).isUserIterationChangeEventSubscribedForGivenDocument(user, docR);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision[] getDocumentRevisionsWithReference(String pWorkspaceId, String reference, int maxResults) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        List<DocumentRevision> docRs = new DocumentRevisionDAO(new Locale(user.getLanguage()), em).findDocsRevisionsWithReferenceLike(pWorkspaceId, reference, maxResults);
        return docRs.toArray(new DocumentRevision[docRs.size()]);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public String generateId(String pWorkspaceId, String pDocMTemplateId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, DocumentMasterTemplateNotFoundException {

        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        DocumentMasterTemplate template = new DocumentMasterTemplateDAO(new Locale(user.getLanguage()), em).loadDocMTemplate(new DocumentMasterTemplateKey(user.getWorkspaceId(), pDocMTemplateId));

        String newId = null;
        try {
            String latestId = new DocumentRevisionDAO(new Locale(user.getLanguage()), em).findLatestDocMId(pWorkspaceId, template.getDocumentType());
            String inputMask = template.getMask();
            String convertedMask = Tools.convertMask(inputMask);
            newId = Tools.increaseId(latestId, convertedMask);
        } catch (NoResultException ex){
            LOGGER.log(Level.FINER,null,ex);
            //may happen when no document of the specified type has been created
        } catch (ParseException ex) {
            LOGGER.log(Level.SEVERE, null,ex);
            //may happen when a different mask has been used for the same document type
        }
        return newId;

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision[] searchDocumentRevisions(DocumentSearchQuery pQuery) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, ESServerException {
        User user = userManager.checkWorkspaceReadAccess(pQuery.getWorkspaceId());
        List<DocumentRevision> fetchedDocRs = esSearcher.search(pQuery);                                                // Get Search Results
        List<DocumentRevision> docList = new ArrayList<>();

        if(!fetchedDocRs.isEmpty()){
            for(DocumentRevision docR : fetchedDocRs){
                DocumentRevision filteredDocR = applyDocumentRevisionReadAccess(user, docR);
                if(filteredDocR!=null){
                    docList.add(filteredDocR);
                }
            }

            return docList.toArray(new DocumentRevision[docList.size()]);
        }
        return new DocumentRevision[0];
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentMasterTemplate[] getDocumentMasterTemplates(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        return new DocumentMasterTemplateDAO(new Locale(user.getLanguage()), em).findAllDocMTemplates(pWorkspaceId);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentMasterTemplate getDocumentMasterTemplate(DocumentMasterTemplateKey pKey)
            throws WorkspaceNotFoundException, DocumentMasterTemplateNotFoundException, UserNotFoundException, UserNotActiveException {
        User user = userManager.checkWorkspaceReadAccess(pKey.getWorkspaceId());
        return new DocumentMasterTemplateDAO(new Locale(user.getLanguage()), em).loadDocMTemplate(pKey);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentMasterTemplate updateDocumentMasterTemplate(DocumentMasterTemplateKey pKey, String pDocumentType, String pMask, InstanceAttributeTemplate[] pAttributeTemplates, boolean idGenerated, boolean attributesLocked) throws WorkspaceNotFoundException, AccessRightException, DocumentMasterTemplateNotFoundException, UserNotFoundException {
        User user = userManager.checkWorkspaceWriteAccess(pKey.getWorkspaceId());

        DocumentMasterTemplateDAO templateDAO = new DocumentMasterTemplateDAO(new Locale(user.getLanguage()), em);
        DocumentMasterTemplate template = templateDAO.loadDocMTemplate(pKey);
        Date now = new Date();
        template.setCreationDate(now);
        template.setAuthor(user);
        template.setDocumentType(pDocumentType);
        template.setMask(pMask);
        template.setIdGenerated(idGenerated);
        template.setAttributesLocked(attributesLocked);

        Set<InstanceAttributeTemplate> attrs = new HashSet<>();
        Collections.addAll(attrs, pAttributeTemplates);

        Set<InstanceAttributeTemplate> attrsToRemove = new HashSet<>(template.getAttributeTemplates());
        attrsToRemove.removeAll(attrs);

        InstanceAttributeTemplateDAO attrDAO = new InstanceAttributeTemplateDAO(em);
        for (InstanceAttributeTemplate attrToRemove : attrsToRemove) {
            attrDAO.removeAttribute(attrToRemove);
        }

        template.setAttributeTemplates(attrs);
        return template;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deleteTag(TagKey pKey) throws WorkspaceNotFoundException, AccessRightException, TagNotFoundException, UserNotFoundException {
        User user = userManager.checkWorkspaceWriteAccess(pKey.getWorkspaceId());
        Locale userLocale = new Locale(user.getLanguage());
        Tag tagToRemove = new Tag(user.getWorkspace(), pKey.getLabel());
        List<DocumentRevision> docRs = new DocumentRevisionDAO(userLocale, em).findDocRsByTag(tagToRemove);
        for (DocumentRevision docR : docRs) {
            docR.getTags().remove(tagToRemove);
        }
        List<ChangeItem> changeItems = new ChangeItemDAO(userLocale, em).findChangeItemByTag(pKey.getWorkspaceId(),tagToRemove);
        for (ChangeItem changeItem : changeItems) {
            changeItem.getTags().remove(tagToRemove);
        }

        new TagDAO(userLocale, em).removeTag(pKey);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void createTag(String pWorkspaceId, String pLabel) throws WorkspaceNotFoundException, AccessRightException, CreationException, TagAlreadyExistsException, UserNotFoundException {
        User user = userManager.checkWorkspaceWriteAccess(pWorkspaceId);
        Locale userLocale = new Locale(user.getLanguage());
        TagDAO tagDAO = new TagDAO(userLocale, em);
        Tag tag = new Tag(user.getWorkspace(), pLabel);
        tagDAO.createTag(tag);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision createDocumentMaster(String pParentFolder, String pDocMId, String pTitle, String pDescription, String pDocMTemplateId, String pWorkflowModelId, ACLUserEntry[] pACLUserEntries, ACLUserGroupEntry[] pACLUserGroupEntries, Map<String, String> roleMappings) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, NotAllowedException, FolderNotFoundException, DocumentMasterTemplateNotFoundException, FileAlreadyExistsException, CreationException, DocumentRevisionAlreadyExistsException, RoleNotFoundException, WorkflowModelNotFoundException, DocumentMasterAlreadyExistsException {

        User user = userManager.checkWorkspaceWriteAccess(Folder.parseWorkspaceId(pParentFolder));
        Locale locale = new Locale(user.getLanguage());
        checkNameValidity(pDocMId, locale);

        Folder folder = new FolderDAO(locale, em).loadFolder(pParentFolder);
        checkFolderWritingRight(user, folder);

        DocumentMaster docM;
        DocumentRevision docR;
        DocumentIteration newDoc;

        DocumentMasterDAO docMDAO = new DocumentMasterDAO(new Locale(user.getLanguage()), em);
        if (pDocMTemplateId == null) {
            docM = new DocumentMaster(user.getWorkspace(), pDocMId, user);
            //specify an empty type instead of null
            //so the search will find it with the % character
            docM.setType("");
            docMDAO.createDocM(docM);
            docR = docM.createNextRevision(user);
            newDoc = docR.createNextIteration(user);
        } else {
            DocumentMasterTemplate template = new DocumentMasterTemplateDAO(new Locale(user.getLanguage()), em).loadDocMTemplate(new DocumentMasterTemplateKey(user.getWorkspaceId(), pDocMTemplateId));
            docM = new DocumentMaster(user.getWorkspace(), pDocMId, user);
            docM.setType(template.getDocumentType());
            docM.setAttributesLocked(template.isAttributesLocked());

            docMDAO.createDocM(docM);
            docR = docM.createNextRevision(user);
            newDoc = docR.createNextIteration(user);


            Map<String, InstanceAttribute> attrs = new HashMap<>();
            for (InstanceAttributeTemplate attrTemplate : template.getAttributeTemplates()) {
                InstanceAttribute attr = attrTemplate.createInstanceAttribute();
                attrs.put(attr.getName(), attr);
            }
            newDoc.setInstanceAttributes(attrs);

            BinaryResourceDAO binDAO = new BinaryResourceDAO(new Locale(user.getLanguage()), em);
            for (BinaryResource sourceFile : template.getAttachedFiles()) {
                String fileName = sourceFile.getName();
                long length = sourceFile.getContentLength();
                Date lastModified = sourceFile.getLastModified();
                String fullName = docM.getWorkspaceId() + "/documents/" + docM.getId() + "/A/1/" + fileName;
                BinaryResource targetFile = new BinaryResource(fullName, length, lastModified);
                binDAO.createBinaryResource(targetFile);

                newDoc.addFile(targetFile);
                try {
                    dataManager.copyData(sourceFile, targetFile);
                } catch (StorageException e) {
                    Logger.getLogger(DocumentManagerBean.class.getName()).log(Level.INFO, null, e);
                }
            }
        }

        if (pWorkflowModelId != null) {

            UserDAO userDAO = new UserDAO(new Locale(user.getLanguage()),em);
            RoleDAO roleDAO = new RoleDAO(new Locale(user.getLanguage()),em);

            Map<Role,User> roleUserMap = new HashMap<>();

            for (Object o : roleMappings.entrySet()) {
                Map.Entry pairs = (Map.Entry) o;
                String roleName = (String) pairs.getKey();
                String userLogin = (String) pairs.getValue();
                User worker = userDAO.loadUser(new UserKey(Folder.parseWorkspaceId(pParentFolder), userLogin));
                Role role = roleDAO.loadRole(new RoleKey(Folder.parseWorkspaceId(pParentFolder), roleName));
                roleUserMap.put(role, worker);
            }

            WorkflowModel workflowModel = new WorkflowModelDAO(new Locale(user.getLanguage()), em).loadWorkflowModel(new WorkflowModelKey(user.getWorkspaceId(), pWorkflowModelId));
            Workflow workflow = workflowModel.createWorkflow(roleUserMap);
            docR.setWorkflow(workflow);

            Collection<Task> runningTasks = workflow.getRunningTasks();
            for (Task runningTask : runningTasks) {
                runningTask.start();
            }
            mailer.sendApproval(runningTasks, docR);
        }

        docR.setTitle(pTitle);
        docR.setDescription(pDescription);

        if ((pACLUserEntries != null && pACLUserEntries.length > 0) || (pACLUserGroupEntries != null && pACLUserGroupEntries.length > 0)) {
            ACL acl = new ACL();
            if (pACLUserEntries != null) {
                for (ACLUserEntry entry : pACLUserEntries) {
                    acl.addEntry(em.getReference(User.class, new UserKey(user.getWorkspaceId(), entry.getPrincipalLogin())), entry.getPermission());
                }
            }

            if (pACLUserGroupEntries != null) {
                for (ACLUserGroupEntry entry : pACLUserGroupEntries) {
                    acl.addEntry(em.getReference(UserGroup.class, new UserGroupKey(user.getWorkspaceId(), entry.getPrincipalId())), entry.getPermission());
                }
            }
            docR.setACL(acl);
        }
        Date now = new Date();
        docM.setCreationDate(now);
        docR.setCreationDate(now);
        docR.setLocation(folder);
        docR.setCheckOutUser(user);
        docR.setCheckOutDate(now);
        newDoc.setCreationDate(now);
        DocumentRevisionDAO docRDAO = new DocumentRevisionDAO(new Locale(user.getLanguage()), em);
        docRDAO.createDocR(docR);
        return docR;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID,UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public DocumentRevision[] getAllCheckedOutDocumentRevisions(String pWorkspaceId) throws WorkspaceNotFoundException, AccountNotFoundException, AccessRightException {
        Account account = userManager.checkAdmin(pWorkspaceId);
        List<DocumentRevision> docRs = new DocumentRevisionDAO(new Locale(account.getLanguage()), em).findAllCheckedOutDocRevisions(pWorkspaceId);
        return docRs.toArray(new DocumentRevision[docRs.size()]);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentMasterTemplate createDocumentMasterTemplate(String pWorkspaceId, String pId, String pDocumentType,
            String pMask, InstanceAttributeTemplate[] pAttributeTemplates, boolean idGenerated, boolean attributesLocked) throws WorkspaceNotFoundException, AccessRightException, DocumentMasterTemplateAlreadyExistsException, UserNotFoundException, NotAllowedException, CreationException {
        User user = userManager.checkWorkspaceWriteAccess(pWorkspaceId);
        Locale locale = new Locale(user.getLanguage());
        checkNameValidity(pId, locale);

        DocumentMasterTemplate template = new DocumentMasterTemplate(user.getWorkspace(), pId, user, pDocumentType, pMask);
        Date now = new Date();
        template.setCreationDate(now);
        template.setIdGenerated(idGenerated);
        template.setAttributesLocked(attributesLocked);

        Set<InstanceAttributeTemplate> attrs = new HashSet<>();
        Collections.addAll(attrs, pAttributeTemplates);
        template.setAttributeTemplates(attrs);

        new DocumentMasterTemplateDAO(locale, em).createDocMTemplate(template);
        return template;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision moveDocumentRevision(String pParentFolder, DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, DocumentRevisionNotFoundException, NotAllowedException, AccessRightException, FolderNotFoundException, UserNotFoundException, UserNotActiveException {
        //TODO security check if both parameter belong to the same workspace
        User user = checkDocumentRevisionWriteAccess(pDocRPK);

        Folder newLocation = new FolderDAO(new Locale(user.getLanguage()), em).loadFolder(pParentFolder);
        checkFolderWritingRight(user, newLocation);
        DocumentRevisionDAO docRDAO = new DocumentRevisionDAO(new Locale(user.getLanguage()), em);
        DocumentRevision docR = docRDAO.loadDocR(pDocRPK);

        if(isInAnotherUserHomeFolder(user,docR)) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException6");
        } else {
            docR.setLocation(newLocation);
            if ((docR.isCheckedOut()) && (!docR.getCheckOutUser().equals(user))) {
                em.detach(docR);
                docR.removeLastIteration();
            }
            return docR;
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public Folder createFolder(String pParentFolder, String pFolder)
            throws WorkspaceNotFoundException, NotAllowedException, AccessRightException, FolderNotFoundException, FolderAlreadyExistsException, UserNotFoundException, CreationException {
        User user = userManager.checkWorkspaceWriteAccess(Folder.parseWorkspaceId(pParentFolder));
        Locale locale = new Locale(user.getLanguage());
        checkNameValidity(pFolder, locale);

        FolderDAO folderDAO = new FolderDAO(locale, em);
        Folder folder = folderDAO.loadFolder(pParentFolder);
        checkFoldersStructureChangeRight(user);
        checkFolderWritingRight(user, folder);
        Folder newFolder = new Folder(pParentFolder, pFolder);
        folderDAO.createFolder(newFolder);
        return newFolder;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision checkOutDocument(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, AccessRightException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException {
        User user = checkDocumentRevisionWriteAccess(pDocRPK);
        DocumentRevisionDAO docRDAO = new DocumentRevisionDAO(new Locale(user.getLanguage()), em);
        DocumentRevision docR = docRDAO.loadDocR(pDocRPK);
        //Check access rights on docR

        if (isInAnotherUserHomeFolder(user, docR)) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException5");
        }

        if (docR.isCheckedOut()) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException37");
        }

        DocumentIteration beforeLastDocument = docR.getLastIteration();

        DocumentIteration newDoc = docR.createNextIteration(user);
        //We persist the doc as a workaround for a bug which was introduced
        //since glassfish 3 that set the DTYPE to null in the instance attribute table
        em.persist(newDoc);
        Date now = new Date();
        newDoc.setCreationDate(now);
        docR.setCheckOutUser(user);
        docR.setCheckOutDate(now);

        if (beforeLastDocument != null) {
            BinaryResourceDAO binDAO = new BinaryResourceDAO(new Locale(user.getLanguage()), em);
            for (BinaryResource sourceFile : beforeLastDocument.getAttachedFiles()) {
                String fileName = sourceFile.getName();
                long length = sourceFile.getContentLength();
                Date lastModified = sourceFile.getLastModified();
                String fullName = docR.getWorkspaceId() + "/documents/" + docR.getId() + "/" + docR.getVersion() + "/" + newDoc.getIteration() + "/" + fileName;
                BinaryResource targetFile = new BinaryResource(fullName, length, lastModified);
                binDAO.createBinaryResource(targetFile);
                newDoc.addFile(targetFile);
            }

            Set<DocumentLink> links = new HashSet<>();
            for (DocumentLink link : beforeLastDocument.getLinkedDocuments()) {
                DocumentLink newLink = link.clone();
                links.add(newLink);
            }
            newDoc.setLinkedDocuments(links);

            InstanceAttributeDAO attrDAO = new InstanceAttributeDAO(em);
            Map<String, InstanceAttribute> attrs = new HashMap<>();
            for (InstanceAttribute attr : beforeLastDocument.getInstanceAttributes().values()) {
                InstanceAttribute newAttr = attr.clone();
                //Workaround for the NULL DTYPE bug
                attrDAO.createAttribute(newAttr);
                attrs.put(newAttr.getName(), newAttr);
            }
            newDoc.setInstanceAttributes(attrs);
        }

        return docR;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision saveTags(DocumentRevisionKey pDocRPK, String[] pTags) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException, ESServerException {
        User user = checkDocumentRevisionWriteAccess(pDocRPK);

        Locale userLocale = new Locale(user.getLanguage());
        DocumentRevisionDAO docRDAO = new DocumentRevisionDAO(userLocale, em);
        DocumentRevision docR = docRDAO.loadDocR(pDocRPK);
        if (isInAnotherUserHomeFolder(user, docR)) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException5");
        }

        Set<Tag> tags = new HashSet<>();
        for (String label : pTags) {
            tags.add(new Tag(user.getWorkspace(), label));
        }

        TagDAO tagDAO = new TagDAO(userLocale, em);
        List<Tag> existingTags = Arrays.asList(tagDAO.findAllTags(user.getWorkspaceId()));

        Set<Tag> tagsToCreate = new HashSet<>(tags);
        tagsToCreate.removeAll(existingTags);

        for (Tag t : tagsToCreate) {
            try {
                tagDAO.createTag(t);
            } catch (CreationException | TagAlreadyExistsException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }

        docR.setTags(tags);

        if ((docR.isCheckedOut()) && (!docR.getCheckOutUser().equals(user))) {
            em.detach(docR);
            docR.removeLastIteration();
        }

        for (DocumentIteration documentIteration:docR.getDocumentIterations()){
            esIndexer.index(documentIteration);
        }
        return docR;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision removeTag(DocumentRevisionKey pDocRPK, String pTag)
            throws UserNotFoundException, WorkspaceNotFoundException, UserNotActiveException, AccessRightException, DocumentRevisionNotFoundException, NotAllowedException, ESServerException {

        User user = checkDocumentRevisionWriteAccess(pDocRPK);

        DocumentRevision docR = getDocumentRevision(pDocRPK);
        Tag tagToRemove = new Tag(user.getWorkspace(), pTag);
        docR.getTags().remove(tagToRemove);

        if (isCheckoutByAnotherUser(user,docR)) {
            em.detach(docR);
            docR.removeLastIteration();
        }

        for (DocumentIteration documentIteration:docR.getDocumentIterations()){
            esIndexer.index(documentIteration);
        }
        return docR;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision undoCheckOutDocument(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, DocumentRevisionNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, AccessRightException {

        User user = checkDocumentRevisionWriteAccess(pDocRPK);

        DocumentRevisionDAO docRDAO = new DocumentRevisionDAO(new Locale(user.getLanguage()), em);
        DocumentRevision docR = docRDAO.loadDocR(pDocRPK);
        if (docR.isCheckedOut() && docR.getCheckOutUser().equals(user)) {
            if(docR.getLastIteration().getIteration() <= 1) {
                throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException27");
            }
            DocumentIteration doc = docR.removeLastIteration();
            for (BinaryResource file : doc.getAttachedFiles()) {
                try {
                    dataManager.deleteData(file);
                } catch (StorageException e) {
                    Logger.getLogger(DocumentManagerBean.class.getName()).log(Level.INFO, null, e);
                }
            }

            DocumentDAO docDAO = new DocumentDAO(em);
            docDAO.removeDoc(doc);
            docR.setCheckOutDate(null);
            docR.setCheckOutUser(null);
            return docR;
        } else {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException19");
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision checkInDocument(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException, ESServerException {

        User user = checkDocumentRevisionWriteAccess(pDocRPK);

        DocumentRevisionDAO docRDAO = new DocumentRevisionDAO(new Locale(user.getLanguage()), em);
        DocumentRevision docR = docRDAO.loadDocR(pDocRPK);

        if (isCheckoutByUser(user,docR)) {
            SubscriptionDAO subscriptionDAO = new SubscriptionDAO(em);
            User[] subscribers = subscriptionDAO.getIterationChangeEventSubscribers(docR);
            GCMAccount[] gcmAccounts = subscriptionDAO.getIterationChangeEventSubscribersGCMAccount(docR);

            docR.setCheckOutDate(null);
            docR.setCheckOutUser(null);

            if (subscribers.length != 0) {
                mailer.sendIterationNotification(subscribers, docR);
            }

            if (gcmAccounts.length != 0) {
                gcmNotifier.sendIterationNotification(gcmAccounts, docR);
            }

            for(DocumentIteration docIteration : docR.getDocumentIterations()){
                esIndexer.index(docIteration);                                                                          // Index all iterations in ElasticSearch (decrease old iteration boost factor)
            }

            return docR;
        } else {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException20");
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevisionKey[] deleteFolder(String pCompletePath) throws WorkspaceNotFoundException, NotAllowedException, AccessRightException, UserNotFoundException, FolderNotFoundException, ESServerException {
        User user = userManager.checkWorkspaceWriteAccess(Folder.parseWorkspaceId(pCompletePath));
        FolderDAO folderDAO = new FolderDAO(new Locale(user.getLanguage()), em);
        Folder folder = folderDAO.loadFolder(pCompletePath);
        checkFoldersStructureChangeRight(user);
        if (isAnotherUserHomeFolder(user, folder) || (folder.isRoot()) || folder.isHome()) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException21");
        } else {
            List<DocumentRevision> docRs = folderDAO.removeFolder(folder);
            DocumentRevisionKey[] pks = new DocumentRevisionKey[docRs.size()];
            int i = 0;
            for (DocumentRevision docR : docRs) {
                pks[i++] = docR.getKey();
                for (DocumentIteration doc : docR.getDocumentIterations()) {
                    for (BinaryResource file : doc.getAttachedFiles()) {
                        //indexer.removeFromIndex(file.getFullName());
                        try {
                            dataManager.deleteData(file);
                        } catch (StorageException e) {
                            Logger.getLogger(DocumentManagerBean.class.getName()).log(Level.INFO, null, e);
                        }
                    }
                    esIndexer.delete(doc);
                    // Remove ElasticSearch Index for this DocumentIteration
                }
            }
            return pks;
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevisionKey[] moveFolder(String pCompletePath, String pDestParentFolder, String pDestFolder) throws WorkspaceNotFoundException, NotAllowedException, AccessRightException, UserNotFoundException, FolderNotFoundException, CreationException, FolderAlreadyExistsException {
        //TODO security check if both parameter belong to the same workspace
        String workspace = Folder.parseWorkspaceId(pCompletePath);
        User user = userManager.checkWorkspaceWriteAccess(workspace);
        FolderDAO folderDAO = new FolderDAO(new Locale(user.getLanguage()), em);
        Folder folder = folderDAO.loadFolder(pCompletePath);
        checkFoldersStructureChangeRight(user);
        if (isAnotherUserHomeFolder(user, folder) || (folder.isRoot()) || folder.isHome()) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException21");
        } else if (!workspace.equals(Folder.parseWorkspaceId(pDestParentFolder))) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException23");
        } else {
            Folder newFolder = createFolder(pDestParentFolder, pDestFolder);
            List<DocumentRevision> docRs = folderDAO.moveFolder(folder, newFolder);
            DocumentRevisionKey[] pks = new DocumentRevisionKey[docRs.size()];
            int i = 0;
            for (DocumentRevision docR : docRs) {
                pks[i++] = docR.getKey();
            }
            return pks;
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deleteDocumentRevision(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException, ESServerException, EntityConstraintException {

        User user = checkDocumentRevisionWriteAccess(pDocRPK);
        DocumentMasterDAO documentMasterDAO = new DocumentMasterDAO(new Locale(user.getLanguage()), em);
        DocumentRevisionDAO docRDAO = new DocumentRevisionDAO(new Locale(user.getLanguage()), em);
        BaselinedDocumentDAO baselinedDocumentDAO = new BaselinedDocumentDAO(new Locale(user.getLanguage()), em);
        DocumentRevision docR = docRDAO.loadDocR(pDocRPK);
        DocumentMaster documentMaster = docR.getDocumentMaster();
        boolean isLastRevision = documentMaster.getDocumentRevisions().size() == 1;

        if (isInAnotherUserHomeFolder(user, docR)) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException22");
        }

        if(baselinedDocumentDAO.hasPartRevision(pDocRPK.getDocumentMasterId(),pDocRPK.getVersion())) {
            throw new EntityConstraintException(new Locale(user.getLanguage()),"EntityConstraintException6");
        }

        for (DocumentIteration doc : docR.getDocumentIterations()) {
            for (BinaryResource file : doc.getAttachedFiles()) {
                try {
                    dataManager.deleteData(file);
                } catch (StorageException e) {
                    Logger.getLogger(DocumentManagerBean.class.getName()).log(Level.INFO, null, e);
                }

                esIndexer.delete(doc);
                // Remove ElasticSearch Index for this DocumentIteration
            }
        }

        if (isLastRevision) {
            documentMasterDAO.removeDocM(documentMaster);
        } else {
            documentMaster.removeRevision(docR);
            docRDAO.removeRevision(docR);
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void deleteDocumentMasterTemplate(DocumentMasterTemplateKey pKey)
            throws WorkspaceNotFoundException, AccessRightException, DocumentMasterTemplateNotFoundException, UserNotFoundException {
        User user = userManager.checkWorkspaceWriteAccess(pKey.getWorkspaceId());
        DocumentMasterTemplateDAO templateDAO = new DocumentMasterTemplateDAO(new Locale(user.getLanguage()), em);
        DocumentMasterTemplate template = templateDAO.removeDocMTemplate(pKey);
        for (BinaryResource file : template.getAttachedFiles()) {
            try {
                dataManager.deleteData(file);
            } catch (StorageException e) {
                Logger.getLogger(DocumentManagerBean.class.getName()).log(Level.INFO, null, e);
            }
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision removeFileFromDocument(String pFullName) throws WorkspaceNotFoundException, DocumentRevisionNotFoundException, NotAllowedException, AccessRightException, FileNotFoundException, UserNotFoundException, UserNotActiveException {
        User user = userManager.checkWorkspaceReadAccess(BinaryResource.parseWorkspaceId(pFullName));

        BinaryResourceDAO binDAO = new BinaryResourceDAO(new Locale(user.getLanguage()), em);
        BinaryResource file = binDAO.loadBinaryResource(pFullName);

        DocumentIteration document = binDAO.getDocumentOwner(file);
        DocumentRevision docR = document.getDocumentRevision();

        //check access rights on docR
        user = checkDocumentRevisionWriteAccess(docR.getKey());

        if (isCheckoutByUser(user,docR) && docR.getLastIteration().equals(document)) {
            try {
                dataManager.deleteData(file);
            } catch (StorageException e) {
                Logger.getLogger(DocumentManagerBean.class.getName()).log(Level.INFO, null, e);
            }
            document.removeFile(file);
            binDAO.removeBinaryResource(file);
            return docR;
        } else {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException24");
        }
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentMasterTemplate removeFileFromTemplate(String pFullName) throws WorkspaceNotFoundException, DocumentMasterTemplateNotFoundException, AccessRightException, FileNotFoundException, UserNotFoundException, UserNotActiveException {
        User user = userManager.checkWorkspaceWriteAccess(BinaryResource.parseWorkspaceId(pFullName));

        BinaryResourceDAO binDAO = new BinaryResourceDAO(new Locale(user.getLanguage()), em);
        BinaryResource file = binDAO.loadBinaryResource(pFullName);

        DocumentMasterTemplate template = binDAO.getDocumentTemplateOwner(file);
        try {
            dataManager.deleteData(file);
        } catch (StorageException e) {
            Logger.getLogger(DocumentManagerBean.class.getName()).log(Level.INFO, null, e);
        }
        template.removeFile(file);
        binDAO.removeBinaryResource(file);
        return template;
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision updateDocument(DocumentIterationKey iKey, String pRevisionNote, InstanceAttribute[] pAttributes, DocumentIterationKey[] pLinkKeys) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, AccessRightException, UserNotFoundException, UserNotActiveException {
        DocumentRevisionKey rKey = new DocumentRevisionKey(iKey.getWorkspaceId(), iKey.getDocumentMasterId(), iKey.getDocumentRevisionVersion());
        User user = checkDocumentRevisionWriteAccess(rKey);

        DocumentRevisionDAO docRDAO = new DocumentRevisionDAO(new Locale(user.getLanguage()), em);
        DocumentLinkDAO linkDAO = new DocumentLinkDAO(new Locale(user.getLanguage()), em);
        DocumentRevision docR = docRDAO.loadDocR(rKey);
        //check access rights on docR ?
        if (isCheckoutByUser(user,docR) && docR.getLastIteration().getKey().equals(iKey)) {
            DocumentIteration doc = docR.getLastIteration();

            Set<DocumentIterationKey> linkKeys = new HashSet<>(Arrays.asList(pLinkKeys));
            Set<DocumentIterationKey> currentLinkKeys = new HashSet<>();

            Set<DocumentLink> currentLinks = new HashSet<>(doc.getLinkedDocuments());

            for(DocumentLink link:currentLinks){
                DocumentIterationKey linkKey = link.getTargetDocumentKey();
                if(!linkKeys.contains(linkKey)){
                    doc.getLinkedDocuments().remove(link);
                }else {
                    currentLinkKeys.add(linkKey);
                }
            }

            for(DocumentIterationKey link:linkKeys){
                if(!currentLinkKeys.contains(link)){
                    DocumentLink newLink = new DocumentLink(em.getReference(DocumentIteration.class,link));
                    linkDAO.createLink(newLink);
                    doc.getLinkedDocuments().add(newLink);
                }
            }

            
            Map<String, InstanceAttribute> attrs = new HashMap<>();
            for (InstanceAttribute attr : pAttributes) {
                attrs.put(attr.getName(), attr);
            }

            Set<InstanceAttribute> currentAttrs = new HashSet<>(doc.getInstanceAttributes().values());

            for(InstanceAttribute attr:currentAttrs){
                if(!attrs.containsKey(attr.getName())){
                    doc.getInstanceAttributes().remove(attr.getName());
                }
            }

            for(InstanceAttribute attr:attrs.values()){
                if(!doc.getInstanceAttributes().containsKey(attr.getName())){
                    doc.getInstanceAttributes().put(attr.getName(), attr);
                }else if(doc.getInstanceAttributes().get(attr.getName()).getClass() != attr.getClass()){
                    doc.getInstanceAttributes().remove(attr.getName());
                    doc.getInstanceAttributes().put(attr.getName(), attr);
                }else{
                    doc.getInstanceAttributes().get(attr.getName()).setValue(attr.getValue());
                }
            }

            doc.setRevisionNote(pRevisionNote);
            //doc.setLinkedDocuments(links);
            return docR;

        } else {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException25");
        }

    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public DocumentRevision[] createDocumentRevision(DocumentRevisionKey pOriginalDocRPK, String pTitle, String pDescription, String pWorkflowModelId, ACLUserEntry[] pACLUserEntries, ACLUserGroupEntry[] pACLUserGroupEntries, Map<String,String> roleMappings) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, NotAllowedException, DocumentRevisionAlreadyExistsException, CreationException, WorkflowModelNotFoundException, RoleNotFoundException, DocumentRevisionNotFoundException, FileAlreadyExistsException {
        User user = userManager.checkWorkspaceWriteAccess(pOriginalDocRPK.getWorkspaceId());
        DocumentRevisionDAO docRDAO = new DocumentRevisionDAO(new Locale(user.getLanguage()), em);
        DocumentRevision originalDocR = docRDAO.loadDocR(pOriginalDocRPK);
        DocumentMaster docM = originalDocR.getDocumentMaster();
        Folder folder = originalDocR.getLocation();
        checkFolderWritingRight(user, folder);

        if (originalDocR.isCheckedOut()) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException26");
        }

        if (originalDocR.getNumberOfIterations() == 0) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException27");
        }

        DocumentRevision docR = docM.createNextRevision(user);

        //create the first iteration which is a copy of the last one of the original docR
        //of course we duplicate the iteration only if it exists !
        DocumentIteration lastDoc = originalDocR.getLastIteration();
        DocumentIteration firstIte = docR.createNextIteration(user);
        if (lastDoc != null) {
            BinaryResourceDAO binDAO = new BinaryResourceDAO(new Locale(user.getLanguage()), em);
            for (BinaryResource sourceFile : lastDoc.getAttachedFiles()) {
                String fileName = sourceFile.getName();
                long length = sourceFile.getContentLength();
                Date lastModified = sourceFile.getLastModified();
                String fullName = docR.getWorkspaceId() + "/documents/" + docR.getId() + "/" + docR.getVersion() + "/1/" + fileName;
                BinaryResource targetFile = new BinaryResource(fullName, length, lastModified);
                binDAO.createBinaryResource(targetFile);
                firstIte.addFile(targetFile);
                try {
                    dataManager.copyData(sourceFile, targetFile);
                } catch (StorageException e) {
                    Logger.getLogger(DocumentManagerBean.class.getName()).log(Level.INFO, null, e);
                }
            }

            Set<DocumentLink> links = new HashSet<>();
            for (DocumentLink link : lastDoc.getLinkedDocuments()) {
                DocumentLink newLink = link.clone();
                links.add(newLink);
            }
            firstIte.setLinkedDocuments(links);

            Map<String, InstanceAttribute> attrs = new HashMap<>();
            for (InstanceAttribute attr : lastDoc.getInstanceAttributes().values()) {
                InstanceAttribute clonedAttribute = attr.clone();
                attrs.put(clonedAttribute.getName(), clonedAttribute);
            }
            firstIte.setInstanceAttributes(attrs);
        }

        if (pWorkflowModelId != null) {

            UserDAO userDAO = new UserDAO(new Locale(user.getLanguage()),em);
            RoleDAO roleDAO = new RoleDAO(new Locale(user.getLanguage()),em);

            Map<Role,User> roleUserMap = new HashMap<>();

            for (Object o : roleMappings.entrySet()) {
                Map.Entry pairs = (Map.Entry) o;
                String roleName = (String) pairs.getKey();
                String userLogin = (String) pairs.getValue();
                User worker = userDAO.loadUser(new UserKey(pOriginalDocRPK.getWorkspaceId(), userLogin));
                Role role = roleDAO.loadRole(new RoleKey(pOriginalDocRPK.getWorkspaceId(), roleName));
                roleUserMap.put(role, worker);
            }

            WorkflowModel workflowModel = new WorkflowModelDAO(new Locale(user.getLanguage()), em).loadWorkflowModel(new WorkflowModelKey(user.getWorkspaceId(), pWorkflowModelId));
            Workflow workflow = workflowModel.createWorkflow(roleUserMap);
            docR.setWorkflow(workflow);

            Collection<Task> runningTasks = workflow.getRunningTasks();
            for (Task runningTask : runningTasks) {
                runningTask.start();
            }
            mailer.sendApproval(runningTasks, docR);
        }
        docR.setTitle(pTitle);
        docR.setDescription(pDescription);
        if ((pACLUserEntries != null && pACLUserEntries.length > 0) || (pACLUserGroupEntries != null && pACLUserGroupEntries.length > 0)) {
            ACL acl = new ACL();
            if (pACLUserEntries != null) {
                for (ACLUserEntry entry : pACLUserEntries) {
                    acl.addEntry(em.getReference(User.class, new UserKey(user.getWorkspaceId(), entry.getPrincipalLogin())), entry.getPermission());
                }
            }

            if (pACLUserGroupEntries != null) {
                for (ACLUserGroupEntry entry : pACLUserGroupEntries) {
                    acl.addEntry(em.getReference(UserGroup.class, new UserGroupKey(user.getWorkspaceId(), entry.getPrincipalId())), entry.getPermission());
                }
            }
            docR.setACL(acl);
        }
        Date now = new Date();
        docR.setCreationDate(now);
        docR.setLocation(folder);
        docR.setCheckOutUser(user);
        docR.setCheckOutDate(now);
        firstIte.setCreationDate(now);

        docRDAO.createDocR(docR);
        return new DocumentRevision[]{originalDocR, docR};
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void subscribeToStateChangeEvent(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException {
        User user = checkDocumentRevisionReadAccess(pDocRPK);
        DocumentRevision docR = new DocumentRevisionDAO(new Locale(user.getLanguage()), em).loadDocR(pDocRPK);
        if (isAnotherUserHomeFolder(user, docR.getLocation())) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException30");
        }

        new SubscriptionDAO(em).createStateChangeSubscription(new StateChangeSubscription(user, docR));
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void unsubscribeToStateChangeEvent(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException, DocumentRevisionNotFoundException {
        User user = checkDocumentRevisionReadAccess(pDocRPK);
        SubscriptionKey key = new SubscriptionKey(user.getWorkspaceId(), user.getLogin(), pDocRPK.getDocumentMaster().getWorkspace(), pDocRPK.getDocumentMaster().getId(), pDocRPK.getVersion());
        new SubscriptionDAO(em).removeStateChangeSubscription(key);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void subscribeToIterationChangeEvent(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, NotAllowedException, DocumentRevisionNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException {
        User user = checkDocumentRevisionReadAccess(pDocRPK);
        DocumentRevision docR = new DocumentRevisionDAO(new Locale(user.getLanguage()), em).getDocRRef(pDocRPK);
        if (isAnotherUserHomeFolder(user,docR.getLocation())) {
            throw new NotAllowedException(new Locale(user.getLanguage()), "NotAllowedException30");
        }

        new SubscriptionDAO(em).createIterationChangeSubscription(new IterationChangeSubscription(user, docR));
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public void unsubscribeToIterationChangeEvent(DocumentRevisionKey pDocRPK) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, AccessRightException, DocumentRevisionNotFoundException {
        User user = checkDocumentRevisionReadAccess(pDocRPK);
        SubscriptionKey key = new SubscriptionKey(user.getWorkspaceId(), user.getLogin(), pDocRPK.getDocumentMaster().getWorkspace(), pDocRPK.getDocumentMaster().getId(), pDocRPK.getVersion());
        new SubscriptionDAO(em).removeIterationChangeSubscription(key);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public User savePersonalInfo(String pWorkspaceId, String pName, String pEmail, String pLanguage) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        user.setName(pName);
        user.setEmail(pEmail);
        user.setLanguage(pLanguage);
        return user;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID,UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public User[] getUsers(String pWorkspaceId) throws WorkspaceNotFoundException, AccessRightException, AccountNotFoundException, UserNotFoundException, UserNotActiveException {
        if(userManager.isCallerInRole(UserGroupMapping.ADMIN_ROLE_ID)){
            Account account = new AccountDAO(em).loadAccount(ctx.getCallerPrincipal().toString());
            return new UserDAO(new Locale(account.getLanguage()), em).findAllUsers(pWorkspaceId);
        }
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        return new UserDAO(new Locale(user.getLanguage()), em).findAllUsers(pWorkspaceId);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public User[] getReachableUsers(String workspaceId) throws AccountNotFoundException {
        String callerLogin = ctx.getCallerPrincipal().getName();
        Account account = new AccountDAO(em).loadAccount(callerLogin);
        return new UserDAO(new Locale(account.getLanguage()), em).findReachableUsersForCaller(callerLogin, workspaceId);
    }

    @RolesAllowed(UserGroupMapping.REGULAR_USER_ROLE_ID)
    @Override
    public String[] getTags(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException {
        User user = userManager.checkWorkspaceReadAccess(pWorkspaceId);
        Tag[] tags = new TagDAO(new Locale(user.getLanguage()), em).findAllTags(pWorkspaceId);

        String[] labels = new String[tags.length];
        int i = 0;
        for (Tag t : tags) {
            labels[i++] = t.getLabel();
        }
        return labels;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID,UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public long getDiskUsageForDocumentsInWorkspace(String pWorkspaceId) throws WorkspaceNotFoundException, AccessRightException, AccountNotFoundException {
        Account account = userManager.checkAdmin(pWorkspaceId);
        DocumentRevisionDAO documentRevisionDAO = new DocumentRevisionDAO(new Locale(account.getLanguage()),em);
        return documentRevisionDAO.getDiskUsageForDocumentsInWorkspace(pWorkspaceId);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID,UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public long getDiskUsageForDocumentTemplatesInWorkspace(String pWorkspaceId) throws WorkspaceNotFoundException, AccessRightException, AccountNotFoundException {
        Account account = userManager.checkAdmin(pWorkspaceId);
        DocumentRevisionDAO documentRevisionDAO = new DocumentRevisionDAO(new Locale(account.getLanguage()),em);
        return documentRevisionDAO.getDiskUsageForDocumentTemplatesInWorkspace(pWorkspaceId);
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public SharedDocument createSharedDocument(DocumentRevisionKey pDocRPK, String pPassword, Date pExpireDate) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, DocumentRevisionNotFoundException, UserNotActiveException, NotAllowedException {
        User user = userManager.checkWorkspaceWriteAccess(pDocRPK.getDocumentMaster().getWorkspace());
        SharedDocument sharedDocument = new SharedDocument(user.getWorkspace(), user, pExpireDate, pPassword, getDocumentRevision(pDocRPK));
        SharedEntityDAO sharedEntityDAO = new SharedEntityDAO(new Locale(user.getLanguage()),em);
        sharedEntityDAO.createSharedDocument(sharedDocument);
        return sharedDocument;
    }

    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID})
    @Override
    public void deleteSharedDocument(SharedEntityKey sharedEntityKey) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, SharedEntityNotFoundException {
        User user = userManager.checkWorkspaceWriteAccess(sharedEntityKey.getWorkspace());
        SharedEntityDAO sharedEntityDAO = new SharedEntityDAO(new Locale(user.getLanguage()),em);
        SharedDocument sharedDocument = sharedEntityDAO.loadSharedDocument(sharedEntityKey.getUuid());
        sharedEntityDAO.deleteSharedDocument(sharedDocument);
    }



    @RolesAllowed({UserGroupMapping.REGULAR_USER_ROLE_ID,UserGroupMapping.ADMIN_ROLE_ID})
    @Override
    public boolean canAccess(User user, DocumentRevisionKey docRKey) throws DocumentRevisionNotFoundException {
        DocumentRevision docRevision = new DocumentRevisionDAO(new Locale(user.getLanguage()), em).loadDocR(docRKey);
        return hasDocumentRevisionReadAccess(user,docRevision);
    }


    /**
     * Apply read access policy on a document revision
     * @param user The user to test
     * @param documentRevision The document revision to test
     * @return The readable document revision or null if the user has no access to it.
     */
    private DocumentRevision applyDocumentRevisionReadAccess(User user, DocumentRevision documentRevision){
        if(hasDocumentRevisionReadAccess(user, documentRevision)){
            if(!isCheckoutByAnotherUser(user, documentRevision)){
                return documentRevision;
            }
            em.detach(documentRevision);
            documentRevision.removeLastIteration();
            return documentRevision;
        }
        return null;
    }


    /**
     * Check if the current account have read access on a document revision.
     * @param documentRevisionKey The key of the document revision.
     * @return The user if he has read access to the document revision.
     * @throws UserNotFoundException If there are any User matching the Workspace and the current account login.
     * @throws UserNotActiveException If the user is not actif.
     * @throws WorkspaceNotFoundException If the workspace doesn't exist.
     * @throws AccessRightException If the user has no read access to the document revision.
     * @throws DocumentRevisionNotFoundException If the document revision doesn't exist.
     */
    private User checkDocumentRevisionReadAccess(DocumentRevisionKey documentRevisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, DocumentRevisionNotFoundException {
        User user = userManager.checkWorkspaceReadAccess(documentRevisionKey.getDocumentMaster().getWorkspace());
        DocumentRevision documentRevision = new DocumentRevisionDAO(em).loadDocR(documentRevisionKey);

        if(!hasDocumentRevisionReadAccess(user, documentRevision)){
            throw new AccessRightException(new Locale(user.getLanguage()),user);
        }

        return user;
    }
    /**
     * Check if the current account user have write access on a document revision.
     * @param documentRevisionKey The key of the document revision.
     * @return The user if he has write access to the document revision.
     * @throws UserNotFoundException If there are any User matching the Workspace and the current account login.
     * @throws UserNotActiveException If the user is not actif.
     * @throws WorkspaceNotFoundException If the workspace doesn't exist.
     * @throws AccessRightException If the user has no write access to the document revision.
     * @throws DocumentRevisionNotFoundException If the document revision doesn't exist.
     */
    private User checkDocumentRevisionWriteAccess(DocumentRevisionKey documentRevisionKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, AccessRightException, DocumentRevisionNotFoundException {
        User user = userManager.checkWorkspaceReadAccess(documentRevisionKey.getDocumentMaster().getWorkspace());
        if(user.isAdministrator()){
            return user;
        }
        DocumentRevision documentRevision = new DocumentRevisionDAO(em).loadDocR(documentRevisionKey);

        if(documentRevision.getACL()==null){
            return userManager.checkWorkspaceWriteAccess(documentRevisionKey.getDocumentMaster().getWorkspace());
        }else if(documentRevision.getACL().hasWriteAccess(user)){
            return user;
        }else{
            throw new AccessRightException(new Locale(user.getLanguage()),user);
        }
    }


    /**
     * Check if a user, which have access to the workspace, have the right of change the folder structure.
     * @param pUser A user which have read access to the workspace.
     * @throws NotAllowedException If access is deny.
     */
    private void checkFoldersStructureChangeRight(User pUser) throws NotAllowedException {
        Workspace wks = pUser.getWorkspace();
        if (wks.isFolderLocked() && (!pUser.isAdministrator())) {
            throw new NotAllowedException(new Locale(pUser.getLanguage()), "NotAllowedException7");
        }
    }
    /**
     * Check if a user, which have access to the workspace, have write access in a folder
     * @param pUser A user which have read access to the workspace
     * @param pFolder The folder wanted
     * @return The folder access is granted.
     * @throws NotAllowedException If the folder access is deny.
     */
    private Folder checkFolderWritingRight(User pUser, Folder pFolder) throws NotAllowedException {
        if(isAnotherUserHomeFolder(pUser,pFolder)) {
            throw new NotAllowedException(new Locale(pUser.getLanguage()), "NotAllowedException33");
        }
        return pFolder;
    }


    /**
     * Say if a user, which have access to the workspace, have read access to a document revision
     * @param user A user which have read access to the workspace
     * @param documentRevision The document revision wanted
     * @return True if access is granted, False otherwise
     */
    private boolean hasDocumentRevisionReadAccess(User user, DocumentRevision documentRevision){
        return isInSameWorkspace(user, documentRevision) &&
                (user.isAdministrator() || isACLGrantReadAccess(user,documentRevision)) &&
                !isInAnotherUserHomeFolder(user, documentRevision);
    }
    /**
     * Say if a user, which have access to the workspace, have write access to a document revision
     * @param user A user which have read access to the workspace
     * @param documentRevision The document revision wanted
     * @return True if access is granted, False otherwise
     */
    private boolean hasDocumentRevisionWriteAccess(User user, DocumentRevision documentRevision){
        return isInSameWorkspace(user, documentRevision) &&
                (user.isAdministrator() || isACLGrantWriteAccess(user,documentRevision)) &&
                !isInAnotherUserHomeFolder(user, documentRevision);
    }


    private boolean isInSameWorkspace(User user, DocumentRevision documentRevision){
        return user.getWorkspaceId().equals(documentRevision.getWorkspaceId());
    }
    private boolean isAuthor(User user, DocumentRevision documentRevision){
        return documentRevision.getAuthor().getLogin().equals(user.getLogin());
    }
    private boolean isACLGrantReadAccess(User user, DocumentRevision documentRevision){
        return documentRevision.getACL()==null || documentRevision.getACL().hasReadAccess(user);
    }
    private boolean isACLGrantWriteAccess(User user, DocumentRevision documentRevision){
        return documentRevision.getACL()==null || documentRevision.getACL().hasWriteAccess(user);
    }
    private boolean isAnotherUserHomeFolder(User user, Folder folder){
        return folder.isPrivate() && !folder.getOwner().equals(user.getLogin());
    }
    private boolean isInAnotherUserHomeFolder(User user, DocumentRevision documentRevision){
        return isAnotherUserHomeFolder(user,documentRevision.getLocation());
    }
    private boolean isCheckoutByUser(User user, DocumentRevision documentRevision){
        return documentRevision.isCheckedOut() && documentRevision.getCheckOutUser().equals(user);
    }
    private boolean isCheckoutByAnotherUser(User user, DocumentRevision documentRevision){
        return documentRevision.isCheckedOut() && !documentRevision.getCheckOutUser().equals(user);
    }


    private void checkNameValidity(String name, Locale locale) throws NotAllowedException {
        if (!NamingConvention.correct(name)) {
            throw new NotAllowedException(locale, "NotAllowedException9");
        }
    }
    private void checkNameFileValidity(String name, Locale locale) throws NotAllowedException {
        if (!NamingConvention.correctNameFile(name)) {
            throw new NotAllowedException(locale, "NotAllowedException9");
        }
    }
}