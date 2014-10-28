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


package org.mitre.scap.ocil.check;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.xmlbeans.XmlException;

import gov.nist.scap.schema.ocil.x20.*;
import gov.nist.scap.schema.ocil.x20.OcilDocument;
import gov.nist.scap.schema.ocil.x20.OCILType;



public class OCILResult {
    public enum Status {
        SUCCESSFUL,
        ERROR;
    }

    private static Logger log = Logger.getLogger(OCILResult.class.getName());

    private final String href;
    private final Status status;
    private final String statusMessage;
    private final File resultFile;
    private final Map<String, OCILQuestionnaireResult> questionnaireResultMap;


    public OCILResult( final String href, final String message ){
      this.href = href;
      this.resultFile = null;
      this.status = Status.ERROR;
      this.statusMessage = message;
      this.questionnaireResultMap = Collections.emptyMap();
    }


    public OCILResult( final String href, final File resultFile ) throws XmlException, IOException {
      this.href           = href;
      this.resultFile     = resultFile;
      this.status         = Status.SUCCESSFUL;
      this.statusMessage  = null;

      OcilDocument              document              = OcilDocument.Factory.parse( resultFile );
      OCILType                  ocil                  = document.getOcil();
      ResultsType               results               = ocil.getResults();
      QuestionnaireResultsType  questionnaireResults  = ( results != null ) ? results.getQuestionnaireResults() : null ;
      
      int mapSize = ( questionnaireResults != null ) ? questionnaireResults.sizeOfQuestionnaireResultArray() : 0;

      if( mapSize > 0 ){
        this.questionnaireResultMap = new HashMap<String, OCILQuestionnaireResult>( mapSize );
        List<QuestionnaireResultType> resultList = questionnaireResults.getQuestionnaireResultList();

        for( QuestionnaireResultType result : resultList ){
          OCILQuestionnaireResult ocilResult = new OCILQuestionnaireResult( result );
          questionnaireResultMap.put( result.getQuestionnaireRef(), ocilResult );
        }
      }
      else{
        this.questionnaireResultMap = Collections.emptyMap();
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

    public OCILQuestionnaireResult getResult(final String id) {
        return questionnaireResultMap.get(id);
    }

    public Map<String, OCILQuestionnaireResult> getQuestionnaireResultMap() {
        return questionnaireResultMap;
    }



}
