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

import org.mitre.scap.xccdf.check.CheckEvaluator;
import org.mitre.scap.xccdf.check.CheckEvaluatorHelper;
import org.mitre.scap.xccdf.properties.BuildProperties;
import org.mitre.scap.xccdf.check.CheckEvaluator;
import org.mitre.scap.xccdf.check.CheckEvaluatorHelper;
import org.mitre.scap.xccdf.check.CheckSystem;

import java.math.BigDecimal;
import java.util.Calendar;

import gov.nist.scap.schema.ocil.x20.*;


public class OCILCheckSystem extends CheckSystem {

  public static final BigDecimal  SCHEMA_VERSION    = new BigDecimal("2.0");
  public static final String      OCIL_SYSTEM_NAME  = "http://scap.nist.gov/schema/ocil/2";


  public OCILCheckSystem(){
    super( OCIL_SYSTEM_NAME );
  }


  @Override
  public CheckEvaluator newEvaluatorInstance( final CheckEvaluatorHelper helper ) {
    return new OCILCheckEvaluator( this, helper );
  }


  public GeneratorType getGenerator() {
    GeneratorType retval = GeneratorType.Factory.newInstance();
    retval.setProductName(BuildProperties.getInstance().getApplicationName());
    retval.setProductVersion(BuildProperties.getInstance().getApplicationVersion());
    retval.setSchemaVersion(SCHEMA_VERSION);
    retval.setTimestamp(Calendar.getInstance());
    return retval;
  }


}
