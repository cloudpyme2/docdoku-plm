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

package com.docdoku.core.common;

import java.io.Serializable;
import javax.persistence.*;

/**
 * <a href="BinaryResource.html">BinaryResource</a> is the representation
 * of a file contained in either a document or a document template.
 * 
 * @author Florent Garin
 * @version 1.0, 02/06/08
 * @since   V1.0
 */
@Table(name="BINARYRESOURCE")
@Inheritance()
@NamedQueries ({
        @NamedQuery(name="BinaryResource.diskUsageInPath", query = "SELECT sum(br.contentLength) FROM BinaryResource br WHERE br.fullName like :path")
})
@Entity
public class BinaryResource implements Serializable, Comparable<BinaryResource>{

    
    @Id
    protected String fullName="";
    protected long contentLength;
    
    public BinaryResource() {
    }
    
    public BinaryResource(String pFullName, long pContentLength) {
        fullName = pFullName;
        contentLength = pContentLength;
    }
    
    public String getWorkspaceId(){
        return BinaryResource.parseWorkspaceId(fullName);
    }
    
    public static String parseWorkspaceId(String pFullName){
        int index = pFullName.indexOf('/');
        return pFullName.substring(0, index);
    }

    public String getOwnerType(){
        return BinaryResource.parseOwnerType(fullName);
    }
    
    public static String parseOwnerType(String pFullName){
        String[] parts = pFullName.split("/");
        return parts[1];
    }
    
    public String getOwnerRef(){
        return BinaryResource.parseOwnerRef(fullName);
    }
    
    public static String parseOwnerRef(String pFullName){
        String[] parts = pFullName.split("/",3);
        int index= parts[2].lastIndexOf('/');
        return parts[2].substring(0, index);
    }
    
    public void setFullName(String pFullName) {
        fullName = pFullName;
    }
    
    public void setContentLength(long pContentLength) {
        contentLength = pContentLength;
    }

    public boolean isNativeCADFile(){
        String[] parts = fullName.split("/");
        return (parts.length==7 && "nativecad".equals(parts[5]));
    }

    public BinaryResource getPrevious(){
        if(getOwnerType().equals("document-templates") || getOwnerType().equals("part-templates"))
            return null;
        
        int lastS = fullName.lastIndexOf('/');
        String name = fullName.substring(lastS+1);

        if(isNativeCADFile()){
            String[] parts = fullName.split("/");
            int iteration=Integer.parseInt(parts[4]);
            iteration--;
            if(iteration>0){
                String previousFullName=parts[0] + "/parts/" + parts[2] + "/" +parts[3]  + "/" + iteration + "/nativecad/" +name;
                return new BinaryResource(previousFullName,contentLength);
            }else
                return null;
        }else{
            String truncatedName=fullName.substring(0,lastS);
            int beforeLastS = truncatedName.lastIndexOf('/');
            int iteration=Integer.parseInt(truncatedName.substring(beforeLastS+1));
            iteration--;
            if(iteration>0){
                String previousFullName=truncatedName.substring(0,beforeLastS)+"/"+iteration+"/"+name;
                return new BinaryResource(previousFullName,contentLength);
            }else
                return null;
        }
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public String getName(){
        int index= fullName.lastIndexOf('/');
        return fullName.substring(index+1);
    }
    
    public long getContentLength() {
        return contentLength;
    }
    
    
    @Override
    public boolean equals(Object pObj) {
        if (this == pObj) {
            return true;
        }
        if (!(pObj instanceof BinaryResource))
            return false;
        BinaryResource bin = (BinaryResource) pObj;
        return bin.fullName.equals(fullName);
    }
    
    @Override
    public int hashCode() {
        return fullName.hashCode();
    }
    
    public int compareTo(BinaryResource pBinaryResource) {
        return fullName.compareTo(pBinaryResource.fullName);
    }
    
    @Override
    public String toString() {
        return getName();
    }
}
