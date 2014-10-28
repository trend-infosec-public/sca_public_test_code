@rem echo ""

@cd TC_C_124_v1046\testData

@rem TC_C_124_TFTP_Good1
..\..\SetupTFTPSocketC\Release\SetupTFTPSocketC.exe somefile1.txt
@echo ""

@rem TC_C_124_TFTP_Good2
..\..\SetupTFTPSocketC\Release\SetupTFTPSocketC.exe somefile1.txt localhost
@echo ""

@rem TC_C_124_TFTP_Bad1
..\..\SetupTFTPSocketC\Release\SetupTFTPSocketC.exe somefile1.txt -s
@echo ""

@cd ..\..

@rem End of file
