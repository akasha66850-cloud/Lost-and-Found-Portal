package com.campus.lostfound.service;

import com.campus.lostfound.dto.*;
import com.campus.lostfound.entity.*;
import com.campus.lostfound.exception.DuplicateResourceException;
import com.campus.lostfound.exception.ForbiddenException;
import com.campus.lostfound.exception.ResourceNotFoundException;
import com.campus.lostfound.repository.ItemRepository;
import com.campus.lostfound.repository.MatchResultRepository;
import com.campus.lostfound.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class AdminService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchingService matchingService;
    private final StateTransitionService stateTransitionService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public AdminService(ItemRepository itemRepository,
                        UserRepository userRepository,
                        MatchResultRepository matchResultRepository,
                        MatchingService matchingService,
                        StateTransitionService stateTransitionService,
                        PasswordEncoder passwordEncoder,
                        AuditService auditService) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.matchResultRepository = matchResultRepository;
        this.matchingService = matchingService;
        this.stateTransitionService = stateTransitionService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        Map<String, Long> statusCounts = new HashMap<>();
        for (ItemStatus status : ItemStatus.values()) {
            statusCounts.put(status.name(), itemRepository.countByStatus(status));
        }

        long totalLost = itemRepository.countByType(ItemType.LOST);
        long totalFound = itemRepository.countByType(ItemType.FOUND);
        long totalReturned = itemRepository.countByTypeAndStatus(ItemType.FOUND, ItemStatus.RETURNED);

        double recoveryRate = 0.0;
        if (totalFound > 0) {
            recoveryRate = Math.round(((double) totalReturned / totalFound * 100.0) * 10.0) / 10.0;
        }

        List<Object[]> topLocObjects = itemRepository.findTopLostLocations();
        List<AdminStatsDto.LocationCountDto> topLocations = new ArrayList<>();
        for (int i = 0; i < Math.min(5, topLocObjects.size()); i++) {
            Object[] row = topLocObjects.get(i);
            topLocations.add(AdminStatsDto.LocationCountDto.builder()
                    .location((String) row[0])
                    .count(((Number) row[1]).longValue())
                    .build());
        }

        List<Item> returnedItems = itemRepository.findByTypeAndStatus(ItemType.FOUND, ItemStatus.RETURNED);
        double avgDaysVal = 0.0;
        if (!returnedItems.isEmpty()) {
            double avg = returnedItems.stream()
                    .filter(item -> item.getReturnedAt() != null && item.getEventDate() != null)
                    .mapToLong(item -> ChronoUnit.DAYS.between(item.getEventDate(), item.getReturnedAt().toLocalDate()))
                    .average()
                    .orElse(0.0);
            avgDaysVal = Math.round(avg * 10.0) / 10.0;
        }

        return AdminStatsDto.builder()
                .statusCounts(statusCounts)
                .totalLost(totalLost)
                .totalFound(totalFound)
                .totalReturned(totalReturned)
                .recoveryRate(recoveryRate)
                .topLostLocations(topLocations)
                .averageDaysToReturn(avgDaysVal)
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserDto)
                .toList();
    }

    public UserDto createUser(UserCreateDto dto, User actor) {
        if (userRepository.existsByEmailIgnoreCase(dto.getEmail().trim())) {
            throw new DuplicateResourceException("Email already exists: " + dto.getEmail());
        }
        if (dto.getRollNumber() != null && !dto.getRollNumber().isBlank()) {
            if (userRepository.existsByRollNumberIgnoreCase(dto.getRollNumber().trim())) {
                throw new DuplicateResourceException("Roll number already exists: " + dto.getRollNumber());
            }
        }

        User user = User.builder()
                .name(dto.getName().trim())
                .email(dto.getEmail().trim().toLowerCase())
                .rollNumber(dto.getRollNumber() != null && !dto.getRollNumber().isBlank()
                        ? dto.getRollNumber().trim().toUpperCase() : null)
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .active(true)
                .build();

        User saved = userRepository.save(user);
        auditService.log("USER", saved.getId(), "ADMIN_CREATE_USER", actor.getId(), "Created user: " + saved.getEmail() + " with role: " + saved.getRole());
        return toUserDto(saved);
    }

    public UserDto updateUserActive(Long userId, boolean active, User actor) {
        if (userId.equals(actor.getId())) {
            throw new ForbiddenException("An administrator cannot deactivate their own account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setActive(active);
        User saved = userRepository.save(user);
        auditService.log("USER", saved.getId(), "UPDATE_ACTIVE", actor.getId(), "Set active=" + active);
        return toUserDto(saved);
    }

    public UserDto updateUserRole(Long userId, Role newRole, User actor) {
        if (userId.equals(actor.getId()) && newRole != Role.ADMIN) {
            throw new ForbiddenException("An administrator cannot demote their own account");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setRole(newRole);
        User saved = userRepository.save(user);
        auditService.log("USER", saved.getId(), "UPDATE_ROLE", actor.getId(), "Set role=" + newRole);
        return toUserDto(saved);
    }

    public MatchResponseDto createManualMatch(ManualMatchRequestDto dto, User actor) {
        Item lost = itemRepository.findById(dto.getLostId())
                .orElseThrow(() -> new ResourceNotFoundException("Lost item not found with id: " + dto.getLostId()));
        Item found = itemRepository.findById(dto.getFoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Found item not found with id: " + dto.getFoundId()));

        if (lost.getType() != ItemType.LOST || found.getType() != ItemType.FOUND) {
            throw new IllegalArgumentException("First item must be LOST and second item must be FOUND");
        }

        MatchResult match = matchingService.createManualMatch(lost, found, actor.getId());
        auditService.log("MATCH", match.getId(), "MANUAL_MATCH", actor.getId(), "Manual match between #" + lost.getId() + " and #" + found.getId());

        return toMatchDto(match);
    }

    public void mergeItems(ItemMergeRequestDto dto, User actor) {
        Item keep = itemRepository.findById(dto.getKeepId())
                .orElseThrow(() -> new ResourceNotFoundException("Keep item not found with id: " + dto.getKeepId()));
        Item remove = itemRepository.findById(dto.getRemoveId())
                .orElseThrow(() -> new ResourceNotFoundException("Remove item not found with id: " + dto.getRemoveId()));

        if (keep.getType() != remove.getType()) {
            throw new IllegalArgumentException("Cannot merge items of different types (LOST vs FOUND)");
        }
        if (keep.getId().equals(remove.getId())) {
            throw new IllegalArgumentException("Cannot merge an item with itself");
        }

        // Move matches from removed item to kept item
        if (remove.getType() == ItemType.LOST) {
            List<MatchResult> removeMatches = matchResultRepository.findByLostItemOrderByScoreDesc(remove);
            for (MatchResult m : removeMatches) {
                if (!matchResultRepository.existsByLostItemAndFoundItem(keep, m.getFoundItem())) {
                    m.setLostItem(keep);
                    matchResultRepository.save(m);
                } else {
                    matchResultRepository.delete(m);
                }
            }
        } else {
            List<MatchResult> removeMatches = matchResultRepository.findByFoundItemOrderByScoreDesc(remove);
            for (MatchResult m : removeMatches) {
                if (!matchResultRepository.existsByLostItemAndFoundItem(m.getLostItem(), keep)) {
                    m.setFoundItem(keep);
                    matchResultRepository.save(m);
                } else {
                    matchResultRepository.delete(m);
                }
            }
        }

        remove.setMergedIntoId(keep.getId());
        stateTransitionService.transition(remove, ItemStatus.CANCELLED, "Merged into item #" + keep.getId(), actor.getId());
        itemRepository.save(remove);

        auditService.log("ITEM", keep.getId(), "MERGE", actor.getId(), "Merged item #" + remove.getId() + " into #" + keep.getId());
    }

    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .rollNumber(user.getRollNumber())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private MatchResponseDto toMatchDto(MatchResult m) {
        return MatchResponseDto.builder()
                .id(m.getId())
                .lostItem(toPublicDto(m.getLostItem()))
                .foundItem(toPublicDto(m.getFoundItem()))
                .score(m.getScore())
                .status(m.getStatus())
                .source(m.getSource())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private ItemPublicDto toPublicDto(Item item) {
        return ItemPublicDto.builder()
                .id(item.getId())
                .type(item.getType())
                .category(item.getCategory())
                .title(item.getTitle())
                .description(item.getDescription())
                .color(item.getColor())
                .brand(item.getBrand())
                .location(item.getLocation())
                .eventDate(item.getEventDate())
                .status(item.getStatus())
                .createdById(item.getCreatedBy().getId())
                .createdByName(item.getCreatedBy().getName())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
