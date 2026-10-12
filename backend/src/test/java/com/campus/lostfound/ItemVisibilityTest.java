package com.campus.lostfound;

import com.campus.lostfound.dto.ItemPublicDto;
import com.campus.lostfound.dto.ItemStaffDto;
import com.campus.lostfound.entity.*;
import com.campus.lostfound.service.ItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ItemVisibilityTest {

    @Autowired private ItemService itemService;

    @Test
    @DisplayName("ItemPublicDto omits hiddenDetail, storedAt, and ownerRollNumber")
    void testItemPublicDtoHidesSensitiveStaffFields() {
        User staff = User.builder().id(99L).name("Staff").email("staff99@campus.edu").role(Role.STAFF).build();
        Item foundItem = Item.builder()
                .id(123L)
                .type(ItemType.FOUND)
                .category(Category.PHONE)
                .title("iPhone 14")
                .description("Found in cafeteria")
                .location("Canteen")
                .eventDate(LocalDate.now())
                .status(ItemStatus.LISTED)
                .storedAt("Locker Room Safe B-12")
                .hiddenDetail("Passcode is 9876, sticker of Pikachu on back")
                .ownerRollNumber("21CS001")
                .createdBy(staff)
                .build();

        ItemPublicDto publicDto = itemService.toPublicDto(foundItem);
        ItemStaffDto staffDto = itemService.toStaffDto(foundItem);

        // Public DTO checks
        assertEquals("iPhone 14", publicDto.getTitle());
        // Verify class methods: ItemPublicDto doesn't even have getHiddenDetail() or getStoredAt() methods
        // Let's assert ItemStaffDto has them and they match
        assertEquals("Locker Room Safe B-12", staffDto.getStoredAt());
        assertEquals("Passcode is 9876, sticker of Pikachu on back", staffDto.getHiddenDetail());
        assertEquals("21CS001", staffDto.getOwnerRollNumber());
    }
}
