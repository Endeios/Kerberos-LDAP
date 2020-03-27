# How to use kerberos to authenticate to LDAP in Java

This article describes how one can use Kerberos to authenticate to an LDAP service.
It requires that one knows [how kerberos works](https://cyberx.tech/kerberos-authentication/) and
some basic java programming. This app is not for web app kerberos authentication: if you need
that please go to [Baeldung](https://www.baeldung.com/spring-security-kerberos-integration)

The demo app needs to be configured corrrectly in your environment in order to run correctly. 
During this guide I will make the following assumptions:

- Java 11 SDk installed on the development computer
- Windows environemnt for development in an ative directory domain (A.K.A. Enterprise environmnt)

## How do I use Kerberos to auth LDAP. What does it mean for java?

In order to use a Kerberos [Subject/User](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/security/auth/Subject.html) to 
authenticate an LDAP interaction, one needs 
1. first to *login to the kerberos* and *get a tgt* (*t*icket-*g*rantig *t*icket), or reuse the cached one 
2. second _execute a code fragment *"as"*_ the authenticated user.

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
In the example, the configuration is set to any name of configuration entry.
Finally, you can login
```java
LoginContext ctx = new LoginContext("SampleClient", new SampleCallbackHandler());
ctx.login();
Subject subject = ctx.getSubject();
System.out.println("Login OK");
```
Now that one has the Subject it is possible to proceed to the second step.

### How to execute a code fragment "as" a subject

Java has a standard way to execute a fragment of code as a user: the [Subject.doAs](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/security/auth/Subject.html#doAs(javax.security.auth.Subject,java.security.PrivilegedAction)) method. With this method, a [PrivilegedAction](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/security/PrivilegedAction.html) is launched within a [AccessControlContext](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/security/AccessControlContext.html) in which the current subject is the desired one. At this point, the only needed thing is to create a subclass of PrivilegedAction in which one
calls the LDAP server using as [SECURITY_AUTHENTICATION](https://docs.oracle.com/en/java/javase/11/docs/api/java.naming/javax/naming/Context.html#SECURITY_AUTHENTICATION) the ["GSSAPI"](https://docs.oracle.com/javase/jndi/tutorial/ldap/security/gssapi.html) subsystem. In case Authenticatin is not enough, the suggestion is of course encryption with
setting in the LDAP context ["javax.security.sasl.qop" to "auth-conf,auth-int,auth"](https://docs.oracle.com/en/java/javase/11/docs/api/java.security.sasl/javax/security/sasl/Sasl.html#QOP)

In code, just call the ```Subject.doAs`` function with your subject
```java
Attributes attributes = Subject.doAs(lc.getSubject(), new LDAPAttributesRetrieverAction(true));
```

whre the action is as specified
```java
class LDAPAttributesRetrieverAction implements java.security.PrivilegedAction<Attributes> {
    private final boolean secureMode;

    public LDAPAttributesRetrieverAction(boolean secureMode) {
        this.secureMode = secureMode;
    }

    public Attributes run() {
        Attributes attributes = performJndiOperation();
        return attributes;
    }

    private Attributes performJndiOperation() {

        Hashtable<String, String> env = new Hashtable<>();

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://MYDCSERVER.example.com:389/");
        env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");

        if (secureMode) {
            env.put("javax.security.sasl.qop", "auth-conf,auth-int,auth");
        }
        try {
            final DirContext ctx = new InitialDirContext(env);
            Attributes attributes = ctx.getAttributes("");
            // Close the context when we're done please!
            ctx.close();
            return attributes;
        } catch (final NamingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
```
In this example, the return value is the attributes from your Directory.
This concludes the basic walktrhough.

### Additional configurations and information

The [Kerberos login context](https://docs.oracle.com/en/java/javase/11/docs/api/jdk.security.auth/com/sun/security/auth/module/Krb5LoginModule.html) configuration deserves a bit of more dissertation, because it has the potential to help in many situations. It is of course all in documentation, but this is a reminder.

| Want | Configuration |
|------|---------------|
| I want to use the currently stored and shared tgt token | ```useTicketCache = true``` |
| I want to ask username and password | ```doNotPrompt = false``` |
| I want to authenticate as service, with no user interaction (works also for users!) (see next session for how to keytab) | ```useKeyTab = true ,  principal = <principal name>```  |

#### Some useful definitions

- TGT : ticket grantig ticket : with this ticket you can ask for service ticket and you get it as the first part of the Kerberoos protocol
- Keytab : store for principal/password, where your app has the possibility to retrieve the shared secret to authenticate to the KDC. can be created with a tool.

## Java Tools and basic usage
Sythesis from [here](https://docs.oracle.com/en/java/javase/11/tools/security-tools-and-commands.html)
### [kinit](https://docs.oracle.com/en/java/javase/11/tools/kinit.html)
Initilizes the cached tgt, which means it saved it in a "default" position. Do not worry about the position, usually other tools know where to find it. You can 
request tgts for many principals!
```shell
kinit duke@EXAMPLE.LOCAL
... (changeit)
kinit endeios@EXAMPLE.LOCAL
... (myzuperduperpassword)
```

### [ktab](https://docs.oracle.com/en/java/javase/11/tools/ktab.html)
Creates a keytab file in the "default" location for the givben principal.
You can have mutiple.
```shell
ktab -a endeios@EXAMPLE.COM
```
you can have mutiple entries once you have many service running. ```-a``` means add.

### [klist](https://docs.oracle.com/en/java/javase/11/tools/klist.html)
Displays the tgt you currently have cached in your machine

```shell
klist
Credentials cache C:\Users\duke\krb5cc_duke not found
```

## Links and doc
- [Single Sign On using Kerberos in Java](https://docs.oracle.com/en/java/javase/11/security/single-sign-using-kerberos-java1.html)
- [JNDI tutorial(the download, inside ldap/security/src/GssExample.java and others)](https://docs.oracle.com/javase/jndi/tutorial/)
- [GSS-API/Kerberos v5 Authentication](https://docs.oracle.com/javase/jndi/tutorial/ldap/security/gssapi.html)
- [GSS-API/Kerberos Mechanism ](https://docs.oracle.com/en/java/javase/11/security/kerberos-5-gss-api-mechanism.html)
## Kerberos specific links

- https://web.mit.edu/kerberos/
- https://web.mit.edu/kerberos/krb5-latest/doc/admin/conf_files/krb5_conf.html
