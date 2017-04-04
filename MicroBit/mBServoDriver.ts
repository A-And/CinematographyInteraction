
/**
 * Use this file to define custom functions and blocks.
 * Read more at https://pxt.microbit.org/blocks/custom
 */

enum MyEnum {
    //% block="one"
    One,
    //% block="two"
    Two
}

/**
 * PCA9685 
 */
//% weight=100 color=#0fbc11 icon=""
namespace custom {

    let PCA9685_ADDRESS = 64
    let MODE1 = 0
    let MODE2 = 1
    let SUBADR1 = 2
    let SUBADR2 = 3
    let SUBADR3 = 4
    let PRESCALE = 254
    let LED0_ON_L = 6
    let LED0_ON_H = 7
    let LED0_OFF_L = 8
    let LED0_OFF_H = 9
    let ALL_LED_ON_L = 250
    let ALL_LED_ON_H = 251
    let ALL_LED_OFF_L = 252
    let ALL_LED_OFF_H = 253

    // Bits:
    let RESTART = 128
    let SLEEP = 16
    let NOT_SLEEP = -17
    let ALLCALL = 1
    let INVRT = 16
    let OUTDRV = 4
    let RESET = 0

    let gPeriod = 0
    let gFreq = 0
    let gDegrees = 0
    
    let gMaxDuty = 0
    let gMinDuty = 0

    /**
     * TODO: describe your function here
     * @param n describe parameter here, eg: 5
     * @param s describe parameter here, eg: "Hello"
     * @param e describe parameter here
     */
    //% block


    export function initPWM(): void {
        let buffer = pins.createBuffer(2);
        buffer[0] = MODE1
        buffer[1] = RESET
        pins.i2cWriteBuffer(PCA9685_ADDRESS, buffer);
        //reset not sure if needed but other libraries do it
        setAllPwm(0, 0);

        buffer[0] = MODE2
        buffer[1] = OUTDRV
        pins.i2cWriteBuffer(PCA9685_ADDRESS, buffer);

        buffer[0] = MODE1
        buffer[1] = ALLCALL
        pins.i2cWriteBuffer(PCA9685_ADDRESS, buffer);

        basic.pause(100)
        pins.i2cWriteNumber(PCA9685_ADDRESS, MODE1, NumberFormat.Int8LE);

        let readBuffer = pins.i2cReadBuffer(PCA9685_ADDRESS, 1);
        let mode1 = readBuffer.getNumber(NumberFormat.Int8LE, 0)

        basic.pause(100)

        mode1 = mode1 & (NOT_SLEEP) //# wake up (reset sleep)

        buffer[0] = MODE1
        buffer[1] = mode1
        pins.i2cWriteBuffer(PCA9685_ADDRESS, buffer);
        basic.pause(100)
    }
    export function setPwmFreq(freqHz: number): void {

        //"""Set the PWM frequency to the provided value in hertz."""
        let prescaleval = 25000000.0 // 25MHz
        prescaleval /= 4096.0      // 12 - bit
        prescaleval /= freqHz
        prescaleval -= 1.0

        // print('Setting PWM frequency to {0} Hz'.format(freq_hz))
        // print('Estimated pre-scale: {0}'.format(prescaleval))

        // print('Final pre-scale: {0}'.format(prescale))
        pins.i2cWriteNumber(PCA9685_ADDRESS, MODE1, NumberFormat.Int8LE) // write register we want to read from first

        let readBuffer2 = pins.i2cReadBuffer(PCA9685_ADDRESS, 1);
        let oldmode = readBuffer2.getNumber(NumberFormat.Int8LE, 0)
        let newmode = (oldmode & 0x7F) | 0x10 // # sleep

        let tempBuff = pins.createBuffer(2);

        tempBuff[0] = MODE1
        tempBuff[1] = newmode
        pins.i2cWriteBuffer(PCA9685_ADDRESS, tempBuff);


        tempBuff[0] = PRESCALE
        tempBuff[1] = prescaleval
        pins.i2cWriteBuffer(PCA9685_ADDRESS, tempBuff);

        tempBuff[0] = MODE1
        tempBuff[1] = oldmode
        pins.i2cWriteBuffer(PCA9685_ADDRESS, tempBuff);


        basic.pause(100)

        tempBuff[0] = MODE1
        tempBuff[1] = oldmode | 0x80
        pins.i2cWriteBuffer(PCA9685_ADDRESS, tempBuff);
    }
    export function setAllPwm(on: number, off: number): void {
        let tempBuff = pins.createBuffer(2);

        tempBuff[0] = ALL_LED_ON_L
        tempBuff[1] = on & 0xFF
        pins.i2cWriteBuffer(PCA9685_ADDRESS, tempBuff);


        tempBuff[0] = ALL_LED_ON_H
        tempBuff[1] = on >> 8
        pins.i2cWriteBuffer(PCA9685_ADDRESS, tempBuff);

        tempBuff[0] = ALL_LED_OFF_L
        tempBuff[1] = off & 0xFF
        pins.i2cWriteBuffer(PCA9685_ADDRESS, tempBuff);

        tempBuff[0] = ALL_LED_OFF_H
        tempBuff[1] = off >> 8
        pins.i2cWriteBuffer(PCA9685_ADDRESS, tempBuff);
    };

    export function setPWM(channel: number, on?: number, off?: number): number[] {

        let dataArr: number[];
        //"""Sets a single PWM channel."""
        if (on === undefined || off === undefined) {
            let tempBuff = pins.createBuffer(2);
            pins.i2cWriteNumber(PCA9685_ADDRESS, LED0_ON_L + 4 * channel, NumberFormat.Int8LE);

            let data = pins.i2cReadBuffer(PCA9685_ADDRESS, NumberFormat.Int8LE);


            for (let i = 0; i < data.length; i++) {
                dataArr[i] = data.getNumber(NumberFormat.Int8LE, i);
            }
        }
        else {
            let tempBuff = pins.createBuffer(2);

            tempBuff[0] = LED0_ON_L + 4 * channel
            tempBuff[1] = on & 0xFF
            pins.i2cWriteBuffer(PCA9685_ADDRESS, tempBuff);

            tempBuff[0] = LED0_ON_H + 4 * channel
            tempBuff[1] = on >> 8
            pins.i2cWriteBuffer(PCA9685_ADDRESS, tempBuff);

            tempBuff[0] = LED0_OFF_L + 4 * channel
            tempBuff[1] = off & 0xFF
            pins.i2cWriteBuffer(PCA9685_ADDRESS, tempBuff);

            tempBuff[0] = LED0_OFF_H + 4 * channel
            tempBuff[1] = off >> 8
            pins.i2cWriteBuffer(PCA9685_ADDRESS, tempBuff);

        }
        return dataArr;

    }
    export function duty(index: number, value?: number, invert?: boolean): number{

        if (value === undefined) {
            let pwm = setPWM(index);
            if (pwm[0] === 0)
                value = 0;
            else if (pwm[0] === 4096)
                value = 4095;
            value = pwm[1];
            if (invert)
                value = 4095 - value;
            return value
        }
        if (!(0 <= value) || !(value <= 4095))
            return undefined;
        if (invert)
            value = 4095 - value;
        if (value === 0)
            setPWM(index, 0, 4096);
        else if (value === 4095)
            setPWM(index, 4096, 0);
        else
            setPWM(index, 0, value);
        return undefined
    }

    export function initialize(address?:number, freq?:number, min_us?:number, max_us?:number,
                     degrees?:number){
            address = address || 0x40;
            freq = freq || 50;
            min_us = min_us || 600;
            max_us = max_us || 2400;
            degrees = degrees || 180;
            
            gPeriod = 1000000 / freq
            gMinDuty = _us2duty(min_us)
            gMaxDuty = _us2duty(max_us)
            gDegrees = degrees
            freq = freq
            
            setPwmFreq(freq)

    }
    
    function _us2duty(value:number):number{
        return 4095 * value / gPeriod;
    }
            
    function position(index: number, degr?:number, radians?:number, us?:number, duty?:number):number{
        let span = gMaxDuty - gMinDuty
        if(degr != undefined){
            duty = gMaxDuty + span * degr / gDegrees
        }
        else if (radians != undefined){
            duty = gMinDuty + span * radians / (gDegrees * (Math.PI / 180))
        }
        else if(us != undefined){
            duty = _us2duty(us)
        }
        else if(duty != undefined){
            return undefined;
        }

        else{
            return duty(index)
        }

        duty = Math.min(gMaxDuty, Math.max(gMinDuty, duty))
        duty(index, duty)
    }
    function release(index:number) :void{
        duty(index, 0)
    }
}
