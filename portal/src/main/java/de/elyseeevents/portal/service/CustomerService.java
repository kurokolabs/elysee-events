package de.elyseeevents.portal.service;

import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.User;
import de.elyseeevents.portal.repository.CustomerRepository;
import de.elyseeevents.portal.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    public Optional<Customer> findByUserId(Long userId) {
        return customerRepository.findByUserId(userId);
    }

    public List<Customer> search(String query) {
        return customerRepository.search(query);
    }

    public record CreateResult(Customer customer, String temporaryPassword) {}

    @Transactional
    public CreateResult createWithAccount(Customer customer, String email) {
        String tempPassword = generateTempPassword();

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setRole("CUSTOMER");
        user.setActive(true);
        user.setForcePwChange(true);
        user.setTwoFaEnabled(true);
        user = userRepository.save(user);

        customer.setUserId(user.getId());
        customer = customerRepository.save(customer);

        return new CreateResult(customer, tempPassword);
    }

    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    public Customer update(Customer customer) {
        return customerRepository.save(customer);
    }

    public long count() {
        return customerRepository.count();
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
