package com.campus.lostfound.service;

import com.campus.lostfound.dto.*;
import com.campus.lostfound.entity.*;
import com.campus.lostfound.exception.ForbiddenException;
import com.campus.lostfound.exception.IllegalStateTransitionException;
import com.campus.lostfound.exception.ResourceNotFoundException;
import com.campus.lostfound.repository.ItemImageRepository;
import com.campus.lostfound.repository.ItemRepository;
import com.campus.lostfound.repository.UserRepository;
import com.campus.lostfound.util.ItemSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class ItemService {

    private static final Set<Category> HIGH_VALUE_CATEGORIES = Set.of(
            Category.PHONE, Category.WALLET, Category.LAPTOP, Category.KEYS
    );

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/jpg"
    );

    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final UserRepository userRepository;
    private final StateTransitionService stateTransitionService;
    private final MatchingService matchingService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public ItemService(ItemRepository itemRepository,
                       ItemImageRepository itemImageRepository,
                       UserRepository userRepository,
                       StateTransitionService stateTransitionService,
                       MatchingService matchingService,
                       NotificationService notificationService,
                       AuditService auditService) {
        this.itemRepository = itemRepository;
        this.itemImageRepository = itemImageRepository;
        this.userRepository = userRepository;
        this.stateTransitionService = stateTransitionService;
        this.matchingService = matchingService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    public ItemPublicDto createLostItem(ItemCreateDto dto, MultipartFile photo, User user) {
        validateCommon(dto);

        Item item = Item.builder()
                .type(ItemType.LOST)
                .category(dto.getCategory())
                .title(dto.getTitle().trim())
                .description(dto.getDescription().trim())
                .color(dto.getColor() != null ? dto.getColor().trim() : null)
                .brand(dto.getBrand() != null ? dto.getBrand().trim() : null)
                .location(dto.getLocation().trim())
                .eventDate(dto.getEventDate())
                .status(ItemStatus.OPEN)
                .createdBy(user)
                .build();

        Item saved = itemRepository.save(item);
        handlePhotoUpload(saved, photo);

        auditService.log("ITEM", saved.getId(), "CREATE_LOST", user.getId(), "Reported lost item: " + saved.getTitle());

        // Run matching engine asynchronously/synchronously
        matchingService.runMatching(saved);

        return toPublicDto(saved);
    }

    public ItemStaffDto createFoundItem(ItemCreateDto dto, MultipartFile photo, User staff) {
        validateCommon(dto);

        if (dto.getStoredAt() == null || dto.getStoredAt().isBlank()) {
            throw new IllegalArgumentException("storedAt is required for found items");
        }

        if (HIGH_VALUE_CATEGORIES.contains(dto.getCategory())) {
            if (dto.getHiddenDetail() == null || dto.getHiddenDetail().isBlank()) {
                throw new IllegalArgumentException("hiddenDetail is required for high-value category: " + dto.getCategory());
            }
        }

        Item item = Item.builder()
                .type(ItemType.FOUND)
                .category(dto.getCategory())
                .title(dto.getTitle().trim())
                .description(dto.getDescription().trim())
                .color(dto.getColor() != null ? dto.getColor().trim() : null)
                .brand(dto.getBrand() != null ? dto.getBrand().trim() : null)
                .location(dto.getLocation().trim())
                .eventDate(dto.getEventDate())
                .status(ItemStatus.LISTED)
                .storedAt(dto.getStoredAt().trim())
                .hiddenDetail(dto.getHiddenDetail() != null ? dto.getHiddenDetail().trim() : null)
                .ownerRollNumber(dto.getOwnerRollNumber() != null && !dto.getOwnerRollNumber().isBlank()
                        ? dto.getOwnerRollNumber().trim().toUpperCase() : null)
                .createdBy(staff)
                .build();

        Item saved = itemRepository.save(item);
        handlePhotoUpload(saved, photo);

        auditService.log("ITEM", saved.getId(), "CREATE_FOUND", staff.getId(), "Logged found item: " + saved.getTitle());

        // Special handling for ID cards with roll number
        if (saved.getCategory() == Category.ID_CARD && saved.getOwnerRollNumber() != null) {
            Optional<User> ownerOpt = userRepository.findByRollNumberIgnoreCase(saved.getOwnerRollNumber());
            ownerOpt.ifPresent(owner -> {
                String msg = String.format("Your ID card was found, collect from %s", saved.getStoredAt());
                notificationService.createNotification(owner, msg, "ID_CARD_FOUND", saved.getId());
            });
        }

        // Run matching engine
        matchingService.runMatching(saved);

        return toStaffDto(saved);
    }

    private void validateCommon(ItemCreateDto dto) {
        if (dto.getEventDate() != null && dto.getEventDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Event date cannot be in the future");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().length() < 5) {
            throw new IllegalArgumentException("Description must be at least 5 characters");
        }
    }

    private void handlePhotoUpload(Item item, MultipartFile photo) {
        if (photo == null || photo.isEmpty()) return;

        if (photo.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Photo file size cannot exceed 5 MB");
        }

        String ct = photo.getContentType();
        if (ct == null || !ALLOWED_IMAGE_TYPES.contains(ct.toLowerCase())) {
            throw new IllegalArgumentException("Photo must be image/jpeg or image/png");
        }

        try {
            ItemImage image = ItemImage.builder()
                    .item(item)
                    .contentType(ct.toLowerCase())
                    .content(photo.getBytes())
                    .build();
            itemImageRepository.save(image);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store photo", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<?> searchItems(String q, Category category, String location, ItemStatus status,
                               ItemType type, LocalDate dateFrom, LocalDate dateTo,
                               Pageable pageable, User currentUser) {
        Specification<Item> spec = ItemSpecification.buildSpecification(
                q, category, location, status, type, dateFrom, dateTo, currentUser
        );

        Page<Item> page = itemRepository.findAll(spec, pageable);

        if (currentUser.getRole() == Role.STUDENT) {
            return page.map(this::toPublicDto);
        } else {
            return page.map(this::toStaffDto);
        }
    }

    @Transactional(readOnly = true)
    public Object getItemById(Long id, User currentUser) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        if (currentUser.getRole() == Role.STUDENT) {
            if (item.getType() == ItemType.LOST && !item.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new ResourceNotFoundException("Item not found with id: " + id);
            }
            return toPublicDto(item);
        } else {
            return toStaffDto(item);
        }
    }

    @Transactional(readOnly = true)
    public ItemImage getItemPhoto(Long id, User currentUser) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        if (currentUser.getRole() == Role.STUDENT) {
            if (item.getType() == ItemType.LOST && !item.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new ResourceNotFoundException("Item not found with id: " + id);
            }
        }

        return itemImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No photo available for item: " + id));
    }

    public ItemPublicDto editLostItem(Long id, ItemUpdateDto dto, User user) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        if (!item.getCreatedBy().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Cannot edit an item you did not report");
        }

        if (item.getType() != ItemType.LOST) {
            throw new IllegalArgumentException("Only lost items can be edited via this endpoint");
        }

        if (item.getStatus() != ItemStatus.OPEN && item.getStatus() != ItemStatus.MATCHED) {
            throw new IllegalStateTransitionException("Cannot edit item in status: " + item.getStatus());
        }

        if (dto.getEventDate() != null && dto.getEventDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Event date cannot be in the future");
        }

        item.setTitle(dto.getTitle().trim());
        item.setDescription(dto.getDescription().trim());
        item.setColor(dto.getColor() != null ? dto.getColor().trim() : null);
        item.setBrand(dto.getBrand() != null ? dto.getBrand().trim() : null);
        item.setLocation(dto.getLocation().trim());
        item.setEventDate(dto.getEventDate());

        Item saved = itemRepository.save(item);
        auditService.log("ITEM", saved.getId(), "EDIT_LOST", user.getId(), "Updated lost item details");

        matchingService.runMatching(saved);

        return toPublicDto(saved);
    }

    public ItemPublicDto cancelLostItem(Long id, User user) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        if (!item.getCreatedBy().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Cannot cancel an item you did not report");
        }

        stateTransitionService.transition(item, ItemStatus.CANCELLED, "Cancelled by user", user.getId());
        Item saved = itemRepository.save(item);

        return toPublicDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ItemPublicDto> getMyReports(User user) {
        return itemRepository.findByCreatedByOrderByCreatedAtDesc(user).stream()
                .map(this::toPublicDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ItemStaffDto> getInventory(Pageable pageable) {
        Specification<Item> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("type"), ItemType.FOUND),
                root.get("status").in(List.of(ItemStatus.LISTED, ItemStatus.CLAIM_PENDING))
        );
        return itemRepository.findAll(spec, pageable).map(this::toStaffDto);
    }

    public ItemStaffDto disposeItem(Long id, String actionStr, User staff) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        ItemStatus target;
        try {
            target = ItemStatus.valueOf(actionStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Action must be DONATED or DISPOSED");
        }

        if (target != ItemStatus.DONATED && target != ItemStatus.DISPOSED) {
            throw new IllegalArgumentException("Action must be DONATED or DISPOSED");
        }

        stateTransitionService.transition(item, target, "Disposal action by staff", staff.getId());
        Item saved = itemRepository.save(item);

        return toStaffDto(saved);
    }

    public ItemPublicDto toPublicDto(Item item) {
        boolean hasPhoto = itemImageRepository.existsById(item.getId());
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
                .hasPhoto(hasPhoto)
                .build();
    }

    public ItemStaffDto toStaffDto(Item item) {
        boolean hasPhoto = itemImageRepository.existsById(item.getId());
        return ItemStaffDto.builder()
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
                .hiddenDetail(item.getHiddenDetail())
                .storedAt(item.getStoredAt())
                .ownerRollNumber(item.getOwnerRollNumber())
                .createdById(item.getCreatedBy().getId())
                .createdByName(item.getCreatedBy().getName())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .expiryWarned(item.isExpiryWarned())
                .returnedAt(item.getReturnedAt())
                .mergedIntoId(item.getMergedIntoId())
                .version(item.getVersion())
                .hasPhoto(hasPhoto)
                .build();
    }
}
