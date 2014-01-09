package de.imc.mirror.sdk.java.data;

/**
 * This class provides a set of strings for creation, deletion and all columnnames of the database table
 * where all spaces information will be saved.
 * @author Mach
 */
public class SpacesTable {
	public static final String TABLE_NAME = "spaces_table";
	public static final String SPACE_ID = "space_id";
	public static final String SPACE_NAME = "space_name";
	public static final String SPACE_DOMAIN = "space_domain";
	public static final String SPACE_TYPE = "space_type";
	public static final String SPACE_PERSISTENT = "space_persistent";
	public static final String USER = "user";
	public static final String SQL_CREATE = 
										"CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
										SPACE_ID + " VARCHAR(64) NOT NULL," +
										SPACE_NAME + " VARCHAR(64) NOT NULL," +
										SPACE_DOMAIN + " VARCHAR(64) NOT NULL, " +
										SPACE_TYPE + " VARCHAR(64) NOT NULL, " +
										SPACE_PERSISTENT + " VARCHAR(64) NOT NULL, " +
										USER + " VARCHAR(64) NOT NULL);";

	public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
}
