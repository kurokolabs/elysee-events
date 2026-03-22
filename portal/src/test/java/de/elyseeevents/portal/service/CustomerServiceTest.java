package de.elyseeevents.portal.service;

import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.CustomerRepository;
import de.elyseeevents.portal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createWithAccountCreatesUserAndCustomer() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        Customer customer = new Customer();
        customer.setFirstName("Max");
        customer.setLastName("Mustermann");

        CustomerService.CreateResult result = customerService.createWithAccount(customer, "max@test.de");

        assertNotNull(result.customer());
        assertNotNull(result.temporaryPassword());
        assertEquals(1L, result.customer().getId());
        assertEquals(1L, result.customer().getUserId());

        verify(userRepository).save(any(User.class));
        verify(customerRepository).save(any(Customer.class));
        verify(passwordEncoder).encode(anyString());
    }

    @Test
    void generatedPasswordHas12Chars() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer customer = new Customer();
        CustomerService.CreateResult result = customerService.createWithAccount(customer, "test@test.de");

        assertEquals(12, result.temporaryPassword().length());
    }

    @Test
    void generatedPasswordsAreUnique() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId((long) (Math.random() * 100000));
            return u;
        });
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            Customer customer = new Customer();
            CustomerService.CreateResult result = customerService.createWithAccount(customer, "user" + i + "@test.de");
            passwords.add(result.temporaryPassword());
        }

        // With SecureRandom generating 12-char passwords from a 57-char alphabet,
        // 20 passwords should all be unique (collision probability is negligible)
        assertEquals(20, passwords.size(), "All 20 generated passwords should be unique");
    }
}
