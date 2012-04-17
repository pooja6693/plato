/*******************************************************************************
 * Copyright (c) 2006-2010 Vienna University of Technology, 
 * Department of Software Technology and Interactive Systems
 *
 * All rights reserved. This program and the accompanying
 * materials are made available under the terms of the
 * Apache License, Version 2.0 which accompanies
 * this distribution, and is available at
 * http://www.apache.org/licenses/LICENSE-2.0 
 *******************************************************************************/

package eu.planets_project.pp.plato.model;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;

/**
 * This class represents a file, or more generally a digital object, that has been uploaded
 * by the user and shall be stored in the database.
 *
 * @author Hannes Kulovits
 */
@Entity
@Inheritance
@DiscriminatorColumn(name = "type")
@DiscriminatorValue("DO")
public class DigitalObject implements Serializable, ITouchable {

    private static final long serialVersionUID = -163440832511570828L;

    @Id
    @GeneratedValue
    protected int id;

    /**
     * This is the persistent identifier of the object, 
     * it is compliant with the syntax of Fedora persistent identifiers {@link https://wiki.duraspace.org/display/FEDORA35/Fedora+Identifiers} 
     */
    @Pattern(regexp="^([A-Za-z0-9]|-|\\.)+:(([A-Za-z0-9])|-|\\.|~|_|(%[0-9A-F]{2}))+$")
    protected String pid;
    
    /**
     * Name of the object.
     */
    protected String fullname = "";

    /**
     * In most cases this is the mime-type of the object.
     */
    protected String contentType = "";
    
    @OneToOne(cascade=CascadeType.ALL)
    protected ChangeLog changeLog = new ChangeLog();
    
    @OneToOne(cascade=CascadeType.ALL)
    protected ByteStream data = new ByteStream();
    
    public ByteStream getData() {
        return data;
    }

    public void setData(ByteStream data) {
        this.data = data;
    }    

    @Length(max = 2000000)
    @Column(length = 2000000)
    protected String jhoveXMLString;

    @Length(max = 2000000)
    @Column(length = 2000000)
    protected String fitsXMLString;
    
    public String getFitsXMLString() {
        return fitsXMLString;
    }

    public void setFitsXMLString(String fitsXMLString) {
        this.fitsXMLString = fitsXMLString;
    }

    /**
     * Real size of the upload in Mb
     */
    private Double sizeInMB = 0.0;

    @OneToOne(cascade=CascadeType.ALL)
    protected XcdlDescription xcdlDescription = null;

    /**
     * Detailed information about the sample record's format.
     */
    @OneToOne(cascade=CascadeType.ALL)
    protected FormatInfo formatInfo = new FormatInfo();
    
    /**
     * Method responsible for indicating if the DigitalObject contains data.
     * 
     * @return true if the upload contains data.
     */
    public boolean isDataExistent() {
    	// pid indicates associated data
        if (pid != null && !pid.equals("")) {
        	return true;
        }
        
        // if no pid is set check the bytestream directly.
    	return data.isDataExistent();
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public void assignValues(DigitalObject source){
    	contentType = source.getContentType();
    	fullname = source.getFullname();
    	data = source.getData().clone();
    	pid = source.getPid();
    	sizeInMB = source.getSizeInMB();
    	fitsXMLString = source.getFitsXMLString();
    	formatInfo.assignValues(source.getFormatInfo());
    }

    @Transient
    public DigitalObject clone() {
        DigitalObject u = new DigitalObject();
        u.assignValues(this);
        return u;
    }

    public ChangeLog getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(ChangeLog value) {
        changeLog = value;
    }

    public boolean isChanged(){
        return changeLog.isAltered();
    }
    
    public void touch() {
        changeLog.touch();
    }

    /**
     * @see ITouchable#handleChanges(IChangesHandler)
     */
    public void handleChanges(IChangesHandler h) {
        h.visit(this);
        formatInfo.handleChanges(h);
        if (xcdlDescription != null) 
            xcdlDescription.handleChanges(h);
    }

    
    public String getJhoveXMLString() {
        return jhoveXMLString;
    }

    public void setJhoveXMLString(String jhoveXMLString) {
        this.jhoveXMLString = jhoveXMLString;
    }

    public XcdlDescription getXcdlDescription() {
        return xcdlDescription;
    }

    public void setXcdlDescription(XcdlDescription xcdlDescription) {
        this.xcdlDescription = xcdlDescription;
    }

    public FormatInfo getFormatInfo() {
        return formatInfo;
    }

    public void setFormatInfo(FormatInfo value) {
        formatInfo = value;
    }    
    

    @Override
    /**
     * checks only the ID, if it exists - if it doesnt exist, it checks object identity.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof DigitalObject) {
            int id2 = ((DigitalObject) o).getId();
            boolean result =  ((id != 0) && (id == id2));
            return result;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return new Long(id).hashCode();
    }

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public Double getSizeInMB() {
		return sizeInMB;
	}

	public void setSizeInMB(Double sizeInMB) {
		this.sizeInMB = sizeInMB;
	}
}
