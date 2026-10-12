package com.campus.lostfound.config;

import com.campus.lostfound.entity.*;
import com.campus.lostfound.repository.ItemRepository;
import com.campus.lostfound.repository.UserRepository;
import com.campus.lostfound.service.ItemService;
import com.campus.lostfound.service.MatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final MatchingService matchingService;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      ItemRepository itemRepository,
                      MatchingService matchingService,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.matchingService = matchingService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains users. Skipping initial data seeding.");
            return;
        }

        log.info("Seeding initial users and items...");

        // 1. Admin
        User admin = User.builder()
                .name("Campus Administrator")
                .email("admin@campus.edu")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(admin);

        // 2. Staff
        User staff = User.builder()
                .name("Security & Helpdesk Staff")
                .email("staff@campus.edu")
                .passwordHash(passwordEncoder.encode("Staff@123"))
                .role(Role.STAFF)
                .active(true)
                .build();
        userRepository.save(staff);

        // 3. Student
        User student = User.builder()
                .name("Akash Patel")
                .email("student@campus.edu")
                .rollNumber("21CS001")
                .passwordHash(passwordEncoder.encode("Student@123"))
                .role(Role.STUDENT)
                .active(true)
                .build();
        userRepository.save(student);

        // 4. Sample items
        // Lost item reported by student
        Item lostPhone = Item.builder()
                .type(ItemType.LOST)
                .category(Category.PHONE)
                .title("Black iPhone 13 in matte case")
                .description("Lost my black iPhone 13 with a tiny crack near the top-left bezel. Blue silicone cover.")
                .color("Black")
                .brand("Apple")
                .location("Library")
                .eventDate(LocalDate.now().minusDays(1))
                .status(ItemStatus.OPEN)
                .createdBy(student)
                .build();
        itemRepository.save(lostPhone);

        // Found item reported by staff (potential match for the phone!)
        Item foundPhone = Item.builder()
                .type(ItemType.FOUND)
                .category(Category.PHONE)
                .title("iPhone 13 Apple smartphone")
                .description("Black iPhone 13 found near Library 2nd floor reading hall with blue casing.")
                .color("Black")
                .brand("Apple")
                .location("Library")
                .eventDate(LocalDate.now().minusDays(1))
                .status(ItemStatus.LISTED)
                .storedAt("Security Desk - Block A")
                .hiddenDetail("Lock screen has a golden retriever wallpaper")
                .createdBy(staff)
                .build();
        itemRepository.save(foundPhone);

        // Found ID card matching student's roll number
        Item foundIdCard = Item.builder()
                .type(ItemType.FOUND)
                .category(Category.ID_CARD)
                .title("College Student ID Card")
                .description("Student identity card found on cafeteria bench near juice counter.")
                .color("White")
                .location("Canteen")
                .eventDate(LocalDate.now())
                .status(ItemStatus.LISTED)
                .storedAt("Admin Block Office Room 102")
                .ownerRollNumber("21CS001")
                .createdBy(staff)
                .build();
        itemRepository.save(foundIdCard);

        // Another found item (Water bottle)
        Item foundBottle = Item.builder()
                .type(ItemType.FOUND)
                .category(Category.BOTTLE)
                .title("HydroFlask Stainless Steel Bottle")
                .description("Silver insulated bottle with mountain decals.")
                .color("Silver")
                .brand("Hydro Flask")
                .location("Sports Ground")
                .eventDate(LocalDate.now().minusDays(2))
                .status(ItemStatus.LISTED)
                .storedAt("Sports Complex Lost & Found Shelf")
                .createdBy(staff)
                .build();
        itemRepository.save(foundBottle);

        // Run matching on the items
        matchingService.runMatching(lostPhone);
        matchingService.runMatching(foundPhone);

        log.info("Initial data seeding completed successfully! Seeded Admin, Staff, Student, and sample items.");
    }
}
