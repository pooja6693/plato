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
package eu.scape_project.planning.xml.plan;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.apache.commons.codec.binary.Base64;

import eu.scape_project.planning.model.ByteStream;
import eu.scape_project.planning.xml.PlanXMLConstants;

/**
 * Helper class for {@link eu.scape_project.planning.xml.ProjectImporter} to
 * decode Base64 encoded strings. Can set the decoded data to other objects
 * which have a function setData(byte[] data)
 * 
 * @author Michael Kraxner
 * 
 */
public class BinaryDataWrapper implements Serializable {

    private static final long serialVersionUID = 2080538998419720006L;
    
    private static final Base64 decoder = new Base64(PlanXMLConstants.BASE64_LINE_LENGTH, PlanXMLConstants.BASE64_LINE_BREAK);

    byte[] value = null;

    /**
     * The methodName used to set the data to the object on the stack.
     * By default the method "setData" is used.
     */
    private String methodName = "setData";

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * Decodes the Base64 encoded string <code>value</code> and keep this data
     * for the next call of {@link #setData(Object)}
     * 
     * @param value
     */
    public void setFromBase64Encoded(String value) {
        this.value = decoder.decode(value); //decoder.encodeBase64(binaryData, isChunked)decode(value.replaceAll("\\s", ""));
    }

    /**
     * Invokes the function "setData" on <code>object</code> via reflection -
     * with previously decoded data as parameter.
     * 
     * @param object
     */
    public void setData(Object object) {
        try {
            ByteStream data = new ByteStream();
            data.setData(value);
            data.setSize(value.length);
            Method setData = object.getClass().getMethod(methodName, ByteStream.class);
            setData.invoke(object, new Object[] {data});
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    /**
     * Invokes the previously set function <methodName> on <code>object</code>
     * via reflection - with previously decoded data as parameter.
     * 
     * @param object
     */
    public void setString(Object object) {
        try {
            Method setData = object.getClass().getMethod(methodName, String.class);
            String dataString =  new String(value, PlanXMLConstants.ENCODING_CHARSET).replaceAll("\uFFFD", "");
            setData.invoke(object, new Object[] {dataString});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
