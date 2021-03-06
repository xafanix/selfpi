package selfpi;

/**
 * to launch: sudo java -cp ".:/home/pi/selfpi/lib/*" selfpi.Launcher 
 */

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.usb.UsbException;

import org.apache.commons.lang3.text.WordUtils;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import io.nayuki.qrcodegen.QrCode;
import selfpi.Gui.SHARE_STATE;

public class SelfPi implements KeyListener {
	public static final String ROOTPATH = "/home/pi/selfpi";
	public static final String SETUP_PATH = ROOTPATH+"/setup/";
	public static final String CONFIG_FILE_PATH = SETUP_PATH+"config.txt";
	public static final String PICTURE_COUNTER_FILE_PATH = SETUP_PATH+"counter.txt";
	public static final String QUOTE_SENTENCES_FILE_PATH = SETUP_PATH+"phrase.txt";
	public static final String FACEBOOK_CONFIG_FILE_PATH = SETUP_PATH+"facebook.txt";
	public static final String TWITTER_CONFIG_FILE_PATH = SETUP_PATH+"twitter.txt";
	public static final String souvenirImageFilefolder = ROOTPATH+"/souvenir/";
	public static final String playerImageFilefolder = ROOTPATH+"/player/";
	public static final String funnyImageFilefolder = ROOTPATH+"/funny/";
	public static final String dslrImageFilefolder = ROOTPATH+"/dslr/";

	public static final String GPhoto2 = "gphoto2 ";
	public static final String capture_tethered_keep_raw_keep = "--capture-tethered --keep-raw --keep ";
	public static final String auto_detect = "--auto-detect ";
	public static final String list_files = "--list-files ";
	public static final String get_file = "--get-file ";

	public static String PI4J_GPIO_ = "GPIO ";
	public static boolean DEBUG = false; 
	
	public static SelfpiState selfpiState = SelfpiState.IDLE_SOUVENIR;

	private static int historySouvenirFileIndex = 0;
	private static int historyPlayerFileIndex = 0;
	private static int historyDSLRFileIndex = 0;

	private static File historyPlayerDirectory = new File(SelfPi.playerImageFilefolder);
	private static File historyFunnyDirectory = new File(SelfPi.funnyImageFilefolder);
	private static File historySouvenirDirectory = new File(SelfPi.souvenirImageFilefolder);
	private static File historyDSLRDirectory = new File(SelfPi.dslrImageFilefolder);


	private static PiCamera3 picam;
	private static EpsonESCPOSPrinter printer;
	private static ButtonLed redButtonLed;
	private static GpioPinDigitalOutput facebookButtonLed;
	private static GpioPinDigitalOutput twitterButtonLed;
	private static FacebookButtonListener facebookButtonListener;
	private static TwitterButtonListener twitterButtonListener;
	private static RedButtonListener redButtonListener;

	private Thread pictureTakingThread;
	private Thread printingThread;
	private Thread waitForSharingThread;
	private Thread facebookSharingThread;
	private Thread twitterSharingThread;
	private Thread websiteSharingThread;

	private FacebookEngine facebook;
	private TwitterEngine twitter;

	private static final String RANDOM_WINNER_FREQKEY = "RANDOM_WINNER_FREQUENCY:";
	private static final String RANDOM_SOUVENIR_FREQKEY = "RANDOM_SOUVENIR_FREQUENCY:";
	
	private static final String RANDOM_WINNER_IMAGESKEY = "RANDOM_WINNER_IMAGES:";
	private static final String RANDOM_FUNNY_IMAGESKEY = "RANDOM_FUNNY_IMAGES:";
	private static final String RANDOM_SOUVENIR_IMAGESKEY = "RANDOM_SOUVENIR_IMAGES:";
	private static final String RANDOM_DSLR_IMAGESKEY = "RANDOM_DSLR_IMAGES:";

	private static final String FUNNY_QUOTEKEY = "FUNNY_QUOTE:";

	private static final String PRINTERPRODUCTIDKEY = "PRINTER_PRODUCT_ID:";
	private static final String PRINTERDOTSKEY = "PRINTERDOTS:";
	private static final String PRINTERSPEEDKEY = "PRINTERSPEED:";
	private static final String PRINTDENSITYKEY = "PRINTDENSITY:";
	private static final String USE_FACEBOOK_KEY = "USE_FACEBOOK:";
	private static final String USE_TWITTER_KEY = "USE_TWITTER:";
	private static final String USE_WEBSHARE_KEY = "USE_WEBSHARE:";
	private static final String SCREEN_HEIGHT_KEY = "SCREEN_HEIGHT:";
	private static final String COUNTDOWNLENGTHKEY = "COUNTDOWNLENGTH:";
	private static final String DITHERINGKEY = "DITHERING_METHOD:";
	private static final String CAMEXPOSUREKEY = "CAMERA_EXPOSURE:";
	private static final String CAMERACONTRASTKEY = "CAMERA_CONTRAST:";
	private static final String CAMERAFPSTKEY = "CAMERA_FPS:";
	private static final String CAMERACOMMANDSKEY = "CAMERA_COMMANDS:";
	private static final String CAMERASTARTSWITHTIMESCANSKEY = "CAMERA_STARTS_WITH_TIMESCAN:";
	private static final String NORMALISEHISTOGRAMKEY = "NORMALISE_HISTOGRAM:";
	private static final String GAMMACORRECTIONKEY = "GAMMA_CORRECTION:";
	private static final String GPIO_REDBUTTONKEY = "GPIO_REDBUTTON:";
	private static final String GPIO_FACEBOOKBUTTONKEY = "GPIO_FACEBOOKBUTTON:";
	private static final String GPIO_TWITTERBUTTONKEY = "GPIO_TWITTERBUTTON:";
	private static final String GPIO_REDLEDKEY = "GPIO_REDLED:";
	private static final String GPIO_FACEBOOKLEDKEY = "GPIO_FACEBOOKLED:";
	private static final String GPIO_TWITTERLEDKEY = "GPIO_TWITTERLED:";
	
	private static final String DEBUGKEY = "DEBUG:";
	
	public static int winnerTicketCounter = 0;
	public static int souvenirTicketCounter = 0;

	// default values
	public static int frequencyRandomTicketWin = 10;
	public static int frequencyRandomTicketSouvenir = 10;
	public static boolean useRandomWinnerImages = false; 
	public static boolean useRandomFunnyImages = false;
	public static boolean useRandomSouvenirImages = false;
	public static boolean useRandomDSLRImages = false;
	public static boolean printFunnyQuote = true;
	public static short printerProductID = 0x0e15;  //
	public static int printerdots = 576;
	public static int printerSpeed = 2;
	public static int printDensity = 65533;
	public static boolean useFacebook = false;
	public static boolean useTwitter = false;
	public static boolean useWebShare = false;
	public static int screenHeight = 1024;
	public static File ticketHeader = new File(SETUP_PATH+"header.png");
	public static File ticketSouvenirFoot = new File(SETUP_PATH+"footsouvenir.png");
	public static File ticketWinnerFoot = new File(SETUP_PATH+"footwinner.png");
	public static int countdownLength = 9;
	public static int ditheringMethod = 2;
	public static String cameraExposure = "+0.1";
	public static String cameraContast = "50";
	public static String cameraFPS = "90";
	public static String cameraCommands = "";
	public static boolean cameraStartsWithTimeScan = true;
	public static boolean normalyseHistogram = true;
	public static double gamma = 1.0; 
	public static String gpio_red_button = "GPIO 4";
	public static String gpio_facebook_button = "GPIO 5";
	public static String gpio_twitter_button = "GPIO 2";
	public static String gpio_red_led = "GPIO 1";
	public static String gpio_facebook_led = "GPIO 6";
	public static String gpio_twitter_led = "GPIO 3";
	
	private Gui gui;
	private File lastSouvenirPictureFile;
	private File lastSouvenirAnimationFile;
	private ArrayList<String> sentences;
	
	private static boolean sharedOnTwitter = false;
	private static boolean sharedOnFacebook = false;
	private static boolean sharedReprinted = false;

	private Thread printHistoryThread;

	public SelfPi() {
		//read config file
		String line;
		try (BufferedReader br = new BufferedReader( new FileReader(CONFIG_FILE_PATH) )){

			line = br.readLine();
			if (line != null && line.contains(RANDOM_WINNER_FREQKEY)) {
				frequencyRandomTicketWin = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(RANDOM_SOUVENIR_FREQKEY)) {
				frequencyRandomTicketSouvenir = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(RANDOM_WINNER_IMAGESKEY)) {
				useRandomWinnerImages = br.readLine().contains("true");
			}
			line = br.readLine();
			if (line != null && line.contains(RANDOM_FUNNY_IMAGESKEY)) {
				useRandomFunnyImages = br.readLine().contains("true");
			}
			line = br.readLine();
			if (line != null && line.contains(RANDOM_SOUVENIR_IMAGESKEY)) {
				useRandomSouvenirImages = br.readLine().contains("true");
			}
			line = br.readLine();
			if (line != null && line.contains(RANDOM_DSLR_IMAGESKEY)) {
				useRandomDSLRImages = br.readLine().contains("true");
			}
			line = br.readLine();
			if (line != null && line.contains(FUNNY_QUOTEKEY)) {
				printFunnyQuote = br.readLine().contains("true");
			}
			line = br.readLine();
			if (line != null && line.contains(USE_FACEBOOK_KEY)) {
				useFacebook = br.readLine().contains("true");
			}
			line = br.readLine();
			if (line != null && line.contains(USE_TWITTER_KEY)) {
				useTwitter = br.readLine().contains("true");
			}
			line = br.readLine();
			if (line != null && line.contains(USE_WEBSHARE_KEY)) {
				useWebShare = br.readLine().contains("true");
			}
			line = br.readLine();
			if (line != null && line.contains(PRINTERPRODUCTIDKEY)) {
				printerProductID = Short.parseShort( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(PRINTERDOTSKEY)) {
				printerdots = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(PRINTERSPEEDKEY)) {
				printerSpeed = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(PRINTDENSITYKEY)) {
				printDensity = Integer.parseInt( br.readLine() );
				printDensity = printDensity < 4 ? (65532 + printDensity) : (printDensity - 4);
			}
			line = br.readLine();
			if (line != null && line.contains(SCREEN_HEIGHT_KEY)) {
				screenHeight = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(COUNTDOWNLENGTHKEY)) {
				countdownLength = Integer.parseInt( br.readLine() );
				countdownLength = countdownLength < 4 ? 4 : countdownLength;
			}
			line = br.readLine();
			if (line != null && line.contains(DITHERINGKEY)) {
				ditheringMethod = Integer.parseInt( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(CAMEXPOSUREKEY)) {
				cameraExposure = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(CAMERACONTRASTKEY)) {
				cameraContast = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(CAMERAFPSTKEY)) {
				cameraFPS = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(CAMERACOMMANDSKEY)) {
				cameraCommands = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(CAMERASTARTSWITHTIMESCANSKEY)) {
				cameraStartsWithTimeScan = br.readLine().contains("true");
			}
			line = br.readLine();
			if (line != null && line.contains(NORMALISEHISTOGRAMKEY)) {
				normalyseHistogram = br.readLine().contains("true");
			}
			line = br.readLine();
			if (line != null && line.contains(GAMMACORRECTIONKEY)) {
				gamma = Double.parseDouble( br.readLine() );
			}
			line = br.readLine();
			if (line != null && line.contains(GPIO_REDBUTTONKEY)) {
				gpio_red_button = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(GPIO_FACEBOOKBUTTONKEY)) {
				gpio_facebook_button = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(GPIO_TWITTERBUTTONKEY)) {
				gpio_twitter_button = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(GPIO_REDLEDKEY)) {
				gpio_red_led = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(GPIO_FACEBOOKLEDKEY)) {
				gpio_facebook_led = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(GPIO_TWITTERLEDKEY)) {
				gpio_twitter_led = br.readLine();
			}
			line = br.readLine();
			if (line != null && line.contains(DEBUGKEY)) {
				DEBUG = br.readLine().contains("true");
			}

		} catch (IOException e) {
			System.out.println("Error in config.txt, trying with default values");
		};

		// Build quotes list
		sentences = new ArrayList<>();
		try (BufferedReader br = new BufferedReader( new FileReader(QUOTE_SENTENCES_FILE_PATH) )){
			do {
				line = br.readLine();
				if (line != null) sentences.add(line);
			} while (line != null);
		} catch (IOException e) {
			System.out.println("Error in phrase.txt");
		};


		// build GUI
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				gui = new Gui();
			}
		});

		//read global counter
		try (BufferedReader br = new BufferedReader( new FileReader(PICTURE_COUNTER_FILE_PATH))){
			souvenirTicketCounter = Integer.parseInt( br.readLine() );
		} catch (IOException e) {
			System.out.println("Error in counter.txt");
		};

		// start Pinter
		try {
			printer = new EpsonESCPOSPrinter(printerProductID);
		} catch (SecurityException | UsbException e) {
			e.printStackTrace();
			if (!DEBUG) System.exit(1);
		}

		// Instanciate Facebook
		if (useFacebook) {
			try {
				facebook = new FacebookEngine();
			} catch (Exception e) {
				System.out.println("Error in Facebook setup");
			}
		}
		
		// Instanciate Twitter
		if (useTwitter) {
			try {
				twitter = new TwitterEngine();
			} catch (Exception e) {
				System.out.println("Error in Twitter setup");
			}
		}

		// GPIO Button and led setup
		GpioController gpio;
		GpioPinDigitalInput redButton;
		GpioPinDigitalInput facebookButton;
		GpioPinDigitalInput twitterButton;
		gpio = GpioFactory.getInstance();

		if (gpio_red_button.contains(PI4J_GPIO_)) {
			//				redButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_04, PinPullResistance.PULL_UP);
			redButton = gpio.provisionDigitalInputPin(RaspiPin.getPinByName(gpio_red_button), PinPullResistance.PULL_UP);
			redButtonListener = new RedButtonListener();
			redButton.addListener(redButtonListener);
		}

		if (gpio_facebook_button.contains(PI4J_GPIO_)) {
			facebookButton = gpio.provisionDigitalInputPin(RaspiPin.getPinByName(gpio_facebook_button), PinPullResistance.PULL_UP);
			facebookButtonListener = new FacebookButtonListener();
			facebookButton.addListener(facebookButtonListener); 
		}
		
		if (gpio_twitter_button.contains(PI4J_GPIO_)) {
			twitterButton = gpio.provisionDigitalInputPin(RaspiPin.getPinByName(gpio_twitter_button), PinPullResistance.PULL_UP);
			twitterButtonListener = new TwitterButtonListener();
			twitterButton.addListener(twitterButtonListener); 
		}

		if (gpio_facebook_led.contains(PI4J_GPIO_)) {
			facebookButtonLed = gpio.provisionDigitalOutputPin(RaspiPin.getPinByName(gpio_facebook_led));
			facebookButtonLed.high();
		}
		
		if (gpio_twitter_led.contains(PI4J_GPIO_)) {
			twitterButtonLed = gpio.provisionDigitalOutputPin(RaspiPin.getPinByName(gpio_twitter_led));
			twitterButtonLed.high();
		}

		if (gpio_red_led.contains(PI4J_GPIO_)) {
			GpioPinPwmOutput buttonLedPin = gpio.provisionPwmOutputPin(RaspiPin.getPinByName(gpio_red_led));
			redButtonLed = new ButtonLed(buttonLedPin);
			redButtonLed.startSoftBlink();
		} else {
			redButtonLed = new ButtonLed(null);
		}

		// Start Pi camera
		picam = new PiCamera3(printerdots, printerdots, cameraStartsWithTimeScan);
		new Thread(picam).start();

	}

	// keyboard listener
	@Override
	public void keyPressed(KeyEvent e) {
		if (DEBUG) {
			System.out.println("key "+e.getKeyCode()+" pressed !");
		}
		if (e.getKeyCode() == KeyEvent.VK_S){
			stateMachineTransition(SelfPiEvent.HISTORY_SOUVERNIR_BUTTON);
		}
		if (e.getKeyCode() == KeyEvent.VK_W){
			stateMachineTransition(SelfPiEvent.HISTORY_PLAYER_BUTTON);
		}
		if (e.getKeyCode() == KeyEvent.VK_D){
			stateMachineTransition(SelfPiEvent.HISTORY_DSLR_BUTTON);
		}
		if (e.getKeyCode() == KeyEvent.VK_I){
			stateMachineTransition(SelfPiEvent.DSLR_IMPORT_BUTTON);
		}
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE){
			stateMachineTransition(SelfPiEvent.RESET);
		}
		if (e.getKeyCode() == KeyEvent.VK_LEFT){
			decrementHistoryFileIndex();
			populateHistoricImages();
		}
		if (e.getKeyCode() == KeyEvent.VK_RIGHT){
			incrementHistoryFileIndex();
			populateHistoricImages();
		}
		if (e.getKeyCode() == KeyEvent.VK_1){
			printHistory(1);
		}
		if (e.getKeyCode() == KeyEvent.VK_2){
			printHistory(2);
		}
		if (e.getKeyCode() == KeyEvent.VK_3){
			printHistory(3);
		}
		if (e.getKeyCode() == KeyEvent.VK_4){
			printHistory(4);
		}
		if (e.getKeyCode() == KeyEvent.VK_5){
			printHistory(5);
		}
		if (e.getKeyCode() == KeyEvent.VK_6){
			printHistory(6);
		}
		if (e.getKeyCode() == KeyEvent.VK_P && (
				selfpiState == SelfpiState.HISTORIC_SOUVENIR ||
				selfpiState == SelfpiState.HISTORIC_PLAYERS ||
				selfpiState == SelfpiState.HISTORIC_DSLR )){
			if (printHistoryThread != null && printHistoryThread.isAlive()) return;
			printHistoryThread = new Thread(new RunPrint6History());
			printHistoryThread.start();
		}
		if (e.getKeyCode() == KeyEvent.VK_SPACE || 
				e.getKeyCode() == KeyEvent.VK_NUMPAD0 ||
				e.getKeyCode() == KeyEvent.VK_0){
			stateMachineTransition(SelfPiEvent.RED_BUTTON);
		}
		if (e.getKeyCode() == KeyEvent.VK_F){
			stateMachineTransition(SelfPiEvent.FACEBOOK_BUTTON);
		}
		if (e.getKeyCode() == KeyEvent.VK_T){
			stateMachineTransition(SelfPiEvent.TWITTER_BUTTON);
		}
	}

	@Override
	public void keyReleased(KeyEvent arg0) {
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	public Gui getGui() {
		return gui;
	}

	protected void stateMachineTransition(SelfPiEvent event){

		switch (selfpiState) {
		case IDLE_SOUVENIR:
			switch (event) {
			case RED_BUTTON:
				selfpiState = SelfpiState.TAKING_PICT;
				takeApicture();
				break;
			case FACEBOOK_BUTTON:
				cameraTimeScanMode();
				break;
			case TWITTER_BUTTON:
				cameraTimeScanMode();
				break;
			case HISTORY_SOUVERNIR_BUTTON:
				selfpiState = SelfpiState.HISTORIC_SOUVENIR;
				populateHistoricImages();
				break;
			case HISTORY_PLAYER_BUTTON:
				selfpiState = SelfpiState.HISTORIC_PLAYERS;
				populateHistoricImages();
				break;
			case HISTORY_DSLR_BUTTON:
				selfpiState = SelfpiState.HISTORIC_DSLR;
				populateHistoricImages();
				break;
			case DSLR_IMPORT_BUTTON:
				selfpiState = SelfpiState.IMPORT_DSLR;
				launchImportDSLR();
				break;
			default:
				break;
			}
			break;

		case TAKING_PICT:
			switch (event) {
			case PRINT:
				selfpiState = SelfpiState.PRINTING;
				print();
				break;
			default:
				break;
			}
			break;

		case PRINTING:
			switch (event) {
			case END_PRINTING:
				selfpiState = SelfpiState.WAIT_FOR_SHARE;
				waitForShare();
				break;
			default:
				break;
			}
			break;

		case WAIT_FOR_SHARE:
			switch (event) {
			case RED_BUTTON:
				rePrint();
				break;
			case FACEBOOK_BUTTON:
				facebookShare();
				break;
			case TWITTER_BUTTON:
				twitterShare();
				break;
			case END_SHARING:
				selfpiState = SelfpiState.IDLE_SOUVENIR;
				resetMode();
				break;
			default:
				break;
			}
			break;

		case HISTORIC_SOUVENIR:
			switch (event) {
			case RESET:
				selfpiState = SelfpiState.IDLE_SOUVENIR;
				break;
			default:
				break;
			}
			break;

		case HISTORIC_PLAYERS:
			switch (event) {
			case RESET:
				selfpiState = SelfpiState.IDLE_SOUVENIR;
				break;
			default:
				break;
			}
			break;

		case HISTORIC_DSLR:
			switch (event) {
			case RESET:
				selfpiState = SelfpiState.IDLE_SOUVENIR;
				break;
			default:
				break;
			}
			break;

		case IMPORT_DSLR:
			switch (event) {
			case RESET:
				selfpiState = SelfpiState.IDLE_SOUVENIR;
				break;
			default:
				break;
			}
			break;

		default:
			break;
		}

		// actualize gui
		gui.setMode(selfpiState);

	}

	private void takeApicture(){
		// Take a picture
		pictureTakingThread = new Thread(new RunTakeAPicture());
		pictureTakingThread.start(); 
	}

	private void print() {
		// print
		printingThread = new Thread(new RunPrinting());
		printingThread.start(); 
	}

	private void waitForShare(){
		if (useFacebook && !(facebookSharingThread != null && facebookSharingThread.isAlive())){
			setFacebookLedON();
			gui.setFacebookShareStatus(Gui.SHARE_STATE.ENABLE);
		} else {
			setFacebookLedOFF();
			gui.setFacebookShareStatus(Gui.SHARE_STATE.DISABLE);
		}
		
		if (useTwitter && !(twitterSharingThread != null && twitterSharingThread.isAlive())){
			setTwitterLedON();
			gui.setTwitterShareStatus(Gui.SHARE_STATE.ENABLE);
		} else {
			setTwitterLedOFF();
			gui.setTwitterShareStatus(Gui.SHARE_STATE.DISABLE);
		}
		
		gui.setReprintShareStatus(Gui.SHARE_STATE.ENABLE);
		
		SelfPi.redButtonLed.startSoftBlink();
		
		waitForSharingThread = new Thread(new RunWaitForSharing());
		waitForSharingThread.start(); 
	}

	private void facebookShare(){
		if (DEBUG) System.out.println("DEBUG: Share to Facebook: "+useFacebook);

		setFacebookLedOFF();

		if (!useFacebook){
			gui.setFacebookShareStatus(Gui.SHARE_STATE.DISABLE); 
			return;
		}
		
		if ( !sharedOnFacebook && !(facebookSharingThread != null && facebookSharingThread.isAlive()) ){
			sharedOnFacebook = true;
			facebookSharingThread = new Thread(new RunShareToFacebook());
			facebookSharingThread.start();
		}
	}
	
	private void twitterShare(){
		if (DEBUG) System.out.println("DEBUG: Share to twitter: "+useTwitter);
		
		setTwitterLedOFF();
		
		if (!useTwitter ){
			gui.setTwitterShareStatus(Gui.SHARE_STATE.DISABLE); 
			return;
		}
		
		if (!sharedOnTwitter && !(twitterSharingThread != null && twitterSharingThread.isAlive()) ) {
			sharedOnTwitter = true;
			twitterSharingThread = new Thread(new RunShareToTwitter());
			twitterSharingThread.start();
		}
	}
	
	private void cameraTimeScanMode() {
		picam.timeScanSwitch();
	}

	private void rePrint(){
		if (sharedReprinted) {
			gui.setReprintShareStatus(Gui.SHARE_STATE.DISABLE); 
			return;
		}
		
		sharedReprinted = true;
		gui.setReprintShareStatus(Gui.SHARE_STATE.ONGOING); 
		SelfPi.redButtonLed.startBlinking();
		if (printer.waitUnlocked()) {
			printer.lock();
			printer.print(lastSouvenirPictureFile);
			printer.cut();
			printer.unlock();
		} else {
			System.out.println("Printer busy / locked");
		}
		SelfPi.redButtonLed.startSoftBlink();
		gui.setReprintShareStatus(Gui.SHARE_STATE.DISABLE); 
	}

	private void incrementHistoryFileIndex() {
		if (selfpiState == SelfpiState.HISTORIC_PLAYERS) {
			File[] listOfFiles = historyFunnyDirectory.listFiles();
			if (historyPlayerFileIndex+6 < listOfFiles.length){
				historyPlayerFileIndex +=6;
			}
		}
		if (selfpiState == SelfpiState.HISTORIC_SOUVENIR) {
			File[] listOfFiles = historySouvenirDirectory.listFiles();
			if (historySouvenirFileIndex+6 < listOfFiles.length){
				historySouvenirFileIndex +=6;
			}
		}
		if (selfpiState == SelfpiState.HISTORIC_DSLR) {
			File[] listOfFiles = historyDSLRDirectory.listFiles();
			if (historyDSLRFileIndex+6 < listOfFiles.length){
				historyDSLRFileIndex +=6;
			}
		}
	}

	private void decrementHistoryFileIndex() {
		if (selfpiState == SelfpiState.HISTORIC_PLAYERS) {
			if (historyPlayerFileIndex-6 >=0 ) historyPlayerFileIndex-=6;
		}
		if (selfpiState == SelfpiState.HISTORIC_SOUVENIR) {
			if (historySouvenirFileIndex-6 >=0 ) historySouvenirFileIndex-=6;
		}
		if (selfpiState == SelfpiState.HISTORIC_DSLR) {
			if (historyDSLRFileIndex-6 >=0 ) historyDSLRFileIndex-=6;
		}
	}

	private void populateHistoricImages() {
		int historyFileIndex;
		File directory;
		if (selfpiState == SelfpiState.HISTORIC_PLAYERS) {
			directory = historyFunnyDirectory;
			historyFileIndex = historyPlayerFileIndex;
		} else if (selfpiState == SelfpiState.HISTORIC_SOUVENIR) {
			directory = historySouvenirDirectory;
			historyFileIndex = historySouvenirFileIndex;
		} else if (selfpiState == SelfpiState.HISTORIC_DSLR) {
			directory = historyDSLRDirectory;
			historyFileIndex = historyDSLRFileIndex;
		} else {
			return;
		}

		System.out.println("populateHistoricImages");
		File[] listOfFiles = directory.listFiles();
		Arrays.sort(listOfFiles);
		final File[] subSetListOfFiles = Arrays.copyOfRange(listOfFiles, historyFileIndex, historyFileIndex+6);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					gui.displayHistoricImages(subSetListOfFiles);
				} catch (InvocationTargetException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();

		//		new Thread( new Runnable() {
		//			@Override
		//			public void run() {
		//				gui.displayHistoricImages(subSetListOfFiles);
		//			}
		//		}).start();
	}

	private void printHistory(int index) {
		File folder;
		int historyFileIndex;
		if (selfpiState == SelfpiState.HISTORIC_PLAYERS) {
			folder = historyFunnyDirectory;
			historyFileIndex = historyPlayerFileIndex;
		} else if (selfpiState == SelfpiState.HISTORIC_SOUVENIR) {
			folder = historySouvenirDirectory;
			historyFileIndex = historySouvenirFileIndex;
		} else if (selfpiState == SelfpiState.HISTORIC_DSLR) {
			folder = historyDSLRDirectory;
			historyFileIndex = historyDSLRFileIndex;
		} else {
			return;
		}

		File[] listOfFiles = folder.listFiles();
		Arrays.sort(listOfFiles);
		int i = historyFileIndex + (index-1);
		if (i < listOfFiles.length && index <= 6 && index > 0) {
			System.out.println("printing: "+folder+"/"+listOfFiles[i].getName());
			try { Thread.sleep(500); } catch (InterruptedException e) {}
			printer.print(listOfFiles[i]);
			// printing delay
			try { Thread.sleep(2000); } catch (InterruptedException e) {}
			printer.cut();
			printer.print(ticketHeader);
			System.out.println("printed.");
		}
	}

	private void launchImportDSLR() {
		new Thread(new ImportDSLR()).start();
	}

	private void resetMode(){
		SelfPi.redButtonLed.startSoftBlink();
		setFacebookLedOFF();
		setTwitterLedOFF();
		sharedOnTwitter = false;
		sharedOnFacebook = false;
		sharedReprinted = false;
	}

	public File chooseWinnerImage(){
		File folder = new File(playerImageFilefolder);
		return chooseRandomFileFromFolder(folder);
	}
	
	public File chooseFunnyImage(){
		File folder = new File(funnyImageFilefolder);
		return chooseRandomFileFromFolder(folder);
	}

	public File chooseRandomSouvenirImage(){
		File folder = new File(SelfPi.souvenirImageFilefolder);
		return chooseRandomFileFromFolder(folder);
	}
	
	public File chooserRandomDSLRImage(){
		File folder = new File(SelfPi.dslrImageFilefolder);
		return chooseRandomFileFromFolder(folder);
	}
	
	public File chooseRandomFileFromFolder(File folder){
		File[] listOfFiles = folder.listFiles();
		int random = (int) (Math.random()*listOfFiles.length);
		return listOfFiles[random];
	}

	public String getRandomQuote(){
		int rand = (int) (Math.random()*sentences.size());
		String sentence = WordUtils.wrap(sentences.get(rand), 48) +"\n";
		return sentence;
	}
	
	public void setFacebookLedON(){
		if (SelfPi.facebookButtonLed != null){
			SelfPi.facebookButtonLed.low();
		}
	}
	
	public boolean isFacebookLedON(){
		return facebookButtonLed.isLow();
	}
	
	public void setFacebookLedOFF(){
		if (SelfPi.facebookButtonLed != null){
			SelfPi.facebookButtonLed.high();
		}
	}
	
	public void setTwitterLedON(){
		if (SelfPi.twitterButtonLed != null){
			SelfPi.twitterButtonLed.low();
		}
	}
	
	public boolean isTwitterLedON(){
		return twitterButtonLed.isLow();
	}
	
	public void setTwitterLedOFF(){
		if (SelfPi.twitterButtonLed != null){
			SelfPi.twitterButtonLed.high();
		}
	}

	public static void main(String[] args) {
		final JFrame frame = new JFrame("SelfPi");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		final SelfPi selfpi = new SelfPi();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				frame.add(selfpi.getGui());
				frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
				frame.setUndecorated(true);
				frame.setVisible(true);
			}
		});

		frame.addKeyListener(selfpi);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("Closing...");
				printer.close();
				picam.close();
//				System.exit(0);
			}
		});
	}

	class RedButtonListener implements GpioPinListenerDigital {

		private long lastPressedTime;

		public RedButtonListener() {
			lastPressedTime = System.currentTimeMillis();
		}

		@Override
		public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
			if (event.getState().isHigh()) return;
			handle();
		}

		public void doPress() {
			handle();
		}

		private void handle() {
			long currentTime = System.currentTimeMillis();
			if ( currentTime - lastPressedTime < 500 ) return; // reject if less than 500 ms
			lastPressedTime = currentTime;

			System.out.println("Red Button pressed !");

			stateMachineTransition(SelfPiEvent.RED_BUTTON);
		}
	} 

	class FacebookButtonListener implements GpioPinListenerDigital {
		private long lastTime;

		public FacebookButtonListener() {
			lastTime = System.currentTimeMillis();
		}

		@Override
		public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
			if (event.getState().isHigh()) return;
			handle();
		}

		public void doPress() {
			handle();
		}

		private void handle() {
			long currentTime = System.currentTimeMillis();
			if ( currentTime - lastTime < 500 ) return; // reject if less than 500 ms
			lastTime = currentTime;

			System.out.println("Facebook Button pressed !");

			stateMachineTransition(SelfPiEvent.FACEBOOK_BUTTON);
		}
	}
	
	class TwitterButtonListener implements GpioPinListenerDigital {
		private long lastTime;

		public TwitterButtonListener() {
			lastTime = System.currentTimeMillis();
		}

		@Override
		public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
			if (event.getState().isHigh()) return;
			handle();
		}

		public void doPress() {
			handle();
		}

		private void handle() {
			long currentTime = System.currentTimeMillis();
			if ( currentTime - lastTime < 500 ) return; // reject if less than 500 ms
			lastTime = currentTime;

			System.out.println("Twitter Button pressed !");

			stateMachineTransition(SelfPiEvent.TWITTER_BUTTON);
		}
	}

	private class RunShareToFacebook implements Runnable{

		@Override
		public void run() {
			
			SelfPi.this.gui.setFacebookShareStatus(Gui.SHARE_STATE.ONGOING);

			if (facebook != null) {
				try {
					String facebookURL = facebook.publishApicture(lastSouvenirPictureFile);
					//print a ticket with QRcode
					System.out.println("facebookURL: "+facebookURL);

					QrCode qr0 = QrCode.encodeText(facebookURL, QrCode.Ecc.LOW);
					BufferedImage img = qr0.toImage(6, 1);
					if (printer.waitUnlocked()) {
						printer.lock();
						printer.print(ticketHeader);
						printer.print(img);
						printer.print("Find your picture: "+facebookURL+"\n");
						printer.cut();
						printer.unlock();
					} else {
						System.out.println("Printer busy / locked");
					}
					
				} catch (IOException e) {
					SelfPi.this.gui.setFacebookShareStatus(Gui.SHARE_STATE.DISABLE);
					e.printStackTrace();
				}
				
			} else {
				System.out.println("facebook is null");
			}
			
			SelfPi.this.gui.setFacebookShareStatus(Gui.SHARE_STATE.DISABLE);
		}
	}
	
	private class RunShareToTwitter implements Runnable{
		

		@Override
		public void run() {
			
			SelfPi.this.gui.setTwitterShareStatus(Gui.SHARE_STATE.ONGOING);

			if (twitter != null) {
				try {
					String tweetURL;
					
					// Ask to compute the last animation as a gif file
					String numberFileName = Integer.toString( (int) (new Date().getTime() /1000) );
					lastSouvenirAnimationFile = new File(souvenirImageFilefolder+numberFileName+".gif");
					picam.saveLastAnimationToFile(lastSouvenirAnimationFile);
					
					if (DEBUG) System.out.println("Twitter: saveLastAnimationToFile = "+lastSouvenirAnimationFile.getAbsolutePath());
					
					// publish
					tweetURL = twitter.publishApicture(lastSouvenirAnimationFile);
					
					//print a ticket with QRcode
					System.out.println("tweetURL: "+tweetURL);
					
					QrCode qr0 = QrCode.encodeText(tweetURL, QrCode.Ecc.LOW);
					BufferedImage img = qr0.toImage(6, 1);
					if (printer.waitUnlocked()) {
						printer.lock();
						printer.print(img);
						printer.print("Find your animated gif: "+tweetURL+" !\n");
						printer.cut();
						printer.print(ticketHeader);
						printer.unlock();
					} else {
						System.out.println("Printer busy / locked");
					}
					
				} catch (IOException e) {
					SelfPi.this.gui.setTwitterShareStatus(Gui.SHARE_STATE.DISABLE);
					e.printStackTrace();
				}
				
			} else {
				System.out.println("twitter is null");
			}
			
			SelfPi.this.gui.setTwitterShareStatus(Gui.SHARE_STATE.DISABLE);
		}
	}
	
	private class RunShareToWebsite implements Runnable{

		@Override
		public void run() {
			if ( UploadFTP.store(lastSouvenirPictureFile) ){
				System.out.println("Upload with FTP: success");
			} else {
				System.out.println("Upload with FTP: failed");
			}
			// end
			SelfPi.this.stateMachineTransition(SelfPiEvent.RESET);
		}
	}

	private class RunPrint6History implements Runnable{
		private int historyFileIndex;
		private File directory;
		public RunPrint6History() {
			if (selfpiState == SelfpiState.HISTORIC_PLAYERS) {
				this.directory = historyFunnyDirectory;
				this.historyFileIndex = historyPlayerFileIndex;
			}
			if (selfpiState == SelfpiState.HISTORIC_SOUVENIR) {
				this.directory = historySouvenirDirectory;
				this.historyFileIndex = historySouvenirFileIndex;
			}
			if (selfpiState == SelfpiState.HISTORIC_DSLR) {
				this.directory = historyDSLRDirectory;
				this.historyFileIndex = historyDSLRFileIndex;
			}
		}

		@Override
		public void run() {

			SelfPi.redButtonLed.startBlinking();

			File folder = directory;
			File[] listOfFiles = folder.listFiles();
			Arrays.sort(listOfFiles);

			for (int i = historyFileIndex; i < listOfFiles.length && i < historyFileIndex+6; i++) {
				System.out.println("printing: "+listOfFiles[i].getName());
				try { Thread.sleep(500); } catch (InterruptedException e) {}
				printer.print(listOfFiles[i]);
				// printing
				try { Thread.sleep(4000); } catch (InterruptedException e) {}
				System.out.println("printed.");
			}

			printer.cut();
			printer.print(ticketHeader);
			SelfPi.redButtonLed.startSoftBlink();
		}

	}

	private class RunWaitForSharing implements Runnable {
		@Override
		public void run() {
			
			if (useFacebook && !(SelfPi.this.facebookSharingThread != null && SelfPi.this.facebookSharingThread.isAlive()) ) {
				setFacebookLedON();
			}
			
			if(useTwitter && !(SelfPi.this.twitterSharingThread != null && SelfPi.this.twitterSharingThread.isAlive()) ) {
				setTwitterLedON();
			}
			
			// wait for sharing
			try { Thread.sleep(10000); } catch (InterruptedException e) {}
			
			// wait for the sharing threads to complete
			int loops = 0;
			while( loops < 30000 ){
				if ( SelfPi.this.facebookSharingThread == null && SelfPi.this.twitterSharingThread == null) break;
				
				if ( (SelfPi.this.facebookSharingThread != null && SelfPi.this.facebookSharingThread.isAlive()) || 
						(SelfPi.this.twitterSharingThread != null && SelfPi.this.twitterSharingThread.isAlive()) ) {
					
					try { Thread.sleep(100); } catch (InterruptedException e) {};
					
				} else break;
				
				loops+=100;
			}

			// end
			setFacebookLedOFF();
			setTwitterLedOFF();
			SelfPi.redButtonLed.startSoftBlink();

			SelfPi.this.stateMachineTransition(SelfPiEvent.END_SHARING);
		}
	}

	private class RunPrinting implements Runnable {

		@Override
		public void run() {
			File fileToPrint = lastSouvenirPictureFile;	//default
			File footToPrint = ticketSouvenirFoot; // default
			boolean randomTicket = souvenirTicketCounter%frequencyRandomTicketSouvenir == 0;

			// if it's a special ticket and according to all the options 
			if (randomTicket){

				if ( SelfPi.useRandomSouvenirImages && SelfPi.useRandomFunnyImages && SelfPi.useRandomDSLRImages ){
					if ( (souvenirTicketCounter/frequencyRandomTicketSouvenir) %3 == 0 ) {
						fileToPrint = chooseRandomSouvenirImage();
					} else if ((souvenirTicketCounter/frequencyRandomTicketSouvenir) %3 == 1 ) {
						fileToPrint = chooseFunnyImage();
					} else if ((souvenirTicketCounter/frequencyRandomTicketSouvenir) %3 == 2 ) {
						fileToPrint = chooserRandomDSLRImage();
					}
					footToPrint = ticketWinnerFoot;
				}
				if ( !SelfPi.useRandomSouvenirImages && SelfPi.useRandomFunnyImages && SelfPi.useRandomDSLRImages ){
					if ( (souvenirTicketCounter/frequencyRandomTicketSouvenir) %2 == 0 ) {
						fileToPrint = chooseFunnyImage();
					} else if ( (souvenirTicketCounter/frequencyRandomTicketSouvenir) %2 == 1 ) {
						fileToPrint = chooserRandomDSLRImage();
					}
					footToPrint = ticketWinnerFoot;
				}
				if ( SelfPi.useRandomSouvenirImages && !SelfPi.useRandomFunnyImages && SelfPi.useRandomDSLRImages ){
					if ( (souvenirTicketCounter/frequencyRandomTicketSouvenir) %2 == 0 ) {
						fileToPrint = chooseRandomSouvenirImage();
					} else if ( (souvenirTicketCounter/frequencyRandomTicketSouvenir) %2 == 1 ) {
						fileToPrint = chooserRandomDSLRImage();
					}
					footToPrint = ticketWinnerFoot;
				}
				if ( SelfPi.useRandomSouvenirImages && SelfPi.useRandomFunnyImages && !SelfPi.useRandomDSLRImages ){
					if ( (souvenirTicketCounter/frequencyRandomTicketSouvenir) %2 == 0 ) {
						fileToPrint = chooseRandomSouvenirImage();
					} else if ( (souvenirTicketCounter/frequencyRandomTicketSouvenir) %2 == 1 ) {
						fileToPrint = chooseFunnyImage();
					}
					footToPrint = ticketWinnerFoot;
				}
				if ( !SelfPi.useRandomSouvenirImages && !SelfPi.useRandomFunnyImages && SelfPi.useRandomDSLRImages ){
					fileToPrint = chooserRandomDSLRImage();
					footToPrint = ticketWinnerFoot;
				}
				if ( SelfPi.useRandomSouvenirImages && !SelfPi.useRandomFunnyImages && !SelfPi.useRandomDSLRImages ){
					fileToPrint = chooseRandomSouvenirImage();
					footToPrint = ticketWinnerFoot;
				}
				if ( !SelfPi.useRandomSouvenirImages && SelfPi.useRandomFunnyImages && !SelfPi.useRandomDSLRImages ){
					fileToPrint = chooseFunnyImage();
					footToPrint = ticketWinnerFoot;
				}

			}

			// print sequence
			if (printer.waitUnlocked()) {
				printer.lock();
				printer.print(ticketHeader);
				printer.print(fileToPrint);
				if (printFunnyQuote) printer.print(getRandomQuote());
				printer.print(footToPrint);
				printer.cut();
				printer.unlock();
			} else {
				System.out.println("Printer busy / locked");
			}

			System.out.println("souvenir ticket number was: "+souvenirTicketCounter);
			
			// increment print counter var and file
			souvenirTicketCounter++;
			Path file = Paths.get(PICTURE_COUNTER_FILE_PATH);
			String line = Integer.toString(souvenirTicketCounter);
			List<String> lines = Arrays.asList(line);
			try {
				Files.write(file, lines);
			} catch (IOException e) {
				e.printStackTrace();
			}				

			// end
			SelfPi.redButtonLed.startSoftBlink();

			SelfPi.this.stateMachineTransition(SelfPiEvent.END_PRINTING);
		}

	}

	private class RunTakeAPicture implements Runnable{

		@Override
		public void run() {

			SelfPi.redButtonLed.startBlinking();

			// wait the countdown
			try { Thread.sleep((countdownLength)*1000); } catch (InterruptedException e) {}

			// take a picture
			String numberFileName = Integer.toString( (int) (new Date().getTime() /1000) );
			lastSouvenirPictureFile = new File(souvenirImageFilefolder+numberFileName+".jpg");
			
			try {
				picam.takeApictureToFile(lastSouvenirPictureFile);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			try { Thread.sleep(100); } catch (InterruptedException e) {}
			SelfPi.this.stateMachineTransition(SelfPiEvent.PRINT);
		}
	} 

	private class ButtonLed {
		private GpioPinPwmOutput ledPin;
		private Timer timer;
		private int pwmValue = -50; 

		public ButtonLed(GpioPinPwmOutput ledPin) {
			this.ledPin = ledPin;
		}

		public void startSoftBlink() {
			if (ledPin == null) return;
			reset();
			TimerTask toggler = new TimerTask() {
				@Override
				public void run() {
					if (pwmValue<50) pwmValue++;
					else pwmValue = -50;
					ledPin.setPwm( Math.abs(pwmValue)*5 );
				}
			};
			timer = new Timer();
			timer.schedule(toggler, 1, 25);
		}

		public void startBlinking() {
			if (ledPin == null) return;
			reset();
			TimerTask toggler = new TimerTask() {
				@Override
				public void run() {
					if (pwmValue >= 50) pwmValue = 0;
					else pwmValue = 1000;
					ledPin.setPwm(pwmValue);
				}
			};

			timer = new Timer();
			timer.schedule(toggler, 1, 300);
		}

		public void reset() {
			if (timer != null) {
				timer.cancel();
				timer = null;
			}
		}
	}
	
	private class ImportDSLR implements Runnable {

		@Override
		public void run() {
			importToDSLRfolder();			
		}

		private void importToDSLRfolder() {
			String output = new String("");
			output += "DSLR import with gphoto2\n";

			gui.setImportText(output);

			File folder = new File(dslrImageFilefolder);
			File[] listOfExisitingFiles = folder.listFiles();

			try {
				Process p = Runtime.getRuntime().exec(GPhoto2 + auto_detect);
				BufferedReader reader = new BufferedReader( new InputStreamReader(p.getInputStream()));

				String line = reader.readLine();
				while (line != null) {
					output += line;
					output += "\n";
					line = reader.readLine();
				}
				p.destroy();
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			gui.setImportText(output);

			output += "Found the following JPEG: \n";
			gui.setImportText(output);

			HashMap<String, Integer> files = new HashMap<>();

			try {
				Process p = Runtime.getRuntime().exec(GPhoto2 + list_files);
				BufferedReader reader = new BufferedReader( new InputStreamReader(p.getInputStream()));

				int fileNumber = 0;
				String line = reader.readLine();
				while (line != null) {

					if (line.startsWith("#")) {
						String[] lineParts = line.split(" ");
						String fileName;
						for (int i = 1; i < lineParts.length; i++) {
							if (lineParts[i].contains("JPG")) {
								fileName = lineParts[i];
								output += lineParts[0]+" ";
								files.put( fileName, Integer.parseInt(lineParts[0].replaceFirst("#", "")) );
								output += fileName;
								if (fileNumber < 7) {
									output += ", ";
								} else {
									output += "\n";
									fileNumber = 0;
								}
								fileNumber++;
								break;
							}
						}
					}
					line = reader.readLine();
				}
				p.destroy();
				reader.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
			output += "\n";
			gui.setImportText(output);

			//		for (int i = 0; i < listOfExisitingFiles.length; i++) {
			//			output += listOfExisitingFiles[i]+"\n";
			//		}
			//		
			ArrayList<Integer> numbersToRetreive = new ArrayList<>();  
			for (Iterator<String> file = files.keySet().iterator(); file.hasNext();) {
				boolean found = false;
				String key = file.next();
				for (int i = 0; i < listOfExisitingFiles.length; i++) {
					if (listOfExisitingFiles[i].getName().equalsIgnoreCase( key )) {
						found = true;
						break;
					}
				}
				if (!found) numbersToRetreive.add( files.get(key) );
			}

			String range = "";
			if (numbersToRetreive.size() > 0) {
				output += "Will get the following files : ";
				for (Iterator<Integer> iterator = numbersToRetreive.iterator(); iterator.hasNext();) {
					int toGet = iterator.next();
					output += toGet + " ";
					range += toGet + ", ";
				}
			} else {
				output += "No new file found.";
			}

			output += "\n";
			gui.setImportText(output);

			if (numbersToRetreive.size() > 0) {
				try {
					Process p = Runtime.getRuntime().exec(GPhoto2+get_file+range, null, new File(dslrImageFilefolder));
					BufferedReader reader = new BufferedReader( new InputStreamReader(p.getInputStream()));

					String line = reader.readLine();
					while (line != null) {
						output += line;
						output += "\n";
						line = reader.readLine();
					}
					p.destroy();
					reader.close();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			output += "\n End";
			gui.setImportText(output);
			output = "";
		}

	}

}
