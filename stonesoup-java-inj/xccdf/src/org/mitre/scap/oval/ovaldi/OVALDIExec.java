/*****************************************************************************
 *  License Agreement
 *
 *  Copyright (c) 2011, The MITRE Corporation
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright notice, this list
 *        of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright notice, this
 *        list of conditions and the following disclaimer in the documentation and/or other
 *        materials provided with the distribution.
 *      * Neither the name of The MITRE Corporation nor the names of its contributors may be
 *        used to endorse or promote products derived from this software without specific
 *        prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 *  SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 *  OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 *  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 *  This code was originally developed at the National Institute of Standards and Technology,
 *  a government agency under the United States Department of Commerce, and was subsequently
 *  modified at The MITRE Corporation. Subsequently, the resulting program is not an official
 *  NIST version of the original source code. Pursuant to Title 17, Section 105 of the United
 *  States Code the original NIST code and any accompanying documentation created at NIST are
 *  not subject to copyright protection and are in the public domain.
 *
 *****************************************************************************/



package org.mitre.scap.oval.ovaldi;

import org.mitre.scap.xccdf.util.XCCDFInterpreterProperties;
import org.mitre.util.exec.FileExecutor;
import org.mitre.util.exec.PropertyMappedFileExecutionStrategy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The OVAL Interpreter accepts a number of command-line options:
 *
 * Command Line: ovaldi [options] MD5Hash
 * 
 * Options:
 * -h          = Show options available from the command line.
 * -o filename = Path to the definitions xml file.
 *                 DEFAULT="definitions.xml"
 * -d filename = Save collected system configuration data to XML file.
 *                 DEFAULT="system-characteristics.xml"
 * -r filename = Save results to XMl file. DEFAULT="results.xml"
 * -v filename = Get external variable values from the specified XML file. 
 *                 DEFAULT="external-variables.xml"
 * -e <string> = evaluate the specified list of definitions. Supply 
 *                 definition ids as a comma seperated list like: 
 *                 oval:com.example:def:123,oval:com.example:def:234
 * -n          = Perform Schematron validation of the oval-defiitions file.
 * -c filename = Use the specified xsl for oval-definitions Schematron validation. 
 *                 DEFAULT="oval-definitions-schematron.xsl"
 * -i filename = Use data from input Systems Characteristics file.
 * -m          = Do not verify MD5 of the definitions xml file.
 * -p          = Print all information and error messages to screen.
 * -s          = Do not apply an xsl to the oval results xml.
 * -t filename = Apply the sepcified xsl to the oval results xml. 
 *                  DEFAULT="results_to_html.xsl"
 * -x filename = Output xsl formatted results to the specified file.
 *                  DEFAULT="results.html"
 * -z          = Return md5 of current definitions xml file.
 *
 */
public class OVALDIExec {
    private File definitionXmlFile;
    private File outputSystemsCharacteristicsFile;
    private File resultsFile;
    private File externalVariableFile;
    private List<String> evaluateDefinitionIds;
    private boolean performSchematronValidation = false;
    private File schematronValidationStylesheet;
    private File inputSystemsCharacteristicsFile;
    private boolean verifyDefinitionsFileMD5Hash = true;
    private boolean outputInfoAndErrorsToSTDOUT = false;
    private boolean applyXSLStylesheetToResults = true;
    private File resultsXSLStylesheetFile;
    private File resultsXSLOutputFile;
    private File externalEvaluatedDefinitionsFile;

//    private File ovaldiDir;


    /**
     * Returns the pathname of the OVAL Definition file to use. If none is
     * specified than the Interpreter will default to "definitions.xml" in the
     * Interpreter directory. (CMD: -o)
     */
    public File getDefinitionXmlFile() {
        return definitionXmlFile;
    }

    /**
     * Specifies the pathname of the OVAL Definition file to use. If none is
     * specified than the Interpreter will default to "definitions.xml" in the
     * Interpreter directory. (CMD: -o)
     */
    public void setDefinitionXmlFile(File definitionXmlFile) {
        this.definitionXmlFile = definitionXmlFile;
    }

    /**
     * Returns the pathname of the file to which collected configuration data is
     * to be saved. This data is stored in the format defined by the Systems
     * Characteristics Schema. (CMD: -d)
     */
    public File getOutputSystemsCharacteristicsFile() {
        return outputSystemsCharacteristicsFile;
    }

    /**
     * Specifies the pathname of the file to which collected configuration data is
     * to be saved. This data is stored in the format defined by the Systems
     * Characteristics Schema. (CMD: -d)
     */
    public void setOutputSystemsCharacteristicsFile(File outputSystemsCharacteristicsFile) {
        this.outputSystemsCharacteristicsFile = outputSystemsCharacteristicsFile;
    }

    /**
     * Returns the pathname of the file to which analysis results are to be
     * saved.  This data is stored according to the format defined by the OVAL
     * Results Schema.  If none is specified than the Interpreter will default
     * to "results.xml" in the Interpreter directory. (CMD: -r)
     */
    public File getResultsFile() {
        return resultsFile;
    }

    /**
     * Specifies the pathname of the file to which analysis results are to be
     * saved.  This data is stored according to the format defined by the OVAL
     * Results Schema.  If none is specified than the Interpreter will default
     * to "results.xml" in the Interpreter directory. (CMD: -r)
     */
    public void setResultsFile(File resultsFile) {
        this.resultsFile = resultsFile;
    }

    /**
     * Returns the pathname of the external variable file to use. If none is
     * specified then the Interpreter will default to "external-variables.xml"
     * in the Interpreter directory. (CMD: -v)
     */
    public File getExternalVariableFile() {
        return externalVariableFile;
    }

    /**
     * Specifies the pathname of the external variable file to use. If none is
     * specified then the Interpreter will default to "external-variables.xml"
     * in the Interpreter directory. (CMD: -v)
     */
    public void setExternalVariableFile(File externalVariableFile) {
        this.externalVariableFile = externalVariableFile;
    }

    /**
     * Returns a set of OVAL Definition ids to evaluate in the input OVAL
     * Definitions Document. Definition Ids should be comma sepearated without
     * spaces. All OVAL Definitions in the list are evaluated in the input OVAL
     * Definitions Document if they exist in the input document. Any Definition
     * Ids not found will be assigned an error status. Any Definitions in the
     * input document that are not in the list will be marked as
     * 'Not Evaluated'. (CMD: -e)
     */
    public List<String> getEvaluateDefinitionIds() {
        return evaluateDefinitionIds;
    }

    /**
     * Specifies a set of OVAL Definition ids to evaluate in the input OVAL
     * Definitions Document. Definition Ids should be comma sepearated without
     * spaces. All OVAL Definitions in the list are evaluated in the input OVAL
     * Definitions Document if they exist in the input document. Any Definition
     * Ids not found will be assigned an error status. Any Definitions in the
     * input document that are not in the list will be marked as
     * 'Not Evaluated'. (CMD: -e)
     */
    public void setEvaluateDefinitionIds(List<String> evaluateDefinitionIds) {
        this.evaluateDefinitionIds = evaluateDefinitionIds;
    }

    /**
     * If set run Schematron validation on the input OVAL Definitions Document.
     * Schematron validation is currently optional. In the future the OVAL
     * Compatibility program will likely require Schematron validation. Once
     * Schematron validation is required this reference implementation will
     * also require Schematon validation. (CMD: -n)
     */
    public boolean isPerformSchematronValidation() {
        return performSchematronValidation;
    }

    /**
     * If set run Schematron validation on the input OVAL Definitions Document.
     * Schematron validation is currently optional. In the future the OVAL
     * Compatibility program will likely require Schematron validation. Once
     * Schematron validation is required this reference implementation will
     * also require Schematon validation. (CMD: -n)
     */
    public void setPerformSchematronValidation(boolean performSchematronValidation) {
        this.performSchematronValidation = performSchematronValidation;
    }

    /**
     * Return the pathname of the oval-definitions-schematron.xsl to be used
     * for Schematron validation. If none is specified then the Interpreter will
     * default to "oval-definitions-schematron.xsl" in the Interpreter
     * directory. (CMD: -c)
     */
    public File getSchematronValidationStylesheet() {
        return schematronValidationStylesheet;
    }

    /**
     * Specifies the pathname of the oval-definitions-schematron.xsl to be used
     * for Schematron validation. If none is specified then the Interpreter will
     * default to "oval-definitions-schematron.xsl" in the Interpreter
     * directory. (CMD: -c)
     */
    public void setSchematronValidationStylesheet(File schematronValidationStylesheet) {
        this.schematronValidationStylesheet = schematronValidationStylesheet;
    }

    /**
     * Returns the pathname of a System Characteristics file that is to be
     * used as the basis of the analysis.  In this mode, the Interpreter does
     * not perform data collection on the local system, but relies upon the
     * input file, which may have been generated on another system. (CMD: -i)
     */
    public File getInputSystemsCharacteristicsFile() {
        return inputSystemsCharacteristicsFile;
    }

    /**
     * Specifies the pathname of a System Characteristics file that is to be
     * used as the basis of the analysis.  In this mode, the Interpreter does
     * not perform data collection on the local system, but relies upon the
     * input file, which may have been generated on another system. (CMD: -i)
     */
    public void setInputSystemsCharacteristicsFile(File inputSystemsCharacteristicsFile) {
        this.inputSystemsCharacteristicsFile = inputSystemsCharacteristicsFile;
    }

    /**
     * Run without requiring an MD5 checksum.  Running the Interpreter with this
     * option DISABLES an important security feature.  In normal usage, a
     * trusted checksum provided on the command line is used to verify the
     * integrity of the OVAL Definitions file. (CMD: -m)
     *
     * Use of this option is recommended only when testing your own draft
     * definitions before submitting them to the OVAL Community Forum for
     * public review.
     */
    public boolean isVerifyDefinitionsFileMD5Hash() {
        return verifyDefinitionsFileMD5Hash;
    }

    /**
     * Run without requiring an MD5 checksum.  Running the Interpreter with this
     * option DISABLES an important security feature.  In normal usage, a
     * trusted checksum provided on the command line is used to verify the
     * integrity of the OVAL Definitions file. (CMD: -m)
     *
     * Use of this option is recommended only when testing your own draft
     * definitions before submitting them to the OVAL Community Forum for
     * public review.
     */
    public void setVerifyDefinitionsFileMD5Hash(boolean verifyDefinitionsFileMD5Hash) {
        this.verifyDefinitionsFileMD5Hash = verifyDefinitionsFileMD5Hash;
    }

    /**
     * Verbose output.  Print all information and error message to the console.
     * (CMD: -p)
     */
    public boolean isOutputInfoAndErrorsToSTDOUT() {
        return outputInfoAndErrorsToSTDOUT;
    }

    /**
     * Verbose output.  Print all information and error message to the console.
     * (CMD: -p)
     */
    public void setOutputInfoAndErrorsToSTDOUT(boolean outputInfoAndErrorsToSTDOUT) {
        this.outputInfoAndErrorsToSTDOUT = outputInfoAndErrorsToSTDOUT;
    }

    /**
     * If set do not apply the xsl to the OVAL Results xml. (CMD: -s)
     */
    public boolean isApplyXSLStylesheetToResults() {
        return applyXSLStylesheetToResults;
    }

    /**
     * If set do not apply the xsl to the OVAL Results xml. (CMD: -s)
     */
    public void setApplyXSLStylesheetToResults(boolean applyXSLStylesheetToResults) {
        this.applyXSLStylesheetToResults = applyXSLStylesheetToResults;
    }

    /**
     * Returns the pathname of the xsl file which should be used to
     * transform the oval results. If none is specified then the Interpreter
     * will default to "results_to_html.xsl" in the Interpreter directory.
     * (CMD: -t)
     */
    public File getResultsXSLStylesheetFile() {
        return resultsXSLStylesheetFile;
    }

    /**
     * Specifies the pathname of the xsl file which should be used to
     * transform the oval results. If none is specified then the Interpreter
     * will default to "results_to_html.xsl" in the Interpreter directory.
     * (CMD: -t)
     */
    public void setResultsXSLStylesheetFile(File resultsXSLStylesheetFile) {
        this.resultsXSLStylesheetFile = resultsXSLStylesheetFile;
    }

    /**
     * Returns the pathname of the file which xsl transform results are to be
     * saved.  If none is specified then the Interpreter will default to
     * "results.html" in the Interpreter directory. (CMD: -x)
     */
    public File getResultsXSLOutputFile() {
        return resultsXSLOutputFile;
    }

    /**
     * Specifies the pathname of the file which xsl transform results are to be
     * saved.  If none is specified then the Interpreter will default to
     * "results.html" in the Interpreter directory. (CMD: -x)
     */
    public void setResultsXSLOutputFile(File resultsXSLOutputFile) {
        this.resultsXSLOutputFile = resultsXSLOutputFile;
    }

//    public File getOvaldiDir() {
//        return ovaldiDir;
//    }
//
//    public void setOvaldiDir(File ovaldiDir) {
//        this.ovaldiDir = ovaldiDir;
//    }

    public File getExternalEvaluatedDefinitionsFile() {
        return externalEvaluatedDefinitionsFile;
    }

    public void setExternalEvaluatedDefinitionsFile(File externalEvaluatedDefinitionsFile) {
        this.externalEvaluatedDefinitionsFile = externalEvaluatedDefinitionsFile;
    }

    /**
     * @param args the command line arguments
     */
/*
    public static void main(String[] args) throws Exception {
        OVALDIExec exec = new OVALDIExec();
//        exec.setOvaldiDir(new File("C:\\Program Files\\OVAL\\ovaldi"));
        exec.setDefinitionXmlFile(new File("C:\\Program Files\\OVAL\\ovaldi\\SCAP-WinXPPro-CPE.xml"));
        exec.setVerifyDefinitionsFileMD5Hash(false);
        exec.setOutputInfoAndErrorsToSTDOUT(true);

        File resultFile = new File("CPE-result.xml");
        exec.setResultsFile(resultFile);

        int exitValue = exec.exec(System.out);
        System.out.println("command exited with exit code: "+exitValue);

        OVALResult result = new OVALResult(resultFile);
    }
*/
    public List<String> buildArgumentList() {
        List<String> result = new ArrayList<String>();

        // -p          = Print all information and error messages to screen.
        if (outputInfoAndErrorsToSTDOUT) result.add("-p");

        // -o filename = Path to the definitions xml file.
        // DEFAULT="definitions.xml"
        if (definitionXmlFile != null) {
            result.add("-o");
            result.add(definitionXmlFile.getAbsolutePath());
        }

        // -d filename = Save collected system configuration data to XML file.
        // DEFAULT="system-characteristics.xml"
        if (outputSystemsCharacteristicsFile != null) {
            result.add("-d");
            result.add(outputSystemsCharacteristicsFile.getAbsolutePath());
        }

        // -r filename = Save results to XMl file.
        // DEFAULT="results.xml"
        if (resultsFile != null) {
            result.add("-r");
            result.add(resultsFile.getAbsolutePath());
        }

        // -v filename = Get external variable values from the specified XML
        // file.
        // DEFAULT="external-variables.xml"
        if (externalVariableFile != null) {
            result.add("-v");
            result.add(externalVariableFile.getAbsolutePath());
        }

        // -f <string>  = path to a file containing a list of definitions to be
        // evaluated. The file must comply with the evaluation-id schema.
        if (externalEvaluatedDefinitionsFile != null) {
            result.add("-f");
            result.add(externalEvaluatedDefinitionsFile.getAbsolutePath());
        }
        
        // -e <string> = evaluate the specified list of definitions. Supply
        // definition ids as a comma seperated list like:
        // oval:com.example:def:123,oval:com.example:def:234
        if (evaluateDefinitionIds != null && evaluateDefinitionIds.size() > 0) {
            String s = null;
            for (String id : evaluateDefinitionIds) {
                if (id == null || id.length() == 0) continue;
                if (s == null) {
                    s = id;
                } else {
                    s = s + "," + id;
                }
            }
            result.add("-e");
            result.add(s);
        }

        // -n          = Perform Schematron validation of the oval-defiitions
        // file.
        if (performSchematronValidation) result.add("-n");

        // -c filename = Use the specified xsl for oval-definitions Schematron
        // validation.
        // DEFAULT="oval-definitions-schematron.xsl"
        if (schematronValidationStylesheet != null) {
            result.add("-c");
            result.add(schematronValidationStylesheet.getAbsolutePath());
        }

        // -i filename = Use data from input Systems Characteristics file.
        if (inputSystemsCharacteristicsFile != null) {
            result.add("-i");
            result.add(inputSystemsCharacteristicsFile.getAbsolutePath());
        }

        // -m          = Do not verify MD5 of the definitions xml file.
        if (!verifyDefinitionsFileMD5Hash) result.add("-m");

        // -s          = Do not apply an xsl to the oval results xml.
        if (!applyXSLStylesheetToResults) result.add("-s");

        // -t filename = Apply the sepcified xsl to the oval results xml.
        // DEFAULT="results_to_html.xsl"
        if (resultsXSLStylesheetFile != null) {
            result.add("-t");
            result.add(resultsXSLStylesheetFile.getAbsolutePath());
        }

        // -x filename = Output xsl formatted results to the specified file.
        // DEFAULT="results.html"
        if (resultsXSLOutputFile != null) {
            result.add("-t");
            result.add(resultsXSLOutputFile.getAbsolutePath());
        }
        return result;
    }


    public int exec() throws IOException, InterruptedException {
      return this.exec( System.out );
    }


    public int exec(final OutputStream out) throws IOException, InterruptedException, FileNotFoundException {
      XCCDFInterpreterProperties xccdfProperties = XCCDFInterpreterProperties.getInstance();

      PropertyMappedFileExecutionStrategy executionStrategy 
        = new PropertyMappedFileExecutionStrategy(  xccdfProperties.getProperties(),
                                                    XCCDFInterpreterProperties.PROP_OVAL_DIR, 
                                                    XCCDFInterpreterProperties.PROP_OVAL_BIN );

        String executable = executionStrategy.getExecutableFile().getCanonicalPath();
        FileExecutor executor = new FileExecutor(executionStrategy);
        executor.setRedirectErrorStream( this.outputInfoAndErrorsToSTDOUT );

        List<String> arguments = buildArgumentList();


        if ( this.outputInfoAndErrorsToSTDOUT ){
          if (!arguments.isEmpty()) {
            System.out.println("Invoking " + executable + " with arguments:");
            for (String argument : arguments) {
              System.out.println("  "+argument);
            }
          }
        }

        Process p = executor.execute(arguments);

        String processName = xccdfProperties.getPropertyValue( XCCDFInterpreterProperties.PROP_OVAL_BIN ) + " : " + this.getDefinitionXmlFile().getName();
        IORedirect  ioRedirect = new IORedirect( p.getInputStream(), out, outputInfoAndErrorsToSTDOUT );
        ioRedirect.setProcessName( processName );

        Thread t = ioRedirect; //totally unnecessary, just to make it clear that IORedirect is a Thread object
        t.start();
        t.join();
        
        return p.waitFor();
    }
}
