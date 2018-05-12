import java.io.*;
import java.net.*; 


public class WebServerWorker implements Runnable {

	private Socket workerSock;
	private static int newCookie = 0;
	private boolean gzipEnabled = false;

	public WebServerWorker(Socket clientSocket){
		workerSock = clientSocket;
	}

	public void run() {
		try{

			InputStreamReader istream = new InputStreamReader(workerSock.getInputStream());
			OutputStream socketOut = workerSock.getOutputStream();

			//Parses the request and stores the important information
		    HttpParser httpparser = new HttpParser(istream);
		    //Gets the type of the request (GET or POST here)
		    String requestType = httpparser.getRequestType();
		    //Gets the path that is requested
		    String path = httpparser.getPath();
		    //boolean acceptGzip = httpparser.acceptGzipEncoding();
		    //Get the cookie associated with the request
		    int cookie = httpparser.getCookie();
			
			
			//When the path requested is "/", we're redirecting to "/play.html"
			if(path.equals("/")){
				System.out.println("Redirecting...");
				//Headers
				socketOut.write("HTTP/1.1 303 See Other\r\n".getBytes());
				socketOut.write("Location: /play.html\r\n".getBytes());
				socketOut.write("Connection: close\r\n".getBytes());
				socketOut.write("\r\n".getBytes());
				socketOut.flush();
			}

			
			//Shows the main page and create new game : chunked encoding
			else if((requestType.equals("GET") && path.equals("/play.html")) || 
					(requestType.equals("POST") && path.equals("/replay.html"))){
				
				//Creating new game
				newCookie++;
				GameInterface.createGame(newCookie);

				//Headers
				StringBuilder header = new StringBuilder();
		    	header.append("HTTP/1.1 200 OK\r\n");
			    header.append("Content-Type: text/html\r\n");
			    header.append("Connection: close\r\n");
			    //If gzip is not enabled, we chunk
			    if(!gzipEnabled){
			    	header.append("Transfer-Encoding: chunked\r\n");
			    }
			    header.append("Set-Cookie: SESSID=" + newCookie + "; path=/\r\n");
			    header.append("\r\n");

				//Body					    

			    String previousexchanges = ""; //Empty previous exchanges to create blank page
    			HTMLCreator myhtmlcreator = new HTMLCreator(previousexchanges,socketOut,header.toString(),gzipEnabled);
				myhtmlcreator.createPage();			
			}
			


			//AJAX Request 
			else if(requestType.equals("GET") && path.startsWith("/play.html?")){

				//Get the guess from the header and submit it
				cookie = httpparser.getCookie();
				String guess = httpparser.getGuess_GET();
				String result = GameInterface.submitGuess(cookie,guess);

				//Extract the correct number of well placed colors
				int wellPlacedColor = Character.getNumericValue(result.charAt(0));
				int numberOfGuesses = 0;

				//Get the result of the guess and all the exchanges, including the number of total exchanges
			   	String previousexchanges = GameInterface.getPreviousExchanges(cookie);

	   			if(previousexchanges.length() > 0 && previousexchanges.length() <= 55){

					numberOfGuesses = Character.getNumericValue(previousexchanges.charAt(0));
				}
				else if(previousexchanges.length() > 55){
					numberOfGuesses = Integer.parseInt(previousexchanges.substring(0,2));
				}

				//HTTP Header
		    	socketOut.write("HTTP/1.1 200 OK\r\n".getBytes());
			    socketOut.write("Content-Type: text/html\r\n".getBytes());
			    socketOut.write("Connection: close\r\n".getBytes());
		   		//If we won or lost, we must delete the cookie and delete the game
			   	if(numberOfGuesses == 12 || wellPlacedColor == 4){
			    	socketOut.write("Set-Cookie: SESSID=deleted; path=/;expires=Thu, 01 Jan 1970 00:00:00 GMT\r\n".getBytes());
			    	GameInterface.deleteGame(cookie);
			   	}
			    socketOut.write("\r\n".getBytes());
		
				//HTTP Body
				//Body consists only of the result, no need to chunk or compress
			    socketOut.write(result.getBytes()); 
			    socketOut.flush();
			    socketOut.close();
			}


			//POST request
			else if(requestType.equals("POST") && path.equals("/play.html")){

				//Submit the guess received in the body
				cookie = httpparser.getCookie();
				String guess = httpparser.getGuess_POST();
				String result = GameInterface.submitGuess(cookie,guess); 

				//Extract the correct number of well placed colors
				int wellPlacedColor = Character.getNumericValue(result.charAt(0));
				int numberOfGuesses = 0;

				//Get the result of the guess and all the exchanges, including the number of total exchanges
			   	String previousexchanges = GameInterface.getPreviousExchanges(cookie);

	   			if(previousexchanges.length() > 0 && previousexchanges.length() <= 55){
					numberOfGuesses = Character.getNumericValue(previousexchanges.charAt(0));
				}
				else if(previousexchanges.length() > 55){
					numberOfGuesses = Integer.parseInt(previousexchanges.substring(0,2));
				}

				//HTTP Header
				StringBuilder header = new StringBuilder();

		    	header.append("HTTP/1.1 200 OK\r\n");
			    header.append("Content-Type: text/html\r\n");
			    header.append("Connection: close\r\n");
			    //If gzip is not enabled, we chunk
			    if(!gzipEnabled){
			    	header.append("Transfer-Encoding: chunked\r\n");
			    }		   		
			    //If we won or lost, we must delete the cookie.
			   	if(numberOfGuesses == 12 || wellPlacedColor == 4){
			    	header.append("Set-Cookie: SESSID=deleted; path=/;expires=Thu, 01 Jan 1970 00:00:00 GMT\r\n");
			   	}
			    header.append("\r\n");
			  

				//HTTP Body
			    //POST request needs to recreate the whole page, so we're passing all the previous guesses as argument
	    		HTMLCreator myhtmlcreator = new HTMLCreator(previousexchanges,socketOut,header.toString(),gzipEnabled);
				myhtmlcreator.createPage();			
			}



			//All others paths, these are wrong
			else if(requestType.equals("GET")){

				//Headers
				socketOut.write("HTTP/1.1 404 Not Found\r\n".getBytes());
				socketOut.write("\r\n".getBytes());

				//Generate the HTML error page
				String error404 = generateError("404 NOT FOUND");
				
  				//socketOut.write(error404.toString());
				socketOut.flush();
				socketOut.close();

			}
			
	


			istream.close();

		}catch(Exception e){
			e.printStackTrace();
		}
	}

	// Generate the HTML error pages
	private String generateError(String error){

		StringBuilder page = new StringBuilder();

		page.append("<!DOCTYPE html><html>");
		page.append("<head><meta charset=\"utf-8\"/><title>Error 404</title>");
		page.append("<style>body{font-family: \"Times New Roman\", Arial, serif;font-weight: normal; background-image: radial-gradient(circle at center, rgb(180,255,160), rgb(10,50,0));} .message{font-size: 3.5em; text-align: center; color: rgb(10,50,0);}</style>");
		page.append("</head>");
		page.append("<body><div class=\"message\"><p> <b>"+ error +"</b></p></div></body>");
		page.append("</html>");

		return page.toString();
	}
}