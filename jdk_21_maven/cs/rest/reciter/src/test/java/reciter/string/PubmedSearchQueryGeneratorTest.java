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
package reciter.string;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.utils.PubmedSearchQueryGenerator;

public class PubmedSearchQueryGeneratorTest {

	private static final Logger log = LoggerFactory.getLogger(PubmedSearchQueryGeneratorTest.class);
	
	@Test
	public void testRegex1() {
		String s = "Williams JR. (III). Brackens O'Connor";
		String r = s.replaceAll("(IX|IV|V?I{0,3})", "").replaceAll("(JR)", "").replaceAll("[^A-Za-z0-9'\\s+]", "")
				.replaceAll("\\s+", " ").trim();
		assertEquals("Williams Brackens O'Connor", r);
	}
	
	@Test
	public void testRegex2() {
		String s = "Del Silva Marquez";
		String r = s.replaceAll("\\b\\w{1,3}\\b\\s?", "");
		assertEquals("Silva Marquez", r);
	}
	
	@Test
	public void testGenerate1() {
		String firstName = "Roland";
		String middleName = "";
		String lastName = "Di Silva Marquez";
		
		PubmedSearchQueryGenerator p = new PubmedSearchQueryGenerator();
		Set<String> result = p.generate(firstName, middleName, lastName);
		
		assertEquals(7, result.size());
		assertEquals(true, result.contains("Di Silva Marquez R"));
		assertEquals(true, result.contains("Di-Silva-Marquez R"));
		//assertEquals(true, result.contains("DiSilvaMarquez R"));
		//assertEquals(true, result.contains("Silva Marquez R"));
		assertEquals(true, result.contains("Roland Di Silva Marquez"));
	}
	
	@Test
	public void testGenerate2() {
		String firstName = "Wafaa";
		String middleName = "";
		String lastName = "Sekkal-Gherbi";
		
		PubmedSearchQueryGenerator p = new PubmedSearchQueryGenerator();
		Set<String> result = p.generate(firstName, middleName, lastName);
		
		assertEquals(3, result.size());
		assertEquals(true, result.contains("Wafaa Sekkal-Gherbi"));
		assertEquals(true, result.contains("Sekkal-Gherbi W"));
		//assertEquals(true, result.contains("SekkalGherbi W"));
		assertEquals(true, result.contains("Sekkal Gherbi W"));
	}
	
	@Test
	public void testGenerate3() {
		String firstName = "Roland";
		String middleName = "";
		String lastName = "Silva Di Marquez";
		
		PubmedSearchQueryGenerator p = new PubmedSearchQueryGenerator();
		Set<String> result = p.generate(firstName, middleName, lastName);
		
		assertEquals(7, result.size());
		assertEquals(true, result.contains("Roland Silva Di Marquez"));
		assertEquals(true, result.contains("Silva Di Marquez R"));
		assertEquals(true, result.contains("Silva-Di-Marquez R"));
		//assertEquals(true, result.contains("SilvaDiMarquez R"));
		//assertEquals(true, result.contains("Silva Marquez R"));
	}
	
	@Test
	public void testGenerate4() {
		String firstName = "Mari";
		String middleName = "Ann";
		String lastName = "Del De Potro Marque dos-Santos Domingo";
		
		PubmedSearchQueryGenerator p = new PubmedSearchQueryGenerator();
		Set<String> result = p.generate(firstName, middleName, lastName);
		log.info("Generated queries: {}", result);
		
		assertEquals(11, result.size());
		assertEquals(true, result.contains("Mari Ann Del De Potro Marque dos-Santos Domingo"));
		assertEquals(true, result.contains("Del De Potro Marque dos-Santos Domingo M"));
		//assertEquals(true, result.contains("DelDePotroMarquedos-SantosDomingo M"));
		assertEquals(true, result.contains("Del-De-Potro-Marque-dos-Santos-Domingo M"));
		assertEquals(true, result.contains("Del De Potro Marque dos-Santos Domingo M"));
		//assertEquals(true, result.contains("Potro Marque -Santos Domingo M"));
		//assertEquals(true, result.contains("Del De Potro Marque dosSantos Domingo M"));
	}
}
