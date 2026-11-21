/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package reciter.xml.retriever.pubmed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PubMedPersistenceWorker implements Runnable {

	private final static Logger slf4jLogger = LoggerFactory.getLogger(PubMedPersistenceWorker.class);

	private final String url;
	private final String commonDirectory;
	private final String uid;
	private final String xmlFileName;
	
	public PubMedPersistenceWorker(String url, String commonDirectory, String uid, String xmlFileName) {
		this.url = url;
		this.commonDirectory = commonDirectory;
		this.uid = uid;
		this.xmlFileName = xmlFileName;
	}
	
	@Override
	public void run() {
		try {
			slf4jLogger.info("persisting PubMed articles for uid=[" + uid + "].");
			persist(url, commonDirectory, uid, xmlFileName);
		} catch (IOException e) {
			slf4jLogger.error("Error persisting PubMed XML file for uid=[" + uid + "].", e);
		}
	}
	
	/**
	 * Save the url (XML) content in the {@code directoryLocation} with directory
	 * name {@code directoryName} and file name {@code fileName}.
	 * 
	 * @param url URL
	 * @param commonDirectory directory path.
	 * @param uid directory name.
	 * @param xmlFileName file name.
	 * @throws IOException 
	 * @throws MalformedURLException 
	 * @throws UnsupportedEncodingException 
	 */
	public void persist(String url, String commonDirectory, String uid, String xmlFileName) 
			throws UnsupportedEncodingException, MalformedURLException, IOException {

		File dir = new File(commonDirectory + uid);
		if (!dir.exists()) {
			dir.mkdirs();
		}

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(url).openStream(), "UTF-8"));
		String outputFileName = commonDirectory + uid + "/" + xmlFileName + ".xml";
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName), "UTF-8"));

		String inputLine;
		while ((inputLine = bufferedReader.readLine()) != null) {
			bufferedWriter.write(inputLine);
			bufferedWriter.newLine();
		}

		bufferedReader.close();
		bufferedWriter.close();

	}
}
