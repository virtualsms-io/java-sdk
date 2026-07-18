import io.virtualsms.sdk.VirtualSmsClient;
import io.virtualsms.sdk.model.Common;
import io.virtualsms.sdk.model.Orders;

/**
 * Basic SMS verification flow: find a service, check price + stock, buy a
 * number, wait for the code, cancel if you no longer need it.
 * <p>
 * Run: {@code javac -cp sdk-2.0.0.jar ActivationFlowExample.java && java -cp .:sdk-2.0.0.jar:jackson-databind.jar ActivationFlowExample}
 */
public class ActivationFlowExample {

    public static void main(String[] args) {
        String apiKey = System.getenv("VIRTUALSMS_API_KEY");
        VirtualSmsClient client = new VirtualSmsClient(apiKey);

        // 1. Find the service code for "Telegram".
        Orders.SearchServicesResult search = client.searchServices("telegram");
        if (search.matches.isEmpty()) {
            System.out.println("No service match: " + search.message);
            return;
        }
        String service = search.matches.get(0).code;

        // 2. Check price + real stock before buying.
        Common.Price price = client.getPrice(service, "GB");
        if (!price.available) {
            System.out.println("Not in stock right now. Try findCheapest to see alternatives.");
            Orders.FindCheapestResult cheapest = client.findCheapest(service, 5);
            cheapest.cheapestOptions.forEach(o ->
                    System.out.printf("  %s (%s): $%.2f%n", o.countryName, o.country, o.priceUsd));
            return;
        }
        System.out.printf("Price: $%.2f%n", price.priceUsd);

        // 3. Buy the number.
        Orders.Order order = client.createOrder(service, "GB");
        System.out.println("Bought " + order.phoneNumber + " (order " + order.orderId + ")");

        // 4. Block until the code arrives (default 300s timeout).
        Orders.WaitForSmsResult result = client.waitForSms(order.orderId);
        if (result.success) {
            System.out.println("Code: " + result.code);
        } else {
            System.out.println("No code yet: " + result.message);
            // Refund if you no longer need it (only works inside the cancel cooldown window rules).
            Orders.CancelResult cancel = client.cancelOrder(order.orderId);
            System.out.println("Cancelled, refunded=" + cancel.refunded);
        }
    }
}
