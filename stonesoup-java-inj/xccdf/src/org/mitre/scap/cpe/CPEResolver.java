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


package org.mitre.scap.cpe;

import gov.nist.checklists.xccdf.x11.ResultEnumType;
import gov.nist.checklists.xccdf.x11.ValueType;
import org.mitre.scap.oval.ovaldi.check.OVALCheckSystem;
import org.mitre.scap.xccdf.XCCDFInterpreter;
import org.mitre.scap.xccdf.check.Check;
import org.mitre.scap.xccdf.check.CheckEvaluator;
import org.mitre.scap.xccdf.check.CheckEvaluatorHelper;
import org.mitre.scap.xccdf.check.CheckResult;
import org.mitre.scap.xccdf.check.CheckSystem;
import org.mitre.scap.xccdf.check.CheckSystemRegistry;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.xmlbeans.XmlException;


public class CPEResolver implements CheckEvaluatorHelper {
    public enum Status {
        PASS,
        FAIL,
        ERROR,
        UNKNOWN;
    }

    private final Map<String,CPELanguage> idToCpeLanguageMap;
    private final Map<CPEName,Status> cpeToStatusMap = new HashMap<CPEName,Status>();
    private final Map<String,Status> cpeLanguageToStatusMap = new HashMap<String,Status>();
    private final Set<CPEName> foundCPENames = new HashSet<CPEName>();
    private final File cpeDictionaryFile;
    private final File ovalInventoryDefinitionFile;
    private final File resultDir;
    private final Logger log = Logger.getLogger(CPEResolver.class.getName());

    //    private final Map<String,CPE> cpeMap;

    public CPEResolver(
            final Map<String,CPELanguage> idToCpeLanguageMap,
            final File cpeDictionaryFile,
            final File ovalInventoryDefinitionFile,
            final File resultDir) throws CPEEvaluationException {
            this.idToCpeLanguageMap = idToCpeLanguageMap;
            this.cpeDictionaryFile = cpeDictionaryFile;
            this.ovalInventoryDefinitionFile = ovalInventoryDefinitionFile;
            this.resultDir = resultDir;

        try {
            processDictionary();
        } catch ( Exception ex ) {
          
            if( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, ex);
            else System.err.println("!! could not process CPE dictionary : " + cpeDictionaryFile.getName() );

            CPEEvaluationException ex2 = new CPEEvaluationException( ex.getLocalizedMessage() );
            ex2.initCause(ex);
            throw(ex2);
        } 
    }

    public Status evaluateCPE(String idref) throws URISyntaxException {
        Status retval = cpeToStatusMap.get(new CPEName(idref));
        if (retval == null) {
            retval = cpeLanguageToStatusMap.get(idref);
        }
        if (retval == null) return Status.UNKNOWN;
        return retval;
    }

    public Map<CPEName,Status> getCPEStatusMap() {
        return Collections.unmodifiableMap(cpeToStatusMap);
    }

    public Map<String,Status> getCPELanguageStatusMap() {
        return Collections.unmodifiableMap(cpeLanguageToStatusMap);
    }

    private void processDictionary() throws XmlException, IOException,
            URISyntaxException {

        CPEDictionary dictionary = new CPEDictionary(cpeDictionaryFile,
                new CheckHelper() {
                    @Override
                    public String resolveHref(String href) {
                        return ovalInventoryDefinitionFile.toURI().toString();
                    }
                }
        );
        CheckSystem checkSystem = CheckSystemRegistry.lookupCheckSystem(OVALCheckSystem.OVAL_SYSTEM_NAME);
        CheckEvaluator evaluator = checkSystem.newEvaluatorInstance(this);

        for (CPE cpe : dictionary.getCPEs()) {
            for (Check check : cpe.getChecks()) {
                evaluator.addCheck(check);
            }
        }

        for (CPE cpe : dictionary.getCPEs()) {
            CPEName cpeName = cpe.getCpeName();
            for (Check check : cpe.getChecks()) {
                List<CheckResult> checkResults = evaluator.evaluateCheck(check);
                for (CheckResult result : checkResults) {
                    ResultEnumType.Enum resultEnum = result.getResult();
                    if (!cpeToStatusMap.containsKey(cpeName)) {
                        Status status;
                        switch (resultEnum.intValue()) {
                            case ResultEnumType.INT_ERROR:
                                status = Status.ERROR;
                                break;
                            case ResultEnumType.INT_FAIL:
                                status = Status.FAIL;
                                break;
                            case ResultEnumType.INT_FIXED:
                            case ResultEnumType.INT_PASS:
                                status = Status.PASS;
                                foundCPENames.add(cpeName);
                                break;
                            case ResultEnumType.INT_UNKNOWN:
                            default:
                                status = Status.UNKNOWN;
                        }
                        cpeToStatusMap.put(cpeName, status);
                    }
                }
            }
        }

        for (CPELanguage language : idToCpeLanguageMap.values()) {
            Status status = language.evaluate(foundCPENames);
            cpeLanguageToStatusMap.put(language.getId(), status);
        }
    }

    public ValueType getValue(String id) {
        throw new UnsupportedOperationException("Not supported.");
    }

    public File getResultDir() {
        return resultDir;
    }

    public URL resolveHref(String href) throws MalformedURLException, IOException {
        return ovalInventoryDefinitionFile.getCanonicalFile().toURI().toURL();
    }
}
