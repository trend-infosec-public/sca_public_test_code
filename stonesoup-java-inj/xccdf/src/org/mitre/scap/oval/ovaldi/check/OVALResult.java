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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.xmlbeans.XmlException;
import org.mitre.oval.xmlSchema.ovalResults5.DefinitionType;
import org.mitre.oval.xmlSchema.ovalResults5.DefinitionsType;
import org.mitre.oval.xmlSchema.ovalResults5.OvalResultsDocument;
import org.mitre.oval.xmlSchema.ovalDefinitions5.OvalDefinitionsDocument;
import org.mitre.oval.xmlSchema.ovalResults5.ResultsType;
import org.mitre.oval.xmlSchema.ovalResults5.SystemType;

public class OVALResult {
    public enum Status {
        SUCCESSFUL,
        ERROR;
    }

    private static Logger log = Logger.getLogger(OVALResult.class.getName());

    private final String href;
    private final Status status;
    private final String statusMessage;
    private final File resultFile;
    private final Map<String, OVALDefinitionResult> definitionResultMap;
    private final Map<String, org.mitre.oval.xmlSchema.ovalCommon5.ClassEnumeration.Enum> definitionTypeMap;

    public OVALResult(final String href,final String message) {
        this.href = href;
        this.resultFile = null;
        this.status = Status.ERROR;
        this.statusMessage = message;
        definitionResultMap = Collections.emptyMap();
        definitionTypeMap   = Collections.emptyMap();
    }

    public OVALResult(final String href,final File resultFile) throws XmlException, IOException {
        this.href = href;
        this.resultFile = resultFile;
        this.status = Status.SUCCESSFUL;
        this.statusMessage = null;

        OvalResultsDocument document = OvalResultsDocument.Factory.parse(resultFile);
        OvalResultsDocument.OvalResults results = document.getOvalResults();
        ResultsType resultsType = results.getResults();
        List<SystemType> systems = resultsType.getSystemList();
        if (systems.size() != 1) {
            System.err.println("!! OVAL result set contained results from multiple systems : " + resultFile.getAbsolutePath() );
            definitionResultMap = Collections.emptyMap();
            definitionTypeMap   = Collections.emptyMap();
        } else {
            SystemType system = systems.iterator().next();
            DefinitionsType definitions = system.getDefinitions();
            definitionResultMap = new HashMap<String, OVALDefinitionResult>(definitions.sizeOfDefinitionArray());

            org.mitre.oval.xmlSchema.ovalDefinitions5.DefinitionsType ovalDefinitions = results.getOvalDefinitions().getDefinitions();
            definitionTypeMap = new HashMap<String, org.mitre.oval.xmlSchema.ovalCommon5.ClassEnumeration.Enum>( ovalDefinitions.sizeOfDefinitionArray() );

            for( org.mitre.oval.xmlSchema.ovalDefinitions5.DefinitionType definition : ovalDefinitions.getDefinitionList() ){
              final org.mitre.oval.xmlSchema.ovalCommon5.ClassEnumeration.Enum definitionClass = definition.getClass1();
              definitionTypeMap.put( definition.getId(), definitionClass );
            }

            for (DefinitionType definition : definitions.getDefinitionList()) {
                String id = definition.getDefinitionId();
                org.mitre.oval.xmlSchema.ovalCommon5.ClassEnumeration.Enum definitionClass = definitionTypeMap.get(id);

                OVALDefinitionResult defResult = new OVALDefinitionResult(definition, definitionClass);
                definitionResultMap.put(defResult.getId(), defResult);
            }
        }
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Status getStatus() {
        return status;
    }

    public String getHref() {
        return href;
    }

    public File getResultFile() {
        return resultFile;
    }

    public OVALDefinitionResult getDefinitionResult(final String id) {
        return definitionResultMap.get(id);
    }

    public Map<String, OVALDefinitionResult> getDefinitionResultMap() {
        return definitionResultMap;
    }
}
