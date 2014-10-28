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




package org.mitre.scap.xccdf.check;

import gov.nist.checklists.xccdf.x11.CheckExportType;
import gov.nist.checklists.xccdf.x11.RuleType;
import gov.nist.checklists.xccdf.x11.SelStringType;
import gov.nist.checklists.xccdf.x11.ValueType;
import org.mitre.scap.xccdf.XCCDFInterpreter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class CheckInterpreter implements CheckEvaluatorHelper {
    protected Map<RuleType,List<Check>> ruleToCheckMap
            = new LinkedHashMap<RuleType,List<Check>>();
    
    protected Map<CheckSystem, Set<Check>> checkSystemTocheckMap
            = new HashMap<CheckSystem, Set<Check>>();

    protected Map<RuleType,List<CheckResult>> ruleResults
            = new HashMap<RuleType,List<CheckResult>>();

    protected List<Check> processedChecks = new ArrayList<Check>();

    protected final XCCDFInterpreter interpreter;

    
    public CheckInterpreter(final XCCDFInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    public List<CheckResult> getCheckResults(final RuleType rule) {
        return ruleResults.get(rule);
/*
        List<CheckResult> retval = ruleResults.get(rule);
        if (retval == null) return Collections.emptyList();
        return retval;
 */
    }

    
    protected Map<String, String> getValuesUsed( Check check ){
      Map<String, String> retval = new LinkedHashMap<String, String>();
      
      for (CheckExportType export : check.getExports()) {
          String valueId = export.getValueId();
          if (!retval.containsKey(valueId)) {
              ValueType valueType = interpreter.lookupValue(valueId);
              String value = null;
             for (SelStringType valueData : valueType.getValueList()) {
                  if (!valueData.isSetSelector()) {
                      value = valueData.getStringValue();
                      break;
                  }
              }
              retval.put(valueId, value);
          }
      }

      return retval;
    }


    public Map<String,String> getValuesUsedMap() {
        Map<String,String> retval = new LinkedHashMap<String,String>();
        for (List<Check> checks : ruleToCheckMap.values()) {
            for (Check check : checks) {
                Map<String, String> valuesUsed = this.getValuesUsed( check );
                retval.putAll( valuesUsed );
            }
        }
        return retval;
    }

    public void processChecks() {
        Map<CheckSystem,CheckEvaluator> evaluators = new HashMap<CheckSystem,CheckEvaluator>();
        for (Map.Entry<RuleType,List<Check>> entry : ruleToCheckMap.entrySet()) {
            List<Check> checks = entry.getValue();
            // Iterate over each check evaluating them
            for (Check check : checks) {
                CheckSystem checkSystem = check.getSystem();

                CheckEvaluator evaluator = evaluators.get(checkSystem);
                if (evaluator == null) {
                    evaluator = checkSystem.newEvaluatorInstance(this);
                    evaluators.put(checkSystem, evaluator);
                }
                evaluator.addCheck(check);
            }
        }

        for (Map.Entry<RuleType,List<Check>> entry : ruleToCheckMap.entrySet()) {
            RuleType    rule    = entry.getKey();
            List<Check> checks  = entry.getValue();

            List<CheckResult> checkResults = this.ruleResults.get( rule );
            if( checkResults == null ){
              checkResults = new ArrayList<CheckResult>();
              ruleResults.put( rule, checkResults );
            }

            // Iterate over each check evaluating them
            for (Check check : checks) {
                CheckSystem       checkSystem = check.getSystem();
                CheckEvaluator    evaluator   = evaluators.get(checkSystem);
                List<CheckResult> results     = evaluator.evaluateCheck(check);

                if (results != null) {
                    checkResults.addAll( results );
                    // break;   do we break? what are the implications of breaking versus iterating over the checks?
                } else {
                    checkResults.addAll( Collections.singletonList((CheckResult)new UncheckedCheckResult( check ) ) );
                }
            }
        }
    }

    public void registerCheck(RuleType rule, Check check) {
        List<Check> checkList = ruleToCheckMap.get(rule);
        if (checkList == null) {
            checkList = new ArrayList<Check>();
            ruleToCheckMap.put(rule, checkList);
        }

        checkList.add(check);

        Set<Check> checks = checkSystemTocheckMap.get(check.getSystem());
        if (checks == null) {
            checks = new HashSet<Check>();
            checkSystemTocheckMap.put(check.getSystem(), checks);
        }

        if (!checks.contains(check)) checks.add(check);
    }


    public void unregisterCheck( RuleType rule, Check check ){
      List<Check> checkList = ruleToCheckMap.get( rule );
      if ( checkList != null ){
        checkList.remove( check );
      }

      Set<Check> checks = checkSystemTocheckMap.get(check.getSystem());
      if( checks != null ){
        checks.remove( check );
      }
    }


    public XCCDFInterpreter getInterpreter() {
        return interpreter;
    }

    public ValueType getValue(String id) {
        return interpreter.lookupValue(id);
    }

    public File getResultDir() {
        return interpreter.getResultDirectory();
    }

    public URL resolveHref(String href) throws MalformedURLException,IOException {
        return new URL(interpreter.getXCCDFFile().getCanonicalFile().toURI().toURL(),href);
    }
}
