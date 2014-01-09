package de.imc.mirror.sdk.java.data;

/**
 * Class for specifying which type of database should be used, if any.
 * @author mach
 *
 */
public class DatabaseConfig {
	
	public enum Type{
		POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://", 5432),
		MYSQL("com.mysql.jdbc.Driver", "jdbc:mysql://", 3306),
		HSQLDB("org.hsqldb.jdbcDriver", "jdbc:hsqldb:hsql://", 9001),
		EMBEDDED("org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:", -1),
		NONE;
		
		private final String driver;
		private final String connectionURL;
		private final int port;
		
		Type(){
			driver = null;
			connectionURL = null;
			port = 0;
		}
		
		Type(String driver, String connection, int port){
			this.driver = driver;
			this.connectionURL = connection;
			this.port = port;
		}
		
		public String getDriver(){
			return driver;
		}
		
		public String getConnectionURL(){
			return connectionURL;
		}
		
		public int getPort(){
			return port;
		}
	}
	
	private Type type = Type.EMBEDDED;
	private String dbName = "SDKDb";
	private String hostName = "";
	private String dbUser = "SA";
	private String dbPassword = "";
	
	/**
	 * Method to create a new DatabaseConfig object with the specified parameters.
	 * @param type The type the database will be.
	 * @param hostName The name of the host the database is on.
	 * @param dbName The name of the database.
	 * @param dbUser The id of the user with which to access the database.
	 * @param dbPassword The password of the user.
	 * @return A new DatabaseConfigObject.
	 */
	private static DatabaseConfig getDatabaseConfig(Type type,String hostName, String dbName, String dbUser, String dbPassword){
		DatabaseConfig config = new DatabaseConfig();
		config.setType(type);
		config.setHostName(hostName);
		config.setDbName(dbName);
		config.setDbUser(dbUser);
		config.setDbPassword(dbPassword);
		return config;
	}
	
	/**
	 * Method to create a new DatabaseConfig object for using the embedded hsqldb Database.
	 * @param databaseLocation The location the database will be saved at.
	 * @return A new DatabaseConfigObject.
	 */
	public static DatabaseConfig getDefaultConfig(String databaseLocation){
		DatabaseConfig config = new DatabaseConfig();
		config.setDatabaseLocation(databaseLocation);
		return config;
	}
	
	/**
	 * Method to create a new DatabaseConfig object for using a Postgresql database.
	 * @param hostName The name of the host the database is on.
	 * @param dbName The name of the database.
	 * @param dbUser The id of the user with which to access the database.
	 * @param dbPassword The password of the user.
	 * @return A new DatabaseConfigObject.
	 */
	public static DatabaseConfig getPostGresqlConfig(String hostName, String dbName, String dbUser, String dbPassword){
		return getDatabaseConfig(Type.POSTGRESQL, hostName, dbName, dbUser, dbPassword);
	}
	
	/**
	 * Method to create a new DatabaseConfig object for using a MySQL database.
	 * @param hostName The name of the host the database is on.
	 * @param dbName The name of the database.
	 * @param dbUser The id of the user with which to access the database.
	 * @param dbPassword The password of the user.
	 * @return A new DatabaseConfigObject.
	 */
	public static DatabaseConfig getMySqlConfig(String hostName, String dbName, String dbUser, String dbPassword){
		return getDatabaseConfig(Type.MYSQL, hostName, dbName, dbUser, dbPassword);
	}
	
	/**
	 * Method to create a new DatabaseConfig object for using a Hsqldb database.
	 * @param hostName The name of the host the database is on.
	 * @param dbName The name of the database.
	 * @param dbUser The id of the user with which to access the database.
	 * @param dbPassword The password of the user.
	 * @return A new DatabaseConfigObject.
	 */
	public static DatabaseConfig getHsqldbConfig(String hostName, String dbName, String dbUser, String dbPassword){
		return getDatabaseConfig(Type.HSQLDB, hostName, dbName, dbUser, dbPassword);
	}
	
	/**
	 * Method to create a new DatabaseConfig object for using no database at all. With this all data will be saved intern 
	 * during runtime.
	 * @return A new DatabaseConfigObject.
	 */
	public static DatabaseConfig disableDatabaseCaching(){
		DatabaseConfig config = new DatabaseConfig();
		config.setType(Type.NONE);
		return config;
	}
	
	/**
	 * Setter for the hostName. The hostname will act as the location when using the embedded database.
	 * @param location The location of the database.
	 */
	private void setDatabaseLocation(String location){
		if (location.contains("\\") && !location.endsWith("\\")){
			this.hostName = location + "\\";
			
		}
		else if(location.contains("/") && !location.endsWith("/")){
			this.hostName = location + "/";			
		}
		else {
			this.hostName = location;
		}
	}
	
	/**
	 * Getter for the connectionurl of the database. This Url consists of Jdbc-and-dbtype-specific part + hostname + port + dbname
	 * @return The connection url to the database.
	 */
	public String getConnectionURL(){
		if (type == Type.EMBEDDED){
			if (hostName == null){
				throw new IllegalArgumentException("If you want to use the embedded database you have to set the databaselocation first!");
			}
			return type.getConnectionURL() + hostName + dbName;
		}
		else if (type == Type.NONE){
			return "";
		}
		return type.getConnectionURL() + hostName + ":" + type.getPort() + "/" + dbName;
	}
	
	/**
	 * Getter for the driverlocation of the specified databasetype.
	 * @return The driverlocation.
	 */
	public String getDriverLocation(){
		return type.getDriver();
	}
	
	/**
	 * Returns the specified type of the database.
	 * @return The specified type of the database.
	 */
	public Type getType(){
		return type;
	}
	
	/**
	 * Getter for the username.
	 * @return The set username.
	 */
	public String getDbUser() {
		return dbUser;
	}
	
	/**
	 * Getter for the set password.
	 * @return The set password.
	 */
	public String getDbPassword() {
		return dbPassword;
	}
	
	/**
	 * Getter for the name of the database.
	 * @return The set name of the database.
	 */
	public String getDbName(){
		return dbName;
	}
	
	/**
	 * Sets the type of the database to the given one.
	 * @param type The new type of the database.
	 */
	private void setType(Type type){
		this.type = type;
	}
	
	/**
	 * Setter for the hostname.
	 * @param hostName The new hostname.
	 */
	private void setHostName(String hostName){
		this.hostName = hostName;
	}
	
	/**
	 * Setter for the username.
	 * @param user The new username.
	 */
	private void setDbUser(String user){
		this.dbUser = user;
	}
	
	/**
	 * Setter for the password.
	 * @param password The new password.
	 */
	private void setDbPassword(String password){
		this.dbPassword = password;
	}
	
	/**
	 * Setter for the name of the database.
	 * @param dbName The new name of the database.
	 */
	private void setDbName(String dbName){
		this.dbName = dbName;
	}
	
}
