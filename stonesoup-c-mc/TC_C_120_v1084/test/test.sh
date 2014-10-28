#!/bin/sh

echo ""

# TC_C_120_Stdin_bostonGood
../Release/TC_C_120_v1084 bostonOut.bmp < boston.bmp
echo ""

# TC_C_120_Stdin_btrflyGood
../Release/TC_C_120_v1084 butterfly24Out.bmp < butterfly24.bmp
echo ""

# TC_C_120_Stdin_googleGood
../Release/TC_C_120_v1084 googleOut.bmp < google.bmp
echo ""

# TC_C_120_Stdin_maineGood
../Release/TC_C_120_v1084 bostonOut.bmp < boston.bmp
echo ""

# TC_C_120_Stdin_sunGood
../Release/TC_C_120_v1084 sunOut.bmp < sun.bmp
echo ""

# TC_C_120_Stdin_Bad
../Release/TC_C_120_v1084 googleBaadOut.bmp < googleBaad.bmp
echo ""

# TC_C_120_Stdin_lena_Bad
../Release/TC_C_120_v1084 lenaOut.bmp < lena.bmp
echo ""

# End of file
