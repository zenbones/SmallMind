# FIPS Update

Everything required to run this repository against `bc-fips` (Bouncy Castle FIPS), and what is lost by doing so.
This is a survey of the code as of 2026-11-13 (`main`, after `de322a512`). Nothing has been changed yet.

**Confidence markers.** Items marked **(verify)** rest on my knowledge of the BC-FIPS API and have *not* been checked against the jar. Resolve them with `jar --describe-module`, a compile, or a test run against the chosen `bc-fips` version before relying on them. The exact algorithm set depends on the `bc-fips` line you pick (1.0.x is FIPS 140-2 validated; 2.x is FIPS 140-3), so decide that first (section 1.1).

---

## 0. What "FIPS" means here

Swapping the dependency does not by itself make anything FIPS-compliant. Three things are required:

1. The FIPS jars on the classpath, and the non-FIPS Bouncy Castle jars *off* it. They share package names and cannot coexist.
2. The FIPS providers registered **ahead of** the JDK providers, so unqualified `getInstance("...")` calls resolve to them.
3. **Approved-only mode** switched on (`-Dorg.bouncycastle.fips.approved_only=true`, or `CryptoServicesRegistrar.setApprovedOnlyMode(true)`). Without it BC-FIPS still permits non-approved algorithms, so the code "works" but is not compliant.

Everything the JDK does on its own (SunJSSE, SunJCE, SUN keystores, JAR signature verification, default `SecureRandom`) is **not** FIPS-validated. Anything that bypasses the registered providers silently falls out of compliance.

---

## 1. Build and dependency changes

### 1.1 Decisions to make first

| Decision | Options |
|---|---|
| `bc-fips` line | 1.0.2.x (140-2) vs 2.x (140-3). Affects EdDSA (3.3), 3DES, and which certificate you can cite. |
| TLS provider | `bctls-fips` (BCJSSE) as the JSSE provider. SunJSSE is not validated. |
| Keystore format | `BCFKS` (recommended). JKS and the JDK's PKCS12 are not usable in approved mode. |

### 1.2 Root `pom.xml`

- Line 86: `bouncycastle.version` is `1.84` (the non-FIPS line). Replace with separate properties, because the FIPS artifacts version independently of `bcprov-jdk18on`: `bc-fips.version`, `bcpkix-fips.version`, `bctls-fips.version`, `bcutil-fips.version`.
- Lines ~551-558 (`dependencyManagement`):
  - `org.bouncycastle:bcpkix-jdk18on` -> `org.bouncycastle:bcpkix-fips`
  - `org.bouncycastle:bcprov-jdk18on` -> `org.bouncycastle:bc-fips`
  - add `org.bouncycastle:bctls-fips` and `org.bouncycastle:bcutil-fips`
- Add a Maven Enforcer `bannedDependencies` rule for `org.bouncycastle:bcprov-*`, `bcpkix-jdk18on`, `bctls-jdk18on`, `bcutil-jdk18on`. Third-party libraries can pull them in transitively (check `mvn dependency:tree` for sshd-core, kafka, mongodb, liquibase, jersey). A mix of FIPS and non-FIPS jars fails in confusing ways.

### 1.3 Module POMs

| File | Current | Change |
|---|---|---|
| `nutsnbolts/pom.xml:94-105` | `bcpkix-jdk18on`, `bcprov-jdk18on`, `compile`, `optional` | `bcpkix-fips`, `bc-fips` (plus `bctls-fips`/`bcutil-fips` only if TLS helpers move here); keep `optional` |
| `ansible/pom.xml:17-21` | `bcprov-jdk18on`, `test` | `bc-fips`, `test` |
| `testbench/foundation/pom.xml:17-21` | `bcprov-jdk18on`, `compile` | `bc-fips`, `compile` |
| `web/*`, `mongodb/*`, `quorum`, `phalanx`, `kafka` | no BC dependency | document `bctls-fips`/`bc-fips` as a caller-supplied runtime dependency, consistent with "caller-owned" |

Keep BC-FIPS `optional` in `nutsnbolts`. Integrations in this repo are caller-chosen, so FIPS must not become mandatory for every consumer.

### 1.4 JPMS (`module-info.java`)

- `nutsnbolts/src/main/java/module-info.java:55-56`: `requires static org.bouncycastle.pkix; requires static org.bouncycastle.provider;` -> the FIPS module names. **(verify)** I believe they are `org.bouncycastle.fips.core`, `org.bouncycastle.fips.pkix`, `org.bouncycastle.fips.tls`, `org.bouncycastle.fips.util`; confirm with `jar --describe-module --file bc-fips-<v>.jar`.
- `testbench/foundation/src/main/java/module-info.java:35`: `requires org.bouncycastle.provider;` -> same replacement.
- `ansible/src/main/java/module-info.java` has no BC requirement (BC is test-only there). No change.
- Update `JIGSAW.md` where it lists BC module names.

### 1.5 JVM and deployment settings (not code; document for deployers)

- `-Dorg.bouncycastle.fips.approved_only=true`
- Provider order, via a `java.security` override file (`-Djava.security.properties=`) or programmatically (2.1):
  1. `org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider` (`BCFIPS`)
  2. `org.bouncycastle.jsse.provider.BouncyCastleJsseProvider` (`BCJSSE`, built with `fipsMode=true` and the BCFIPS provider)
  3. demote or remove `SUN`, `SunJCE`, `SunJSSE`, `SunEC`, `SunRsaSign` where possible
- `securerandom.source` / `securerandom.strongAlgorithms`: point at the BCFIPS DRBG, or stop relying on JDK defaults (3.5).
- The BC-FIPS jar must be the **unmodified validated jar**: no repackaging, shading, or relocation. Check `maven-shade-plugin`/`maven-assembly-plugin` usage, the Spark Singularity boot jar, and the `GatingClassLoader`/`SingularityClassLoader`/`MavenScanner` classloaders. **(verify)** whether `spark/singularity` re-packs dependencies. The jar carries a signature/self-integrity check at provider load.

---

## 2. Provider registration

### 2.1 `SecurityProvider` and `SecurityProviderInitializingBean` (`nutsnbolts/.../security/`)

- `SecurityProvider.java`: the enum is `DEFAULT(null), BOUNCY_CASTLE("BC")`. Add `BOUNCY_CASTLE_FIPS("BCFIPS")` and `BOUNCY_CASTLE_JSSE("BCJSSE")`. With `bc-fips` on the classpath `"BC"` will not resolve; keep it only if non-FIPS use must remain possible, otherwise retire it (API break).
- `SecurityProviderInitializingBean.java`: uses `Security.addProvider(provider)`, which **appends** and therefore loses to the JDK providers. Add an ordering option (`Security.insertProviderAt`) or document that callers must use `java.security` overrides. A FIPS provider must be first. Keep the current behavior as the default.
- Add a startup assertion helper (new type, additive): after registration, fail fast if `BCFIPS` is missing, not at position 1, or `CryptoServicesRegistrar.isInApprovedOnlyMode()` is false. This matches the repo's "fail early on contradictory configuration" rule.
- `testbench/foundation/src/main/resources/spring/org/smallmind/testbench/foundation/foundation.xml:15`: `org.bouncycastle.jce.provider.BouncyCastleProvider` -> `org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider`.

### 2.2 Hard-coded provider names

| Location | Current | Change |
|---|---|---|
| `security/CertificateUtility.java:106` | `.setProvider("BC")` on `JcaX509CertificateConverter`; `JcaContentSignerBuilder` has **no** provider | `"BCFIPS"` on both, otherwise signing uses whichever provider is first |
| `ssl/KeyStoreUtility.java:155` | `KeyStore.getInstance("JKS", "SUN")` | `KeyStore.getInstance("BCFKS", "BCFIPS")` |
| `security/AsymmetricKeyType.java:95` | uses `SecurityProvider.getProviderName()` | works once the enum has the FIPS entry; callers (Spring XML, `AsymmetricKeyFactoryBean`) must pass it |

---

## 3. Code changes by class, and functionality lost

### 3.1 Hashing: `HashAlgorithm`, `EncryptionUtility.hash`

| Constant | Status under BC-FIPS approved mode |
|---|---|
| `MD5` | **Unavailable.** `EncryptionUtility.hash(HashAlgorithm.MD5, ...)` throws `NoSuchAlgorithmException`. |
| `SHA_1` | Available for non-signature digests only; deprecated by NIST. Discouraged. |
| `SHA_256/384/512` | Approved. |
| `SHA3_256/384/512` | Approved. |

- Deprecate (preferred) or remove `MD5` and `SHA_1`. Removal is an API break; AGENTS.md says preserve public APIs unless required. If removed, `EncryptionUtilityTest.java:67,147` (asserting MD5) must go.
- `util/BloomFilter.java:77` uses `MessageDigest.getInstance(HashAlgorithm.SHA_1...)`. A Bloom filter needs no cryptographic strength, but it needs a digest. Options: SHA-256 (changes bit positions, so any persisted filter is invalidated) or a non-cryptographic hash outside JCA. Three constructor Javadocs say "SHA-1 ... unavailable" and need updating.
- `memcached/cubby/.../MaglevKeyLocator.java:206-207` (SHA-256, SHA3-256) and `LargeKeyHashingTranslator.java:95` (SHA3-512): approved. No change. They go through `EncryptionUtility.hash`, so they will use BCFIPS once it is first in order. SipHash in `MaglevKeyLocator` is third-party, non-JCA, non-cryptographic use. Not FIPS-relevant.

### 3.2 MACs: `HMACSigningAlgorithm`, `EncryptionUtility.sign/verify` (symmetric)

| Constant | Status |
|---|---|
| `HMAC_MD5` | **Lost.** |
| `HMAC_SHA_1` | HMAC-SHA-1 is still allowed by SP 800-131A but legacy; discouraged. |
| `HMAC_SHA_256` | Approved. |

- **Key-length floor.** BC-FIPS approved mode rejects HMAC keys shorter than 112 bits. `generateKey(byte[] secret)` wrapped raw bytes in `SecretKeySpec` with no check, so a short secret would fail at `Mac.init`. **(verify)** the threshold for your version. **Done:** `HMACSigningAlgorithm.generateKey` now rejects `null` and secrets under `MINIMUM_KEY_BYTES` (14 bytes, 112 bits) with `IllegalArgumentException`, with or without a FIPS provider. This tightens non-FIPS behavior too; every caller in this repo already passes at least 32 bytes. `web/jwt/SymmetricJWTKeyMaster.java:64` already SHA-256-hashes short secrets to 256 bits, which passes the floor but is plain-hash stretching, not a KDF.

### 3.3 Signatures: `RSASigningAlgorithm`, `ECDSASigningAlgorithm`, `EdDSASigningAlgorithm`, `AsymmetricAlgorithm`

- `RSASigningAlgorithm.SHA_256_WITH_RSA`: approved (RSA >= 2048).
- **Done.** `ECDSASigningAlgorithm` now has `SHA_256_WITH_ECDSA`, `SHA_384_WITH_ECDSA`, `SHA_512_WITH_ECDSA` (real JCA names; the old jose4j-style constant never worked). `AsymmetricAlgorithm.EC` was added. Signatures are DER, not the R||S form JOSE `ES256` needs.
- `EdDSASigningAlgorithm.ED_DSA("EdDSA")` and `AsymmetricAlgorithm.ED25519`: **version-dependent.** Ed25519/Ed448 became approved with FIPS 186-5. **(verify)** support in your `bc-fips` line. If absent, EdDSA is **lost**, which affects JWT `EDDSA`, `AsymmetricJWTKeyMaster`, OpenSSH `ssh-ed25519` handling, and `AsymmetricKeyGenerator ed25519`.
- **Done.** Key sizes: `generateKeyPair(AsymmetricAlgorithm)` now initializes RSA to 3072 and EC to 384 (secp384r1, pairing with `SHA_384_WITH_ECDSA`); new overload `generateKeyPair(AsymmetricAlgorithm, int keySize)`. `generateKey(SymmetricAlgorithm)` now initializes AES to 256 and resolves the algorithm by `getAlgorithmName()` (the old `name()` lookup broke `TRIPLE_DES`/`RC4`); new overload `generateKey(SymmetricAlgorithm, int keySize)`. Ed25519 has a single size. Remaining for FIPS: BC-FIPS-specific size restrictions for RSA/EC are unverified **(verify)**.
- **Done.** OpenSSH public key text prefix is read from the encoded blob (`ssh-rsa`, `ssh-ed25519`, `ecdsa-sha2-nistpNNN`) instead of derived from the JCA algorithm name.

### 3.4 Symmetric ciphers: `SymmetricAlgorithm`, `EncryptionUtility.encrypt/decrypt`, `json/Encryption`

| Constant | Status |
|---|---|
| `AES` | Approved. |
| `DES` | **Lost.** Not in BC-FIPS. |
| `TRIPLE_DES` (`DESede`) | Withdrawn for encryption by NIST (2023); 140-3 modules are decrypt-only. Treat as lost. **(verify)** |
| `RC4` (`ARCFOUR`) | **Lost.** |
| `BLOWFISH` | **Lost.** |

- Deprecate or remove `DES`, `TRIPLE_DES`, `RC4`, `BLOWFISH`.
- `EncryptionUtility.encrypt(Key, byte[])` without a cipher string uses `Cipher.getInstance(key.getAlgorithm())`, i.e. bare `"AES"`, which is AES/ECB in most providers. ECB is an approved mode but not a sound default. Require an explicit transformation, or default new overloads to AES/GCM/NoPadding.
- AES-GCM in BC-FIPS expects the module to generate the IV; a caller-supplied `GCMParameterSpec` on encryption can be rejected in approved mode. If GCM helpers are added, let the cipher make the IV and return it with the ciphertext. **(verify)**
- **Done.** `json/Encryption.AES` is now AES/GCM/NoPadding via `EncryptionUtility`, with a caller-generated random 96-bit IV (`SecureRandom`, isolated in `generateIv()`), stored as IV then ciphertext and 128-bit tag. Old fixed-IV AES/CBC payloads are not readable; re-encrypting stored `BinaryData` is handled outside this repo. **(verify)** BC-FIPS approved mode may reject a caller-supplied GCM IV on encryption; if so, `generateIv()` and the `GCMParameterSpec` passed to `EncryptionUtility` must be replaced by a provider-generated IV.
- `PKCS5Padding` is accepted as an alias of PKCS7 in BC. **(verify)**

### 3.5 Randomness

- `security/password/PBKDF2PasswordEngine.java:62` and `Argon2IDPasswordEngine.java:63`: `SecureRandom.getInstance("SHA1PRNG")`. That is a SUN-provider algorithm, not provided by BC-FIPS and not an approved DRBG. Use `new SecureRandom()` (first provider's default, the BCFIPS DRBG) or `CryptoServicesRegistrar.getSecureRandom()`. Drop the "force the instance to seed itself" workaround.
- `util/SnowflakeId.java:64`: `new SecureRandom(Bytes.getBytes(System.currentTimeMillis()))`. Remove the time-based seed argument; it is weak input and not a way to control the DRBG. Line 145 uses `ThreadLocalRandom`, which is non-cryptographic and not FIPS-relevant provided it never feeds secrets.
- `quorum/.../juggler/Juggler.java:75`, `ssl/NaiveSSLSocketFactory.java:75`, `MongoClientSettingsFactoryBean.java:340`, `web/jersey/.../ClientUtility.java:62`: `new SecureRandom()`. Fine; becomes the BCFIPS DRBG automatically.
- `file/ephemeral` (`EphemeralBasicFileAttributes`, `HeapFileContent`) only mention `SecureRandom` in comments about avoiding it. No change.
- DRBG startup can be slow on low-entropy hosts or containers. **(verify)** at deployment.

### 3.6 Password hashing: `PBKDF2PasswordEngine`, `Argon2IDPasswordEngine`

- **`Argon2IDPasswordEngine` is lost.** Argon2 is not an approved KDF, and `bc-fips` does not ship `Argon2BytesGenerator`/`Argon2Parameters` (compile error). Delete the class, or move it to a separate non-FIPS artifact that depends on `bcprov-jdk18on`, so FIPS consumers never see it. Hashes already stored in Argon2 form cannot be verified under FIPS; migrate with re-hash-on-login.
- `PBKDF2PasswordEngine`:
  - Uses `PBKDF2WithHmacSHA1` (line 179). Move to `PBKDF2WithHmacSHA256` or 512. That **invalidates all stored hashes**; add a scheme marker or dual verification during migration.
  - BC-FIPS approved mode enforces a **minimum PBKDF2 password length of 14 bytes (112 bits)** and a salt of at least 128 bits. **(verify)** Shorter passwords make `generateSecret` throw, while the engine only rejects empty ones. This is a **functional loss**: short passwords cannot be hashed in approved mode. Enforce a minimum in application policy.
  - Defaults (50 000 iterations, 32-byte salt, 256-bit key) satisfy SP 800-132.
- `security/password/PasswordEngine.java`: update the Javadoc if it lists Argon2.

### 3.7 Key encoding and certificates: `AsymmetricKeySpec`, `X509KeyFactorUtility`, `CertificateUtility`

These use non-FIPS Bouncy Castle APIs directly and are the largest compile-level gap.

- `AsymmetricKeySpec.OPENSSH` uses `OpenSSHPublicKeyUtil`, `OpenSSHPrivateKeyUtil`, `PublicKeyFactory`, `PrivateKeyFactory` (lightweight `org.bouncycastle.crypto.util`) and `OpenSSHPublicKeySpec`/`OpenSSHPrivateKeySpec` (`org.bouncycastle.jcajce.spec`). **(verify)** These are not exposed by `bc-fips`. Expect either a hand-written OpenSSH wire-format encoder/decoder (length-prefixed strings; `openssh-key-v1` private format has a cipher/KDF header) or dropping OPENSSH. At risk: `AsymmetricKeyGenerator` output and `AsymmetricKeyFactoryBean` with `spec=OPENSSH`.
- `AsymmetricKeySpec.PKCS8`/`X509` use `PemWriter`, `PemReader`, `PemObject` (`org.bouncycastle.util.io.pem`). **(verify)** presence in `bc-fips`; otherwise use `java.util.Base64`, which also removes the dependency.
- `X509KeyFactorUtility` uses `org.bouncycastle.asn1.*` (`ASN1Sequence`, `ASN1Integer`, `ASN1BitString`, `DEROctetString`, `ASN1ObjectIdentifier`). **(verify)** each class exists in `bc-fips`. Parsing is not a cryptographic operation, so approved mode is not a concern.
- `CertificateUtility` uses `X509v3CertificateBuilder`, `JcaX509v3CertificateBuilder`, `JcaContentSignerBuilder`, `JcaX509CertificateConverter`, `OperatorCreationException`. The FIPS equivalent is `bcpkix-fips` with the same `org.bouncycastle.cert.*`/`org.bouncycastle.operator.*` packages. **(verify)** by compiling. Set the provider on `JcaContentSignerBuilder` (2.2) and offer only approved signature algorithm names.

### 3.8 Keystores and trust: `KeyStoreUtility`, `TrustManagerUtility`, `ClassVerifier`, `SecureStore`

- `ssl/KeyStoreUtility.java`:
  - `KeyStore.getInstance("JKS", "SUN")` -> `("BCFKS", "BCFIPS")`. **JKS is lost**: it uses proprietary PBE and SHA-1 integrity.
  - The default file name `keystore.jks` and `.jks` suffix logic (line ~66) -> `.bcfks`. Convert existing files with `keytool -importkeystore -srcstoretype JKS -deststoretype BCFKS -providerpath bc-fips.jar -provider org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider`. Callers hard-coding `.jks` via `KeyStoreInfo` break.
  - The default password `"changeit"` (8 chars) is below the 14-byte PBKDF2 floor BCFKS uses. **(verify)** It will throw in approved mode. Remove the default or require a password. A default password is a smell regardless of FIPS.
  - `CertificateFactory.getInstance("X509")` (line ~213) -> `"X.509"`. **(verify)** BCFIPS accepts the former.
  - Existing issue, unrelated to FIPS: the `Files.newOutputStream(...)`/`newInputStream(...)` streams are never closed.
- `ssl/TrustManagerUtility.java:74-79`: `KeyStore.getInstance(KeyStore.getDefaultType())` is the JDK `pkcs12`. Use `BCFKS`/`BCFIPS` explicitly (in memory, null password, so no length issue). Use `TrustManagerFactory.getInstance("PKIX", "BCJSSE")`.
- `lang/ClassVerifier.java:80`: the keystore type is caller-supplied with a `KeyStore.getDefaultType()` fallback; callers must now pass `"BCFKS"`. `certificates[0].verify(publicKey)` runs on the default provider; use `verify(key, "BCFIPS")`. **Gap:** `codeSource.getCertificates()` is populated by the **JDK's JAR verifier**, which uses SUN providers and cannot be redirected to BC-FIPS. JAR-signature validation remains a non-validated crypto path. Document it, or verify signatures explicitly instead.
- `lang/SecureStore.java`: bytes plus password, no crypto. Used by Jetty/Grizzly.

### 3.9 TLS servers and clients

All of these default to SunJSSE and JKS/PKCS12 unless told otherwise.

- `ssl/NaiveSSLSocketFactory.java:75`: `SSLContext.getInstance("SSL")` -> `("TLS", "BCJSSE")`. **(verify)** BCJSSE support for the `"SSL"` alias. The `Naive*` classes (`NaiveTrustManager`, `NaiveHostNameVerifier`, `NaiveSSLSocketFactory`) disable certificate and hostname validation. That is not a FIPS-algorithm problem, but it is incompatible with any compliance posture that includes server authentication. Restrict to test scope or exclude from FIPS builds.
- `web/jersey/.../ssl/ClientUtility.java:60-69`: `"TLS"` context with `NaiveTrustManager` and `NaiveHostNameVerifier`; it also calls `HttpsURLConnection.setDefaultSSLSocketFactory(...)`, which mutates JVM-global state. Pass the provider; naive trust is non-compliant.
- `web/http/.../apache/HttpClient.java:66`: `SSLContexts.custom().loadTrustMaterial((chain, authType) -> true)` is trust-all on the default `TLS` context. Needs `setProvider("BCJSSE")` and a real trust strategy.
- `web/jetty/.../JettyInitializingBean.java:338-361`: `SslContextFactory.Server` with in-memory stores. Add `setKeyStoreType("BCFKS")`, `setKeyStoreProvider("BCFIPS")`, `setTrustStoreType("BCFKS")`, `setTrustStoreProvider("BCFIPS")`, `setProvider("BCJSSE")`, `setIncludeProtocols("TLSv1.2","TLSv1.3")`, and an explicit `setIncludeCipherSuites(...)` allow-list. Jetty's default store type is `PKCS12`.
- `web/grizzly/.../GrizzlyInitializingBean.java:640-665`: `SSLContextConfigurator` defaults to **JKS** key and trust stores. Call `setKeyStoreType("BCFKS")`, `setTrustStoreType("BCFKS")`, `setKeyStoreProvider("BCFIPS")`, `setTrustStoreProvider("BCFIPS")`, `setSecurityProtocol("TLS")`, and restrict protocols and suites on `SSLEngineConfigurator`. `ClientAuthProxyFilter.java:160` uses `CertificateFactory.getInstance("X.509")`: fine.
- `web/grizzly/.../SSLInfo.java` and the Jetty SSL info type: add fields for store type, provider names, protocols, and cipher suites so configuration stays caller-owned and visible. Defaults must preserve current behavior.
- `mongodb/utility/.../MongoClientSettingsFactoryBean.java:339`: `SSLContext.getInstance("TLS")` -> pass `"BCJSSE"`.
- `nutsnbolts/.../email/Protocol.java:75`: sets `mail.<selector>.starttls.enable`. Jakarta Mail builds its own default socket factory. Set `mail.<selector>.ssl.socketFactory` (and `ssl.protocols`/`ssl.ciphersuites`) to a BCJSSE-backed factory.
- `quorum/.../ldap/LdapContextCreator.java:133`: `Context.SECURITY_PROTOCOL = "ssl"`. JNDI uses the **default** `SSLSocketFactory`. Set `java.naming.ldap.factory.socket` to a class returning a BCJSSE factory, or `SSLContext.setDefault(...)`. The `unboundid-ldapsdk` dependency (`nutsnbolts/pom.xml:19`) builds its own `SSLUtil`; same remedy.
- `phalanx` (RabbitMQ `amqp-client`; Kafka in `phalanx`, `kafka/utility`, `bayeux/oumuamua/server/spi`): this repo does not configure TLS, so the caller must. Kafka: `ssl.keystore.type=BCFKS`, `ssl.truststore.type=BCFKS`, `ssl.provider=BCJSSE`, `ssl.enabled.protocols`, `ssl.cipher.suites`; also check the SASL mechanism. RabbitMQ: `ConnectionFactory.useSslProtocol(SSLContext)` with a BCJSSE context. Document, do not hard-wire.
- Oumuamua (`OumuamuaConfiguration`): no crypto settings, no change. TLS terminates in the hosting container (Jetty/Grizzly above).

### 3.10 Wire-level consequences (functionality lost)

In approved mode via BCJSSE:
- Disabled: SSLv3, TLS 1.0, TLS 1.1; suites using RC4, DES/3DES, MD5, export ciphers, anonymous or static-DH key exchange; non-approved curves **(verify)** (X25519 depends on version).
- ChaCha20-Poly1305 suites are not approved and drop out.
- Practical effect: legacy clients, brokers, MongoDB, or LDAP servers offering only TLS 1.0/1.1 can no longer connect.

### 3.11 Authentication and other mechanisms

- `mongodb/utility/.../MongoCredentialFactoryBean.java:118`: `MongoCredential.createScramSha1Credential(...)`. MongoDB's SCRAM-SHA-1 pre-hashes the password with **MD5** inside the driver, so authentication **fails under BC-FIPS**. Switch to `createScramSha256Credential` (server 4.0+, user created with SCRAM-SHA-256), or X.509/Kerberos. **Functional loss** for servers older than 4.0 or users with SCRAM-SHA-1 only. Javadoc lines 50/60 and `MongoCredentialFactoryBeanTest` reference SCRAM-SHA-1.
- `liquibase` (5.0.1): confirm it does not call MD5 for changeset checksums on your path. **(verify)** If it does, schema migration fails in approved mode.
- `nutsnbolts/.../namespace/shiro/realm/ActiveDirectoryLdapRealm.java`: Shiro is used for LDAP only. `shiro-crypto-hash` is on the classpath but no code here calls it. If a deployer configures Shiro hashing with MD5/SHA-1/bcrypt, it fails. Document.
- `UUIDVersion.NAMESPACE_MD5(3)`/`NAMESPACE_SHA1(5)`: enum constants only; no digest computed here. A caller using `UUID.nameUUIDFromBytes` (JDK, MD5) would hit BCFIPS first and fail. Document.
- `spark/singularity/boot`, `lang/*ClassLoader*`, `SandboxSecurityPolicy`, `MinimalPermissions`, `AnnotationLiteral`, `ClassGate` variants, `FormattedNoSuchAlgorithmException`, `persistence/.../CacheAsAspect`: use `java.security` for permissions/code sources or match only on name; no crypto, no change.

### 3.12 Ansible vault (`ansible` module)

`VaultTumbler`/`VaultCodec` implement Ansible Vault 1.1/1.2 `AES256`: PBKDF2-HMAC-SHA256 (10 000 iterations), AES-256-CTR, HMAC-SHA256. All three primitives are approved, so the format **can** remain compliant. But:

- `VaultTumbler.java:73` uses `AES/CTR/PKCS7Padding`, which relies on the non-FIPS `BC` provider (the tests register it by hand). CTR is a stream mode and BC-FIPS does not offer padding with it. **(verify)** Use `AES/CTR/NoPadding` with explicit PKCS7 padding of the plaintext before encryption and stripping after decryption (this is how Ansible does it, so file compatibility is preserved).
- A vault password shorter than 14 bytes is rejected by PBKDF2 in approved mode (3.6). **Functional loss:** short vault passwords stop working.
- 10 000 iterations is fixed by the Ansible format. Above the SP 800-132 floor.
- The IV is derived from PBKDF2 output, as the format specifies. An auditor may flag it as a legacy-format caveat; it is safe here because the salt is random per file.

### 3.13 JWT (`web/jwt`)

- `JWTEncryptionAlgorithm`: `HS256` (approved, subject to the key floor), `RS256` (approved, RSA >= 2048), `EDDSA` (version-dependent, 3.3). No other variants are defined, so nothing else is lost.
- `AsymmetricJWTKeyMaster`: key-type switch on `"RSA"` and `"EdDSA","Ed25519"`: same EdDSA dependency.
- `jose4j` 0.9.6 (`JWTConsumer`, `JsonWebSignature`, `JsonWebEncryption`) resolves algorithms through JCA by name and supports non-approved ones (`RSA1_5`, `PBES2`, `none`). In approved mode unapproved operations throw at use time. Add an explicit `AlgorithmConstraints` allow-list on every consumer so they are rejected up front with a clear error.

---

## 4. Summary of functionality lost or restricted

| Capability | Where | Outcome |
|---|---|---|
| MD5 hashing | `HashAlgorithm.MD5` | Lost (`NoSuchAlgorithmException`) |
| HMAC-MD5 | `HMACSigningAlgorithm.HMAC_MD5` | Lost |
| SHA-1 digest / HMAC-SHA-1 | `HashAlgorithm.SHA_1`, `HMAC_SHA_1`, `BloomFilter` | Discouraged; not for signatures; BloomFilter needs a replacement |
| DES | `SymmetricAlgorithm.DES` | Lost |
| 3DES | `SymmetricAlgorithm.TRIPLE_DES` | Effectively lost for encryption **(verify)** |
| RC4 | `SymmetricAlgorithm.RC4` | Lost |
| Blowfish | `SymmetricAlgorithm.BLOWFISH` | Lost |
| Argon2id password hashing | `Argon2IDPasswordEngine` | Lost; stored hashes need migration |
| PBKDF2-HMAC-SHA1 | `PBKDF2PasswordEngine` | Replace with SHA-256; stored hashes need migration |
| Passwords shorter than 14 bytes | PBKDF2 engine, `KeyStoreUtility` default, Ansible vault | Rejected in approved mode **(verify)** |
| HMAC keys shorter than 112 bits | `HMACSigningAlgorithm.generateKey` | Rejected |
| `SHA1PRNG` | both password engines | Lost; use the DRBG |
| EdDSA / Ed25519 | `EdDSASigningAlgorithm`, JWT `EDDSA`, OpenSSH keys | Lost on the 140-2 line; may be available on 2.x **(verify)** |
| JKS keystores | `KeyStoreUtility`, Grizzly defaults, `ClassVerifier` | Lost; move to BCFKS |
| JDK PKCS12 keystores | `TrustManagerUtility` default type | Replace with BCFKS |
| OpenSSH key import/export | `AsymmetricKeySpec.OPENSSH` | Needs rewrite or removal **(verify)** |
| MongoDB SCRAM-SHA-1 | `MongoCredentialFactoryBean` | Lost (driver uses MD5); move to SCRAM-SHA-256 |
| TLS 1.0/1.1, SSLv3, RC4/3DES/MD5 suites, ChaCha20 | all TLS endpoints | Lost; legacy peers cannot connect |
| Trust-all TLS | `Naive*`, jersey `ClientUtility`, `web/http` `HttpClient` | Not an algorithm loss, but non-compliant; exclude from FIPS builds |
| Fixed-IV AES-CBC | `json/Encryption` | Works, but non-compliant practice; fixing it breaks stored data |
| JAR-signature verification | `ClassVerifier`, JDK class loading | Cannot move to BC-FIPS; remains a JDK-crypto gap |

---

## 5. Documentation to update

Root chapters with affected content: `NUTSNBOLTS.adoc` (lines ~48-49, 73, 225, 299-300, 368-369, 610, 1654, 1703), `ANSIBLE.adoc`, `WEB.adoc`, `TESTBENCH.adoc`, `README.adoc`, plus `JIGSAW.md`. Per the chapter style, describe what users configure and observe (artifact names, provider order, approved-only flag, keystore type, failure messages), not internals.

Add a "Running under FIPS" section (in `NUTSNBOLTS.adoc` or a new chapter) covering artifact selection, library-versus-application dependency guidance, provider order, approved-only mode, keystore conversion, and the lost-functionality table above.

---

## 6. Tests

- Register `BouncyCastleFipsProvider` (and BCJSSE where TLS is under test) instead of `BouncyCastleProvider` in:
  - `nutsnbolts/src/test/.../security/CertificateUtilityTest.java:45,64-67`
  - `nutsnbolts/src/test/.../ssl/TrustManagerUtilityTest.java:50,86-89`
  - `ansible/src/test/.../VaultCodecTest.java:39,62-63` and `VaultTumblerTest.java:37,56-57`
- Tests asserting behavior that goes away: `EncryptionUtilityTest` (MD5, lines 67, 147), `Argon2IDPasswordEngineTest`, `PBKDF2PasswordEngineTest`, `AsymmetricSigningAlgorithmTest` and the `web/jwt` tests (EdDSA, ECDSA names), `MongoCredentialFactoryBeanTest` (SCRAM-SHA-1), `KeyStoreUtilityTest` (JKS, line 96), `ClassVerifierTest` (JKS, line 48), `GrizzlyInitializingBeanSSLBootTest` (JKS at 171), `JettyServerBootScenariosTest` (JKS at 220), `JettySupportTypesTest`.
- Run these suites under two configurations: the current default JVM, and a `fips` Maven profile that swaps the dependencies, sets `-Dorg.bouncycastle.fips.approved_only=true`, and supplies the `java.security` override. Tests for removed algorithms (Argon2, MD5, DES, ...) are skipped in the FIPS profile or live in a separate non-FIPS artifact.
- Test keystores and cert fixtures under `src/test/resources` must be regenerated as BCFKS.
- Docker-gated `AbstractGroundwaterTest` suites are unrelated to FIPS and keep failing without a Docker daemon.

---

## 7. Suggested sequence

1. Pick the `bc-fips` line and resolve the **(verify)** items in a throwaway module compiled against the jars: EdDSA support, OpenSSH utilities, PEM and ASN.1 classes, module names, PKCS12 under BCFIPS, the PBKDF2 and HMAC key floors, `AES/CTR` padding.
2. Add the `fips` Maven profile and the enforcer ban; get the build compiling with FIPS jars (resolves the OPENSSH, Argon2, and `CertificateUtility` compile errors).
3. Provider registration: `SecurityProvider` entries, ordered insertion, startup assertion.
4. Replace hard-coded provider and algorithm names (2.2, 3.5, 3.8).
5. TLS wiring in Jetty, Grizzly, Mongo, `HttpClient`, jersey, mail, LDAP, with explicit store types, providers, protocols, and suites.
6. Migrations: PBKDF2-SHA256 (retire Argon2 from the FIPS artifact), the `BinaryData` IV decision, MongoDB SCRAM-SHA-256.
7. Tests under both configurations; docs.

Keep the repo's own rules in view: FIPS support is an **opt-in assembly** (BC-FIPS stays `optional`, no auto-registration, no hidden static provider insertion), callers own provider order and approved-only mode, and public APIs stay in place unless removal is the only honest option. Every removal above can instead be a deprecation that fails at runtime with a clear message under FIPS.
