import io.virtualsms.sdk.VirtualSmsClient;
import io.virtualsms.sdk.model.Rentals;

/**
 * Rental flow: check availability, create a Full Access rental (local SIM
 * inventory, any service), extend it, then cancel for a full refund while
 * still inside the 20-minute / pre-first-SMS refund window.
 */
public class RentalFlowExample {

    public static void main(String[] args) {
        String apiKey = System.getenv("VIRTUALSMS_API_KEY");
        VirtualSmsClient client = new VirtualSmsClient(apiKey);

        // 1. Check availability + pricing for a country.
        VirtualSmsClient.RentalsAvailableParams availParams = new VirtualSmsClient.RentalsAvailableParams();
        availParams.country = "GB";
        availParams.tier = "full_access";
        Rentals.RentalAvailabilityResult availability = client.rentalsAvailable(availParams);
        System.out.println("Available countries: " + availability.totalAvailable);

        // 2. Create a Full Access rental (24h, any service).
        VirtualSmsClient.CreateRentalParams createParams = new VirtualSmsClient.CreateRentalParams();
        createParams.tier = "full_access";
        createParams.country = "GB";
        createParams.durationHours = 24;
        Rentals.CreateRentalResult rental = client.createRental(createParams);
        System.out.println("Rented " + rental.phoneNumber + " (rental " + rental.rentalId + ")");

        // 3. Extend it by another 24h.
        Rentals.RentalActionResult extended = client.extendRental(rental.rentalId, 24);
        System.out.println("Extended, new expiry: " + extended.newExpiresAt);

        // 4. Cancel for a full refund (only works within 20 min, before first SMS).
        Rentals.RentalActionResult cancelled = client.cancelRental(rental.rentalId);
        System.out.println("Cancelled, refund=" + cancelled.refund);
    }
}
