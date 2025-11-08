package com.local_review_deal_sys.controller;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.Shop;
import com.local_review_deal_sys.service.IShopService;
import com.local_review_deal_sys.utils.SystemConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopControllerTest {

    @InjectMocks
    private ShopController shopController;

    @Mock
    private IShopService shopService;

    private Shop sampleShop;

    @BeforeEach
    void setUp() {
        sampleShop = new Shop();
        sampleShop.setId(1L);
        sampleShop.setName("Test Shop");
    }

    // ------------------- queryShopById -------------------

    @Test
    void queryShopById_ShouldReturnShopResult() {
        // Given
        Result expected = Result.ok(sampleShop);
        when(shopService.queryById(1L)).thenReturn(expected);

        // When
        Result result = shopController.queryShopById(1L);

        // Then
        assertEquals(expected, result);
        verify(shopService, times(1)).queryById(1L);
    }

    // ------------------- saveShop -------------------

    @Test
    void saveShop_ShouldReturnShopId_WhenSaved() {
        // Given
        when(shopService.save(any(Shop.class))).thenReturn(true);

        // When
        Result result = shopController.saveShop(sampleShop);

        // Then
        assertTrue(result.getSuccess());
        assertEquals(sampleShop.getId(), result.getData());
        verify(shopService, times(1)).save(sampleShop);
    }

    // ------------------- updateShop -------------------

    @Test
    void updateShop_ShouldCallServiceUpdate() {
        // Given
        Result expected = Result.ok();
        when(shopService.update(sampleShop)).thenReturn(expected);

        // When
        Result result = shopController.updateShop(sampleShop);

        // Then
        assertEquals(expected, result);
        verify(shopService, times(1)).update(sampleShop);
    }

    // ------------------- queryShopByType -------------------

    @Test
    void queryShopByType_ShouldReturnPagedResult() {
        // Given
        Result expected = Result.ok(Collections.singletonList(sampleShop));
        when(shopService.queryShopByType(1, 1, null, null)).thenReturn(expected);

        // When
        Result result = shopController.queryShopByType(1, 1, null, null);

        // Then
        assertEquals(expected, result);
        verify(shopService, times(1)).queryShopByType(1, 1, null, null);
    }
}