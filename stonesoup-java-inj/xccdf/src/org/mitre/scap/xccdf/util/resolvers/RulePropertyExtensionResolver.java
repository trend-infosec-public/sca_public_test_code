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

import java.util.*;

import org.apache.xmlbeans.*;



public class RulePropertyExtensionResolver extends ItemPropertyExtensionResolver {
  private     final   RuleType  extendingRule,
                                extendedRule;


  public RulePropertyExtensionResolver( RuleType extending, RuleType extended ){
    super( extending, extended );
    this.extendingRule  = extending;
    this.extendedRule   = extended;
  }

  @Override
  public void resolve(){
    super.resolve();

    // APPEND: requires, conflicts, ident, fix
    
    PropertyExtensionResolver.getIdrefListTypeResolver().resolve(
      this.extendingRule.getRequiresList(),
      this.extendedRule.getRequiresList(),
      PropertyExtensionResolver.Action.APPEND);


    PropertyExtensionResolver.getIdrefTypeResolver().resolve(
      this.extendingRule.getConflictsList(),
      this.extendedRule.getConflictsList(),
      PropertyExtensionResolver.Action.APPEND);


    PropertyExtensionResolver.getIdentTypeResolver().resolve(
      this.extendingRule.getIdentList(),
      this.extendedRule.getIdentList(),
      PropertyExtensionResolver.Action.APPEND);


    // might need to develop a new resolver for this...
    PropertyExtensionResolver.getXmlObjectResolver().resolve(
      this.extendingRule.getFixList(),
      this.extendedRule.getFixList(),
      PropertyExtensionResolver.Action.APPEND);



    // REPLACE: multiple (?),  severity, weight, selected, complex-check, check

    if( this.extendedRule.isSetWeight() && !this.extendingRule.isSetWeight()){
      this.extendingRule.setWeight( this.extendedRule.getWeight() );
    }

    if( this.extendedRule.isSetSelected() && !this.extendingRule.isSetSelected()){
      this.extendingRule.setSelected( this.extendedRule.getSelected() );
    }

    PropertyExtensionResolver.getXmlObjectResolver().resolve(
      this.extendingRule.getCheckList(),
      this.extendedRule.getCheckList(),
      PropertyExtensionResolver.Action.REPLACE );


    if( this.extendedRule.isSetComplexCheck() && !this.extendingRule.isSetComplexCheck() ){
      this.extendingRule.setComplexCheck( this.extendedRule.getComplexCheck() );
    }

    if( this.extendedRule.isSetSeverity() && !this.extendingRule.isSetSeverity() ){
      this.extendingRule.setSeverity( this.extendedRule.getSeverity() );
    }

    
  // OVERRIDE: warning, question, rationale, platform, profileNote, fixtext
    
    PropertyExtensionResolver.getHtmlTextWithSubTypeResolver().resolve(
      this.extendingRule.getWarningList(),
      this.extendedRule.getWarningList(),
      PropertyExtensionResolver.Action.OVERRIDE);

    PropertyExtensionResolver.getTextTypeResolver().resolve(
      this.extendingRule.getQuestionList(),
      this.extendedRule.getQuestionList(),
      PropertyExtensionResolver.Action.OVERRIDE);

    PropertyExtensionResolver.getHtmlTextWithSubTypeResolver().resolve(
      this.extendingRule.getRationaleList(),
      this.extendedRule.getRationaleList(),
      PropertyExtensionResolver.Action.OVERRIDE);

    PropertyExtensionResolver.getURIIdRefTypeResolver().resolve(
      new ArrayList<URIidrefType>( this.extendingRule.getPlatformList() ),
      new ArrayList<URIidrefType>( this.extendedRule.getPlatformList() ),
      PropertyExtensionResolver.Action.OVERRIDE );

    PropertyExtensionResolver.getProfileNoteTypeResolver().resolve(
      this.extendingRule.getProfileNoteList(),
      this.extendedRule.getProfileNoteList(),
      PropertyExtensionResolver.Action.OVERRIDE );

    PropertyExtensionResolver.getHtmlTextWithSubTypeResolver().resolve(
      this.extendingRule.getFixtextList(),
      this.extendedRule.getFixtextList(),
      PropertyExtensionResolver.Action.OVERRIDE);

  }



}
