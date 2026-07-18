package io.virtualsms.sdk;

import io.virtualsms.sdk.model.Common;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Minimum CI smoke test: get_balance + list_services + get_price succeed
 * against a real (throwaway-key) account. Skipped automatically unless
 * VIRTUALSMS_API_KEY is set in the environment — CI provides it as a repo
 * secret; local `mvn test` without the env var just skips this class.
 */
@EnabledIfEnvironmentVariable(named = "VIRTUALSMS_API_KEY", matches = ".+")
class SmokeTest {

    private final VirtualSmsClient client = new VirtualSmsClient(System.getenv("VIRTUALSMS_API_KEY"));

    @Test
    void getBalanceSucceeds() {
        Common.Balance balance = client.getBalance();
        assertNotNull(balance);
        assertTrue(balance.balanceUsd >= 0);
    }

    @Test
    void listServicesSucceeds() {
        List<Common.Service> services = client.listServices();
        assertNotNull(services);
        assertTrue(services.size() > 0, "expected at least one service in the catalog");
        assertNotNull(services.get(0).code);
    }

    @Test
    void getPriceSucceeds() {
        List<Common.Service> services = client.listServices();
        assertTrue(services.size() > 0);
        List<Common.Country> countries = client.listCountries();
        assertTrue(countries.size() > 0);

        Common.Price price = client.getPrice(services.get(0).code, countries.get(0).iso);
        assertNotNull(price);
        assertTrue(price.priceUsd >= 0);
    }
}
