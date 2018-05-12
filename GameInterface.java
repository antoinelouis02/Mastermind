import java.io.*;
import java.net.*; 
import java.util.Map;
import java.util.HashMap;

public class GameInterface {

	private static Map<Integer, Thread> currentGames = new HashMap<>();
	private static Map<Integer, PipedOutputStream> currentGamesOutput = new HashMap<>();
	private static Map<Integer, PipedInputStream> currentGamesInput = new HashMap<>();

	public static String submitGuess(int cookie,String guess){
		System.out.println("Submitting guess");

		PipedOutputStream gameOut = currentGamesOutput.get(cookie);

		byte[] formattedGuess = formatGuessToByte(guess);
		try{
			gameOut.write(formattedGuess);
			gameOut.flush();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}

		return getResponse(cookie);
	}


	//Returns the previous exchanges and stores the total number of exchanges
	public static String getPreviousExchanges(int cookie){

		System.out.println("Getting previous exchanges");
		PipedOutputStream gameOut = currentGamesOutput.get(cookie);

		//The formatGuessToByte function automatically prepends "12"
		byte[] formattedPrevExchanges = formatGuessToByte("");
		System.out.println("formateddprevexchanges: " + formattedPrevExchanges);
		try{
			gameOut.write(formattedPrevExchanges);
			gameOut.flush();
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}


		return getResponse(cookie);
	}


	
	public static void createGame(int cookie){
		try{
			//Pipes from the interface to the worker and vice versa
			//Interface is going to send through interfaceOut and
			//Worker is going to send through workerOut
			//Both are going to listen to their respective inputstreams
			PipedOutputStream interfaceOut =  new PipedOutputStream();
			PipedInputStream interfaceIn = new PipedInputStream();
			PipedOutputStream workerOut =  new PipedOutputStream();
			PipedInputStream workerIn = new PipedInputStream();


			// InterfaceOut <------> WorkerIn
			// WorkerOut <-------> InterfaceIn
			interfaceOut.connect(workerIn);
			workerOut.connect(interfaceIn);

			System.out.println("Creating new game for cookie " + cookie);
			Thread t = new Thread(new Worker(workerOut,workerIn));
			currentGames.put(cookie,t);
			currentGamesOutput.put(cookie,interfaceOut);
			currentGamesInput.put(cookie,interfaceIn);
			t.start();

			//Start the game
			interfaceOut.write("10".getBytes());
			interfaceOut.flush();

			//Getting response to flush the inputStream
			getResponse(cookie);
			

		}catch(IOException ioe){
			ioe.printStackTrace();
		}	
	}


	private static String getResponse(int cookie) {
		PipedInputStream gameIn = currentGamesInput.get(cookie);
		byte[] rawGuess = new byte[128];
		int length = 0;
		try{
			length = gameIn.read(rawGuess);

		}catch(IOException ioe){
			ioe.printStackTrace();
		}	
		
		String formattedGuess = formatGuessToString(length, rawGuess);
		System.out.println("Response: " + formattedGuess);
		
		return formattedGuess;
	}


	private static byte[] formatGuessToByte(String guess){
		StringBuilder builder = new StringBuilder("12");
		builder.append(guess);

		return builder.toString().getBytes();
	}

	private static String formatGuessToString(int length, byte[] guess){
		String rawGuess = new String(guess);
		rawGuess = rawGuess.substring(0, length);
		String colors = rawGuess.substring(2); //Remove the header
		return new String(colors);
	}
}