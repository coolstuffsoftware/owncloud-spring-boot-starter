package software.coolstuff.springframework.owncloud.config;

/**
 * A markup Interface to inform the Method {@link OwncloudResourceFileTestExecutionListener#beforeTestClass(org.springframework.test.context.TestContext)} to copy the Classpath Resource
 * <code>classpath:/owncloud.xml</code> to the File-Location set within the application.yml File.
 *
 * This Interface will also be used by {@link AbstractOwncloudResourceTest#setUp()} to always load the Resource-Service with the Data from <code>classpath:/owncloud.xml</code> just right before any
 * Test starts. This will ensure, that every Testcase has the same Information which it builds upon.
 *
 * Also right after a Test the Method {@link AbstractOwncloudResourceTest#tearDown()} will call any Methods annotated by {@link CompareResourceAfter} to compare the written Data with the one, the
 * Tests assumes (normally on no Modification with the Original Source <code>classpath:/owncloud.xml</code>)
 *
 * @author mufasa1976@coolstuff.software
 * @see OwncloudResourceFileTestExecutionListener#beforeTestClass(org.springframework.test.context.TestContext)
 *
 */
public interface OwncloudResourceFileTest {

  default boolean isCheckAllResourcesAgainstOriginal() {
    return true;
  }

}
