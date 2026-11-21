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
package reciter.tfidf;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import reciter.utils.ReCiterStringUtil;

public class ReCiterStringUtilTest {

	@Test
	public void testTokenize() {
		String s = "King's College School of Medicine and Dentistry, University of London (United Kingdom) 1974";
		String[] tokens = ReCiterStringUtil.tokenize(s);
		
		assertEquals("Size of tokens: ", 13, tokens.length);
		assertEquals("0th elem=", "Kings", tokens[0]);
		assertEquals("1th elem=", "College", tokens[1]);
		assertEquals("2th elem=", "School", tokens[2]);
		assertEquals("3th elem=", "of", tokens[3]);
		assertEquals("4th elem=", "Medicine", tokens[4]);
		assertEquals("5th elem=", "and", tokens[5]);
		assertEquals("6th elem=", "Dentistry", tokens[6]);
		assertEquals("7th elem=", "University", tokens[7]);
		assertEquals("8th elem=", "of", tokens[8]);
		assertEquals("9th elem=", "London", tokens[9]);
		assertEquals("10th elem=", "United", tokens[10]);
		assertEquals("1th elem=", "Kingdom", tokens[11]);
		assertEquals("12th elem=", "1974", tokens[12]);
	}
	
	@Test
	public void testComputeNumberOfOverlapTokens() {
		String s1 = "King's College School of Medicine and Dentistry, University of London (United Kingdom) 1974";
		String s2 = "King's College Hospital King's College Hospital London United Kingdom";
		
		int numOverlap = ReCiterStringUtil.computeNumberOfOverlapTokens(s1, s2);
		assertEquals("Number of overlaps: ", 5, numOverlap);
	}
	
	@Test
	public void testLevenshteinDistance1() {
		String s1 = "Antony";
		String s2 = "Anthony";
		int dist = ReCiterStringUtil.levenshteinDistance(s1, s2);
		assertEquals("Distance should be 1", 1, dist);
	}
	
	@Test
	public void testLevenshteinDistance2() {
		String s1 = "Bi-Sen";
		String s2 = "BiSen";
		int dist = ReCiterStringUtil.levenshteinDistance(s1, s2);
		assertEquals("Distance should be 1", 1, dist);
	}
	
	@Test
	public void testLevenshteinDistance3() {
		String s1 = "Jeffery";
		String s2 = "Jeffrey";
		int dist = ReCiterStringUtil.levenshteinDistance(s1, s2);
		assertEquals("Distance should be 2", 2, dist);
	}
	
	@Test
	public void testDeAccent1() {
		String s = "å";
		String deAccentedS = ReCiterStringUtil.deAccent(s);
		assertEquals("equal", "a", deAccentedS);
	}
	
	@Test
	public void testDeAccent2() {
		String s = "Ibáñez";
		String deAccentedS = ReCiterStringUtil.deAccent(s);
		assertEquals("equal", "Ibanez", deAccentedS);
	}
	
	@Test
	public void testDeAccent3() {
		String s = "Guzmán";
		String deAccentedS = ReCiterStringUtil.deAccent(s);
		assertEquals("equal", "Guzman", deAccentedS);
	}
	
	@Test
	public void testDeAccent4() {
		String s = "ö";
		String deAccentedS = ReCiterStringUtil.deAccent(s);
		assertEquals("equal", "o", deAccentedS);
	}

	@Test
	public void testStripBackslashesWithBackslashDash() {
		String s = "Non\\-alcoholic fatty liver disease";
		String result = ReCiterStringUtil.stripBackslashes(s);
		assertEquals("Backslash-dash should be stripped", "Non-alcoholic fatty liver disease", result);
	}

	@Test
	public void testStripBackslashesNoBackslashes() {
		String s = "Normal title without backslashes";
		String result = ReCiterStringUtil.stripBackslashes(s);
		assertEquals("String without backslashes should be unchanged", "Normal title without backslashes", result);
	}

	@Test
	public void testStripBackslashesNull() {
		String result = ReCiterStringUtil.stripBackslashes(null);
		assertEquals("Null input should return null", null, result);
	}

	@Test
	public void testStripBackslashesEmpty() {
		String result = ReCiterStringUtil.stripBackslashes("");
		assertEquals("Empty string should return empty", "", result);
	}
}
