/* 
 * @(#)SampleCallbackHandler.java	1.2 01/05/24
 * 
 * Copyright 1997, 1998, 1999 Sun Microsystems, Inc. All Rights
 * Reserved.
 * 
 * Sun grants you ("Licensee") a non-exclusive, royalty free,
 * license to use, modify and redistribute this software in source and
 * binary code form, provided that i) this copyright notice and license
 * appear on all copies of the software; and ii) Licensee does not 
 * utilize the software in a manner which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE 
 * HEREBY EXCLUDED.  SUN AND ITS LICENSORS SHALL NOT BE LIABLE 
 * FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, 
 * MODIFYING OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN 
 * NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST 
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER 
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT 
 * OF THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS 
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * This software is not designed or intended for use in on-line
 * control of aircraft, air traffic, aircraft navigation or aircraft
 * communications; or in the design, construction, operation or
 * maintenance of any nuclear facility. Licensee represents and warrants
 * that it will not use or redistribute the Software for such purposes.  
 */
package kerberosLDAPIntegration;

import javax.security.auth.callback.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Demonstrates how to write a callback handler for use with SASL.
 * Used with UseCallback.java, GssExample.java, and Mutual.java.
 *
 * Standalone test: java SampleCallbackHandler
 */
public class SampleCallbackHandler implements CallbackHandler {
    public void handle(Callback[] callbacks) 
	throws java.io.IOException, UnsupportedCallbackException {
	    for (int i = 0; i < callbacks.length; i++) {
		if (callbacks[i] instanceof NameCallback) {
		    NameCallback cb = (NameCallback)callbacks[i];
		    cb.setName(getInput(cb.getPrompt()));

		} else if (callbacks[i] instanceof PasswordCallback) {
		    PasswordCallback cb = (PasswordCallback)callbacks[i];

		    String pw = getInput(cb.getPrompt());
		    char[] passwd = new char[pw.length()];
		    pw.getChars(0, passwd.length, passwd, 0);

		    cb.setPassword(passwd);
		} else {
		    throw new UnsupportedCallbackException(callbacks[i]);
		}
	    }
    }

    /**
     * A reader from Standard Input. In real world apps, this would
     * typically be a TextComponent or similar widget.
     */
    private String getInput(String prompt) throws IOException {
	System.out.print(prompt);
	BufferedReader in = new BufferedReader(
	    new InputStreamReader(System.in));
	return in.readLine();
    }

    public static void main(String[] args) throws IOException,
    UnsupportedCallbackException {

	// Test handler

	CallbackHandler ch = new SampleCallbackHandler();
	Callback[] callbacks = new Callback[]{
	    new NameCallback("user id:"),
		new PasswordCallback("password:", true)};

	ch.handle(callbacks);
    }
}
