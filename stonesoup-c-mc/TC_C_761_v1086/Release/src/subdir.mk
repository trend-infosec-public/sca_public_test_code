################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../src/TheNextPalindrome.c \
../src/derp.c \
../src/herp.c \
../src/sharemem.c 

OBJS += \
./src/TheNextPalindrome.o \
./src/derp.o \
./src/herp.o \
./src/sharemem.o 

C_DEPS += \
./src/TheNextPalindrome.d \
./src/derp.d \
./src/herp.d \
./src/sharemem.d 


# Each subdirectory must supply rules for building sources it contributes
src/%.o: ../src/%.c
	@echo 'Building file: $<'
	@echo 'Invoking: GCC C Compiler'
	$(SS_CC) -O0 -Wall $(SS_CFLAGS) $(SS_OS_SPECIFIC_CFLAGS) -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"$(@:%.o=%.d)" -o  "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '

