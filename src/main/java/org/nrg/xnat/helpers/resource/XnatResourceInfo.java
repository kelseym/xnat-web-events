/*
 * web: org.nrg.xnat.helpers.resource.XnatResourceInfo
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.helpers.resource;

import org.apache.commons.lang3.StringUtils;
import org.nrg.xft.security.UserI;

import java.io.Serializable;
import java.util.*;

public class XnatResourceInfo implements Serializable {
    private static final long serialVersionUID = 42L;
	private String description,format,content=null;
	private Number event_id=null;
	private List<String> tags= new ArrayList<>();
	private Map<String,String> meta= new HashMap<>();
	private final Date lastModified,created;
	private final UserI user;
	
	public Date getLastModified() {
		return lastModified;
	}
	public Date getCreated() {
		return created;
	}
	public UserI getUser() {
		return user;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(final String description) {
		this.description = description;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(final String format) {
		this.format = format;
	}
	public String getContent() {
		return content;
	}
	public void setContent(final String content) {
		this.content = content;
	}
	public List<String> getTags() {
		return tags;
	}
	public void setMeta(final Map<String, String> meta) {
		this.meta = meta;
	}
	
	public void addMeta(final String key, final String value){
		this.meta.put(key, value);
	}
	public Map<String, String> getMeta() {
		return meta;
	}
	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public void addTag(String tag) {
		this.tags.add(tag);
	}
	
	public XnatResourceInfo(UserI user, Date created, Date lastModified){
		this.created=created;
		this.lastModified=lastModified;
		this.user=user;
	}

	
    public static XnatResourceInfo buildResourceInfo(final String description, final String format, final String content, final String[] tags, final UserI user, Date created, Date modified, Number i){
		XnatResourceInfo info = new XnatResourceInfo(user,created,modified);
        
	    if(description!=null){
	    	info.setDescription(description);
	    }
	    if(format!=null){
	    	info.setFormat(format);
	    }
	    if(content!=null){
	    	info.setContent(content);
	    }
	    
	    if(i!=null){
	    	info.setEvent_id(i);
	    }
	    
	    if(tags!=null){
	    	for(String tag: tags){
	    		tag = tag.trim();
	    		if(StringUtils.isNotBlank(tag)){
	    			for(String s: tag.split("[\\s]*,[\\s]*")){
	    				s=s.trim();
	    				if(!s.equals("")){
	    		    		if(s.contains("=")){
	    		    			info.addMeta(s.substring(0,s.indexOf("=")),s.substring(s.indexOf("=")+1));
	    		    		}else{
	    		    			if(s.contains(":")){
		    		    			info.addMeta(s.substring(0,s.indexOf(":")),s.substring(s.indexOf(":")+1));
		    		    		}else{
		    		    			info.addTag(s);
		    		    		}
	    		    		}
	    				}
	    			}
	    			
	    		}
	    	}
	    }
		
		return info;
	}
	public void setEvent_id(Number event_id) {
		this.event_id = event_id;
	}
	public Number getEvent_id() {
		return event_id;
	}
}
