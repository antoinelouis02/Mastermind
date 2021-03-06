import java.io.*;
import java.net.*; 
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//Class connecting to the client and creating the threadpool to handle connections
public class WebServer {
	
	public static void main (String argv[]) {
		
		ServerSocket serverSocket = null;
		try {	

			serverSocket = new ServerSocket(8006);
			System.out.println("Server started... Listening on " + serverSocket.getLocalPort());
			//Creating a threadpool of threads, the number being passed as an argument when executing 
			//the class
	        ExecutorService executor = Executors.newFixedThreadPool(Integer.parseInt(argv[0]));

			while(true) {
				Socket clientSocket = serverSocket.accept() ;
				Thread t = new Thread(new WebServerWorker(clientSocket));
				executor.execute(t);			
			}
							
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
			try {
				serverSocket.close();
			}
			catch(IOException e1) {
				e1.printStackTrace();
			}
		}
		
	}
}
