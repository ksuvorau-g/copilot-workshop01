The error you're seeing:

```
PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
```

means **Java does not trust the SSL/TLS certificate** of the server you're trying to call (`https://api.exchangeratesapi.io`), because it can't find a trusted certificate chain from that certificate to a known Certificate Authority (CA).

This typically happens when:

* The server is using a **certificate signed by an unknown CA**
* You're missing the **intermediate CA certificates**
* Your JVM truststore doesn't contain the required **root CA**
* Your **corporate network** is performing **SSL inspection** (common in some companies), which replaces certificates with their own (untrusted) CA

---

### ✅ Steps to Fix

#### ✅ 1. **Check Certificate in Browser (Quick Verification)**

Open `https://api.exchangeratesapi.io/v1/latest` in your browser and inspect the certificate:

* Is it valid?
* Who is the issuer?
* Is it a well-known CA (e.g., Let's Encrypt, DigiCert)?

If it's valid in Chrome/Firefox but not in Java, the issue is your **JDK truststore**.

---

#### ✅ 2. **Check JDK Truststore Location**

Since you're using **Eclipse Temurin JDK on Windows**, the truststore is usually located at:

```bash
<JDK_HOME>\lib\security\cacerts
```

To find the location programmatically:

```java
System.out.println(System.getProperty("java.home"));
```

---

#### ✅ 3. **Download the Server Certificate**

Run this command to download the certificate chain (use Git Bash or WSL if on Windows):

```bash
openssl s_client -connect api.exchangeratesapi.io:443 -showcerts
```

Copy the certificate(s) between `-----BEGIN CERTIFICATE-----` and `-----END CERTIFICATE-----` to a file, e.g., `exchangerates.crt`.

---

#### ✅ 4. **Import Certificate into Java Truststore**

Use the `keytool` utility:

```bash
keytool -import -trustcacerts -alias exchangeratesapi -file exchangerates.crt -keystore "<JDK_HOME>\lib\security\cacerts" -storepass changeit
```

Explanation:

* `alias`: a name for the cert
* `-storepass changeit`: default password for the truststore
* Replace `<JDK_HOME>` with the actual path to your JDK (e.g., `C:\Program Files\Eclipse Adoptium\jdk-17\`)

---

#### ✅ 5. **Restart Your Application**

Restart your Spring Boot app so it picks up the updated truststore.

---

### 🧪 Optional: Test With a Custom Truststore

Instead of modifying the global truststore, you can create your **own truststore** and point your Spring Boot app to it:

```bash
keytool -import -trustcacerts -alias exchangeratesapi -file exchangerates.crt -keystore custom_truststore.jks -storepass mypassword
```

Then run your app with:

```bash
-Djavax.net.ssl.trustStore=path/to/custom_truststore.jks
-Djavax.net.ssl.trustStorePassword=mypassword
```

---

### 🛑 If You're Behind a Corporate Proxy

Some proxies do **SSL interception** with custom certificates. In that case:

1. Get your company's **CA certificate**
2. Import it into the truststore, same as above

---

### ✅ TL;DR Fix Summary

1. Download certificate with `openssl`
2. Import it into the JVM truststore with `keytool`
3. Restart your app

Let me know if you need help running any of those commands on Windows or in Eclipse.
