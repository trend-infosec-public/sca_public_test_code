################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../testData/TC_C_690_base4_goodclient/src/client.c 

OBJS += \
./testData/TC_C_690_base4_goodclient/src/client.o 

C_DEPS += \
./testData/TC_C_690_base4_goodclient/src/client.d 


# Each subdirectory must supply rules for building sources it contributes
testData/TC_C_690_base4_goodclient/src/%.o: ../testData/TC_C_690_base4_goodclient/src/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C Compiler'
	gcc -O0 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


