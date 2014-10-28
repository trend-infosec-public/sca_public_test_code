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
import java.util.List;

public class FileExecutor {
	private final FileExecutionStrategy strategy;
	private boolean redirectErrorStream = false;
	private File workingDirectory;

	public FileExecutor(FileExecutionStrategy strategy) {
		if (strategy == null) {
			throw new NullPointerException("strategy");
		}
		this.strategy = strategy;
	}

	/**
	 * @return the strategy
	 */
	public FileExecutionStrategy getStrategy() {
		return strategy;
	}

	/**
	 * @return the redirectErrorStream
	 */
	public boolean isRedirectErrorStream() {
		return redirectErrorStream;
	}

	/**
	 * @param redirectErrorStream the redirectErrorStream to set
	 */
	public void setRedirectErrorStream(boolean redirectErrorStream) {
		this.redirectErrorStream = redirectErrorStream;
	}

	/**
	 * @return the workingDirectory
	 */
	public File getWorkingDirectory() {
		return workingDirectory;
	}

	/**
	 * @param workingDirectory the workingDirectory to set
	 */
	public void setWorkingDirectory(File workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	public Process execute(List<String> arguments) throws IOException, FileNotFoundException {
		List<String> cmdList = new ArrayList<String>(1 + arguments.size());

		File executable = strategy.getExecutableFile();
		if (executable == null) {
			throw new FileNotFoundException("unable to resolve the executable file using the assigned strategy");
		}

		executable = executable.getCanonicalFile();
		if (!executable.exists()) {
			throw new FileNotFoundException(executable.getAbsolutePath());
		}

		cmdList.add(executable.getAbsolutePath());
		cmdList.addAll(arguments);
		ProcessBuilder pb = new ProcessBuilder(cmdList);

		if (workingDirectory != null) {
			pb.directory(workingDirectory);
		} else {
			pb.directory(executable.getParentFile());
		}
		pb.redirectErrorStream(redirectErrorStream);
		return pb.start();
	}
}
