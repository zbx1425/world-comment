package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.platform.neoforge.ServerPlatformImpl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpClient;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class RequestContext {

    public static HttpClient createHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        if (!ServerPlatformImpl.isProduction()) {
            builder.sslContext(InsecureAllowedTrustManager.CONTEXT);
        }
        return builder.build();
    }

    private static class InsecureAllowedTrustManager implements X509TrustManager {

        public static final SSLContext CONTEXT;

        static {
            try {
                CONTEXT = SSLContext.getInstance("TLS");
                CONTEXT.init(null, new TrustManager[]{ new InsecureAllowedTrustManager() }, new SecureRandom());
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
}
