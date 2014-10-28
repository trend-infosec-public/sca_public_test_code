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



package org.mitre.scap.ocil.ocilqi;

/**
 * The OCIL Questionnaire Interpreter is a Java application in JAR format.  Because its
 * execution is dependent on the Benchmark being processed, we are going to use dynamic jar
 * loading to invoke the OCILQI.
 *
 * The OCIL Questionnaire Interpreter accepts a few command line options:
 *
 *  --source=source_filepath
 *  --output=output_filepath
 *  --qnid=questionnaire_id
 *
 *
 */

import org.mitre.scap.oval.ovaldi.IORedirect;
import org.mitre.scap.xccdf.util.XCCDFInterpreterProperties;
import org.mitre.util.exec.JavaJarExecutor;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class OCILQIExec {
  private   File    inputFile,
                    outputFile;


  private   String  questionnaireId;


  private   static final String sourceOpt = "--source=",
                                outputOpt = "--output=",
                                qnidOpt   = "--qnid=";


  private   final JavaJarExecutor executor;

  private   boolean verboseOutput = false;



  public OCILQIExec(){
    this.executor = new JavaJarExecutor();
  }



  public void setVerboseOutput( boolean verbose ){
    this.verboseOutput = verbose;
  }
  


  public boolean getVerboseOutput(){
    return this.verboseOutput;
  }



  public List<String> buildArgumentList() throws IOException {
    List<String> args = new ArrayList<String>(3);

    if( this.inputFile != null && this.outputFile != null ){
      String srcArg = OCILQIExec.sourceOpt  + this.inputFile.getCanonicalPath();
      String outArg = OCILQIExec.outputOpt  + this.outputFile.getCanonicalPath();
      //String idArg  = OCILQIExec.qnidOpt    + this.questionnaireId;

      args.add( srcArg );
      args.add( outArg );
      //args.add( idArg );
    }
    
    return args;
  }


  public int exec( OutputStream out ) throws Exception {
    XCCDFInterpreterProperties xccdfProperties = XCCDFInterpreterProperties.getInstance();
    List<String> arguments = this.buildArgumentList();

    this.executor.addArguments( arguments );
    this.executor.addJavaOptions(xccdfProperties.getPropertyValues( XCCDFInterpreterProperties.PROP_JAVA_OPTS) );
    this.executor.setWorkingDirectory( xccdfProperties.getPropertyValue( XCCDFInterpreterProperties.PROP_OCIL_DIR ) );
    this.executor.setJarName( xccdfProperties.getPropertyValue( XCCDFInterpreterProperties.PROP_OCIL_BIN ) );

    if( this.getVerboseOutput() == true ){
      System.out.println( this.executor.buildExecString() );
    }
    
    Process p = this.executor.execute();
    IORedirect ioRedirect = new IORedirect( p.getInputStream(), out, this.verboseOutput );
    ioRedirect.setProcessName( xccdfProperties.getPropertyValue( XCCDFInterpreterProperties.PROP_OCIL_BIN ) + " : " + this.inputFile.getName() );

    Thread t = ioRedirect;
    t.start();
    t.join();

    return p.waitFor();
  }


  public File getInputFile() {
    return inputFile;
  }

  public void setInputFile(File inputFile) {
    this.inputFile = inputFile;
  }

  public File getOutputFile() {
    return outputFile;
  }

  public void setOutputFile(File outputFile) {
    this.outputFile = outputFile;
  }

  public String getQuestionnaireId() {
    return questionnaireId;
  }

  public void setQuestionnaireId(String questionnaireId) {
    this.questionnaireId = questionnaireId;
  }

}
