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



package org.mitre.util.exec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class JavaJarExecutor {
  private static final String JAVA_HOME           = System.getProperty( "java.home" );
  private static final String JAVA_BIN            = JAVA_HOME + File.separator + "bin" + File.separator + "java";
  private static final String JAVA_CLASSPATH_ARG  = "-cp";
  private static final String JAVA_JAR_ARG        = "-jar";

  private String        jarName             = null;
  private List<String>  classPathList       = new ArrayList<String>(),
                        argumentList        = new ArrayList<String>(),
                        javaOptionList      = new ArrayList<String>();
  private boolean       redirectErrorStream = false;
  private File          workingDirectory    = null;

  private void init(){

  }



  public String getJarName() {
    return this.jarName;
  }



  public void setJarName( final String name ){
    this.jarName = name;
  }


  /**
   * Options to be passed into the JVM.  Example: -Xmx512m
   * @param opt
   */
  public void addJavaOption( final String opt ){
    this.addJavaOptions( Collections.singletonList( opt ) );
  }


  public void addJavaOptions( final List<String> options ){
    this.javaOptionList.addAll( options );
  }



  public void addClassPathEntity( final String entity ){
    this.addClassPathEntities( Collections.singletonList( entity ) );
  }


  public void addClassPathEntities( final List<String> entities ){
    this.classPathList.addAll( entities );
  }



  public void addArgument( final String argument ){
   this.addArguments( Collections.singletonList(argument) );
  }


  public void addArguments( final List<String> arguments ){
    this.argumentList.addAll( arguments );
  }



  public void setRedirectErrorStream( final boolean redirect ){
    this.redirectErrorStream = redirect;
  }


  public void setWorkingDirectory( final File dir ) throws FileNotFoundException {
    if( dir != null && dir.isDirectory() ){
      this.workingDirectory = dir;
    }
    else throw new FileNotFoundException( "Working directory not valid" );
  }

  
  public void setWorkingDirectory( final String dir ) throws FileNotFoundException {
    this.setWorkingDirectory( new File(dir ) );
  }



  public List<String> buildExecString(){
    List<String> exec = new ArrayList<String>();

    exec.add( JAVA_BIN );

    for( String opt : this.javaOptionList ){
      exec.add( opt );
    }

    if( this.classPathList.isEmpty() == false ){
      exec.add( JAVA_CLASSPATH_ARG );
      for( String cpEntity : this.classPathList ){
        exec.add( cpEntity );
      }
    }

    exec.add( JAVA_JAR_ARG );
    exec.add( this.jarName );

    exec.addAll( this.argumentList );

    return exec;
  }


  public Process execute() throws IOException {
    List<String>    cmdList = this.buildExecString();
    ProcessBuilder  pb      = new ProcessBuilder( cmdList );

    if( this.workingDirectory != null ){
      pb.directory( this.workingDirectory );
    }

    return pb.start();
  }

}
