import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Vector;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.xml.bind.DatatypeConverter;

import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;
import sun.security.x509.AuthorityKeyIdentifierExtension;
import sun.security.x509.BasicConstraintsExtension;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.ExtendedKeyUsageExtension;
import sun.security.x509.KeyIdentifier;
import sun.security.x509.SubjectKeyIdentifierExtension;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

// Original: 
// http://bfo.com/blog/2011/03/08/odds_and_ends_creating_a_new_x_509_certificate.html
public class TestX509 {

	/**
	 * Create a self-signed X.509 Certificate
	 * @param dn the X.509 Distinguished Name, eg "CN=Test"
	 * @param pair the KeyPair
	 * @param days how many days from now the Certificate is valid for
	 */ 
	static X509Certificate generateCertificate(String dn, KeyPair pair, int days) throws Exception {
		PrivateKey privkey = pair.getPrivate();
		X509CertInfo info = new X509CertInfo();
		Date from = new Date();
		Date to = new Date(from.getTime() + days * 86400000L);
		CertificateValidity interval = new CertificateValidity(from, to);
		int sn = (int)((System.currentTimeMillis()/1000) & 0xFFFFFFFF);
		X500Name owner = new X500Name(dn);

		AlgorithmId algo = new AlgorithmId(AlgorithmId.sha1WithRSAEncryption_oid);
		info.set(X509CertInfo.VALIDITY, interval);
		info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
		info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
		info.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner));
		info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
		info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
		info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

		// Extensions
		CertificateExtensions ext = new CertificateExtensions();
		ext.set(BasicConstraintsExtension.NAME, new BasicConstraintsExtension(Boolean.TRUE, true, 0)); // Critical|isCA|pathLen
		ext.set(SubjectKeyIdentifierExtension.NAME, new SubjectKeyIdentifierExtension(new KeyIdentifier(pair.getPublic()).getIdentifier()));
		ext.set(AuthorityKeyIdentifierExtension.NAME, new AuthorityKeyIdentifierExtension(new KeyIdentifier(pair.getPublic()), null, null));
		// Extended Key Usage Extension
		Vector<ObjectIdentifier> ekue = new Vector<ObjectIdentifier>();
		ekue.add(new ObjectIdentifier(new int[] { 1, 3, 6, 1, 5, 5, 7, 3, 1 })); // Server
		ekue.add(new ObjectIdentifier(new int[] { 1, 3, 6, 1, 5, 5, 7, 3, 2 })); // Client
		ext.set(ExtendedKeyUsageExtension.NAME, new ExtendedKeyUsageExtension(Boolean.FALSE, ekue));
		info.set(X509CertInfo.EXTENSIONS, ext);
		
		// Sign the X.509
		X509CertImpl cert = new X509CertImpl(info);
		cert.sign(privkey, algo.getName());
		return cert;
	}

	static void writeCertificate(OutputStream out, X509Certificate crt) throws Exception {
		final int BLOCK_SIZE = 64;
		byte[] buf =  DatatypeConverter.printBase64Binary(crt.getEncoded()).getBytes();
		out.write("-----BEGIN CERTIFICATE-----\r\n".getBytes());
		for (int i = 0; i < buf.length; i += BLOCK_SIZE) {
			out.write(buf, i, Math.min(BLOCK_SIZE, buf.length - i));
			out.write('\r');
			out.write('\n');
		}
		out.write("-----END CERTIFICATE-----\r\n".getBytes());
		out.flush();
		out.close();
	}

	static void writeKey(OutputStream out,  PrivateKey pk) throws Exception {
		final int BLOCK_SIZE = 64;
		byte[] buf =  DatatypeConverter.printBase64Binary(pk.getEncoded()).getBytes();
		out.write("-----BEGIN RSA PRIVATE KEY-----\r\n".getBytes());
		for (int i = 0; i < buf.length; i += BLOCK_SIZE) {
			out.write(buf, i, Math.min(BLOCK_SIZE, buf.length - i));
			out.write('\r');
			out.write('\n');
		}
		out.write("-----END RSA PRIVATE KEY-----\r\n".getBytes());
		out.flush();
		out.close();
	}

	public static void main(String[] args) throws Exception {
		KeyPairGenerator kgAsym = KeyPairGenerator.getInstance("RSA");
		kgAsym.initialize(1024); // TODO: RSA { 1024, 1536, 2048 }
		KeyPair kp = kgAsym.genKeyPair();
		X509Certificate crt = generateCertificate("CN=Test1", kp, 365);
		System.out.println(crt);
		File keyFile = new File(System.getProperty("java.io.tmpdir"), "test.key");
		File crtFile = new File(System.getProperty("java.io.tmpdir"), "test.crt");
		writeKey(new FileOutputStream(keyFile), kp.getPrivate());
		writeCertificate(new FileOutputStream(crtFile), crt);
	}

}
