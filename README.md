# How to use kerberos to authenticate to LDAP in Java

This app describes how can one use Kerberos to authenticate to an LDAP service
It requires that one knows [how kerberos works](https://cyberx.tech/kerberos-authentication/) and
some basic java programming. This app is not for web app kerberos authentication: if you need
that please go to [Baeldung](https://www.baeldung.com/spring-security-kerberos-integration)

The app needs to be configured corrrectly in your environment in order to run correctly. 
During this guide I will make the following assumptions:

- Java 11 SDk installed on the development computer
- Windows environemnt for development in an ative directory domain (A.K.A. Enterprise environmnt)

## How do I use Kerberos to auth LDAP. What does it mean for java?

In order to use a Kerberos [Subject/User](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/security/auth/Subject.html) to 
authenticate an LDAP interaction, one needs 
- first to login to the kerberos and get a tgt (*t*icket-*g*rantig *t*icket), or reuse the cached one 
- second execute a code fragment "as" the authenticated user.

### How to Login to kerberos and obtain a subject

As first thing you need to know your domain 
(An excerpt from this [guide](https://insinuator.net/2016/02/how-to-test-kerberos-authenticated-web-applications/) follows)
```powershell
C:\Users\username>systeminfo
[...]
Domain: example.com
[...]

```
After that, you can query the needed details from the domain itself

```powershell
C:\Users\username>nltest /dsgetdc:example.com
           DC: \\kdc.example.com
      Address: \\10.0.0.1
     Dom Guid: 00000000-0000-0000-0000-000000000000
     Dom Name: example.com
  Forest Name: example.com
 Dc Site Name: Default-First-Site-Name
Our Site Name: Default-First-Site-Name
        Flags: PDC DS LDAP KDC TIMESERV WRITABLE DNS_DC DNS_DOMAIN DNS_FOREST CL
OSE_SITE FULL_SECRET WS
The command completed successfully

```
with this data, you can write the kerberos configuration file, which should look ike this

```ini
[logging]
        Default = FILE:/var/log/krb5.log

[libdefaults]
        ticket_lifetime = 24h
        clock-skew = 300
        default_realm = EXAMPLE.COM
        dns_lookup_realm = false
        dns_lookup_kdc = false
        forwardable = true
        renew_lifetime = 7d

[realms]
        EXAMPLE.COM = {
        kdc = kdc.example.com:88
	; often the admin server is on the same machine of the kdc
	; in my case it was something like DCBLABLABLA.example.local
        admin_server = ad.example.com:464

}

[domain_realm]
        .example.com = EXAMPLE.COM
        example.com = EXAMPLE.COM
```
And yes, the uppercase is important!

Once you have the ```krb5.conf``` you can start with the java implementation.

#### Login to kerberos in Java

In order to login using the kerberos login, we can either have a security configuration like the following

```java
SampleClient /* this is the name of the configuration*/ {
   // Example that reuses the stored tgt token
  com.sun.security.auth.module.Krb5LoginModule required useTicketCache=true;
};

```
or set the [Configuration](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/security/auth/login/Configuration.html) programmatically 

```java
    private static Configuration kerberosTicketCacheConfiguration() {
        return new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                System.out.println("Name: " + name);
                HashMap<String, Object> options = new HashMap<>();
                options.put("useTicketCache", "true");
                //options.put("useKeyTab", "true");
                //options.put("principal", "endeios@EXAMPLE.COM");
                //options.put("storeKey", "true");
                //options.put("doNotPrompt", "true");
                System.out.println(options);
                AppConfigurationEntry appConfigurationEntry = new AppConfigurationEntry(Krb5LoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
                return new AppConfigurationEntry[] { appConfigurationEntry };
            }
        };
    }

```
The meaning of the parameter is in the [Krb5LoginModule](https://docs.oracle.com/en/java/javase/11/docs/api/jdk.security.auth/com/sun/security/auth/module/Krb5LoginModule.html) documentation. 
In the example, the configuration is set to any name of configuration entry

## Tools and basic usage

## Links and doc
- [JNDI tutorial(the download, inside ldap/security/src/GssExample.java and others)](https://docs.oracle.com/javase/jndi/tutorial/)
