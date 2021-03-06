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

import com.docdoku.core.document.DocumentIterationKey;
import com.docdoku.core.meta.InstanceAttribute;
import com.docdoku.core.meta.InstanceAttributeTemplate;
import com.docdoku.core.product.*;

import java.io.File;
import javax.annotation.security.RolesAllowed;
import javax.jws.WebService;
import java.util.List;


/**
 * The product service which is the entry point for the API related to products
 * definition and manipulation. The client of these functions must
 * be authenticated and have read or write access rights on the workspace
 * where the operations occur.
 * 
 * @author Florent Garin
 * @version 1.1, 03/10/12
 * @since   V1.1
 */
@WebService
public interface IProductManagerWS{
    
    /**
     * Searchs all instances of a part and returns their paths, defined by a
     * serie of usage links, from the top of the structure to their own usage
     * link.
     * 
     * @param ciKey
     * The configuration item under which context the search is made
     * 
     * @param partMKey
     * The id of the part master to search on the structure
     * 
     * 
     * @return
     * The usage paths to all instances of the supplied part
     * 
     * @throws WorkspaceNotFoundException
     * @throws UserNotFoundException
     * @throws UserNotActiveException
     */
    List<PartUsageLink[]> findPartUsages(ConfigurationItemKey ciKey, PartMasterKey partMKey) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;
    
    /**
     * Resolves the product structure identified by the supplied
     * <a href="ConfigurationItemKey.html">ConfigurationItemKey</a>.
     * The resolution is made according to the given
     * <a href="ConfigSpec.html">ConfigSpec</a> and starts at the specified
     * part usage link if any.
     * 
     * @param ciKey
     * The product structure to resolve
     * 
     * @param configSpec
     * The rules for the resolution algorithm
     * 
     * @param partUsageLink
     * The part usage link id, if null starts from the root part
     * 
     * @param depth
     * The fetch depth
     * 
     * @return
     * The resolved product
     * 
     * @throws ConfigurationItemNotFoundException
     * @throws WorkspaceNotFoundException
     * @throws NotAllowedException
     * @throws UserNotFoundException
     * @throws UserNotActiveException
     */
    PartUsageLink filterProductStructure(ConfigurationItemKey ciKey, ConfigSpec configSpec, Integer partUsageLink, Integer depth) throws ConfigurationItemNotFoundException, WorkspaceNotFoundException, NotAllowedException, UserNotFoundException, UserNotActiveException, PartUsageLinkNotFoundException;
    
    /**
     * Creates a new product structure.
     * 
     * @param workspaceId
     * The workspace in which the product structure will be created
     * 
     * @param id
     * The id of the product structure which must be unique inside
     * the workspace context
     * 
     * @param description
     * The description of the product structure
     * 
     * @param designItemNumber
     * The id of the part master that will be the root of the product structure
     * 
     * @return
     * The newly created configuration item
     * 
     * @throws UserNotFoundException
     * @throws WorkspaceNotFoundException
     * @throws AccessRightException
     * @throws NotAllowedException
     * @throws ConfigurationItemAlreadyExistsException
     * @throws CreationException
     */
    ConfigurationItem createConfigurationItem(String workspaceId, String id, String description, String designItemNumber) throws UserNotFoundException, WorkspaceNotFoundException, AccessRightException, NotAllowedException, ConfigurationItemAlreadyExistsException, CreationException, PartMasterNotFoundException;
    
    /**
     * Creates a new <a href="PartMaster.html">PartMaster</a>. Be aware that
     * the created item will still be in checkout state when returned.
     * Hence the calling client code has the opportunity to perform final
     * modifications on the first, iteration number 1,
     * <a href="PartIteration.html">PartIteration</a>.
     * 
     * @param workspaceId
     * The workspace in which the part master will be created
     * 
     * @param number
     * The part number of the item to create which is its id
     * inside the workspace
     * 
     * @param name
     * The user friendly name of the item
     * 
     * @param partMasterDescription
     * The full item description
     * 
     * @param standardPart
     * Boolean indicating if the item to create is a standard part
     * 
     * @param workflowModelId
     * The id of the workflow template that will be instantiated and attached
     * to the created part master. Actually, it's the first 
     * <a href="PartRevision.html">PartRevision</a> that will hold
     * the reference to the workflow. Obviously this parameter may be null,
     * it's not mandatory to rely on workflows for product definitions. 
     * 
     * @param partRevisionDescription
     * The description of the first revision, version A, of the item.
     * This revision will be created in the same time than
     * the <a href="PartMaster.html">PartMaster</a> itself.
     *
     * @param templateId
     * The id of the template to use to instantiate the part, may be null.
     * Refers to a <a href="PartMasterTemplate.html">PartMasterTemplate</a>.
     *
     * @return
     * The created part master instance
     * 
     * @throws NotAllowedException
     * @throws UserNotFoundException
     * @throws WorkspaceNotFoundException
     * @throws AccessRightException
     * @throws WorkflowModelNotFoundException
     * @throws PartMasterAlreadyExistsException
     * @throws CreationException
     */
    PartMaster createPartMaster(String workspaceId, String number, String name, String partMasterDescription, boolean standardPart, String workflowModelId, String partRevisionDescription, String templateId) throws NotAllowedException, UserNotFoundException, WorkspaceNotFoundException, AccessRightException, WorkflowModelNotFoundException, PartMasterAlreadyExistsException, CreationException, PartMasterTemplateNotFoundException, FileAlreadyExistsException;

    /**
     * Checks out the supplied part revision to allow the operating user to modify it.
     *
     * @param partRPK
     * The id of the part revision to check out
     *
     * @return
     * The part revision which is now in the checkout state
     *
     * @throws UserNotFoundException
     * @throws AccessRightException
     * @throws WorkspaceNotFoundException
     * @throws PartRevisionNotFoundException
     * @throws NotAllowedException
     * @throws FileAlreadyExistsException
     * @throws CreationException
     */
    PartRevision checkOutPart(PartRevisionKey partRPK) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, PartRevisionNotFoundException, NotAllowedException, FileAlreadyExistsException, CreationException;


    /**
     * Undoes checkout the given part revision. As a consequence its current
     * working copy, represented by its latest
     * <a href="PartIteration.html">PartIteration</a> will be deleted.
     * Thus, some modifications may be lost.
     * 
     * @param partRPK
     * The id of the part revision to undo check out
     * 
     * @return
     * The part revision which is now in the checkin state
     * 
     * @throws NotAllowedException
     * @throws PartRevisionNotFoundException
     * @throws UserNotFoundException
     * @throws UserNotActiveException
     * @throws WorkspaceNotFoundException
     */
    PartRevision undoCheckOutPart(PartRevisionKey partRPK) throws NotAllowedException, PartRevisionNotFoundException, UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;
    
    /**
     * Checks in the supplied part revision so its latest iteration,
     * that carries the modifications realized since the checkout operation,
     * will be published and made visible to all users.
     * 
     * @param partRPK
     * The id of the part revision to check in
     * 
     * @return
     * The part revision which has just been checked in
     * 
     * @throws PartRevisionNotFoundException
     * @throws UserNotFoundException
     * @throws WorkspaceNotFoundException
     * @throws AccessRightException
     * @throws NotAllowedException
     */
    PartRevision checkInPart(PartRevisionKey partRPK) throws PartRevisionNotFoundException, UserNotFoundException, WorkspaceNotFoundException, AccessRightException, NotAllowedException;

    /**
     * Creates the <a href="BinaryResource.html">BinaryResource</a> file,
     * which is the native CAD file associated with the part iteration passed as parameter.
     * The part must be in the checkout state and the calling user must have
     * write access rights to the part.
     *
     * @param partIPK
     * The id of the part iteration on which the file will be attached
     *
     * @param name
     * The name of the binary resource to create
     *
     * @param size
     * Number of bytes of the physical file
     *
     * @return
     * The physical file, a java.io.File instance, that now needs to be created
     *
     * @throws UserNotFoundException
     * @throws UserNotActiveException
     * @throws WorkspaceNotFoundException
     * @throws NotAllowedException
     * @throws PartRevisionNotFoundException
     * @throws FileAlreadyExistsException
     * @throws CreationException
     */
    java.io.File saveNativeCADInPartIteration(PartIterationKey partIPK, String name, long size) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, NotAllowedException, PartRevisionNotFoundException, FileAlreadyExistsException, CreationException;

    /**
     * Creates a <a href="Geometry.html">Geometry</a> file,
     * a specialized kind of binary resource which contains CAD data, and
     * attachs it to the part iteration passed as parameter.
     * The part must be in the checkout state and the calling user must have
     * write access rights to the part.
     * 
     * @param partIPK
     * The id of the part iteration on which the file will be attached
     * 
     * @param name
     * The name of the binary resource to create
     * 
     * @param quality
     * The quality of the CAD file, starts at 0, smaller is greater
     * 
     * @param size
     * Number of bytes of the physical file
     * 
     * @return
     * The physical file, a java.io.File instance, that now needs to be created
     * 
     * @throws UserNotFoundException
     * @throws UserNotActiveException
     * @throws WorkspaceNotFoundException
     * @throws NotAllowedException
     * @throws PartRevisionNotFoundException
     * @throws FileAlreadyExistsException
     * @throws CreationException
     */
    java.io.File saveGeometryInPartIteration(PartIterationKey partIPK, String name, int quality, long size) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, NotAllowedException, PartRevisionNotFoundException, FileAlreadyExistsException, CreationException;
    
    /**
     * Creates a regular file, <a href="BinaryResource.html">BinaryResource</a>
     * object, and attachs it to the part iteration instance passed
     * as parameter. The part must be in the checkout state and
     * the calling user must have write access rights to the part.
     * 
     * 
     * @param partIPK
     * The id of the part iteration on which the file will be attached
     * 
     * @param name
     * The name of the binary resource to create
     * 
     * @param size
     * Number of bytes of the physical file
     * 
     * @return
     * The physical file, a java.io.File instance, that now needs to be created
     * 
     * @throws UserNotFoundException
     * @throws UserNotActiveException
     * @throws WorkspaceNotFoundException
     * @throws NotAllowedException
     * @throws PartRevisionNotFoundException
     * @throws FileAlreadyExistsException
     * @throws CreationException
     */
    java.io.File saveFileInPartIteration(PartIterationKey partIPK, String name, long size) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, NotAllowedException, PartRevisionNotFoundException, FileAlreadyExistsException, CreationException;
    
    /**
     * Updates the specified <a href="PartIteration.html">PartIteration</a> with
     * the properties passed as parameters. The corresponding part revision
     * should be in checkout state.
     * 
     * @param key
     * The id of the part iteration to modify
     * 
     * @param iterationNote
     * A note to describe the iteration and thus the modifications made
     * to the part
     * 
     * @param source
     * The <a href="PartIteration.Source.html">PartIteration.Source</a>
     * attribute of the part
     * 
     * @param usageLinks
     * Links to other parts. Only assembly parts can define usage links
     * 
     * @param attributes
     * Custom attributes that may be added to the part
     *
     * @param linkKeys
     * Links to documents
     * 
     * @return
     * The <a href="PartRevision.html">PartRevision</a> of the updated
     * part iteration
     * 
     * @throws UserNotFoundException
     * @throws WorkspaceNotFoundException
     * @throws AccessRightException
     * @throws NotAllowedException
     * @throws PartRevisionNotFoundException
     * @throws PartMasterNotFoundException
     */
    PartRevision updatePartIteration(PartIterationKey key, java.lang.String iterationNote, PartIteration.Source source, java.util.List<PartUsageLink> usageLinks, java.util.List<InstanceAttribute> attributes, DocumentIterationKey[] linkKeys) throws UserNotFoundException, WorkspaceNotFoundException, AccessRightException, NotAllowedException, PartRevisionNotFoundException, PartMasterNotFoundException;
    
    /**
     * Returns the java.io.File object that references the physical file of the
     * supplied binary resource.
     * 
     * @param fullName
     * Id of the <a href="BinaryResource.html">BinaryResource</a> of which the
     * data file will be returned
     * 
     * @return
     * The physical file of the binary resource
     * 
     * @throws UserNotFoundException
     * @throws UserNotActiveException
     * @throws WorkspaceNotFoundException
     * @throws FileNotFoundException
     * @throws NotAllowedException
     */
    File getDataFile(String fullName) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, FileNotFoundException, NotAllowedException;
    
    /**
     * Retrieves all product structures that belong to the given workspace.
     * 
     * @param workspaceId
     * The workspace which is the first level context
     * where all products and parts are referenced
     * 
     * @return
     * The list of product structures
     * 
     * @throws UserNotFoundException
     * @throws UserNotActiveException
     * @throws WorkspaceNotFoundException
     */
    List<ConfigurationItem> getConfigurationItems(String workspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;
    
    /**
     * Retrieves all layers of the given product structure, ie
     * <a href="ConfigurationItem.html">ConfigurationItem</a>.
     * 
     * @param key
     * The id of the configuration item
     * 
     * @return
     * The list of all layers of the current configuration item
     * 
     * @throws UserNotFoundException
     * @throws UserNotActiveException
     * @throws WorkspaceNotFoundException
     */
    List<Layer> getLayers(ConfigurationItemKey key) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;
    
    /**
     * Retrieves a given layer.
     * 
     * @param id
     * Integer value that uniquely identifies the layer to return
     * 
     * @return
     * The layer to fetch
     * 
     * @throws UserNotFoundException
     * @throws UserNotActiveException
     * @throws WorkspaceNotFoundException
     * @throws LayerNotFoundException
     */
    Layer getLayer(int id) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, LayerNotFoundException;
    
    /**
     * Creates a new layer on a given product structure.
     * 
     * @param key
     * The identifier object of the configuration item wherein the layer will
     * be created
     * 
     * @param name
     * The user friendly name of the layer
     * 
     * @return
     * The newly created layer
     * 
     * @throws UserNotFoundException
     * @throws WorkspaceNotFoundException
     * @throws AccessRightException
     * @throws ConfigurationItemNotFoundException
     */
    Layer createLayer(ConfigurationItemKey key, String name) throws UserNotFoundException, WorkspaceNotFoundException, AccessRightException, ConfigurationItemNotFoundException;
    
    /**
     * Creates a new marker that will be a member of the given layer.
     * 
     * @param layerId
     * The layer id on which the marker will be created
     * 
     * @param title
     * The title of the marker
     * 
     * @param description
     * The description of the marker
     * 
     * @param x
     * The x axis coordinate of the marker
     * 
     * @param y
     * The y axis coordinate of the marker
     * 
     * @param z
     * The z axis coordinate of the marker
     * 
     * @return
     * The newly created marker
     * 
     * @throws LayerNotFoundException
     * @throws UserNotFoundException
     * @throws WorkspaceNotFoundException
     * @throws AccessRightException
     */
    Marker createMarker(int layerId, String title, String description, double x, double y, double z) throws LayerNotFoundException, UserNotFoundException, WorkspaceNotFoundException, AccessRightException;

    /**
     * Returns a specific <a href="PartMaster.html">PartMaster</a>.
     *
     * @param partMPK
     * The id of the part master to get
     *
     * @return
     * The part master
     *
     * @throws UserNotFoundException
     * @throws UserNotActiveException
     * @throws WorkspaceNotFoundException
     * @throws PartMasterNotFoundException
     */
    PartMaster getPartMaster(PartMasterKey partMPK) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartMasterNotFoundException;

    /**
     * Returns a specific <a href="PartRevision.html">PartRevision</a>.
     *
     * @param partRPK
     * The id of the part revision to get
     *
     * @return
     * The part revision
     *
     * @throws UserNotFoundException
     * @throws UserNotActiveException
     * @throws WorkspaceNotFoundException
     * @throws PartRevisionNotFoundException
     */
    PartRevision getPartRevision(PartRevisionKey partRPK) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartRevisionNotFoundException;

    /**
     * Finds part masters by their part number using like style query.
     *
     * @param workspaceId
     * The workspace in which part masters will be searched
     *
     * @param partNumber
     * The number of the part master to search for
     *
     * @param maxResults
     * Set the maximum number of results to retrieve
     *
     * @return
     * The list of <a href="PartMaster.html">PartMaster</a>
     *
     * @throws UserNotFoundException
     * @throws AccessRightException
     * @throws WorkspaceNotFoundException
     */
    List<PartMaster> findPartMasters(String workspaceId, String partNumber, int maxResults) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException;

    /**
     * Fetches the components of the supplied part assembly
     *
     * @param partIPK
     * The id of the part iteration of which the components have to be retrieved
     *
     * @return
     * The list of <a href="PartUsageLink.html">PartUsageLink</a>
     *
     * @throws UserNotFoundException
     * @throws UserNotActiveException
     * @throws WorkspaceNotFoundException
     * @throws PartIterationNotFoundException
     * @throws NotAllowedException
     */
    List<PartUsageLink> getComponents(PartIterationKey partIPK) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartIterationNotFoundException, NotAllowedException;

    boolean partMasterExists(PartMasterKey partMasterKey)throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;

    void deleteConfigurationItem(ConfigurationItemKey configurationItemKey) throws UserNotFoundException, WorkspaceNotFoundException, AccessRightException, NotAllowedException, UserNotActiveException, ConfigurationItemNotFoundException, LayerNotFoundException;

    void deleteLayer(String workspaceId, int layerId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, LayerNotFoundException;

    void removeCADFileFromPartIteration(PartIterationKey partIKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartIterationNotFoundException;

    PartMasterTemplate createPartMasterTemplate(String pWorkspaceId, String pId, String pPartType, String pMask, InstanceAttributeTemplate[] pAttributeTemplates, boolean idGenerated) throws WorkspaceNotFoundException, AccessRightException, PartMasterTemplateAlreadyExistsException, UserNotFoundException, NotAllowedException, CreationException;
    File saveFileInTemplate(PartMasterTemplateKey pPartMTemplateKey, String pName, long pSize) throws WorkspaceNotFoundException, NotAllowedException, PartMasterTemplateNotFoundException, FileAlreadyExistsException, UserNotFoundException, UserNotActiveException, CreationException ;
    String generateId(String pWorkspaceId, String pPartMTemplateId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException, PartMasterTemplateNotFoundException;
    PartMasterTemplate[] getPartMasterTemplates(String pWorkspaceId) throws WorkspaceNotFoundException, UserNotFoundException, UserNotActiveException;
    PartMasterTemplate getPartMasterTemplate(PartMasterTemplateKey pKey) throws WorkspaceNotFoundException, PartMasterTemplateNotFoundException, UserNotFoundException, UserNotActiveException;
    PartMasterTemplate updatePartMasterTemplate(PartMasterTemplateKey pKey, String pPartType, String pMask, InstanceAttributeTemplate[] pAttributeTemplates, boolean idGenerated) throws WorkspaceNotFoundException, WorkspaceNotFoundException, AccessRightException, PartMasterTemplateNotFoundException, UserNotFoundException;
    void deletePartMasterTemplate(PartMasterTemplateKey pKey) throws WorkspaceNotFoundException, AccessRightException, PartMasterTemplateNotFoundException, UserNotFoundException;
    PartMasterTemplate removeFileFromTemplate(String pFullName) throws WorkspaceNotFoundException, PartMasterTemplateNotFoundException, AccessRightException, FileNotFoundException, UserNotFoundException, UserNotActiveException;

    List<PartMaster> getPartMasters(String pWorkspaceId, int start, int pMaxResults) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, UserNotActiveException;
    void deletePartMaster(PartMasterKey partMasterKey) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException, PartMasterNotFoundException, EntityConstraintException;
    int getPartMastersCount(String pWorkspaceId) throws UserNotFoundException, AccessRightException, WorkspaceNotFoundException, UserNotActiveException;

    Long getDiskUsageForPartsInWorkspace(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;
    Long getDiskUsageForPartTemplatesInWorkspace(String pWorkspaceId) throws UserNotFoundException, UserNotActiveException, WorkspaceNotFoundException;
}
