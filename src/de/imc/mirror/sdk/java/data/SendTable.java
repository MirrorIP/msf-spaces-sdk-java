package de.imc.mirror.sdk.java.data;

/**
 * This class provides a set of strings for creation, deletion and all columnnames of the database table
 * where all data will be saved which couldn't be sent.
 * @author Mach
 */
public class SendTable {
	public static final String TABLE_NAME = "send_table";
	public static final String SEND_ID = "send_id";
	public static final String SEND_SPACE = "send_node";
	public static final String SEND_NAME = "send_name";
	public static final String SEND_NAMESPACE = "send_namespace";
	public static final String SEND_PAYLOAD = "payload";
	public static final String USER = "user";
	public static final String SQL_CREATE = 
										"CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
										SEND_ID + " VARCHAR(64) NOT NULL," +
										SEND_SPACE + " VARCHAR(64) NOT NULL," +
										SEND_NAME + " VARCHAR(64) NOT NULL," +
										SEND_NAMESPACE + " VARCHAR(64) NOT NULL," + 
										SEND_PAYLOAD + " VARCHAR(1024) NOT NULL," +
										USER + " VARCHAR(64) NOT NULL);";

	public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";

}
