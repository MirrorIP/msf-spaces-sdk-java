package suites;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.imc.mirror.sdk.java.CDMDataBuilderTest;
import de.imc.mirror.sdk.java.cdm.*;

@RunWith(Suite.class)
@SuiteClasses({
	CreationInfoTest.class,
	ReferenceTest.class,
	ReferencesTest.class,
	SummaryTest.class,
	CDMData_1_0Test.class,
	CDMData_2_0Test.class,
	CDMDataBuilderTest.class
})
public class CDMDataSuite {

}
