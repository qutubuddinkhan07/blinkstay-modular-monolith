package blinkstay.payment.repository;

import blinkstay.payment.entities.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {
    boolean existsByProviderAndProviderEventId(String provider, String providerEventId);
}
