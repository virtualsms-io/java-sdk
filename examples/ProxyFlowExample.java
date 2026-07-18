import io.virtualsms.sdk.VirtualSmsClient;
import io.virtualsms.sdk.internal.ProxyEndpointBuilder;
import io.virtualsms.sdk.model.Proxies;

import java.util.List;

/**
 * Proxy flow: browse the catalog, buy traffic, generate a connection string,
 * rotate the exit IP.
 */
public class ProxyFlowExample {

    public static void main(String[] args) {
        String apiKey = System.getenv("VIRTUALSMS_API_KEY");
        VirtualSmsClient client = new VirtualSmsClient(apiKey);

        // 1. Browse the catalog (no auth required).
        List<Proxies.ProxyCatalogPoolType> catalog = client.listProxyCatalog();
        System.out.println("Pool types available: " + catalog.size());

        // 2. Buy 1 GB of residential traffic.
        VirtualSmsClient.BuyProxyParams buyParams = new VirtualSmsClient.BuyProxyParams();
        buyParams.poolType = "residential";
        buyParams.gb = 1.0;
        buyParams.countryCode = "GB";
        Proxies.ProxyPurchaseResult purchase = client.buyProxy(buyParams);
        System.out.println("Bought proxy " + purchase.proxyId);

        // 3. Compose a ready-to-use connection string (pure client-side, no extra call).
        ProxyEndpointBuilder.Params endpointParams = new ProxyEndpointBuilder.Params();
        endpointParams.countryCode = "GB";
        endpointParams.protocol = "HTTP";
        endpointParams.format = "host:port:user:pass";
        Proxies.ProxyEndpointResult endpoint = client.generateProxyEndpoint(purchase.proxyId, endpointParams);
        System.out.println("Connection string: " + endpoint.endpoints.get(0));

        // 4. Rotate to a fresh exit IP.
        Proxies.ProxyRotateResult rotated = client.rotateProxy(purchase.proxyId, null);
        System.out.println("Rotated: " + rotated.message);
    }
}
