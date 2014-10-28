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



package org.mitre.scap.xccdf.check.complexcheck;

import gov.nist.checklists.xccdf.x11.CcOperatorEnumType.Enum;
import java.util.*;

import org.mitre.scap.xccdf.check.*;
import gov.nist.checklists.xccdf.x11.*;
import org.mitre.scap.xccdf.XCCDFInterpreter;
import org.mitre.scap.xccdf.XCCDFProcessor.*;


public class ComplexCheck {
  private List<ComplexCheck>      complexChecks = new ArrayList<ComplexCheck>();  //complex checks can contain complex checks ...this should be interesting...
  private List<Check>             checks        = new ArrayList<Check>();

  private final RuleType                parentRule;
  private final CcOperatorEnumType.Enum operator;
  private final boolean                 negate;
  private final XCCDFInterpreter        interpreter;


  public ComplexCheck( RuleType rule, ComplexCheckType cc, final XCCDFInterpreter interpreter ) throws ComplexCheckException {
    this.parentRule   = rule;
    this.operator     = cc.getOperator();
    this.negate       = cc.getNegate();
    this.interpreter  = interpreter;
    this.init( rule, cc );
  }


  private void init( RuleType rule, ComplexCheckType cc ) throws ComplexCheckException {
    List<ComplexCheckType>  embededComplexChecks  = cc.getComplexCheckList();
    List<CheckType>         embededChecks         = cc.getCheckList();

    if( embededComplexChecks != null ){
      for( ComplexCheckType cct : embededComplexChecks ){
        this.complexChecks.add( new ComplexCheck( this.parentRule, cct, this.interpreter ) );
      } 
    }

    if( embededChecks != null ){
      for( CheckType checkType : embededChecks ){
        String system = checkType.getSystem();
        List<CheckExportType> checkExports = checkType.getCheckExportList();
        CheckSystem checkSystem = CheckSystemRegistry.lookupCheckSystem(system);

        if( checkSystem == null ){
          throw new ComplexCheckException( rule, checkType, RuleAction.NO_SUPPORTED_CHECK, "No suitable checking engine found" );
        }

        if( checkType.isSetCheckContent() ){
          throw new ComplexCheckException( rule, checkType, RuleAction.NO_SUPPORTED_CHECK, "In-line check content not supported" );
        }

        List<CheckContentRefType> checkContent = checkType.getCheckContentRefList();
        Check check = checkSystem.newCheck(checkContent,checkExports);
        this.checks.add( check );
      }
    }
  }


  public  List<ComplexCheck> getEmbeddedComplexChecks(){
    return this.complexChecks;
  }


  public List<Check> getEmbeddedChecks(){
    return this.checks;
  }

  public boolean isNegate() {
    return negate;
  }

  public Enum getOperator() {
    return operator;
  }

  

}
