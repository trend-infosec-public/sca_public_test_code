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



package org.mitre.scap.xccdf.util.resolvers;

import org.mitre.scap.xccdf.util.*;
import gov.nist.checklists.xccdf.x11.*;

import java.util.ArrayList;

import org.apache.xmlbeans.*;


public class ValuePropertyExtensionResolver extends ItemPropertyExtensionResolver {
  private   final   ValueType   extendingValue,
                                extendedValue;


  public ValuePropertyExtensionResolver( ValueType extending, ValueType extended ){
    super( extending, extended );
    this.extendingValue = extending;
    this.extendedValue  = extended;
  }


  @Override
  public void resolve(){
    super.resolve();

    // PREPEND: choices, source
    PropertyExtensionResolver.getKeyedAttrResolver( "selector" ).resolve(
      this.extendingValue.getChoicesList(),
      this.extendedValue.getChoicesList(),
      PropertyExtensionResolver.Action.PREPEND);

    PropertyExtensionResolver.getKeyedAttrResolver( "uri" ).resolve(
      this.extendingValue.getSourceList(),
      this.extendedValue.getSourceList(),
      PropertyExtensionResolver.Action.PREPEND);


    // APPEND:  value, default, match, lower-bound, upper-bound
    PropertyExtensionResolver.getKeyedAttrResolver( "selector" ).resolve(
      this.extendingValue.getValueList(),
      this.extendedValue.getValueList(),
      PropertyExtensionResolver.Action.APPEND);

    PropertyExtensionResolver.getXmlObjectResolver().resolve(
      this.extendingValue.getDefaultList(),
      this.extendedValue.getDefaultList(),
      PropertyExtensionResolver.Action.APPEND);

    PropertyExtensionResolver.getKeyedAttrResolver( "selector" ).resolve(
      this.extendingValue.getMatchList(),
      this.extendedValue.getMatchList(),
      PropertyExtensionResolver.Action.APPEND);

    PropertyExtensionResolver.getKeyedAttrResolver( "selector" ).resolve(
      this.extendingValue.getLowerBoundList(),
      this.extendedValue.getLowerBoundList(),
      PropertyExtensionResolver.Action.APPEND);

    PropertyExtensionResolver.getKeyedAttrResolver( "selector" ).resolve(
      this.extendingValue.getUpperBoundList(),
      this.extendedValue.getUpperBoundList(),
      PropertyExtensionResolver.Action.APPEND);




    // REPLACE: type, operator, interfaceHint, interactive

    if( this.extendedValue.isSetType() && !this.extendingValue.isSetType()){
      this.extendingValue.setType( this.extendedValue.getType() );
    }

    if( this.extendedValue.isSetOperator() && !this.extendingValue.isSetOperator()){
      this.extendingValue.setOperator( this.extendedValue.getOperator() );
    }

    if( this.extendedValue.isSetInterfaceHint() && !this.extendingValue.isSetInterfaceHint()){
      this.extendingValue.setInterfaceHint( this.extendedValue.getInterfaceHint() );
    }

    if( this.extendedValue.isSetInteractive() && !this.extendingValue.isSetInteractive()){
      this.extendingValue.setInteractive( this.extendedValue.getInteractive() );
    }



    // OVERRIDE: warning, question,

    PropertyExtensionResolver.getHtmlTextWithSubTypeResolver().resolve(
      this.extendingValue.getWarningList(),
      this.extendedValue.getWarningList(),
      PropertyExtensionResolver.Action.OVERRIDE);

    PropertyExtensionResolver.getTextTypeResolver().resolve(
      this.extendingValue.getQuestionList(),
      this.extendedValue.getQuestionList(),
      PropertyExtensionResolver.Action.OVERRIDE);
  }

}
