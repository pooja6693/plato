/*******************************************************************************
 * Copyright 2006 - 2012 Vienna University of Technology,
 * Department of Software Technology and Interactive Systems, IFS
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 ******************************************************************************/
package eu.scape_project.planning.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A simple java tool to download textual content from a given url, similar to wget.
 *  
 * @author Michael Kraxner
 *
 */
public class JGet {
	/**
	 * Retrieves content from the given url
	 * - content is read line by line
	 *  
	 * @param url
	 * 		url of the resource to retrieve, must include the protocol (e.g. http://www.url.org)
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static String wget(String url) throws MalformedURLException, IOException {
		StringBuilder content = new StringBuilder();
		String s;
		URL u = new URL(url);

		InputStream is = u.openStream();
		BufferedReader dis = new BufferedReader(new InputStreamReader(is));
		while ((s = dis.readLine()) != null) {
			content.append(s).append("\n");
		}
		return content.toString();
	}
}
