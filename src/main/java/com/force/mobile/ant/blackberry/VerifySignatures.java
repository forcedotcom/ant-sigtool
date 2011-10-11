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

/**
 * @author aditya.joshi
 *
 */
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Ant task to validate COD signatures.
 * @author aditya.joshi
 * @author jschroeder
 *
 */
public class VerifySignatures extends Task {
    /**
     * Minimum COD header version number.
     */
    public static final int MIN_VERSION = 74;
    /**
     * Length, in bytes, of the Signer ID characters.
     */
    private static final int LENGTH_OF_SIGNER_ID = 4;

    /**
     * Set of signer IDs.
     *
     * Each is a string, 1-4 characters long (inclusive).
     */
    private final Set<String> signers;

    /**
     * List of filesets for COD files.
     */
    private List<FileSet> fileSet;

    /**
     * Constructor for a SignVerifier that takes comma separated signer names.
     *
     * @param signersList Name of COD file signers
     */
    public VerifySignatures(final String signersList) {
        signers = new HashSet<String>();
        this.setSigners(signersList);
    }

    /**
     * Create the requisite HashSet and ArrayList.
     */
    public VerifySignatures() {
        signers = new HashSet<String>();
        fileSet = new ArrayList<FileSet>();
    }

    /**
     * Adds a fileset to the list of filesets.
     *
     * @param fileset Set of files to add.
     */
    public final void add(final FileSet fileset) {
        fileSet.add(fileset);
    }

    /**
     * Implements the task. For use by Ant
     */
    @Override
    public final void execute() {
        boolean error = false;
        for (FileSet fs : fileSet) {
            if (brokenFileSet(fs)) {
                error = true;
            }
        }
        if (error) {
            throw new BuildException("Not all cod files are signed");
        }

    }

    /**
     * Goes through every desired cod file in the fileset and checks signature.
     *
     * @param fs FileSet to check.
     * @return false iff the fileset is a set of valid, signed cods.
     */
    protected final boolean brokenFileSet(final FileSet fs) {
        boolean error = false;
        DirectoryScanner ds = fs.getDirectoryScanner();
        for (String fileName : ds.getIncludedFiles()) {
            if (isBroken(new File(ds.getBasedir(), fileName))) {
                error = true;
            }
        }
        return error;
    }

    /**
     * Verifies that the given file has the signers specified by the class data
     * member.
     *
     * @param inputFile The file to be checked
     * @return false if the file is a valid, signed cod. true otherwise
     */
    protected final boolean isBroken(final File inputFile) {
        List<CodSigningInfo> codSignatures = readCod(inputFile);
        if (codSignatures.isEmpty()) {
            return true;
        }
        boolean error = false;
        for (CodSigningInfo oneCod : codSignatures) {
            if (!isSignedBySigners(oneCod, signers)) {
                logError(oneCod.getFilename() + " is missing a signature!");
                error = true;
            }
        }
        return error;
    }

    /**
     * Checks to see if the given CodSigningInfo contains signatures by every
     * signer in expectedSigners.
     *
     * @param cod codfile containing data about signers of a file
     * @param expectedSigners Set of expected signers
     * @return true if all signers are present, false otherwise
     */
    protected final boolean isSignedBySigners(final CodSigningInfo cod,
            final Set<String> expectedSigners) {
        // Pre-condition: CodsigningInfo and expectedSigners have been
        //                initialized
        // Post-condition: Presence/Absence of signers in file has been logged
        if (cod.getSigners().isEmpty()) {
            return false;
        }
        boolean returnValue = true;
        for (String expectedSigner : expectedSigners) {
            if (!cod.getSigners().contains(expectedSigner)) {
                logError(cod.getFilename() + " not signed by "
                        + expectedSigner);
                returnValue = false;
            } else {
                logMsg(cod.getFilename() + " signed by " + expectedSigner);
            }
        }
        logMsg(cod.getSigners().toString());
        return returnValue;
    }

    /**
     * Attempts to read a COD file. If it is zipped, it reads; else it is passed
     * on to {@link readSingleFile()}.
     *
     * @param inputFile COD file to be read.
     * @return List of CodSigningInfos where each element corresponds to a
     *         subfile.
     *
     * @see #readSingleFile(File)
     * @see #signers(InputStream, String)
     */
    protected final List<CodSigningInfo> readCod(final File inputFile) {
        // Pre-condition: - fileName is a valid codfile name
        // Post-condition: - The signers have been returned and the file has
        //                   been closed
        String fileName = inputFile.getName();
        List<CodSigningInfo> returnValue = new ArrayList<CodSigningInfo>();
        JarFile bigcod;
        Enumeration<JarEntry> entries;
        try {
            bigcod = new JarFile(inputFile);
            entries = bigcod.entries();
            try {
                while (entries.hasMoreElements()) {
                    // read Jar entry:
                    JarEntry currentEntry = entries.nextElement();
                    InputStream inputStream = bigcod
                            .getInputStream(currentEntry);

                    // puts its signer set into the arraylist
                    returnValue.add(signers(inputStream, fileName
                            + File.separator + currentEntry));
                }
            } finally {
                try {
                    bigcod.close();
                } catch (IOException e) {
                    logError("Unable to close big cod file " + fileName);
                    log(e, Project.MSG_WARN);
                }
            }
        } catch (ZipException zipException) {
            logMsg("May not be a big COD: opening normally");
            returnValue.add(readSingleFile(inputFile));
        } catch (IOException ioException) {
            logError("Encountered unexpected error while reading big cod file");
            log(ioException, Project.MSG_WARN);
        }

        return returnValue;
    }

    /**
     * Reads a small codfile (not a zipped one) and returns a list of signers.
     *
     * @param inputCodFile File to be read
     * @return HashSet of all signer names
     */
    protected final CodSigningInfo readSingleFile(final File inputCodFile) {
        // Pre-condition: - fileName is a valid codfile name
        // Post-condition: - The file is closed.
        try {
            FileInputStream inputFile = new FileInputStream(inputCodFile);

            CodSigningInfo returnValue = signers(inputFile,
                    inputCodFile.getName());
            try {
                inputFile.close();
            } catch (IOException e) {
                log(e, Project.MSG_WARN);
                logError("Failed to close file");
            }
            return returnValue;
        } catch (FileNotFoundException e1) {
            throw new BuildException(e1);
        }
    }

    /**
     * Print out the signers for a given COD input stream.
     *
     * @param inputStream Open inputStream to a COD file
     * @param fileName Name of codfile being looked through
     * @return CodSigningInfo for the given file
     */
    protected final CodSigningInfo signers(final InputStream inputStream,
            final String fileName) {
        // Pre-condition: - inputStream is not null - inputStream is at the
        //                  beginning of the file
        // Post-condition: - inputStream is at the end of the file, but still
        //                   open

        DataInputStream inputFile;
        StringBuffer currentSigner = new StringBuffer(LENGTH_OF_SIGNER_ID);
        CodSigningInfo returnValue = new CodSigningInfo(fileName);

        inputFile = new DataInputStream(inputStream);
        try {
            int dataSize; // stores the sum of data size and code size
            int signLength; // stores the length of the signature

            checkValidity(inputFile, fileName); // Find out if the flashid is
            // correct and verify the ends

            if (getVersion(inputFile) <= MIN_VERSION) {
                logError("File version not above " + MIN_VERSION + ".");
                return null;
            }

            // code size:
            dataSize = readLittleEndianNibble(inputFile);
            // + data size:
            dataSize += readLittleEndianNibble(inputFile);
            // Skip two bytes for the COD flags.
            inputFile.skip(2 + dataSize);

            while (true) { // read until exception
                currentSigner.delete(0, currentSigner.length());
                int temp = readLittleEndianNibble(inputFile);
                if (temp != 1) {
                    logError("Sign_type is not 1.  Sign_type = " + temp);
                    return null;
                }
                signLength = readLittleEndianNibble(inputFile);
                char c;
                for (short i = 0; i < LENGTH_OF_SIGNER_ID; i++) {
                    // read 4 char signer
                    c = (char) inputFile.readByte();
                    if (c != 0) {
                        currentSigner.append(c);
                    }
                }
                // -4 because 4 characters of signer have already been read
                inputFile.skip(signLength - LENGTH_OF_SIGNER_ID);
                returnValue.addSigner(currentSigner.toString());
            }

        } catch (EOFException e) { // EOF- return list of signers
            /* this is expected; we reached the end of the file. */
        } catch (IOException e) {
            log(e, Project.MSG_WARN);
            logError("Failed to read file.");
            return new CodSigningInfo(fileName);
        } catch (BadCodException e) {
            log(e, Project.MSG_WARN);
            logError("Bad COD file.");
            return new CodSigningInfo(fileName);
        }

        return returnValue;
    }

    /**
     * Ensures that the flashID of the file is correct.
     *
     * @param inputFile file to be checked
     * @param fileName name of file being read. Only for information in case of
     *            failure
     * @throws IOException upon failure to read
     * @throws BadCodException when file has incorrect flashID
     */
    protected static void checkValidity(final DataInputStream inputFile,
            final String fileName) throws BadCodException, IOException {
        // Pre-condition: - inputFile is an open DataInputStream positioned at
        //                  the start
        // Post-condition: - If file is valid, DataInputStream is
        //                   positioned 4 positions ahead of the start position
        //                   Otherwise an exception is thrown
        final int codHeader = 0xDEC0FFFF;
        if (inputFile.readInt() != codHeader) {
            throw new BadCodException(fileName + " is not a COD");
        }
    }

    /**
     * Skips ahead to read the version number after flashID has been read.
     *
     * @param inputFile Open input file
     * @return COD header version number
     * @throws IOException For any unexpected read exception.
     */
    protected static int getVersion(final DataInputStream inputFile)
        throws IOException {
        // Pre-condition: - inputFile is an open DataInputStream and
        //                  checkValidity() has been called.
        // Post-condition: inputFile is an open DataInputStream positioned
        //                 at the +38th offset and version number has been read
        final int versionOffset = 32;
        inputFile.skip(versionOffset);
        return readLittleEndianNibble(inputFile);
    }

    /**
     * Reads two (unsigned) bytes and then makes them into a 16-bit unsigned
     * word.
     *
     * @param inputFile An open DataInputStream
     * @return Representation of the next two bytes
     * @throws IOException Exception upon failure to read file
     */
    protected static int readLittleEndianNibble(
            final DataInputStream inputFile) throws IOException {
        // Pre-condition: - inputFile is an open DataInputStream
        // Post-condition: - inputFile is an open DataInputStream and
        //                   has been moved ahead by two bytes.
        int first = inputFile.readUnsignedByte();
        int second = inputFile.readUnsignedByte();
        final int shiftOneByte = 8;
        return (first | (second << shiftOneByte));
    }

    /**
     * Modifier for signers member.
     *
     * @param newNames Names of new signers
     */
    public final void setSigners(final String newNames) {
        signers.clear();
        String[] signerList = newNames.split(",");
        for (String s : signerList) {
            // otherwise whitespace will matter : "RRT, RCR" != "RRT,RCR"
            s = s.trim();
            if (s.length() > LENGTH_OF_SIGNER_ID) {
                throw new BuildException("Invalid COD Signer ID: '" + s + "'");
            }
            signers.add(s.trim());
        }
    }

    /**
     * Accessor for signers.
     *
     * @return Names of desired signers
     */
    public final Set<String> getSigners() {
        return signers;
    }

    /**
     * Logs messages with low priority. Only seen in ant's verbose mode
     *
     * @param message The message to be logged
     */
    private void logMsg(final String message) {
        log(message, Project.MSG_INFO); /* Use Ant's log */
    }

    /**
     * Logs error messages with high priority.
     *
     * @param errorMessage the error message to be logged
     */
    private void logError(final String errorMessage) {
        /* Use Ant's log */
        log("Error: " + errorMessage, Project.MSG_ERR);
    }

}
