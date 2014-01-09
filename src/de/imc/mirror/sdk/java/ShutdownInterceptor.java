package de.imc.mirror.sdk.java;

/**
 * Hook for the JVM shutdown process to safely store an external HSQL database if used.
 * This will close the database properly when the program is terminated.
 * @author Mach
 *
 */
public class ShutdownInterceptor extends Thread {
	
	private static ShutdownInterceptor instance;
	
	private ShutdownInterceptor(){
	}
	
	public static ShutdownInterceptor getInstance(){
		if (instance == null){
			instance = new ShutdownInterceptor();
		}
		return instance;
	}
	
	@Override
	public void run(){
		DataWrapperExtern.shutdown();
	}

}
