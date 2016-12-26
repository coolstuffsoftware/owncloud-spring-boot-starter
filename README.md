# owncloud-spring-boot-starter
Spring Owncloud Services and AuthenticationProvider (with UserDetailsService)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/software.coolstuff/owncloud-spring-boot-starter/badge.png)](https://maven-badges.herokuapp.com/maven-central/software.coolstuff/owncloud-spring-boot-starter)
[![Travis](https://travis-ci.org/coolstuffsoftware/owncloud-spring-boot-starter.svg?branch=master)](https://travis-ci.org/coolstuffsoftware/owncloud-spring-boot-starter)
[![Test Coverage](https://codeclimate.com/github/coolstuffsoftware/owncloud-spring-boot-starter/badges/coverage.svg)](https://codeclimate.com/github/coolstuffsoftware/owncloud-spring-boot-starter/coverage)

## Audience
This Project aims at Developers who wants to enable their SpringBoot Application to be authenticated against an Owncloud Instance and use the Owncloud Provisioning API.

## Short Usage
### Maven
Add the following Dependency to your ``pom.xml``:
```xml
<dependency>
  <groupId>software.coolstuff</groupId>
  <artifactId>owncloud-spring-boot-starter</artifactId>
  <version>1.0.0</version>
</dependency>
```
### application.properties
Add the following Property to your ``application.properties``
```properties
owncloud.location=https://owncloud.example.com # URL of the Owncloud Instance
```
### WebSecurityConfigurerAdapter
Configure the ``WebSecurityConfigurerAdapter`` the following Way:
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

## Documentation
Further Documentation can be found [here](https://coolstuffsoftware.github.io/owncloud-spring-boot-starter)
