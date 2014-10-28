################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../src/MouseKeyboard.c \
../src/desaturate.c \
../src/interfile1.c \
../src/interfile2.c 

OBJS += \
./src/MouseKeyboard.o \
./src/desaturate.o \
./src/interfile1.o \
./src/interfile2.o 

C_DEPS += \
./src/MouseKeyboard.d \
./src/desaturate.d \
./src/interfile1.d \
./src/interfile2.d 


# Each subdirectory must supply rules for building sources it contributes
src/%.o: ../src/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C Compiler'
	$(SS_CC) -O0 -Wall -c  $(SS_CFLAGS) $(SS_OS_SPECIFIC_CFLAGS) -fmessage-length=0 -std=c99 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o  "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


