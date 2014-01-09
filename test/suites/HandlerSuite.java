package suites;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.imc.mirror.sdk.java.DataObjectBuilderTest;
import de.imc.mirror.sdk.java.DataObjectFilterTest;

@RunWith(Suite.class)
@SuiteClasses({
	DataObjectBuilderTest.class,
	DataObjectFilterTest.class
})
public class HandlerSuite {
}
