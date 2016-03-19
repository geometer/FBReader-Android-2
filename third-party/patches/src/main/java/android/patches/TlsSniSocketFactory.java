// code from http://blog.dev001.net/post/67082904181/android-using-sni-and-tlsv12-with-apache

package android.patches;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import javax.net.ssl.*;

import android.net.SSLCertificateSocketFactory;
import android.os.Build;

import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.params.HttpParams;

public class TlsSniSocketFactory implements LayeredSocketFactory {
	final static HostnameVerifier ourHostnameVerifier = new BrowserCompatHostnameVerifier();

	@Override
	public Socket connectSocket(Socket s, String host, int port, InetAddress localAddress, int localPort, HttpParams params) throws IOException {
		return null;
	}

	@Override
	public Socket createSocket() throws IOException {
		return null;
	}

	@Override
	public boolean isSecure(Socket s) throws IllegalArgumentException {
		return (s instanceof SSLSocket) && ((SSLSocket)s).isConnected();
	}

	// TLS layer
	@Override
	public Socket createSocket(Socket plainSocket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
		if (autoClose) {
			// we don't need the plainSocket
			plainSocket.close();
		}

		// create and connect SSL socket, but don't do hostname/certificate verification yet
		final SSLCertificateSocketFactory sslSocketFactory =
			(SSLCertificateSocketFactory)SSLCertificateSocketFactory.getDefault(0);
		final SSLSocket ssl =
			(SSLSocket)sslSocketFactory.createSocket(InetAddress.getByName(host), port);

		// enable TLSv1.1/1.2 if available; disable SSLv3
		// (see https://github.com/rfc2822/davdroid/issues/229)
		final List<String> protocols = new LinkedList<String>();
		for (String proto : ssl.getSupportedProtocols()) {
			if (proto != null && !proto.toLowerCase().contains("ssl")) {
				protocols.add(proto);
			}
		}
		ssl.setEnabledProtocols(protocols.toArray(new String[protocols.size()]));

		// set up SNI before the handshake
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			sslSocketFactory.setHostname(ssl, host);
		} else {
			try {
				java.lang.reflect.Method setHostnameMethod = ssl.getClass().getMethod("setHostname", String.class);
				setHostnameMethod.invoke(ssl, host);
			} catch (Exception e) {
				// ahh, we cannot set hostname; working with no SNI
			}
		}

		// verify hostname and certificate
		final SSLSession session = ssl.getSession();
		if (!ourHostnameVerifier.verify(host, session)) {
			throw new SSLPeerUnverifiedException("Cannot verify hostname: " + host);
		}

		return ssl;
	}
}
