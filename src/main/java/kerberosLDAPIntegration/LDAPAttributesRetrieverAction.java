package kerberosLDAPIntegration;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

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

        // Set up environment for creating initial context
        Hashtable<String, String> env = new Hashtable<>();

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        // Must use fully qualified hostname
        env.put(Context.PROVIDER_URL, "ldap://DCCZBRNO02.ysoft.local:389/");

        // Request the use of the "GSSAPI" SASL mechanism
        // Authenticate by using already established Kerberos credentials
        env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");

        if (secureMode) {
            env.put("javax.security.sasl.qop", "auth-conf,auth-int,auth");
        }
        try {
            /* Create initial context */
            final DirContext ctx = new InitialDirContext(env);
            Attributes attributes = ctx.getAttributes("");
            // do something useful with the context
            // Close the context when we're done
            ctx.close();
            return attributes;
        } catch (final NamingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}