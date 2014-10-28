echo ""

cd TC_C_124_v1046/testData

#TC_C_124_TFTP_Good1
../../SetupTFTPSocketC/Release/SetupTFTPSocketC somefile1.txt
echo ""

#TC_C_124_TFTP_Good2
../../SetupTFTPSocketC/Release/SetupTFTPSocketC somefile1.txt localhost
echo ""

#TC_C_124_TFTP_Bad1
../../SetupTFTPSocketC/Release/SetupTFTPSocketC somefile1.txt -s
echo ""

cd ../..

#End of file
