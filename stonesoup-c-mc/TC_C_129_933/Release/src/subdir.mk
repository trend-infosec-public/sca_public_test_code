################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../src/keygen.c \
../src/recaman.c \
../src/writefl.c 

OBJS += \
./src/keygen.o \
./src/recaman.o \
./src/writefl.o 

C_DEPS += \
./src/keygen.d \
./src/recaman.d \
./src/writefl.d 


# Each subdirectory must supply rules for building sources it contributes
src/%.o: ../src/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C Compiler'
	$(SS_CC) -O0 -Wall -c  $(SS_CFLAGS) $(SS_OS_SPECIFIC_CFLAGS) -fmessage-length=0 -std=c99 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


