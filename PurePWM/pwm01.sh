#!/usr/bin/env bash
#
gpio -g mode 18 pwm
gpio pwm-ms
gpio pwmc 192
gpio pwmr 2000
gpio -g pwm 18 150
sleep 1
gpio -g pwm 18 200
echo DOne.
