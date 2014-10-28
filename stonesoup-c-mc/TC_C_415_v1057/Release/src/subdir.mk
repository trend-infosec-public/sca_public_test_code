################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../src/clipboard.c \
../src/interfile_1.c \
../src/interfile_2.c \
../src/main.c 

OBJS += \
./src/clipboard.o \
./src/interfile_1.o \
./src/interfile_2.o \
./src/main.o 

C_DEPS += \
./src/clipboard.d \
./src/interfile_1.d \
./src/interfile_2.d \
./src/main.d 


# Each subdirectory must supply rules for building sources it contributes
src/%.o: ../src/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C Compiler'
	$(SS_CC) -O0 -Wall -c  $(SS_CFLAGS) $(SS_OS_SPECIFIC_CFLAGS) -fmessage-length=0 -std=c99 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o"$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '


