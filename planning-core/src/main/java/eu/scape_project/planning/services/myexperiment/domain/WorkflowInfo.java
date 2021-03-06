/*******************************************************************************
 * Copyright 2006 - 2012 Vienna University of Technology,
 * Department of Software Technology and Interactive Systems, IFS
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package eu.scape_project.planning.services.myexperiment.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Information about a workflow response my the myExperiment REST API.
 */
@XmlRootElement(name = "workflow")
public class WorkflowInfo extends ResourceDescription {

    @XmlElement(name = "id")
    private String id;

    @XmlAttribute
    private String version;

    @XmlElement(name = "title")
    private String name;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "content-uri")
    private String contentUri;

    @XmlElement(name = "content-type")
    private String contentType;

    public String getDescriptor() {
        return this.getUri() + "&version=" + this.version;
    }

    public String getContentUri() {
        return contentUri + "?version=" + version;
    }

    // ---------- getter/setter ----------
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String getContentType() {
        return contentType;
    }
}
