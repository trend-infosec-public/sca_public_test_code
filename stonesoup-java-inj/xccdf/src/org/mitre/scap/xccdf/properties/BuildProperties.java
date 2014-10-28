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




package org.mitre.scap.xccdf.properties;

import java.util.*;
import java.io.*;



public class BuildProperties {

    private static BuildProperties INSTANCE = null;
    
    private static String     BUILD_PROPS_FILENAME  ="/org/mitre/resources/build.properties";
    private Properties        XCCDF_IMPL_PROPERTIES = new Properties();
    private File              BUILD_PROPS_FILE;
   

    private static String     BUILD_NUMBER_PROPNAME   = "build.number",
                              BUILD_NAME_PROPNAME     = "build.name",
                              BUILD_DATE_PROPNAME     = "build.date",
                              XCCDF_VERSION_PROPNAME  = "build.xccdf.version";



    private BuildProperties(){
      init();
    }


    public static BuildProperties getInstance(){
      if( INSTANCE == null ){
        INSTANCE = new BuildProperties();
      }

      return INSTANCE;
    }



    private void init(){
        BUILD_PROPS_FILE      = new File( BUILD_PROPS_FILENAME );

        try {
          InputStream is = getClass().getResourceAsStream( BUILD_PROPS_FILENAME );
          XCCDF_IMPL_PROPERTIES.load( is );
        }
        catch( Exception ex ){
          System.err.println("!! Error loading build properties");
        }
    }



    public String getApplicationName() {
      if( XCCDF_IMPL_PROPERTIES.getProperty( BUILD_NAME_PROPNAME ) == null ){
        XCCDF_IMPL_PROPERTIES.put( BUILD_NAME_PROPNAME, "_MYSTERY NAME_" );
      }

      return XCCDF_IMPL_PROPERTIES.getProperty( BUILD_NAME_PROPNAME );
    }




    public String getApplicationVersion() {
      if( XCCDF_IMPL_PROPERTIES.getProperty( XCCDF_VERSION_PROPNAME ) == null ){
        XCCDF_IMPL_PROPERTIES.put( XCCDF_VERSION_PROPNAME, "_MYSTERY VERSION_" );
      }

      return XCCDF_IMPL_PROPERTIES.getProperty( XCCDF_VERSION_PROPNAME );
    }




    public String getApplicationBuild() {
      if( XCCDF_IMPL_PROPERTIES.getProperty( BUILD_NUMBER_PROPNAME ) == null ){
        XCCDF_IMPL_PROPERTIES.put( BUILD_NUMBER_PROPNAME, "_MYSTERY BUILD_" );
      }

      return XCCDF_IMPL_PROPERTIES.getProperty( BUILD_NUMBER_PROPNAME );
    }




    public String getApplicationBuildDate() {
      if( XCCDF_IMPL_PROPERTIES.getProperty( BUILD_DATE_PROPNAME ) == null ){
        XCCDF_IMPL_PROPERTIES.put( BUILD_DATE_PROPNAME, "UNKNOWN" );
      }

      return XCCDF_IMPL_PROPERTIES.getProperty( BUILD_DATE_PROPNAME );
    }

}
