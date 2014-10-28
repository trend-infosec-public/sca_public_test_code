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

import gov.nist.checklists.xccdf.x11.CheckContentRefType;
import gov.nist.checklists.xccdf.x11.CheckExportType;
import gov.nist.checklists.xccdf.x11.CheckType;
import gov.nist.checklists.xccdf.x11.MessageType;
import gov.nist.checklists.xccdf.x11.MsgSevEnumType;
import gov.nist.checklists.xccdf.x11.ResultEnumType;
import org.mitre.scap.xccdf.check.Check;
import org.mitre.scap.xccdf.check.CheckEvaluator;
import org.mitre.scap.xccdf.check.CheckEvaluatorHelper;
import org.mitre.scap.xccdf.check.CheckResult;
import org.mitre.scap.ocil.ocilqi.*;
import org.mitre.scap.oval.ovaldi.check.OVALResult;
import org.mitre.scap.xccdf.XCCDFInterpreter;

import gov.nist.scap.schema.ocil.x20.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collections;




public class OCILCheckEvaluator extends CheckEvaluator {
  
  private static Logger log = Logger.getLogger(OCILCheckEvaluator.class.getName());
  private static final Map<String, OCILResult> hrefToResultMap = new HashMap<String, OCILResult>();
  

  public OCILCheckEvaluator( final OCILCheckSystem checkSystem, final CheckEvaluatorHelper helper ) {
        super( checkSystem, helper );
  }

  @Override
  public OCILCheckSystem getCheckSystem() {
    return (OCILCheckSystem)super.getCheckSystem();
  }


  /**
   * Processes and evaluates a check.  If the check contains a check export, an error will be returned
   * due to a lack of support for external variable value mapping in OCILQI.
   * @param check
   * @return
   */


  @Override
  protected List<CheckResult> handleCheck(Check check) {
    List<CheckResult> retval = null;
    // Set<Check> checks = getChecks(); // Used for external variable stuff...

    for( CheckContentRefType content : check.getContent() ){
      String      href    = content.getHref();
      OCILResult  result  = hrefToResultMap.get( href );

      if( result == null ){
        File contentFile = null;

        if( check.getExports() != null && check.getExports().size() != 0 ){
          result = new OCILResult( href, "OCILQI does not support external variable mappings" );
        }
        else {
          try {
            URL contentURL = getCheckEvaluatorHelper().resolveHref(href);

            if ("file".equals(contentURL.getProtocol())) {
              contentFile = new File(contentURL.toURI());
              if( contentFile.canRead() == false ){
                System.err.println("!! unable to read check-content-ref: " + href );
                continue;
              }
            } else {
              System.err.println("!! external content not supported. Unable to use href: "+href);
              continue;
            }
          } catch (URISyntaxException e) {
              if( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, e);
              continue;
          } catch (IOException e) {
              if( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, e);
              continue;
          }

          OCILQIExec exec = new OCILQIExec();
          exec.setInputFile( contentFile );
          exec.setVerboseOutput( XCCDFInterpreter.verboseOutput );

          try {
            File resultFile = File.createTempFile("ocil-results_" + XCCDFInterpreter.EXECUTION_TIME_STR, ".xml", getCheckEvaluatorHelper().getResultDir() );
            exec.setOutputFile( resultFile );
            //OcilDocument variablesDocument = this.getVariablesDocument( href, checks, contentFile );

            int exitCode = exec.exec( System.out );
            if( exitCode != 0 || resultFile.canRead() == false ){
              System.err.println( "!! execution of OCILQI failed for file: " + contentFile.getAbsolutePath() );
              result = new OCILResult( href, "Execution of OCILQI failed with error code:" + exitCode );
            }
            else { result = new OCILResult( href, resultFile ); }
          }
          catch ( FileNotFoundException ex ) {
            if ( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, ex);
            else System.err.println("!! cannot find OCILQI executable" );
            result = new OCILResult(href,ex.getLocalizedMessage());
          }
          catch( Exception ex ){
            if( XCCDFInterpreter.verboseOutput ) log.log(Level.SEVERE, null, ex);
            else System.err.println("!! execution of OCILQI failed for file: " + contentFile.getAbsolutePath() );
            result = new OCILResult( href, ex.getLocalizedMessage() );
          }
        } // end check export condition

        hrefToResultMap.put( href, result );
      }

      retval = this.handleResult( check, content, result );

      break; // only need to 'successfully evaluate one check-content-ref
             // if we got here then we have a result...no need to carry on
    }

    if( retval == null ){
      retval = Collections.singletonList( new CheckResult( check, ResultEnumType.NOTCHECKED, null ) );
    }

    return retval;
  }



  private List<CheckResult> handleResult( Check check, CheckContentRefType content, OCILResult result ){
    List<CheckResult> retval = null;

    CheckType checkType = CheckType.Factory.newInstance();
    checkType.setSystem(check.getSystem().getSystem());
    checkType.setCheckExportArray(check.getExports().toArray(new CheckExportType[check.getExports().size()]));

    if( result.getStatus().equals( OCILResult.Status.SUCCESSFUL ) ){
        CheckContentRefType ref = checkType.addNewCheckContentRef();
        ref.setHref(result.getResultFile().getAbsolutePath());

        if( content.isSetName() ){
          String name = content.getName();
          OCILQuestionnaireResult questionnaireResult = result.getResult( name );
          
          ref.setName(name);
          
          CheckResult checkResult;
          if( questionnaireResult == null ){
            checkResult = new CheckResult(check,ResultEnumType.ERROR,checkType);
            MessageType messageType = MessageType.Factory.newInstance();
            messageType.setSeverity(MsgSevEnumType.ERROR);
            messageType.setStringValue("The check reference "+check.getSystem().getSystem()+" "+name+" "+result.getResultFile().getAbsolutePath()+" did not return a result.  This could be an improper reference.");
            checkResult.addMessage(messageType);
            System.err.println("!! the check reference "+check.getSystem().getSystem()+" "+name+" "+result.getResultFile().getAbsolutePath()+" did not produce a result.  This could be an improper reference.");
          }
          else{
            checkResult = new CheckResult( check, questionnaireResult.getResult(), checkType );
          }

          retval = Collections.singletonList( checkResult );
        }
        else {
          // A rule must always return a single result for check
          // evaluation if the multiple attribute is not selected
          ResultEnumType.Enum overallResultEnum = null;
          for (OCILQuestionnaireResult qResult : result.getQuestionnaireResultMap().values()) {
              ResultEnumType.Enum resultEnum = qResult.getResult();

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
                          if (overallResultEnum != ResultEnumType.FAIL ) {
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
                  } // End switch
              } // End else
          } // End for


          CheckResult checkResult = new CheckResult(check,overallResultEnum,checkType);

          retval = Collections.singletonList( checkResult );

        } // End else
      }

      if( result.getStatus().equals( OCILResult.Status.ERROR ) ){
        retval = Collections.singletonList( new CheckResult( check, ResultEnumType.ERROR, checkType) );
      }

      if( retval == null ){
        retval = Collections.singletonList( new CheckResult( check, ResultEnumType.NOTCHECKED, null ) );
      }

    return retval;
  }



  private Map<String, Set<String>> getRelatedExports( final Set<Check> checks, final String href ){
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




  private OcilDocument getVariablesDocument( final String href, final Set<Check> checks, final File questionnaireFile ){
    OcilDocument retval = null;
    Map<String,Set<String>> exportMap = getRelatedExports(checks,href);

    if( exportMap.size() != 0 ){

    /*
     * Work with the exports and generate a variables file
     * 
     */
    }

    return retval;
  }



}
