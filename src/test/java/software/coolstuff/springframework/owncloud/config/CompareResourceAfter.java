package software.coolstuff.springframework.owncloud.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import software.coolstuff.springframework.owncloud.service.impl.resource.file.OwncloudFileResourceTest;

/**
 * By setting this Annotation on a Method it will be invoked by {@link AbstractOwncloudResourceTest#tearDown()} right after the TestMethod configured by {@link #value()} and after the changed Resource
 * has been written to the Disk.
 *
 * Plesase note that the Test-Class must also implement the Interface {@link OwncloudFileResourceTest}
 *
 * @author mufasa1976@coolstuff.software
 * @see AbstractOwncloudResourceTest#tearDown()
 * @see OwncloudFileResourceTest
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CompareResourceAfter {

  /** Method Name after this Comparsion of the Input Resource with the written Resource should be started */
  String value() default "";

}
