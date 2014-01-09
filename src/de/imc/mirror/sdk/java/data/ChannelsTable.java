package de.imc.mirror.sdk.java.data;

/**
 * This class provides a set of strings for creation, deletion and all columnnames of the database table
 * where all channels information will be saved.
 * @author Mach
 */
public class ChannelsTable {
	public static final String TABLE_NAME = "channels_table";
	public static final String SPACE = "space";
	public static final String TYPE = "type";
	public static final String KEY = "key";
	public static final String VALUE = "value";
	public static final String SQL_CREATE = 
										"CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
										SPACE + " VARCHAR(64) NOT NULL," +
										TYPE + " VARCHAR(64) NOT NULL," +
										KEY + " VARCHAR(64) NOT NULL," +
										VALUE + " VARCHAR(64) NOT NULL);";

	public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";

}
