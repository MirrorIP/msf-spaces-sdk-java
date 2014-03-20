package suites;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.imc.mirror.sdk.java.ConnectionHandlerTest;
import de.imc.mirror.sdk.java.DataHandlerTest;
import de.imc.mirror.sdk.java.SpaceHandlerTest;

@RunWith(Suite.class)
@SuiteClasses({
	ConnectionHandlerTest.class,
	SpaceHandlerTest.class,
	DataHandlerTest.class
})
public class HandlerSuite {
}
