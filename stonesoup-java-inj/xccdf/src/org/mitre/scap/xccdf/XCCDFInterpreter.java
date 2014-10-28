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



package org.mitre.scap.xccdf;

import org.mitre.scap.xccdf.properties.BuildProperties;
import gov.nist.checklists.xccdf.x11.BenchmarkDocument;
import gov.nist.checklists.xccdf.x11.CheckType;
import gov.nist.checklists.xccdf.x11.GroupType;
import gov.nist.checklists.xccdf.x11.ItemType;
import gov.nist.checklists.xccdf.x11.ModelDocument;
import gov.nist.checklists.xccdf.x11.ProfileRefineRuleType;
import gov.nist.checklists.xccdf.x11.ProfileRefineValueType;
import gov.nist.checklists.xccdf.x11.ProfileSelectType;
import gov.nist.checklists.xccdf.x11.ProfileSetValueType;
import gov.nist.checklists.xccdf.x11.ProfileType;
import gov.nist.checklists.xccdf.x11.RuleType;
import gov.nist.checklists.xccdf.x11.SelChoicesType;
import gov.nist.checklists.xccdf.x11.SelNumType;
import gov.nist.checklists.xccdf.x11.SelStringType;
import gov.nist.checklists.xccdf.x11.SelectableItemType;
import gov.nist.checklists.xccdf.x11.URIidrefType;
import gov.nist.checklists.xccdf.x11.ValueType;
import gov.nist.checklists.xccdf.x11.VersionType;
import org.mitre.scap.cpe.CPEEvaluationException;
import org.mitre.scap.cpe.CPELanguage;
import org.mitre.scap.cpe.CPEName;
import org.mitre.scap.cpe.CPEResolver;
import org.mitre.scap.ocil.check.OCILCheckSystem;
import org.mitre.scap.oval.ovaldi.check.OVALCheckSystem;
import org.mitre.scap.xccdf.check.CheckSystemRegistry;
import org.mitre.scap.xccdf.util.ProfileIdentifiableAdapter;
import org.mitre.scap.xccdf.util.ItemIdentifiableAdapter;
import org.mitre.scap.xccdf.util.ProfileSelectorResolver;
import org.mitre.scap.xccdf.util.PropertyExtensionResolver;
import org.mitre.scap.xccdf.util.resolvers.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.mitre.cpe.language.x20.PlatformType;

/**
 * An XCCDF benchmark is loaded using the following steps described
 * in the XCCDF Specification
 * 
 * 1) Loading.Import - Import the XCCDF document into the program and build an
 * initial internal representation of the Benchmark object, Groups, Rules, and
 * other objects. If the file cannot be read or parsed, then Loading fails.
 * 
 * At the beginning of this step, any inclusion processing specified with
 * XInclude elements should be performed. The resulting XML information set
 * should be validated against the XCCDF schema given in Appendix A.
 * 
 * 2) Loading.Noticing - For each notice property of the Benchmark object, add
 * the notice to the tool’s set of legal notices. If a notice with an identical
 * id value is already a member of the set, then replace it. If the Benchmark’s
 * resolved property is set, then Loading succeeds, otherwise go to the next
 * step: Loading.Resolve.Items.
 * 
 * 3a) Loading.Resolve.Items For each Item in the Benchmark that has an extends
 * property, resolve it by using the following steps: (1) if the Item is Group,
 * resolve all the enclosed Items, (2) resolve the extended Item, (3) prepend
 * the property sequence from the extended Item to the extending Item, (4) if
 * the Item is a Group, assign values for the id properties of Items copied
 * from the extended Group, (5) remove duplicate properties and apply property
 * overrides, and (6) remove the extends property. If any Item’s extends
 * property identifier does not match the identifier of a visible Item of the
 * same type, then Loading fails. If the directed graph formed by the extends
 * properties includes a loop, then Loading fails. Otherwise, go to the next
 * step: Loading.Resolve.Profiles.
 * 
 * SCAP Notes:
 * 
 *      In SCAP use of extended groups and rules is not allowed.
 *
 * 3b) Loading.Resolve.Profiles -  For each Profile in the Benchmark that has
 * an extends property, resolve the set of properties in the extending Profile
 * by applying the following steps:
 *      (1) resolve the extended Profile,
 *      (2) prepend the property sequence from the extended Profile to that of
 *      the extending Profile,
 *      (3) remove all but the last instance of duplicate properties.
 * 
 * If any Profile's extends property identifier does not match the identifier of
 * another Profile in the Benchmark, then Loading fails. If the directed graph
 * formed by the extends properties of Profiles includes a loop, then Loading
 * fails. Otherwise, go to Loading.Resolve.Abstract.
 * 
 * 3c) Loading.Resolve.Abstract - For each Item in the Benchmark for which the
 * abstract property is true, remove the Item. For each Profile in the Benchmark
 * for which the abstract property is true, remove the Profile. Go to the next
 * step: Loading.Resolve.Finalize.
 * 
 * 3d) Loading.Resolve.Finalize - Set the Benchmark resolved property to true;
 * Loading succeeds.
 *
 */
public class XCCDFInterpreter {
    private static Logger log = Logger.getLogger(XCCDFInterpreter.class.getName());

    private final File xccdfFile;
    private final BenchmarkDocument document;
    private final BenchmarkDocument.Benchmark benchmark;

    private boolean processCPE = true;
    private boolean processChecks = true;
    private boolean displayResults = true;
    public static boolean verboseOutput = false;
    private final File workingDir = new File(new File("tmp").getAbsolutePath()).getParentFile().getAbsoluteFile();
    private File resultDirectory = new File(workingDir,"results");
    private File cpeDictionaryFile = new File("cpe-dictionary-2.0.xml");
    private File cpeOVALDefinitionFile = new File("oval-inventory.xml");
    private String xccdfResultsFilename = null;

    private String profileId;
    private String ssValidationId;
    private boolean initialized = false;
    private Map<String, ProfileType> profileMap;
    private Map<String, ValueType> valueMap;
    private Map<String, GroupType> groupMap;
    private Map<String, RuleType> ruleMap;
    private Map<String, ItemType> itemMap;
    private Map<String, SelectableItemType> selectableItemMap;
    private Map<String, List<ItemType>> clusterMap;
    private Map<ItemType,GroupType> parentMap = new HashMap<ItemType,GroupType>();
    private Map<String,CPELanguage> idToCpeLanguageMap = new HashMap<String,CPELanguage>();
    private final Map<String,ModelDocument.Model> supportedScoringModels = new HashMap<String,ModelDocument.Model>();
    public  static final Calendar EXECUTION_TIME  = Calendar.getInstance();
    private static final java.text.SimpleDateFormat DATE_FORMATTER  = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    public  static String EXECUTION_TIME_STR  = "";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, XmlException, URISyntaxException {
        XCCDFInterpreter.generateExecutionTimeStr();
        BuildProperties buildProperties = BuildProperties.getInstance();


        //System.out.println();
        //System.out.println( buildProperties.getApplicationName()+" v"+ buildProperties.getApplicationVersion()+" build "+buildProperties.getApplicationBuild());
        //System.out.println("--------------------");
        //outputOsProperties();
        //System.out.println("--------------------");
        //System.out.println();


        // Define the commandline options
        @SuppressWarnings("static-access")
        Option help = OptionBuilder
                .withDescription("display usage")
                .withLongOpt("help")
                .create('h');

        @SuppressWarnings("static-access")
        Option workingDir = OptionBuilder
                .hasArg()
                .withArgName("FILE")
                .withDescription("use result directory FILE")
                .withLongOpt("result-dir")
                .create('R');

        @SuppressWarnings("static-access")
        Option xccdfResultFilename = OptionBuilder
                .hasArg()
                .withArgName("FILENAME")
                .withDescription("use result filename FILENAME")
                .withLongOpt("xccdf-result-filename")
                .create("F");

        @SuppressWarnings("static-access")
        Option nocpe = OptionBuilder
                .withDescription("do not process CPE references")
                .withLongOpt("no-cpe")
                .create();


        @SuppressWarnings("static-access")
        Option noresults = OptionBuilder
                .withDescription("do not display rule results")
                .withLongOpt("no-results")
                .create();


        @SuppressWarnings("static-access")
        Option nocheck = OptionBuilder
                .withDescription("do not process checks")
                .withLongOpt("no-check")
                .create();

        @SuppressWarnings("static-access")
        Option profile = OptionBuilder
                .hasArg()
                .withArgName("PROFILE")
                .withDescription("use given profile id")
                .withLongOpt("profile-id")
                .create('P');

        @SuppressWarnings("static-access")
        Option ssValidation = OptionBuilder
                .hasArg()
                .withArgName("SS-VALIDATION")
                .withDescription("use given validation id")
                .withLongOpt("ssValidation-id")
                .create("S");

        @SuppressWarnings("static-access")
        Option cpeDictionary = OptionBuilder
                .hasArg()
                .withArgName("FILE")
                .withDescription("use given CPE 2.0 Dictionary file")
                .withLongOpt("cpe-dictionary")
                .create('C');

        @SuppressWarnings("static-access")
        Option cpeOVALDefinition = OptionBuilder
                .hasArg()
                .withArgName("FILE")
                .withDescription("use given CPE OVAL definition file for CPE evaluation")
                .withLongOpt("cpe-oval")
                .create('c');

        @SuppressWarnings("static-access")
        Option verbose = OptionBuilder
                .withDescription("produce verbose output")
                .create("v");

        // Build the options list
        Options options = new Options();
        options.addOption(help);
        options.addOption(workingDir);
        options.addOption(xccdfResultFilename);
        options.addOption(profile);
        options.addOption(ssValidation);
        options.addOption(nocpe);
        options.addOption(noresults);
        options.addOption(nocheck);
        options.addOption(cpeDictionary);
        options.addOption(cpeOVALDefinition);
        options.addOption(verbose);

        // create the parser
        CommandLineParser parser = new GnuParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            String[] remainingArgs = line.getArgs();

            if (line.hasOption("help") || remainingArgs.length != 1) {
                if (remainingArgs.length != 1) {
                    System.err.print("Invalid arguments: ");
                    for (String arg : remainingArgs) {
                        System.err.print( "'" + arg + "' ");
                    }
                    System.out.println();
                }

                // automatically generate the help statement
                System.out.println();
                showHelp(options);
                System.exit(0);
            }

            File xccdfFile = new File(remainingArgs[0]);

            if (!xccdfFile.exists()) {
                System.err.println("!! the specified XCCDF file '" + xccdfFile.getAbsolutePath() + "' does not exist!");
                System.exit(1);
            }

            XCCDFInterpreter interpreter = new XCCDFInterpreter(xccdfFile.getCanonicalFile());

            //System.out.println("** validating XCCDF content");

            if (!interpreter.validate()) {
                System.err.println("!! the XCCDF document is invalid. aborting.");
                System.exit(8);
            }
            
            
            if (line.hasOption( verbose.getOpt() )) {
                verboseOutput = true;
                interpreter.setVerboseOutput(true);
            }


            if (line.hasOption(workingDir.getOpt())) {
                String lineOpt          = line.getOptionValue( workingDir.getOpt() );
                String workingDirValue  = ( lineOpt  == null ) ? "" : lineOpt;

                File f = new File( workingDirValue );
                if (!f.exists()) {
                    if( verboseOutput ) System.out.println("** creating directory: "+f.getAbsolutePath());
                    if (!f.mkdirs()) {
                        System.err.println("!! unable to create the result directory: "+f.getAbsolutePath());
                        System.exit(2);
                    }
                }

                if (!f.isDirectory()) {
                    System.err.println("!! the path specified for the result directory is not a directory: "+f.getAbsolutePath());
                    System.exit(3);
                }

                if (!f.canWrite()) {
                    System.err.println("!! the path specified for the result directory is not writable: "+f.getAbsolutePath());
                    System.exit(4);
                }
                interpreter.setResultDirectory(f);
            }

            if (line.hasOption(xccdfResultFilename.getOpt())) {
                interpreter.setXccdfResultsFilename( line.getOptionValue( xccdfResultFilename.getOpt() ));
            }

            if (line.hasOption(profile.getOpt())) {
                interpreter.setProfileId( line.getOptionValue( profile.getOpt() ) );
            }

            if (line.hasOption(ssValidation.getOpt())) {
                interpreter.setssValidationId( line.getOptionValue( ssValidation.getOpt() ) );
            }

            if (line.hasOption(nocpe.getLongOpt())) {
                interpreter.setProcessCPE(false);
            }


            if(line.hasOption( noresults.getLongOpt())){
                interpreter.setDisplayResults(false);
            }


            if (line.hasOption(nocheck.getLongOpt())) {
                interpreter.setProcessChecks(false);
            }


            if( interpreter.processCPE == true ){

              if (line.hasOption(cpeDictionary.getOpt())) {
                  String lineOpt  = line.getOptionValue( cpeDictionary.getOpt() );
                  String cpeDict  = ( lineOpt  == null ) ? "" : lineOpt;

                  File f = new File( cpeDict );

                  if (!f.exists()) {
                      System.err.println("The CPE dictionary file does not exist: "+f.getAbsolutePath());
                      System.exit(5);
                  }

                  if (!f.isFile()) {
                      System.err.println("The path specified for the CPE dictionary file is not a file: "+f.getAbsolutePath());
                      System.exit(6);
                  }

                  if (!f.canRead()) {
                      System.err.println("The path specified for the CPE dictionary file is not readable: "+f.getAbsolutePath());
                      System.exit(7);
                  }
                  interpreter.setCPEDictionaryFile(f);
              }

              if (line.hasOption(cpeOVALDefinition.getOpt())) {
                  String lineOpt  = line.getOptionValue( cpeOVALDefinition.getOpt() );
                  String cpeOVAL  = ( lineOpt  == null ) ? "" : lineOpt;

                  File f = new File( cpeOVAL );

                  if (!f.exists()) {
                      System.err.println("!! the CPE OVAL inventory definition file does not exist: "+f.getAbsolutePath());
                      System.exit(5);
                  }

                  if (!f.isFile()) {
                      System.err.println("!! the path specified for the CPE OVAL inventory definition file is not a file: "+f.getAbsolutePath());
                      System.exit(6);
                  }

                  if (!f.canRead()) {
                      System.err.println("!! the path specified for the CPE OVAL inventory definition file is not readable: "+f.getAbsolutePath());
                      System.exit(7);
                  }
                  interpreter.setCPEOVALDefinitionFile(f);
              }
            
            } // END IF processCPE
            
            interpreter.process();


        } catch (ParseException ex) {
            System.err.println("!! parsing failed : " + ex.getMessage());
            System.out.println();
            showHelp(options);
        } catch (ProfileNotFoundException ex) {
            if( verboseOutput == true ) ex.printStackTrace();
            else System.err.println("!! checklist processing failed : " + ex.getMessage());
        } catch (CircularReferenceException ex) {
            System.err.println("!! checklist processing failed : " + ex.getMessage());
            if( verboseOutput == true ) ex.printStackTrace();
            else System.err.println("!! checklist processing failed : " + ex.getMessage());
        } catch (ExtensionScopeException ex) {
            if( verboseOutput == true ) ex.printStackTrace();
            else System.err.println("!! checklist processing failed : " + ex.getMessage());
        } catch (ItemNotFoundException ex) {
            if( verboseOutput == true ) ex.printStackTrace();
            else System.err.println("!! checklist processing failed : " + ex.getMessage());
        } catch (PropertyNotFoundException ex) {
            if( verboseOutput == true ) ex.printStackTrace();
            else System.err.println("!! checklist processing failed : " + ex.getMessage());
        } catch (CPEEvaluationException ex) {
            if( verboseOutput == true ) ex.printStackTrace();
            else System.err.println("!! checklist processing failed : " + ex.getMessage());
        } catch ( Exception ex ){
            if( verboseOutput == true ) ex.printStackTrace();
            else System.err.println("!! checklist processing failed : " + ex.getMessage());
        }

    }


    private static void generateExecutionTimeStr(){
      EXECUTION_TIME_STR = DATE_FORMATTER.format( EXECUTION_TIME.getTime() );
    }


    private static void outputOsProperties() {
    	Properties properties = System.getProperties();

    	List<String> keys = new LinkedList<String>();
    	for (Object key : properties.keySet()) {
    		if (key.toString().startsWith("os.")) {
    			keys.add(key.toString());
    		}
    	}
    	Collections.sort(keys);
    	for (String key : keys) {
    		System.out.print(key);
    		System.out.print(": ");
    		System.out.println(System.getProperty(key));
    	}
	}

	private static void showHelp(final Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar xccdfexec.jar XCCDF_SOURCE_FILE", options);
    }

    public XCCDFInterpreter(final File xccdfFile) throws XmlException, IOException {
        this.xccdfFile = xccdfFile;

        //System.out.println("** parsing checklist "+xccdfFile.getAbsolutePath());
        //System.out.println("** parsing checklist ");
        document = BenchmarkDocument.Factory.parse(xccdfFile);
        benchmark = document.getBenchmark();

        CheckSystemRegistry.register( new OVALCheckSystem() );
        CheckSystemRegistry.register( new OCILCheckSystem() );
    }

    public File getXCCDFFile() { return xccdfFile; }

    public BenchmarkDocument getDocument() {
        return document;
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public boolean isProcessCPE() {
        return processCPE;
    }

    public void setProcessCPE(boolean processCPE) {
        this.processCPE = processCPE;
    }

    public void setDisplayResults( boolean displayResults ){
        this.displayResults = displayResults;
    }

    public boolean isDisplayResults() { return this.displayResults; }


    public boolean isProcessChecks() {
        return processChecks;
    }

    public void setProcessChecks(boolean processChecks) {
        this.processChecks = processChecks;
    }

    public boolean isVerboseOutput() {
        return verboseOutput;
    }

    public void setVerboseOutput(boolean verboseOutput) {
        this.verboseOutput = verboseOutput;
    }

    public File getCPEDictionaryFile() {
        return cpeDictionaryFile;
    }

    private void setCPEDictionaryFile(final File file) {
        this.cpeDictionaryFile = file;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getssValidationId() {
        return ssValidationId;
    }

    public void setssValidationId(String ssValidationId) {
        this.ssValidationId = ssValidationId;
    }

    public boolean validate() {
        XmlOptions options = new XmlOptions();
        options.setLoadLineNumbers();
        return document.validate(options);
    }

    public void resolve() throws CircularReferenceException, ExtensionScopeException, Exception {
        try {
            //System.out.println("** resolving the checklist '" + benchmark.getId() + "'");

            /*
             * Loading.Import
             */

            if( this.isVerboseOutput() ){
              System.out.println(" - Loading.Import");
            }
            
            init();

            /*
             * Loading.Noticing
             */
            if( this.isVerboseOutput() ){
              System.out.println(" - Loading.Noticing");
            }
            
            // TODO: P4 - Add support Loading.Noticing

            if (!benchmark.getResolved()) {
                /*
                 * Loading.Resolve.Items
                 */
                if( this.isVerboseOutput() ){
                  System.out.println(" - Loading.Resolve.Items");
                }
                
                if( this.itemMap != null ){
                  for( ItemType item : this.itemMap.values() ){
                    this.resolveItem( item );
                  }
                }
                
                /*
                 * Loading.Resolve.Profiles
                 */
                  if( this.isVerboseOutput() ){
                    System.out.println(" - Loading.Resolve.Profiles");
                  }
                
                  if (profileMap != null) {
                    // Iterate over each Profile calling the resolve() method on each one.
                    for (ProfileType profile : profileMap.values()) {
                        resolveProfile(profile);
                    }
                }

                /*
                 * Loading.Resolve.Abstract
                 */
                if( this.isVerboseOutput() ){
                  System.out.println(" - Loading.Resolve.Abstract");
                }
                
                // Remove abstract Profiles
                if (profileMap != null) {
                    Iterator<ProfileType> i = benchmark.getProfileList().iterator();
                    while (i.hasNext()) {
                        ProfileType profile = i.next();
                        if (profile.getAbstract()) {
                            if (isVerboseOutput()) {
                                System.out.println("removing abstract profile: "+profile.getId());
                            }

                            profileMap.remove(profile.getId());
                            i.remove();
                        }
                    }
                }

                /*
                 * Loading.Resolve.Finalize
                 */
                if (isVerboseOutput()) {
                    System.out.println();
                }

                if( this.isVerboseOutput() ){
                  System.out.println(" - Loading.Resolve.Finalize");
                }
                
                // Mark the Benchmark as resolved
                benchmark.setResolved(true);
                
            }
            XmlOptions opts = new XmlOptions();
            opts.setSavePrettyPrint();


            //File resolvedFile = File.createTempFile("xccdf-resolved", ".xml",resultDirectory);
            File resolvedFile = new File( this.resultDirectory, "xccdf-resolved_" + EXECUTION_TIME_STR + ".xml" );

            //System.out.println("** saving resolved checklist to "+resolvedFile.getCanonicalPath());
            getDocument().save(resolvedFile,opts);
        } catch (IOException ex) {
          if( verboseOutput ) log.log(Level.SEVERE, null, ex);
          else System.err.println("!! encountered an IO issue: " + ex.getLocalizedMessage() );
            
        }
    }

    public void process()
                throws ProfileNotFoundException, CircularReferenceException,
                    ItemNotFoundException, PropertyNotFoundException,
                    IOException, ExtensionScopeException,
                    CPEEvaluationException,
                    URISyntaxException, Exception {

        if (!resultDirectory.exists()) resultDirectory.mkdirs();
        
        File xccdfResultsFile = null;
        if (xccdfResultsFilename != null) {
            xccdfResultsFile = new File(resultDirectory, xccdfResultsFilename);
        } else {
            xccdfResultsFile = new File( this.resultDirectory, "xccdf-results_" + EXECUTION_TIME_STR + ".xml" );
        }

        if ( this.isVerboseOutput() ){
          System.out.println();
          System.out.println("Using the following commandline parameters:");
          System.out.println("  Result Directory: " + resultDirectory.getAbsolutePath());
          System.out.println("  XCCDF File: " + xccdfFile.getAbsolutePath());
          System.out.println("  Profile: " + profileId);
          System.out.println("  ssValidation: " + ssValidationId);
          System.out.println("  CPE Dictionary File: " + cpeDictionaryFile.getAbsolutePath());
          System.out.println("  CPE OVAL File: " + this.cpeOVALDefinitionFile.getAbsolutePath());
          System.out.println("  Checks Enabled: " + processChecks);
          System.out.println("  CPE Enabled: " + processCPE);
          System.out.println("  Verbose Output Enabled: " + verboseOutput);
          System.out.println("  XCCDF Result File: "+ xccdfResultsFile.getAbsolutePath());
          System.out.println();
        }
        /*
         * 1 - Load and resolve the Benchmark
         */
        resolve();

        // Benchmark Processing

        /*
         * Benchmark.Front - Process the properties of the Benchmark object
         */
        if( this.isVerboseOutput() ){
          System.out.println(" - Benchmark.Front");
        }

        // Handle CPEs
        CPEResolver cpeResolver;
        if (isProcessCPE()) {
            System.out.println("** resolving CPE dictionary and platform definitions");

            cpeResolver = new CPEResolver(
                idToCpeLanguageMap,
                this.getCPEDictionaryFile(),
                this.getCPEOVALDefinitionFile(),
                this.getResultDirectory());

            if (this.isVerboseOutput()) {
                for (Map.Entry<CPEName,CPEResolver.Status> entry : cpeResolver.getCPEStatusMap().entrySet()) {
                    System.out.println("CPE "+entry.getKey()+": "+entry.getValue());
                }

                for (Map.Entry<String,CPEResolver.Status> entry : cpeResolver.getCPELanguageStatusMap().entrySet()) {
                    System.out.println("platform "+entry.getKey()+": "+entry.getValue());
                }
            }

            // Qualify benchmark
            boolean isApplicable = false;
            String match = null;
            if (benchmark.sizeOfPlatformArray() > 0) {
                if( this.isVerboseOutput() ){
                  System.out.println("** checking for platform applicability");
                }
                // process CPEs
                for (URIidrefType cpeId : benchmark.getPlatformList()) {
                    if (cpeResolver.evaluateCPE(cpeId.getIdref()) == CPEResolver.Status.PASS) {
                        match = cpeId.getIdref();
                        isApplicable = true;
                        break;
                    }
                }
            } else {
                isApplicable = true;
            }

            if (isVerboseOutput()) {
                if (isApplicable && match != null) {
                    System.out.println("platform match: "+match);
                } else if (!isApplicable) {
                    System.out.println("platform match: failed");
                }
            }
            if (!isApplicable) {
                System.err.println("!! the target checklist is not applicable to this plaform. aborting...");
                return;
            }
        } else {
            cpeResolver = null;
        }

        /*
         * Benchmark.Profile - If a Profile id was specified, then apply the
         * settings in the Profile to the Items of the Benchmark
         */
        if( this.isVerboseOutput() ){
          System.out.println(" - Benchmark.Profile: "+(profileId != null ? profileId : "none"));
        }

        if (profileId != null) {
            this.applyProfile(profileId);
        }

        XCCDFProcessor processor = new XCCDFProcessor(this,cpeResolver);

        if( this.isVerboseOutput()){
          System.out.println( " - Benchmark.Content" );
        }

        BenchmarkDocument resultDocument = processor.process();

        if( this.isProcessChecks() && this.isDisplayResults() ){
          System.out.println();
          processor.printRuleResults();
          System.out.println();
        }


        //System.out.println("** saving results to "+xccdfResultsFile.getCanonicalPath());
        System.out.print("** saving results to ");
        //System.out.println();

        resultDocument.save(xccdfResultsFile,new XmlOptions().setSavePrettyPrint());
    }

    public void applyProfile(final String id)
                throws ProfileNotFoundException,
                    ItemNotFoundException, PropertyNotFoundException {
        /*
         * Loading a Benchmark creates an internal representation of the XCCDF
         * object tree. Applying a Profile makes modifications to that object
         * tree (set-value changes the value property of a Value object,
         * refine-value changes the applied selector for a Value object, and
         * thus changes its value and other properties, select changes the
         * selected property on Rule and Group objects, etc.) Depending on the
         * tool, the user might also be allowed to adjust values, perhaps using
         * a customization UI.
         *
         * When writing an XCCDF benchmark back out to a file, possibly to
         * encapsulate the results of doing a Benchmark compliance test run,
         * any changes to the internal representation should be reflected in the
         * output XML file.
         *
         * It is quite possible for refine-value and set-value to end up in
         * 'conflict', since both can affect the value of a Value object. That's
         * why the order of steps in building the internal representation is
         * important. The steps relevant to handling Values are:
         *
         * 1 - Load and resolve the Benchmark
         * 2 - Apply a Profile (if any)
         * 2a - apply refine-value
         * 2b - apply set-value
         * 3 - Apply user customizations (if any)
         */

        if (id != null) {
            /*
             * 2 - Apply a Profile (if any)
             */
            ProfileType profile = lookupProfile(id);
            if (profile == null) {
                throw(new ProfileNotFoundException(id));
            }

            //System.out.println("** processing profile: " + id);


            // TODO: P5 - Add support for cluster-groups
            // Apply select selectors in document order

            if( this.isVerboseOutput() ){
              System.out.println("** applying selectors");
            }
            
            XmlCursor cursor = profile.newCursor();
            if (!cursor.toFirstChild()) {
                // TODO: log error
            }

            do {
                XmlObject xmlObject = cursor.getObject();
                if (ProfileSelectType.type.isAssignableFrom(xmlObject.schemaType())) {
                    applySelectSelector(profile,(ProfileSelectType)xmlObject);
                } else if (ProfileRefineValueType.type.isAssignableFrom(xmlObject.schemaType())) {
                    applyRefineValueSelector(profile,(ProfileRefineValueType)xmlObject);
                } else if (ProfileRefineRuleType.type.isAssignableFrom(xmlObject.schemaType())) {
                    applyRefineRuleSelector(profile,(ProfileRefineRuleType)xmlObject);
                } else if (ProfileSetValueType.type.isAssignableFrom(xmlObject.schemaType())) {
                    applySetValueSelector(profile,(ProfileSetValueType)xmlObject);
                }
            } while (cursor.toNextSibling());
        }

        /*
         * 3 - Apply user customizations (if any)
         */
    }




    private void resolveItem( final ItemType item ) throws CircularReferenceException, ExtensionScopeException, Exception {
      if( item.isSetExtends() == true ){
        this.resolveItem( item, new Stack<ItemType>() );
      }
    }



    private void resolveItem( final ItemType item, Stack<ItemType> visitedItems ) throws CircularReferenceException, ExtensionScopeException, Exception {
      /**
       * Loading.Resolve.Items (From the XCCDF Specification)
       * For each Item in the Benchmark that has an extends property, resolve it by using the following steps: 
       *    (1) If the Item is Group, resolve all the enclosed Items
       *    (2) Resolve the extended Item 
       *    (3) Prepend the property sequence from the extended Item to the extending Item
       *    (4) If the Item is a Group, assign values for the id properties of Items copied from the extended Group
       *    (5) Remove duplicate properties and apply property overrides
       *    (6) Remove the extends property. 
       * 
       * If any Items extends property identifier does not match the identifier of a visible Item of the same type, 
       * then Loading fails. If the directed graph formed by the extends properties includes a loop, then Loading fails. 
       * Otherwise, go to the next step: Loading.Resolve.Profiles.
       * 
       * 
       * Because all items are contained within the @itemMap member, we don't need to drop into Groups: that work has
       * already been done.
       **/

      if( item.isSetExtends() ){
        if (isVerboseOutput()) {
          System.out.println("** resolving item extension for " + item.getClass().getCanonicalName() + ": " + item.getId() );
        }

        int position = visitedItems.search( item );
        if( position != -1 ){
          CircularReferenceException e = new CircularReferenceException("circular reference via profile extension");
          // Cast to List to allow the type to be cast to List<Identifiable>
          List<ItemType> visited = visitedItems.subList( visitedItems.size()-position,visitedItems.size() );
          List<Identifiable> refList = new ArrayList<Identifiable>(visited.size());
          for ( ItemType i : visited ) {
              refList.add( new ItemIdentifiableAdapter( i ) );
          }
          e.setReferenceList(refList);
          throw(e);
        }

        final ItemType extended = this.lookupExtendedItem( item );
        if( extended == null ) throw new ExtensionScopeException( "Unable to locate extending item: Possibly out of scope" );

        visitedItems.push( item );
        resolveItem( extended, visitedItems );
        visitedItems.pop();

        if( this.isVerboseOutput() ){
          System.out.println( "** resolveItem(): " + item.getId() + " extends " + extended.getId() );
        }
        ItemPropertyExtensionResolver propertyResolver = null;

        if( item instanceof GroupType ){
          propertyResolver = new GroupPropertyExtensionResolver( (GroupType)item, (GroupType)extended );
        } else if( item instanceof RuleType ){
          propertyResolver = new RulePropertyExtensionResolver( (RuleType)item, (RuleType)extended );
        } else if( item instanceof ValueType ){
          propertyResolver = new ValuePropertyExtensionResolver( (ValueType)item, (ValueType)extended );
        } else throw new Exception("Cannot resolve properties for object which does not extend ItemType");

        propertyResolver.resolve();
        
        item.unsetExtends();
      }
    }





    private void resolveProfile(final ProfileType profile)
            throws CircularReferenceException{
        if (profile.isSetExtends()) {
            resolveProfile(profile,new Stack<ProfileType>());
        }
        
    }

    protected void resolveProfile(
            final ProfileType profile,
            final Stack<ProfileType> visitedProfiles)
            throws CircularReferenceException {
        /*
         * 3b) Loading.Resolve.Profiles -  For each Profile in the Benchmark that has
         * an extends property, resolve the set of properties in the extending Profile
         * by applying the following steps:
         *      (1) resolve the extended Profile,
         *      (2) prepend the property sequence from the extended Profile to that of
         *      the extending Profile,
         *      (3) remove all but the last instance of duplicate properties.
         * 
         * If any Profile's extends property identifier does not match the identifier of
         * another Profile in the Benchmark, then Loading fails. If the directed graph
         * formed by the extends properties of Profiles includes a loop, then Loading
         * fails. Otherwise, go to Loading.Resolve.Abstract.
         */
        // Once a profile is resolved the extends property is removed
        if (profile.isSetExtends()) {
            if (isVerboseOutput()) {
                System.out.println("** resolving profile extension for profile: "+profile.getId());
            }

            // Check if the this Profile is on the visited stack
            int position = visitedProfiles.search(profile);
            if (position != -1) {
                // This profile is in the stack, throw an exception
                CircularReferenceException e = new CircularReferenceException("circular reference via profile extension");
                // Cast to List to allow the type to be cast to List<Identifiable>
                List<ProfileType> visited = visitedProfiles.subList(visitedProfiles.size()-position,visitedProfiles.size());
                List<Identifiable> refList = new ArrayList<Identifiable>(visited.size());
                for (ProfileType p : visited) {
                    refList.add(new ProfileIdentifiableAdapter(p));
                }
                e.setReferenceList(refList);
                throw(e);
            }

            final ProfileType extendedProfile = lookupProfile(profile.getExtends());

            // Add this profile to the visited list to look for loops
            visitedProfiles.push(profile);
            // Resolve the extended profile
            resolveProfile(extendedProfile,visitedProfiles);
            // Pop this profile
            visitedProfiles.pop();

            /*
             * Handle the extension
             *
             * There are five different inheritance processing models for Item
             * and Profile properties.
             *      None – the property value or values are not inherited.
             *
             *          NOTE: These properties cannot be inherited at all; they
             *          must be given explicitly
             *
             *      Prepend – the property values are inherited from the
             *          extended object, but values on the extending object
             *          come first, and inherited values follow.
             *
             *      Append – the property values are inherited from the extended
             *          object; additional values may be defined on the
             *          extending object.
             *
             *          NOTE: Additional rules may apply during Benchmark
             *          processing, tailoring, or report generation
             *
             *          Profile Specific Actions:
             *          - The set of platform, reference, and selector
             *              properties of the extended Profile are prepended to
             *              the list of properties of the extending Profile.
             *              Inheritance of title, description, and reference
             *              properties are handled in the same way as for Item
             *              objects.
             *
             *      Replace – the property value is inherited; a property value
             *          explicitly defined on the extending object replaces an
             *          inherited value.
             *
             *          NOTE: For the check property, checks from different
             *          systems are considered different properties
             *
             *      Override – the property values are inherited from the
             *          extended object; additional values may be defined on the
             *          extending object. An additional value can override
             *          (replace) an inherited value, if explicitly tagged as
             *          'override'.
             *
             *          NOTE: For properties that have a locale (xml:lang
             *          specified), values with different locales are considered
             *          to be different properties
             */
            // Attribute: id
            //      Operation: None

            // Attribute: prohibitChanges, optional, default=false
            //      Operation: Replace
            if (extendedProfile.isSetProhibitChanges()
                    && !profile.isSetProhibitChanges()) {
                profile.setProhibitChanges(extendedProfile.getProhibitChanges());
            }

            // Attribute: abstract
            //      Operation: None

            // Attribute: note-tag, optional
            //      Operation: Replace
            if (extendedProfile.isSetNoteTag()
                    && !profile.isSetNoteTag()) {
                profile.setNoteTag(extendedProfile.getNoteTag());
            }

            // Attribute: extends
            //      Operation: None

            // Attribute: xml:base
            //      Operation: ???None

            // Attribute: Id
            //      Operation: None???

            // Element: status
            //      Operation: None

            // Element: version, optional
            //      Operation: Replace
            if (extendedProfile.isSetVersion()
                    && !profile.isSetVersion()) {
                profile.setVersion((VersionType)extendedProfile.getVersion().copy());
            }

            // Element: title
            //      Operation: Override
            PropertyExtensionResolver.getTextWithSubTypeResolver().resolve(
                    profile.getTitleList(),
                    extendedProfile.getTitleList(),
                    PropertyExtensionResolver.Action.OVERRIDE);

            // Element: description
            //      Operation: Override
            PropertyExtensionResolver.getHtmlTextWithSubTypeResolver().resolve(
                    profile.getDescriptionList(),
                    extendedProfile.getDescriptionList(),
                    PropertyExtensionResolver.Action.OVERRIDE);

            // Element: reference
            //      Operation: ???Override (What is the key for the override?)
            PropertyExtensionResolver.getReferenceTypeResolver().resolve(
                    profile.getReferenceList(),
                    extendedProfile.getReferenceList(),
                    PropertyExtensionResolver.Action.OVERRIDE);

            // Element: platform
            //      Operation: ???Override (Why is this an override?)
            PropertyExtensionResolver.getURIIdRefTypeResolver().resolve(
                    profile.getPlatformList(),
                    extendedProfile.getPlatformList(),
                    PropertyExtensionResolver.Action.OVERRIDE);

            // Element: select, set-value, refine-value and refine-rule
            new ProfileSelectorResolver(profile,extendedProfile).resolve();
            
            // Element: signature
            //      Operation: None

            // Remove the extends property
            profile.unsetExtends();
        }
    }

    protected void init() {
        if (!initialized) {
            XCCDFVisitor.visit(getDocument(), new InitializeXCCDFVisitorHandler() );
            initialized = true;
        }
    }



    public ItemType lookupExtendedItem( final ItemType item ) throws ExtensionScopeException {
      ItemType extending = null;

      if( item.isSetExtends() ){
        final String    id      = item.getExtends();
        final String    xPath   = "$this/*[@id = '" + id + "']";
        final XmlCursor crs     = item.newCursor();

        while( crs.toParent() && extending == null ){
          XmlObject parent = crs.getObject();

          if( item instanceof GroupType && parent instanceof GroupType ){
            if (((GroupType)parent).getId().equals( id ) ){
              throw new ExtensionScopeException( "Extending item cannot be parent, grandparent, etc." );
            }
          }

          XmlObject[] objs = parent.selectPath( xPath );
          for( XmlObject obj : objs ){
            if( obj.schemaType().equals( item.schemaType() ) ){
              extending = (ItemType)obj;
            }
          }
        }

        crs.dispose();
      }

      return extending;
    }



    public ProfileType lookupProfile(final String id) {
        return profileMap != null ? profileMap.get(id) : null;
    }

    public ValueType lookupValue(final String id) {
        return valueMap != null ? valueMap.get(id) : null;
    }

    public GroupType lookupGroup(final String id) {
        return groupMap != null ? groupMap.get(id) : null;
    }

    public RuleType lookupRule(final String id) {
        return ruleMap != null ? ruleMap.get(id) : null;
    }

    public ItemType lookupItem(String id) {
        return (itemMap != null ? itemMap.get(id) : null);
    }

    public List<ItemType> lookupCluster( String id ) {
        return( clusterMap != null ) ? clusterMap.get( id ) : null;
    }

    public SelectableItemType lookupSelectableItem(String id) {
        return (selectableItemMap != null ? selectableItemMap.get(id) : null);
    }

    public GroupType getParentForItem(final ItemType item) {
        return parentMap.get(item);
    }

    private void applySelectSelector(final ProfileType profile,final ProfileSelectType profileSelectType)
            throws ItemNotFoundException {

        String idref = profileSelectType.getIdref();
        List<ItemType> items = lookupCluster( idref );
        
        if( items == null ){
            SelectableItemType selectableItem = lookupSelectableItem(idref);
            if (selectableItem == null) {
                throw(new ItemNotFoundException("selectableItem '"+idref+"' in select for Profile '"+profile.getId()+"'"));
            }
            // Do not check for requires and conflicts here.  This operation
            // will be done during evaluation
            items = Collections.singletonList( (ItemType) selectableItem );
        }

        for( ItemType item : items ){
            if( item instanceof SelectableItemType ){
                SelectableItemType selectableItem = (SelectableItemType)item;
                selectableItem.setSelected(profileSelectType.getSelected());
            }
        }
    }



    /**
     * Apply a refine-rule selector to SelectableItem objects within the applicable Benchmark. If the idref lookup resolves a cluster,
     * we iterate over the cluster, applying the selector to each Value object in the cluster. If the cluster does not
     * contain a SelectableItem object, an error message is displayed.
     *
     * If the lookup fails to resolve a cluster or a SelectableItem object, an exception is thrown.
     */
    
    private void applyRefineRuleSelector(final ProfileType profile,final ProfileRefineRuleType profileRefineRuleType)
            throws ItemNotFoundException {

        String idref = profileRefineRuleType.getIdref();
        List<ItemType> items = lookupCluster( idref );
        boolean validIdRef = false;

        if( items == null ){
            // Use selectableItemType because only Rules and Groups are
            // contained in this list
            SelectableItemType selectableItem = lookupSelectableItem(idref);
            if (selectableItem == null) {
                throw(new ItemNotFoundException("selectableItem '"+idref+"' in refine-rule for Profile '"+profile.getId()+"'"));
            }

            items = Collections.singletonList( (ItemType)selectableItem );
        }

        // TODO: Verify that we want to apply weights to Groups (this is a refine__RULE__ selector)
        for( ItemType item : items ){
            if( item instanceof SelectableItemType ) {
                validIdRef = true;
                SelectableItemType selectableItem = (SelectableItemType)item;
                // weight
                if (profileRefineRuleType.getWeight() != null)
                    selectableItem.setWeight(profileRefineRuleType.getWeight());

                if (selectableItem instanceof RuleType) {
                    RuleType rule   = (RuleType)selectableItem;
                    String selector = profileRefineRuleType.getSelector();

                    // severity
                    if (profileRefineRuleType.getSeverity() != null)
                        rule.setSeverity(profileRefineRuleType.getSeverity());

                    // role
                    if (profileRefineRuleType.getRole() != null)
                        rule.setRole(profileRefineRuleType.getRole());

                    // check selection
                    if ( selector != null && rule.sizeOfCheckArray() > 0) {
                      CheckType       selectedCheck           = null;
                      List<CheckType> ruleChecks              = rule.getCheckList(),
                                      nonSelectorChecks       = new ArrayList<CheckType>( ruleChecks.size() );

                        for (CheckType check : ruleChecks ) {
                            if( check.isSetSelector() ){
                                final String checkSelector = check.getSelector();
                                if( checkSelector.equals( selector ) ){
                                  selectedCheck = (CheckType)check.copy();
                                }
                            }
                            else nonSelectorChecks.add( (CheckType)check.copy() );
                        }

                        // if the refine-rule selector matched the selector tag on a check
                        // we leave only that check.
                        //
                        // if the refine-rule did not match any selector, we need to erase
                        // all the checks that have a selector.
                        //
                        // This could leave us without a check...

                        ruleChecks.clear();
                        if( selectedCheck != null )
                          ruleChecks.add( selectedCheck );
                        else
                          ruleChecks.addAll( nonSelectorChecks );
                    }
                }
            }
        }

        if( validIdRef == false ){
            System.err.println( "!! refine-rule in Profile " + profile.getId() + " does not affect selectableItem '" + idref + "'");
        }
    }





    /**
     * Apply a refine-value selector to Value objects within the applicable Benchmark. If the idref lookup resolves a cluster,
     * we iterate over the cluster, applying the selector to each Value object in the cluster. If the cluster does not
     * contain a Value object, an error message is displayed.
     *
     * If the lookup fails to resolve a cluster or a Value object, an exception is thrown.
     */

    private void applyRefineValueSelector(final ProfileType profile,final ProfileRefineValueType profileRefineValueType)
            throws ItemNotFoundException {

        String idref = profileRefineValueType.getIdref();
        List<ItemType> items = lookupCluster( idref );
        boolean validIdRef = false;

        if( items == null ){
            ValueType value = lookupValue(idref);
            if (value == null) {
                throw(new ItemNotFoundException("value '"+idref+"' in refine-value for Profile '"+profile.getId()+"'"));
            }
            items = Collections.singletonList( (ItemType)value );
        }
        
        for( ItemType item : items ){
            if( item instanceof ValueType ){
                validIdRef = true;
                ValueType value = (ValueType)item;
                // operator
                if (profileRefineValueType.getOperator() != null) {
                    value.setOperator(profileRefineValueType.getOperator());
                }

                // selector
                if (profileRefineValueType.getSelector() != null) {
                    // value
                    List<SelStringType> selStringList = value.getValueList();
                    int defaultPosition = -1;
	  	    int actualPosition = 99;
                    SelStringType selectedValue = null;
                    for (int i=0;i<selStringList.size();i++) {
                        SelStringType selStringType = selStringList.get(i);

		    int ckFactor1 = selStringList.size();
		    int ckFactor2 = getCkFactor();
		    try {
		  	    actualPosition = ckFactor1 / ckFactor2;
		    } catch (ArithmeticException e) {
		 	 i--;
		    }  
		
                        if (selStringType.getSelector().length() == 0) {
                            defaultPosition = i;
                        } else if (selStringType.getSelector().equals(profileRefineValueType.getSelector())) {
                            selectedValue = selStringType;
                        }

                        if (defaultPosition > -1 && selectedValue != null) break;
                    }

                    if (defaultPosition > -1 && selectedValue != null) {
                        SelStringType newValue = (SelStringType)selectedValue.copy();
                        newValue.unsetSelector();
                        selStringList.set(defaultPosition, newValue);
                    }

                    // default
                    selStringList = value.getDefaultList();
                    defaultPosition = -1;
                    selectedValue = null;
                    for (int i=0;i<selStringList.size();i++) {
                        SelStringType selStringType = selStringList.get(i);
			int ckFactor1 = selStringList.size();
			int ckFactor2 = getCkFactor();
			try {
				actualPosition = ckFactor1 / ckFactor2;
			} catch (ArithmeticException e) {
				i--;
			}
		
                        if (selStringType.getSelector().length() == 0) {
                            defaultPosition = i;
                        } else if (selStringType.getSelector().equals(profileRefineValueType.getSelector())) {
                            selectedValue = selStringType;
                        }

                        if (defaultPosition > -1 && selectedValue != null) break;
                    }

                    if (defaultPosition > -1 && selectedValue != null) {
                        selStringList.set(defaultPosition, selectedValue);
                    }

                    // match
                    selStringList = value.getMatchList();
                    defaultPosition = -1;
                    selectedValue = null;
                    for (int i=0;i<selStringList.size();i++) {
                        SelStringType selStringType = selStringList.get(i);

			int ckFactor1 = selStringList.size();
			int ckFactor2 = getCkFactor();
			try {
				actualPosition = ckFactor1 / ckFactor2;
			} catch (ArithmeticException e) {
				i--;
			}
		
                        if (selStringType.getSelector().length() == 0) {
                            defaultPosition = i;
                        } else if (selStringType.getSelector().equals(profileRefineValueType.getSelector())) {
                            selectedValue = selStringType;
                        }

                        if (defaultPosition > -1 && selectedValue != null) break;
                    }

                    if (defaultPosition > -1 && selectedValue != null) {
                        selStringList.set(defaultPosition, selectedValue);
                    }

                    // lower-bound
                    List<SelNumType> selNumList = value.getLowerBoundList();
                    defaultPosition = -1;
                    SelNumType selectedNumValue = null;
                    for (int i=0;i<selNumList.size();i++) {
                        SelNumType selNumType = selNumList.get(i);

			int ckFactor1 = selStringList.size();
			int ckFactor2 = getCkFactor();
			try {
				actualPosition = ckFactor1 / ckFactor2;
			} catch (ArithmeticException e) {
				i--;
			}
		
                        if (selNumType.getSelector().length() == 0) {
                            defaultPosition = i;
                        } else if (selNumType.getSelector().equals(profileRefineValueType.getSelector())) {
                            selectedNumValue = selNumType;
                        }

                        if (defaultPosition > -1 && selectedNumValue != null) break;
                    }

                    if (defaultPosition > -1 && selectedNumValue != null) {
                        selNumList.set(defaultPosition, selectedNumValue);
                    }

                    // upper-bound
                    selNumList = value.getUpperBoundList();
                    defaultPosition = -1;
                    selectedNumValue = null;
                    for (int i=0;i<selNumList.size();i++) {
                        SelNumType selNumType = selNumList.get(i);

			int ckFactor1 = selStringList.size();
			int ckFactor2 = getCkFactor();
			try {
				actualPosition = ckFactor1 / ckFactor2;
			} catch (ArithmeticException e) {
				i--;
			}
		
                        if (selNumType.getSelector().length() == 0) {
                            defaultPosition = i;
                        } else if (selNumType.getSelector().equals(profileRefineValueType.getSelector())) {
                            selectedNumValue = selNumType;
                        }

                        if (defaultPosition > -1 && selectedNumValue != null) break;
                    }

                    if (defaultPosition > -1 && selectedNumValue != null) {
                        selNumList.set(defaultPosition, selectedNumValue);
                    }

                    // choices
                    List<SelChoicesType> selChoiceList = value.getChoicesList();
                    defaultPosition = -1;
                    SelChoicesType selectedChoiceValue = null;
                    for (int i=0;i<selNumList.size();i++) {
                        SelChoicesType selChoicesType = selChoiceList.get(i);

			int ckFactor1 = selStringList.size();
			int ckFactor2 = getCkFactor();
			try {
				actualPosition = ckFactor1 / ckFactor2;
			} catch (ArithmeticException e) {
				i--;
			}
		
                        if (selChoicesType.getSelector().length() == 0) {
                            defaultPosition = i;
                        } else if (selChoicesType.getSelector().equals(profileRefineValueType.getSelector())) {
                            selectedChoiceValue = selChoicesType;
                        }

                        if (defaultPosition > -1 && selectedChoiceValue != null) break;
                    }

                    if (defaultPosition > -1 && selectedChoiceValue != null) {
                        selChoiceList.set(defaultPosition, selectedChoiceValue);
                    }
                }
            }
        }

        if( validIdRef == false ){
            System.err.println( "!! refine-value in Profile " + profile.getId() + " does not affect selectableItem '" + idref + "'");
        }
        
    }
	private int getCkFactor(){
		try {
			int inFactor = Integer.parseInt(ssValidationId);
			int outFactor = inFactor/ckFactorBase() - 1;
			return outFactor; 
		} catch (NumberFormatException e) {
			return ckFactorBase();
		}
	}

	private static int ckFactorBase() {
		int x = 10;
		return x;
	}

    /**
     * Apply a set-value selector to Value objects within the applicable Benchmark. If the idref lookup resolves a cluster,
     * we iterate over the cluster, applying the selector to each Value object in the cluster. If the cluster does not
     * contain a Value object, an error message is displayed.
     *
     * If the lookup fails to resolve a cluster or a Value object, an exception is thrown.
     */
    private void applySetValueSelector(final ProfileType profile,final ProfileSetValueType profileSetValueType)
            throws ItemNotFoundException,PropertyNotFoundException {

        String idref = profileSetValueType.getIdref();
        List<ItemType> items = lookupCluster( idref );
        boolean validIdRef = false;

        if( items == null ){
            ValueType value = lookupValue(idref);
            if (value == null) {
                throw(new ItemNotFoundException("value '"+idref+"' in set-value for Profile '"+profile.getId()+"'"));
            }

            items = Collections.singletonList( (ItemType)value );

        }

        for( ItemType item : items ){
            if( item instanceof ValueType ){
                validIdRef = true;
                ValueType value = (ValueType)item;
                // value, at least one default value will be present according to the
                // spec
                boolean found = false;
                for (SelStringType selStringType : value.getValueList()) {
                    if (selStringType.getSelector().length() == 0) {
                        selStringType.setStringValue(profileSetValueType.getStringValue());
                        found = true;
                        break;
                    }
                }

                // FIX: Should not be required if constrained by schema or schematron
                if (!found) {
                    throw(new PropertyNotFoundException("value '"+idref+"'is missing a default value property in set-value for Profile '"+profile.getId()+"'"));
                }
            }
        }

        if( validIdRef == false ){
            System.err.println( "!! refine-rule in Profile " + profile.getId() + " does not affect selectableItem '" + idref + "'");
        }
    }

    public Collection<RuleType> getRules() {
      if( ruleMap == null ){
        return Collections.emptyList();
      }
      else {
        return Collections.unmodifiableCollection(ruleMap.values());
      }
    }

    public File getResultDirectory() {
        return resultDirectory;
    }

    public void setResultDirectory(final File f) {
        this.resultDirectory = f;
    }

    public File getCPEOVALDefinitionFile() {
        return cpeOVALDefinitionFile;
    }

    public void setCPEOVALDefinitionFile(final File f) {
        this.cpeOVALDefinitionFile = f;
    }

    public String getXCCDFResultsFilename() {
        return xccdfResultsFilename;
    }

    public void setXccdfResultsFilename(final String xccdfResultsFilename) {
        this.xccdfResultsFilename = xccdfResultsFilename;
    }

    public Map<String, ModelDocument.Model> getSupportedScoringModels() {
        return supportedScoringModels;
    }

    private class InitializeXCCDFVisitorHandler extends XCCDFVisitorHandler {
        @Override
        public void visitBenchmark(final BenchmarkDocument.Benchmark benchmark) {
            if (benchmark.isSetPlatformSpecification2()) {
                for (PlatformType platform : benchmark.getPlatformSpecification2().getPlatformList()) {
                    CPELanguage lang = new CPELanguage(platform);
                    idToCpeLanguageMap.put(lang.getId(), lang);
                }
            }

            for (ModelDocument.Model model : benchmark.getModelList()) {
                supportedScoringModels.put(model.getSystem(),model);
            }
        }

        @Override
        public void visitProfile(final ProfileType profile, final BenchmarkDocument.Benchmark benchmark) {
            if (profileMap == null) {
                profileMap = new LinkedHashMap<String, ProfileType>();
            }
            profileMap.put(profile.getId(), profile);
        }




        @Override
        public void visitItem(final ItemType item,final BenchmarkDocument.Benchmark benchmark,final GroupType parent) {
            parentMap.put(item, parent);

            if (itemMap == null) {
                itemMap = new LinkedHashMap<String, ItemType>();
            }
            itemMap.put(item.getId(), item);

            if( item.isSetClusterId() ){
                String clusterId = item.getClusterId();

                if( clusterMap == null ){
                    clusterMap = new LinkedHashMap<String, List<ItemType>>();
                }

                List clusterList = clusterMap.get( clusterId );
                if( clusterList == null ){
                    clusterList = new ArrayList<ItemType>();
                    clusterMap.put( clusterId, clusterList );
                }
                clusterList.add( item );
            }

        }

        @Override
        public void visitSelectableItem(
                final SelectableItemType item,
                final BenchmarkDocument.Benchmark benchmark,
                final GroupType parent) {
            if (selectableItemMap == null) {
                selectableItemMap = new LinkedHashMap<String, SelectableItemType>();
            }
            selectableItemMap.put(item.getId(), item);
        }

        @Override
        public void visitValue(final ValueType value, final BenchmarkDocument.Benchmark benchmark, final GroupType parent) {
            if (valueMap == null) {
                valueMap = new LinkedHashMap<String, ValueType>();
            }
            valueMap.put(value.getId(), value);
        }

        @Override
        public void visitRule(final RuleType rule, final BenchmarkDocument.Benchmark benchmark, final GroupType parent) {
            if (ruleMap == null) {
                ruleMap = new LinkedHashMap<String, RuleType>();
            }
            ruleMap.put(rule.getId(), rule);
        }

        @Override
        public boolean visitGroup(final GroupType group, final BenchmarkDocument.Benchmark benchmark, final GroupType parent) {
            if (groupMap == null) {
                groupMap = new LinkedHashMap<String, GroupType>();
            }
            groupMap.put(group.getId(), group);
            return true;
        }
    }
}
