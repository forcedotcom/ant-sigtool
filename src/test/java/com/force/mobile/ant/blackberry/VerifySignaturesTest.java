/*
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 * following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * Neither the name of salesforce.com, inc. nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.force.mobile.ant.blackberry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.force.mobile.ant.blackberry.BadCodException;
import com.force.mobile.ant.blackberry.VerifySignatures;

public class VerifySignaturesTest {

	/**
	 * Validates the flashID.
	 * @throws IOException if internal <code>DataInputStream</code> causes error.
	 */
    @Test
	public void testflashidChecking() throws IOException {
		byte[] incorrectBytes = new byte[] {
		        (byte)0x10, (byte)0x00, (byte)0xBC, (byte)0xFF};

		byte[] correctBytes = new byte[] {
		        (byte)0xDE, (byte)0xC0, (byte)0xFF, (byte)0xFF};

		DataInputStream incorrectByteStream = new DataInputStream(
		        new ByteArrayInputStream(incorrectBytes));
		DataInputStream correctByteStream = new DataInputStream(
		        new ByteArrayInputStream(correctBytes));

		try {
		    // should fail:
		    VerifySignatures.checkValidity(incorrectByteStream, "incorrectBytes");
			fail("incorrectBytes read as correct");
		} catch (BadCodException e) {
			// should happen anyway
		}

		try {
		    // should not fail:
		    VerifySignatures.checkValidity(correctByteStream, "correctBytes");
		} catch (BadCodException e) {
			fail("correctBytes read as incorrect");
		}


	}

	/**
	 * Check the Version number reader
	 * @throws IOException if internal datastream causes error
	 */
    @Test
	public void testVersionNumChecking() throws IOException {
		byte expected = 1;

		/* Version number incorrectVnum */
		byte[] codHeader = new byte[] {
				(byte)0x1D, (byte)0xEE, (byte)0xBC, (byte)0xFF, (byte)0x10,
				(byte)0xA0, (byte)0xBC, (byte)0xFF, (byte)0x1D, (byte)0xEE,
				(byte)0xBC, (byte)0xFF, (byte)0x10, (byte)0xA0, (byte)0xBC,
				(byte)0xFF, (byte)0x1D, (byte)0xEE, (byte)0xBC, (byte)0xFF,
				(byte)0x10, (byte)0xA0, (byte)0xBC, (byte)0xFF, (byte)0x1D,
				(byte)0xEE, (byte)0xBC, (byte)0xFF, (byte)0x10, (byte)0xA0,
				(byte)0xBC, (byte)0xFF,   expected, (byte)0x00};
		DataInputStream incorrectBytes = new DataInputStream(
		        new ByteArrayInputStream(codHeader));

		assertEquals("IncorrectBytes did not return the correct number",
		        expected, VerifySignatures.getVersion(incorrectBytes));
	}

	/**
	 * Check the short reader
	 * @throws IOException if internal datastream causes error
	 */
    @Test
	public void testByteReader() throws IOException {
		byte[] inputBytes = new byte[] {
		        (byte)0x5C, (byte)0x17, (byte)0xBC, (byte)0xEB};
		DataInputStream fakeBytes = new DataInputStream(
		        new ByteArrayInputStream(inputBytes));

		int expected = 0x175C;
		assertEquals("First set of values read were not equal!",
		        expected, VerifySignatures.readLittleEndianNibble(fakeBytes));

		expected = 0xEBBC;
		assertEquals("Second set of values read were not equal!",
		        expected, VerifySignatures.readLittleEndianNibble(fakeBytes));
	}

	/**
	 * Check three files for validity.
	 * @param validFile A perfectly valid codfile signed by SFDC
	 * @param incorrectFile A valid codfile not signed by SFDC
	 * @param invalidFile A file that is not a codfile.  eg XML file
	 */
	public void testCodChecking(File validFile, File incorrectFile, File invalidFile) {
		VerifySignatures test = new VerifySignatures("SFDC");

		assertTrue("valid cod file read as invalid", !test.isBroken(validFile));
		assertTrue("Incorrectly signed cod file read as valid", test.isBroken(incorrectFile));
		assertTrue("Invalid/corrupt cod file read as valid", test.isBroken(invalidFile));
	}


	/**
	 * Check the ability to read a normal cod file
	 */
    @Test
	public void testSmallCodChecking() {
	    // location of the test files
		File testFolder = new File("src" + File.separator + "test"
		        + File.separator + "resources");

		/* Valid Codfile signed by (xyz)*/
		File smallValidCodFile = new File(testFolder,
		        "test_salesforce_chatter-8.cod");

		/* Valid Codfile not signed by SFDC */
		File incorrectValidCodFile = new File(testFolder,
		        "test_salesforce_chatter-7.cod");

		/* Invalid Codfile (it's really XML!) */
		File smallInvalidCodFile = new File(testFolder,
		        "test_badfile.cod");

		testCodChecking(smallValidCodFile, incorrectValidCodFile,
		        smallInvalidCodFile);
	}

	/**
	 * Check the ability to read a big cod (zipped cod)
	 */
    @Test
	public void testBigCodChecking() {
	    // location of the test files
		File testFolder = new File("src" + File.separator + "test"
		        + File.separator + "resources");

		/* Valid Codfile */
		File bigValidCodFile = new File(testFolder,
		        "test_salesforce_chatter-10.cod");

		/* Valid Codfile not signed by SFDC */
		File incorrectValidCodFile = new File(testFolder,
		        "test_salesforce_chatter-9.cod");

		/* Invalid Codfile (more XML)*/
		File bigInvalidCodFile = new File(testFolder,
		        "test_salesforce_chatter-11.cod");

		testCodChecking(bigValidCodFile, incorrectValidCodFile,
		        bigInvalidCodFile);
	}

}
