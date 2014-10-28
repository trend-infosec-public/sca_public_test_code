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



public class GroupPropertyExtensionResolver extends ItemPropertyExtensionResolver {
  private   final   GroupType extendingGroup,
                              extendedGroup;

  public GroupPropertyExtensionResolver( GroupType extending, GroupType extended ) {
    super( extending, extended );
    this.extendingGroup  = extending;
    this.extendedGroup   = extended;
  }

  @Override
  public void  resolve(){
    super.resolve();

    // APPEND: requires, conflicts

    PropertyExtensionResolver.getIdrefListTypeResolver().resolve(
      this.extendingGroup.getRequiresList(),
      this.extendedGroup.getRequiresList(),
      PropertyExtensionResolver.Action.APPEND);


    PropertyExtensionResolver.getIdrefTypeResolver().resolve(
      this.extendingGroup.getConflictsList(),
      this.extendedGroup.getConflictsList(),
      PropertyExtensionResolver.Action.APPEND);

    
    // REPLACE: weight, selected

    if( this.extendedGroup.isSetWeight() && !this.extendingGroup.isSetWeight()){
      this.extendingGroup.setWeight( this.extendedGroup.getWeight() );
    }
    
    if( this.extendedGroup.isSetSelected() && !this.extendingGroup.isSetSelected()){
      this.extendingGroup.setSelected( this.extendedGroup.getSelected() );
    }

    // OVERRIDE: warning, question, rationale, platform

    PropertyExtensionResolver.getHtmlTextWithSubTypeResolver().resolve(
      this.extendingGroup.getWarningList(),
      this.extendedGroup.getWarningList(),
      PropertyExtensionResolver.Action.OVERRIDE);

    PropertyExtensionResolver.getTextTypeResolver().resolve(
      this.extendingGroup.getQuestionList(),
      this.extendedGroup.getQuestionList(),
      PropertyExtensionResolver.Action.OVERRIDE);

    PropertyExtensionResolver.getHtmlTextWithSubTypeResolver().resolve(
      this.extendingGroup.getRationaleList(),
      this.extendedGroup.getRationaleList(),
      PropertyExtensionResolver.Action.OVERRIDE);

    PropertyExtensionResolver.getURIIdRefTypeResolver().resolve(
      this.extendingGroup.getPlatformList(),
      this.extendedGroup.getPlatformList(),
      PropertyExtensionResolver.Action.OVERRIDE );
  }



}
