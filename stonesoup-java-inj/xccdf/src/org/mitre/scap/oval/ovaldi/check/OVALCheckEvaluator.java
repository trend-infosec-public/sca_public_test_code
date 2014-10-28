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



package org.mitre.scap.oval.ovaldi.check;

import gov.nist.checklists.xccdf.x11.CheckContentRefType;
import gov.nist.checklists.xccdf.x11.CheckExportType;
import gov.nist.checklists.xccdf.x11.CheckType;
import gov.nist.checklists.xccdf.x11.MessageType;
import gov.nist.checklists.xccdf.x11.MsgSevEnumType;
import gov.nist.checklists.xccdf.x11.ResultEnumType;
import gov.nist.checklists.xccdf.x11.SelStringType;
import gov.nist.checklists.xccdf.x11.ValueType;
import gov.nist.checklists.xccdf.x11.ValueTypeType;
import org.mitre.scap.oval.OVALResolver;
import org.mitre.scap.oval.ovaldi.OVALDIExec;
import org.mitre.scap.xccdf.XCCDFInterpreter;
import org.mitre.scap.xccdf.check.Check;
import org.mitre.scap.xccdf.check.CheckEvaluator;
import org.mitre.scap.xccdf.check.CheckEvaluatorHelper;
import org.mitre.scap.xccdf.check.CheckResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.mitre.oval.xmlSchema.ovalCommon5.DatatypeEnumeration;
import org.mitre.oval.xmlSchema.ovalCommon5.GeneratorType;
import org.mitre.oval.xmlSchema.ovalCommon5.SimpleDatatypeEnumeration;
import org.mitre.oval.xmlSchema.ovalVariables5.OvalVariablesDocument;
import org.mitre.oval.xmlSchema.ovalVariables5.VariableType;
import org.mitre.oval.xmlSchema.ovalVariables5.VariablesType;
import org.mitre.oval.xmlSchema.ovaldi.evalids.EvalutationDefinitionIdsDocument;


public class OVALCheckEvaluator extends CheckEvaluator {
    private static Logger log = Logger.getLogger(OVALCheckEvaluator.class.getName());
    private static final Map<String, OVALResult> hrefToResultMap = new HashMap<String, OVALResult>();
    
    public OVALCheckEvaluator(final OVALCheckSystem checkSystem,final CheckEvaluatorHelper helper) {
        super(checkSystem,helper);
    }


    @Override
    public OVALCheckSystem getCheckSystem() {
        return (OVALCheckSystem)super.getCheckSystem();
    }


    @Override
    protected List<CheckResult> handleCheck(final Check check) {
        Set<Check> checks = getChecks();
        List<CheckResult> retval = null;
        
        for (CheckContentRefType content : check.getContent()) {
            // Process each content in turn until one is executable
            String href = content.getHref();
            OVALResult result;
            if (!hrefToResultMap.containsKey(href)) {

                File contentFile = null;
                try {
                    URL contentURL = getCheckEvaluatorHelper().resolveHref(href);

                    if ("file".equals(contentURL.getProtocol())) {
                        contentFile = new File(contentURL.toURI());
                        if( contentFile.canRead() == false ){
                          System.err.println("!! unable to read check-content-ref: " + href );
                          continue;
                        }
                    } else {
                        System.err.println("!! external content not supported. unable to use href: "+href);
                        continue;
                    }

                } catch (URISyntaxException e) {
                    if( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, e);
                    continue;
                } catch (IOException e) {
                    if( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, e);
                    continue;
                }

                OVALDIExec exec = new OVALDIExec();
                exec.setDefinitionXmlFile(contentFile);
                exec.setVerifyDefinitionsFileMD5Hash(false);
                exec.setOutputInfoAndErrorsToSTDOUT( XCCDFInterpreter.verboseOutput );
                exec.setApplyXSLStylesheetToResults(false);

                try {

                    File resultFile = File.createTempFile( "oval-results_" + XCCDFInterpreter.EXECUTION_TIME_STR, ".xml", getCheckEvaluatorHelper().getResultDir() );
                    exec.setResultsFile(resultFile);

                    OvalVariablesDocument variablesDocument = getVariablesDocument(href,checks,contentFile);
                    if (variablesDocument != null) {
                        File variablesFile = File.createTempFile( "oval-variables_" + XCCDFInterpreter.EXECUTION_TIME_STR, ".xml", getCheckEvaluatorHelper().getResultDir() );
                        variablesDocument.save( variablesFile,new XmlOptions().setSavePrettyPrint() );
                        exec.setExternalVariableFile( variablesFile );
                    }

                    Set<String> defList = this.getCheckedDefinitions(checks, href);
                    if (!defList.isEmpty()) {
                        EvalutationDefinitionIdsDocument evaluatedDefinitionsDocument = getEvaluatedDefinitionsDocument(defList);
                        if (evaluatedDefinitionsDocument != null) {
                            File evaluatedDefinitionsFile = File.createTempFile( "oval-evaluation-ids_" + XCCDFInterpreter.EXECUTION_TIME_STR, ".xml", getCheckEvaluatorHelper().getResultDir() );
                            evaluatedDefinitionsDocument.save(evaluatedDefinitionsFile,new XmlOptions().setSavePrettyPrint());
                            exec.setExternalEvaluatedDefinitionsFile(evaluatedDefinitionsFile);
                        }
                    }

                    int exitCode = exec.exec( System.out );
                    if ( exitCode != 0 || resultFile.canRead() == false ) {
                        System.err.println("!! execution of OVALDI failed for file: " + contentFile.getAbsolutePath() );
                        result = new OVALResult(href,"Execution of OVALDI failed with error code: "+exitCode);
                    } else {
                        result = new OVALResult(href,resultFile);
                    }
                }  catch ( FileNotFoundException ex ) {
                    if ( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, ex);
                    else System.err.println("!! cannot find OVALDI executable" );
                    result = new OVALResult(href,ex.getLocalizedMessage());
                }
                catch ( Exception ex ) {
                    if ( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, ex);
                    else System.err.println("!! execution of OVALDI failed for file: " + contentFile.getAbsolutePath() );
                    result = new OVALResult(href,ex.getLocalizedMessage());
                }

                hrefToResultMap.put(href,result);

            } else {
                result = hrefToResultMap.get(href);
            }

            CheckType checkType = CheckType.Factory.newInstance();
            checkType.setSystem(check.getSystem().getSystem());
            checkType.setCheckExportArray(check.getExports().toArray(new CheckExportType[check.getExports().size()]));


            if (result.getStatus() == OVALResult.Status.SUCCESSFUL) {
                CheckContentRefType ref = checkType.addNewCheckContentRef();
                ref.setHref(result.getResultFile().getAbsolutePath());

                // TODO: handle variable instance
                if (content.isSetName()) {
                    String name = content.getName();
                    OVALDefinitionResult defResult = result.getDefinitionResult(name);
                    ref.setHref(result.getResultFile().getAbsolutePath());
                    ref.setName(name);

                    CheckResult checkResult;
                    if (defResult == null) {
                        checkResult = new CheckResult(check,ResultEnumType.ERROR,checkType);
                        MessageType messageType = MessageType.Factory.newInstance();
                        messageType.setSeverity(MsgSevEnumType.ERROR);
                        messageType.setStringValue("The check reference "+check.getSystem().getSystem()+" "+name+" "+result.getResultFile().getAbsolutePath()+" did not return a result.  This could be an improper reference.");
                        checkResult.addMessage(messageType);
                        System.err.println("!! the check reference "+check.getSystem().getSystem()+" "+name+" "+result.getResultFile().getAbsolutePath()+" did not produce a result.  This could be an improper reference.");
                    } else {
                        checkResult = new CheckResult(check,defResult.getResult(),checkType);
                        for (MessageType messageType : defResult.getMessages()) {
                            checkResult.addMessage(messageType);
                        }
                    }
                    retval = Collections.singletonList( checkResult );
                    
                } else {
                    // A rule must always return a single result for check
                    // evaluation if the multiple attribute is not selected
                    ResultEnumType.Enum overallResultEnum = null;
                    List<MessageType> messages = new ArrayList<MessageType>();
                    for (OVALDefinitionResult defResult : result.getDefinitionResultMap().values()) {
                        ResultEnumType.Enum resultEnum = defResult.getResult();

                        if (overallResultEnum == null) {
                            overallResultEnum = resultEnum;
                        } else if (overallResultEnum == resultEnum) {
                            // Do nothing
                        } else {
                            switch (resultEnum.intValue()) {
                                case ResultEnumType.INT_PASS:
                                    if (overallResultEnum == ResultEnumType.FAIL
                                        || overallResultEnum == ResultEnumType.ERROR
                                        || overallResultEnum == ResultEnumType.UNKNOWN) {
                                        // Do nothing
                                    } else {
                                        overallResultEnum = ResultEnumType.PASS;
                                    }
                                    break;
                                case ResultEnumType.INT_FAIL:
                                    overallResultEnum = ResultEnumType.FAIL;
                                    break;
                                case ResultEnumType.INT_ERROR:
                                    // Return error if there hasn't been an
                                    // actual fail
                                    if (overallResultEnum != ResultEnumType.FAIL) {
                                        overallResultEnum = ResultEnumType.ERROR;
                                    }
                                    break;
                                case ResultEnumType.INT_UNKNOWN:
                                    if (overallResultEnum != ResultEnumType.FAIL
                                        && overallResultEnum != ResultEnumType.ERROR) {
                                        overallResultEnum = ResultEnumType.UNKNOWN;
                                    }
                                    // Return unknown if there hasn't been an
                                    // actual fail or error
                                    break;
                                case ResultEnumType.INT_FIXED:
                                    // Ignore, status will be fixed if initially
                                    // fixed and an overriding status if another
                                    // is found
                                    break;
                                case ResultEnumType.INT_INFORMATIONAL:
                                    // Ignore
                                    break;
                                case ResultEnumType.INT_NOTAPPLICABLE:
                                    // Ignore
                                    break;
                                case ResultEnumType.INT_NOTCHECKED:
                                    // Ignore, should not be returned?
                                    break;
                                case ResultEnumType.INT_NOTSELECTED:
                                    // Ignore, should not be returned
                                    break;
                            }
                        }

                        messages.addAll(defResult.getMessages());
                    }

                    CheckResult checkResult = new CheckResult(check,overallResultEnum,checkType);
                    for (MessageType messageType : messages) {
                        checkResult.addMessage(messageType);
                    }

                    retval = Collections.singletonList( checkResult );
                }
            }

            if( result.getStatus() == OVALResult.Status.ERROR ){
              retval = Collections.singletonList( new CheckResult( check,ResultEnumType.ERROR, checkType ) );
            }

            break;  // only need to 'successfully' evaluate one check-content-ref
                    // if we got here then we actually evaluated this check
        }

        if ( retval == null ) {
            retval = Collections.singletonList(new CheckResult(check,ResultEnumType.NOTCHECKED,null));
        }
        
        return retval;
    }

    private Map<String,Set<String>> getRelatedExports(final Set<Check> checks,final String href) {
        Map<String,Set<String>> retval = new LinkedHashMap<String,Set<String>>();
        for (Check check : checks) {
            for (CheckContentRefType content : check.getContent()) {
                if (content.getHref().equals(href)) {
                    List<CheckExportType> exports = check.getExports();
                    if (exports != null && exports.size() > 0) {
                        for (CheckExportType export : exports) {
                            String valueId = export.getValueId();
                            Set<String> exportSet = retval.get(valueId);
                            if (exportSet == null) {
                                exportSet = new LinkedHashSet<String>();
                                retval.put(valueId, exportSet);
                            }
                            exportSet.add(export.getExportName());
                        }
                    }
                }
            }
        }
        return retval;
    }

    private Set<String> getCheckedDefinitions(final Set<Check> checks,final String href) {
        Set<String> retval = new LinkedHashSet<String>();
        for (Check check : checks) {
            for (CheckContentRefType content : check.getContent()) {
                if (content.getHref().equals(href)) {
                    if (content.isSetName())
                        retval.add(content.getName());
                }
            }
        }
        return retval;
    }

    private OvalVariablesDocument getVariablesDocument(
            final String href,
            final Set<Check> checks,
            final File definitionFile) throws XmlException, IOException {
        Map<String,Set<String>> exportMap = getRelatedExports(checks,href);
        if (exportMap.size() == 0) {
            return null;
        }

        OVALResolver ovalResolver = new OVALResolver(definitionFile);

        OvalVariablesDocument retval = OvalVariablesDocument.Factory.newInstance();
        OvalVariablesDocument.OvalVariables variables = retval.addNewOvalVariables();

        File schema = new File("ovaldi/oval-variables-schema.xsd").getAbsoluteFile();
        
        XmlCursor cursor = variables.newCursor();
        cursor.toNextToken();
        cursor.insertAttributeWithValue(
                new QName(
                    "http://www.w3.org/2001/XMLSchema-instance",
                    "schemaLocation",
                    "xsi"),
                "http://oval.mitre.org/XMLSchema/oval-variables-5 "+schema.toURI());
        cursor.insertNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

        cursor.dispose();

        GeneratorType generator = variables.addNewGenerator();
        generator.set(getCheckSystem().getGenerator());

        VariablesType variablesType = variables.addNewVariables();
        for (Map.Entry<String, Set<String>> entry : exportMap.entrySet()) {
            String valueId = entry.getKey();
            Set<String> exportIds = entry.getValue();

            for (String export : exportIds) {
                ValueType valueType = getCheckEvaluatorHelper().getValue(valueId);

                VariableType variable = variablesType.addNewVariable();
                org.mitre.oval.xmlSchema.ovalDefinitions5.VariableType defVariable
                        = ovalResolver.getVariable(export);
                if (defVariable != null) {
                    variable.setDatatype(defVariable.getDatatype());
                } else {
                    switch (valueType.getType().intValue()) {
                        case ValueTypeType.INT_BOOLEAN:
                            variable.setDatatype(SimpleDatatypeEnumeration.BOOLEAN);
                            break;
                        case ValueTypeType.INT_NUMBER:
                        case ValueTypeType.INT_STRING:
                        default:
                            variable.setDatatype(SimpleDatatypeEnumeration.STRING);
                            break;
                    }
                }
                variable.setId(export);
                String value = null;
                for (SelStringType valueData : valueType.getValueList()) {
                    if (!valueData.isSetSelector()) {
                        value = valueData.getStringValue();
                        break;
                    }
                }
                variable.addNewValue().setStringValue(value);
                variable.setComment("");
            }
        }

        return retval;
    }

    private EvalutationDefinitionIdsDocument getEvaluatedDefinitionsDocument(Set<String> defList) {
        EvalutationDefinitionIdsDocument retval = EvalutationDefinitionIdsDocument.Factory.newInstance();
        EvalutationDefinitionIdsDocument.EvalutationDefinitionIds evaluationDefinitionIds = retval.addNewEvalutationDefinitionIds();

        File schema = new File("ovaldi/evaluation-ids.xsd").getAbsoluteFile();
        
        XmlCursor cursor = evaluationDefinitionIds.newCursor();
        cursor.toNextToken();
        cursor.insertAttributeWithValue(
                new QName(
                    "http://www.w3.org/2001/XMLSchema-instance",
                    "schemaLocation",
                    "xsi"),
                "http://oval.mitre.org/XMLSchema/ovaldi/evalids "+schema.toURI());
        cursor.insertNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

        cursor.dispose();

        for (String definition : defList) {
            evaluationDefinitionIds.addDefinition(definition);
        }
        
        return retval;
    }
}
