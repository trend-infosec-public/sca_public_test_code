################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../src/recaman.c \
../src/regfuncs.c 

OBJS += \
./src/recaman.o \
./src/regfuncs.o 

C_DEPS += \
./src/recaman.d \
./src/regfuncs.d 


# Each subdirectory must supply rules for building sources it contributes
src/%.o: ../src/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C Compiler'
	$(SS_CC) -O0 -Wall $(SS_CFLAGS) $(SS_OS_SPECIFIC_CFLAGS) -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o  "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '

