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


package org.mitre.scap.xccdf.util;

import java.util.Properties;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class XCCDFInterpreterProperties {

  private   static XCCDFInterpreterProperties instance = null;
   
  protected static final String PROPERTIES_FILE_NAME   = "xccdf_interpreter.properties";

  protected File        propertiesFile  = null;

  protected Properties  properties      = null;

  public static final String    PROP_JAVA_OPTS    = "java.opts",
                                PROP_OCIL_DIR     = "ocil.dir",
                                PROP_OCIL_BIN     = "ocil.bin",
                                PROP_OVAL_DIR     = "oval.dir",
                                PROP_OVAL_BIN     = "oval.bin";

  protected static final String DEFAULT_JAVA_OPTS     = "-Xms32m -Xmx128m",
                                DEFAULT_OVAL_DIR      = "ovaldi",
                                DEFAULT_OVAL_BIN      = "ovaldi.exe",
                                DEFAULT_OCIL_DIR      = "ocilqi",
                                DEFAULT_OCIL_BIN      = "ocilqi.jar";



  private XCCDFInterpreterProperties() throws IOException {
    this.propertiesFile =  new File( XCCDFInterpreterProperties.PROPERTIES_FILE_NAME );

    if( this.propertiesFile.exists() == false ){
      this.propertiesFile.createNewFile();
    }
    
    this.loadProperties();

  }


  public static XCCDFInterpreterProperties getInstance() throws IOException {
    if( XCCDFInterpreterProperties.instance == null ){
      XCCDFInterpreterProperties.instance = new XCCDFInterpreterProperties();
    }

    return XCCDFInterpreterProperties.instance;
  }
  
  
  
  
  private void loadProperties( ) throws FileNotFoundException, IOException {
    this.properties = new Properties();
    FileInputStream inputStream = new FileInputStream( this.propertiesFile );

    this.properties.load( inputStream );

    if( this.properties.getProperty( PROP_JAVA_OPTS ) == null ){
      this.properties.setProperty( PROP_JAVA_OPTS, DEFAULT_JAVA_OPTS );
    }

    if( this.properties.getProperty( PROP_OCIL_DIR ) == null ){
      this.properties.setProperty( PROP_OCIL_DIR, DEFAULT_OCIL_DIR );
    }

    if( this.properties.getProperty( PROP_OCIL_BIN ) == null ){
      this.properties.setProperty( PROP_OCIL_BIN, DEFAULT_OCIL_BIN );
    }

    if( this.properties.getProperty( PROP_OVAL_DIR ) == null ){
      this.properties.setProperty( PROP_OVAL_DIR, DEFAULT_OVAL_DIR );
    }

    if( this.properties.getProperty( PROP_OVAL_BIN ) == null ){
      this.properties.setProperty( PROP_OVAL_BIN, DEFAULT_OVAL_BIN );
    }

    this.properties.store( new FileOutputStream( this.propertiesFile ), "Properties for the XCCDF Interpreter" );
  }


  public String getPropertyValue( final String property ){
    return this.properties.getProperty( property );
  }

  public List<String> getPropertyValues( final String property ){
    String  value = this.properties.getProperty( property );

    if( value != null ){
      String[] values = value.split("\\s");
      List<String> list = new ArrayList<String>(values.length);
      for( String str : values ) list.add( str );
      return list;
    }
    else return Collections.emptyList();

  }


  public Properties getProperties( ) { return this.properties; }

}
