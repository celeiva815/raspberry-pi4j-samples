package raspisamples.servo;

import analogdigitalconverter.mcp3008.MCP3008Reader;
import com.pi4j.io.i2c.I2CFactory;
import i2c.servo.pwm.PCA9685;

/*
 * Standard, using I2C and the PCA9685 servo board
 * Feedback comes from an MCP3008
 */
public class StandardFeedbackServo {

	private static int ADC_CHANNEL =
					MCP3008Reader.MCP3008_input_channels.CH0.ch(); // Between 0 and 7, 8 channels on the MCP3008

	private static boolean go = true;

	public static void delay(long howMuch) {
		try {
			Thread.sleep(howMuch);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}

	private int servo = -1;

	private final static int DEFAULT_SERVO_MIN = 122; // Value for Min position (-90, unit is [0..1023])
	private final static int DEFAULT_SERVO_MAX = 615; // Value for Max position (+90, unit is [0..1023])

	private int servoMin = DEFAULT_SERVO_MIN;
	private int servoMax = DEFAULT_SERVO_MAX;
	private int diff = servoMax - servoMin;

	private PCA9685 servoBoard = null;

	public StandardFeedbackServo(int channel) throws I2CFactory.UnsupportedBusNumberException {
		this(channel, DEFAULT_SERVO_MIN, DEFAULT_SERVO_MAX);
	}

	public StandardFeedbackServo(int channel, int servoMin, int servoMax) throws I2CFactory.UnsupportedBusNumberException {
		this.servoBoard = new PCA9685();

		this.servoMin = servoMin;
		this.servoMax = servoMax;
		this.diff = servoMax - servoMin;

		int freq = 60;
		servoBoard.setPWMFreq(freq); // Set frequency in Hz

		this.servo = channel;
		System.out.println("Channel " + channel + " all set. Min:" + servoMin + ", Max:" + servoMax + ", diff:" + diff);

	}

	public void setAngle(float f) {
		int pwm = degreeToPWM(servoMin, servoMax, f);
		// System.out.println(f + " degrees (" + pwm + ")");
		servoBoard.setPWM(servo, 0, pwm);
	}

	public void setPWM(int pwm) {
		servoBoard.setPWM(servo, 0, pwm);
	}

	public void stop() { // Set to 0
		servoBoard.setPWM(servo, 0, 0);
	}

	/*
	 * deg in [-90..90]
	 */
	private static int degreeToPWM(int min, int max, float deg) {
		int diff = max - min;
		float oneDeg = diff / 180f;
		return Math.round(min + ((deg + 90) * oneDeg));
	}

	/**
	 * To test the servo - namely, the min & max values.
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		int channel = 7; // For the servo
		if (args.length > 0) {
			try {
				channel = Integer.parseInt(args[0]);
			} catch (Exception e) {
				throw e;
			}
		}
		System.out.println("Servo Channel " + channel);

		MCP3008Reader.initMCP3008();

		// Read the ADC in a thread
		Thread adcReader = new Thread(() -> {
			while (go) {
				int adc = MCP3008Reader.readMCP3008(ADC_CHANNEL);
//			int volume = (int) (adc / 10.23); // [0, 1023] ~ [0x0000, 0x03FF] ~ [0&0, 0&1111111111]
				System.out.println(">>                                      readAdc:" + Integer.toString(adc) +
								" (0x" + lpad(Integer.toString(adc, 16).toUpperCase(), "0", 2) +
								", 0&" + lpad(Integer.toString(adc, 2), "0", 8) + ")");
//			System.out.println("Volume:" + volume + "% (" + adc + ")");
			}
			try {
				synchronized (Thread.currentThread()) {
					Thread.currentThread().wait(100L);
				}
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
			System.out.println("Bye, freeing resources.");
			MCP3008Reader.shutdownMCP3008();
		});
		adcReader.start();

		StandardFeedbackServo ss = new StandardFeedbackServo(channel);

		Thread.sleep(10_000);

		try {
			ss.stop();
			delay(2_000);
			System.out.println("Let's go, 1 by 1 (" + ss.servoMin + " to " + ss.servoMax + ")");
			for (int i = ss.servoMin; i <= ss.servoMax; i++) {
				System.out.println("i=" + i + ", " + (-90f + (((float) (i - ss.servoMin) / (float) ss.diff) * 180f)));
				ss.setPWM(i);
				delay(10);
			}
			for (int i = ss.servoMax; i >= ss.servoMin; i--) {
				System.out.println("i=" + i + ", " + (-90f + (((float) (i - ss.servoMin) / (float) ss.diff) * 180f)));
				ss.setPWM(i);
				delay(10);
			}
			ss.stop();
			delay(2_000);
			System.out.println("Let's go, 1 deg by 1 deg, forward");
			for (int i = ss.servoMin; i <= ss.servoMax; i += (ss.diff / 180)) {
				System.out.println("i=" + i + ", " + Math.round(-90f + (((float) (i - ss.servoMin) / (float) ss.diff) * 180f)));
				ss.setPWM(i);
				delay(10);
			}
			System.out.println("... backward");
			for (int i = ss.servoMax; i >= ss.servoMin; i -= (ss.diff / 180)) {
				System.out.println("i=" + i + ", " + Math.round(-90f + (((float) (i - ss.servoMin) / (float) ss.diff) * 180f)));
				ss.setPWM(i);
				delay(10);
			}
			ss.stop();
			delay(2_000);

			System.out.println("More randomly:");
			float[] degValues = {-10, 0, -90, 45, -30, 90, 10, 20, 30, 40, 50, 60, 70, 80, 90, 0};
			for (float f : degValues) {
				System.out.println("In degrees:" + f);
				ss.setAngle(f);
				delay(1_500);
			}
		} finally {
			ss.stop();
			go = false;
		}

		System.out.println("Done.");
	}

	private static String lpad(String str, String with, int len) {
		String s = str;
		while (s.length() < len)
			s = with + s;
		return s;
	}
}