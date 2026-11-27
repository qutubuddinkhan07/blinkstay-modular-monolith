package com.blinkstay.auth.serviceImpl;

import com.blinkstay.auth.entities.User;
import com.blinkstay.auth.enums.UserRole;
import com.blinkstay.common.seed.DemoGuest;
import com.blinkstay.common.seed.DemoGuestAccounts;
import com.blinkstay.common.seed.NewDemoGuest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Review-author accounts for the demo. Each gets a random password that is never stored or
 * printed, so nobody can log in as them; @example.com is reserved and never receives mail.
 */
@Service
@RequiredArgsConstructor
public class DemoGuestAccountsImpl implements DemoGuestAccounts {

    @PersistenceContext
    private EntityManager em;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public List<DemoGuest> ensureGuests(List<NewDemoGuest> guests) {
        List<DemoGuest> result = new ArrayList<>(guests.size());
        for (NewDemoGuest g : guests) {
            String email = g.email().trim().toLowerCase();
            User user = em.createQuery("select u from User u where lower(u.email) = :email", User.class)
                    .setParameter("email", email)
                    .getResultStream().findFirst()
                    .orElseGet(() -> create(g.username(), email));
            result.add(new DemoGuest(user.getId(), user.getUsername()));
        }
        return result;
    }

    private User create(String username, String email) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .roles(new HashSet<>(Set.of(UserRole.USER)))
                .isActive(true)
                .build();
        em.persist(user);
        return user;
    }
}