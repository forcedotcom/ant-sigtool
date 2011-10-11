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
import java.util.HashSet;
import java.util.Set;

/**
 * Contains information about signing for a given COD file.
 *
 * @author aditya.joshi
 */
public class CodSigningInfo {

    /**
     * The filename of the cod. If it is a sibling cod, it will be in the format
     * big.cod/sibling.cod
     */
    private final String filename;

    /**
     * A list of signers for this COD.
     * This will NOT be null, it will empty if there are no signers.
     */
    private final Set<String> signers;

    /**
     * Constructor that takes a filename and initializes a new Hashset.
     * @param file name of file whose signers are being checked
     */
    public CodSigningInfo(final String file) {
        filename = file;
        signers = new HashSet<String>();
    }

    /**
     * Adds a signer to the current CodSigningInfo's list of signers.
     * @param signer Signer name that has been found in this cod file
     * @return true if the set of signers did not already contain this signer
     */
    public final boolean addSigner(final String signer) {
        return signers.add(signer);
    }

    /**
     * Returns the name of the cod file whose signer info is contained in this
     * instance.
     * @return cod file name
     */
    public final String getFilename() {
        return filename;
    }

    /**
     * Accessor for the set of signers for this file.
     * @return all signers who have signed this cod file
     */
    public final Set<String> getSigners() {
        return signers;
    }

}
