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

import org.mitre.scap.xccdf.check.*;

import gov.nist.checklists.xccdf.x11.*;
import org.mitre.scap.xccdf.XCCDFInterpreter;

import java.util.*;


public class ComplexCheckInterpreter extends CheckInterpreter {
  private Map<RuleType, ComplexCheckResult>       complexCheckResults     = new LinkedHashMap<RuleType, ComplexCheckResult>();
  private Map<RuleType, ComplexCheck>             ruleToComplexCheckMap   = new LinkedHashMap<RuleType, ComplexCheck>();
  private final Map<ResultEnumType.Enum, Integer> resultToIndexMap        = new LinkedHashMap<ResultEnumType.Enum, Integer>();
  private final Map<String, String>               valuesUsed              = new LinkedHashMap<String, String>();

  private final ResultEnumType.Enum[][] andTable    = new ResultEnumType.Enum[5][5];
  private final ResultEnumType.Enum[][] orTable     = new ResultEnumType.Enum[5][5];
  private final ResultEnumType.Enum[]   negateTable = new ResultEnumType.Enum[5];

  private static final int  PASS          = 0,
                            FAIL          = 1,
                            UNKNOWN       = 2,
                            ERROR         = 3,
                            NOTAPPLICABLE = 4;

  public ComplexCheckInterpreter( final XCCDFInterpreter interpreter ){
    super( interpreter );
    this.initAndTable();
    this.initOrTable();
    this.initNegateTable();
  }



  private int resultToInt( final ResultEnumType.Enum result ){
    int returnVal = -1;

    switch ( result.intValue() ){
      case ResultEnumType.INT_PASS:           returnVal = PASS;           break;
      case ResultEnumType.INT_FAIL:           returnVal = FAIL;           break;
      case ResultEnumType.INT_UNKNOWN:        returnVal = UNKNOWN;        break;
      case ResultEnumType.INT_ERROR:          returnVal = ERROR;          break;
      case ResultEnumType.INT_NOTAPPLICABLE:  returnVal = NOTAPPLICABLE;  break;
      case ResultEnumType.INT_NOTCHECKED:     returnVal = NOTAPPLICABLE;  break;
    }

    return returnVal;
  }



  private void initNegateTable(){
    this.negateTable[PASS]      = ResultEnumType.FAIL;
    this.negateTable[FAIL]      = ResultEnumType.PASS;
    this.negateTable[UNKNOWN]   = ResultEnumType.UNKNOWN;
    this.negateTable[ERROR]     = ResultEnumType.ERROR;
    this.negateTable[PASS]      = ResultEnumType.NOTAPPLICABLE;
  }


  private void initAndTable(){
    this.andTable[PASS][PASS]                    = ResultEnumType.PASS;
    this.andTable[PASS][FAIL]                    = ResultEnumType.FAIL;
    this.andTable[PASS][UNKNOWN]                 = ResultEnumType.UNKNOWN;
    this.andTable[PASS][ERROR]                   = ResultEnumType.ERROR;
    this.andTable[PASS][NOTAPPLICABLE]           = ResultEnumType.PASS;

    this.andTable[FAIL][PASS]                    = ResultEnumType.FAIL;
    this.andTable[FAIL][FAIL]                    = ResultEnumType.FAIL;
    this.andTable[FAIL][UNKNOWN]                 = ResultEnumType.FAIL;
    this.andTable[FAIL][ERROR]                   = ResultEnumType.FAIL;
    this.andTable[FAIL][NOTAPPLICABLE]           = ResultEnumType.FAIL;

    this.andTable[UNKNOWN][PASS]                 = ResultEnumType.UNKNOWN;
    this.andTable[UNKNOWN][FAIL]                 = ResultEnumType.FAIL;
    this.andTable[UNKNOWN][UNKNOWN]              = ResultEnumType.UNKNOWN;
    this.andTable[UNKNOWN][ERROR]                = ResultEnumType.UNKNOWN;
    this.andTable[UNKNOWN][NOTAPPLICABLE]        = ResultEnumType.UNKNOWN;

    this.andTable[ERROR][PASS]                   = ResultEnumType.ERROR;
    this.andTable[ERROR][FAIL]                   = ResultEnumType.FAIL;
    this.andTable[ERROR][UNKNOWN]                = ResultEnumType.UNKNOWN;
    this.andTable[ERROR][ERROR]                  = ResultEnumType.ERROR;
    this.andTable[ERROR][NOTAPPLICABLE]          = ResultEnumType.ERROR;

    this.andTable[NOTAPPLICABLE][PASS]           = ResultEnumType.PASS;
    this.andTable[NOTAPPLICABLE][FAIL]           = ResultEnumType.FAIL;
    this.andTable[NOTAPPLICABLE][UNKNOWN]        = ResultEnumType.UNKNOWN;
    this.andTable[NOTAPPLICABLE][ERROR]          = ResultEnumType.ERROR;
    this.andTable[NOTAPPLICABLE][NOTAPPLICABLE]  = ResultEnumType.NOTAPPLICABLE;
  }


  private void initOrTable(){
    this.orTable[PASS][PASS]                    = ResultEnumType.PASS;
    this.orTable[PASS][FAIL]                    = ResultEnumType.PASS;
    this.orTable[PASS][UNKNOWN]                 = ResultEnumType.PASS;
    this.orTable[PASS][ERROR]                   = ResultEnumType.PASS;
    this.orTable[PASS][NOTAPPLICABLE]           = ResultEnumType.PASS;

    this.orTable[FAIL][PASS]                    = ResultEnumType.PASS;
    this.orTable[FAIL][FAIL]                    = ResultEnumType.FAIL;
    this.orTable[FAIL][UNKNOWN]                 = ResultEnumType.UNKNOWN;
    this.orTable[FAIL][ERROR]                   = ResultEnumType.ERROR;
    this.orTable[FAIL][NOTAPPLICABLE]           = ResultEnumType.FAIL;

    this.orTable[UNKNOWN][PASS]                 = ResultEnumType.PASS;
    this.orTable[UNKNOWN][FAIL]                 = ResultEnumType.UNKNOWN;
    this.orTable[UNKNOWN][UNKNOWN]              = ResultEnumType.UNKNOWN;
    this.orTable[UNKNOWN][ERROR]                = ResultEnumType.UNKNOWN;
    this.orTable[UNKNOWN][NOTAPPLICABLE]        = ResultEnumType.UNKNOWN;

    this.orTable[ERROR][PASS]                   = ResultEnumType.PASS;
    this.orTable[ERROR][FAIL]                   = ResultEnumType.ERROR;
    this.orTable[ERROR][UNKNOWN]                = ResultEnumType.UNKNOWN;
    this.orTable[ERROR][ERROR]                  = ResultEnumType.ERROR;
    this.orTable[ERROR][NOTAPPLICABLE]          = ResultEnumType.ERROR;

    this.orTable[NOTAPPLICABLE][PASS]           = ResultEnumType.PASS;
    this.orTable[NOTAPPLICABLE][FAIL]           = ResultEnumType.FAIL;
    this.orTable[NOTAPPLICABLE][UNKNOWN]        = ResultEnumType.UNKNOWN;
    this.orTable[NOTAPPLICABLE][ERROR]          = ResultEnumType.ERROR;
    this.orTable[NOTAPPLICABLE][NOTAPPLICABLE]  = ResultEnumType.NOTAPPLICABLE;
  }


  public void registerComplexCheck( final RuleType rule, final ComplexCheck cc ){
    ruleToComplexCheckMap.put( rule, cc );
  }


  public void processComplexChecks(){
    //Map<RuleType, ResultEnumType.Enum> results = new LinkedHashMap<RuleType, ResultEnumType.Enum>( ruleToComplexCheckMap.size() );

    for( Map.Entry<RuleType, ComplexCheck> entry : ruleToComplexCheckMap.entrySet() ){
      RuleType      rule  = entry.getKey();
      ComplexCheck  cc    = entry.getValue();

      ComplexCheckResult result = this.processComplexChecks( rule, cc );
      complexCheckResults.put( rule , result);
    }

    //return results;
  }



  @Override
  public Map<String, String> getValuesUsedMap(){
    return this.valuesUsed;
  }



  public ComplexCheckResult getComplexCheckResult( RuleType rule ){
    return this.complexCheckResults.get( rule );
  }



  private ComplexCheckResult processComplexChecks( final RuleType rule, final ComplexCheck cc ){
    ComplexCheckResult result = new ComplexCheckResult( cc );
    ResultEnumType.Enum resultEnum = this.processComplexChecks( rule, cc, result );
    result.setResult( resultEnum );
    return result;
  }





  /**
   * Depth first evaluation of complex check structure using recursion
   * We utilize a lot of the functionality of the superclass (CheckInterpreter) to evaluate each individual check.
   * @param rule
   * @param cc
   * @return
   */
  private ResultEnumType.Enum processComplexChecks( final RuleType rule, final ComplexCheck cc, final ComplexCheckResult ccResult ){
    ResultEnumType.Enum overallResult = null,
                        result        = null;
    
    CcOperatorEnumType.Enum operator  = cc.getOperator();
    boolean                 negate    = cc.isNegate();

    List<ComplexCheck> ccList = cc.getEmbeddedComplexChecks();
    for( ComplexCheck ecc : ccList ){
      result          = this.processComplexChecks( rule, ecc, ccResult );
      overallResult   = this.determineResult( overallResult, result, operator );
    }

    List<Check> checkList = cc.getEmbeddedChecks();
    for( Check check : checkList ){
      super.registerCheck( rule, check );
      this.valuesUsed.putAll( super.getValuesUsed( check ) );
    }

    super.processChecks();
    List<CheckResult> results = super.getCheckResults( rule );

    result          = this.processResults( results, operator, negate, ccResult );
    overallResult   = this.determineResult( overallResult, result, operator );

    this.cleanUp();
    return overallResult;
  }



  private ResultEnumType.Enum processResults( final List<CheckResult> results, final CcOperatorEnumType.Enum operator, final boolean negate, ComplexCheckResult complexCheckResult ){
    ResultEnumType.Enum overallResult = ResultEnumType.NOTAPPLICABLE;
    int numResults = results.size();
    
    if( numResults == 1){
      return results.get(0).getResult();
    } else if ( numResults > 1 ){
      ResultEnumType.Enum r1 = results.get(0).getResult(),
                          r2 = results.get(1).getResult();

      overallResult = this.determineResult( r1, r2, operator );

      for( int i = 2; i < numResults; i++ ){
        ResultEnumType.Enum result = results.get(i).getResult();
        overallResult = this.determineResult( overallResult, result, operator );
      }
    }
    
    for( CheckResult result : results ){
      complexCheckResult.addCheckType( result );
    }

    overallResult = ( negate ) ? this.negateTable[this.resultToInt( overallResult )] : overallResult;

    return overallResult;
  }





  private ResultEnumType.Enum determineResult( final ResultEnumType.Enum r1, final ResultEnumType.Enum r2, final CcOperatorEnumType.Enum operator ) {
    ResultEnumType.Enum result = ResultEnumType.FAIL;

    if( r1 == null && r2 == null ) return ResultEnumType.NOTAPPLICABLE;
    if( r1 != null && r2 == null ) return r1;
    if( r1 == null && r2 != null ) return r2;


    int r1_intVal = this.resultToInt( r1 ),
        r2_intVal = this.resultToInt( r2 );

    if( operator.equals( CcOperatorEnumType.AND ) ){
      result = this.andTable[r1_intVal][r2_intVal];
    } else if( operator.equals( CcOperatorEnumType.OR) ){
      result = this.orTable[r1_intVal][r2_intVal];
    } else ;

    return result;
  }



  private void cleanUp(){
    super.checkSystemTocheckMap.clear();
    super.ruleResults.clear();
    super.ruleToCheckMap.clear();
  }

}
