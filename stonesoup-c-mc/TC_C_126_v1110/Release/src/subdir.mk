################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../src/getInc.c \
../src/main.c 

OBJS += \
./src/getInc.o \
./src/main.o 

C_DEPS += \
./src/getInc.d \
./src/main.d 


# Each subdirectory must supply rules for building sources it contributes
src/%.o: ../src/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C Compiler'
	$(SS_CC) -O0 -Wall $(SS_CFLAGS) $(SS_OS_SPECIFIC_CFLAGS) -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o  "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


