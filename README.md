# owncloud-spring-boot-starter
Spring Owncloud Services and AuthenticationProvider (with UserDetailsService)

## Audience
This Project aims at Developers who wants to enable their SpringBoot Application to be authenticated against an Owncloud Instance and use the Owncloud Provisioning API and the Share API.

## Usage
### Maven
Include this in your ``pom.xml``:
```xml
<dependency>
  <groupId>software.coolstuff</groupId>
  <artifactId>owncloud-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```
### application.properties
Add these Parameters to your ``application.properties``
```properties
owncloud.location=https://owncloud.example.com # URL of the Owncloud Instance
owncloud.username=Administrator                # Username of the Administrator
owncloud.password=s3cr3t                       # Password of the Administrator
```
### WebSecurityConfigurerAdapter
```java
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class MyWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

  @Autowired
  private AuthenticationProvider authenticationProvider;

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.authenticationProvider(authenticationProvider);
  }

}
```

## Services
### OwncloudAuthenticationProvider
