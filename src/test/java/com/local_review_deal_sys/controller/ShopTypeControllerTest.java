package com.local_review_deal_sys.controller;

import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.ShopType;
import com.local_review_deal_sys.service.IShopTypeService;
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

/**
 * Unit tests for ShopTypeController
 */
@ExtendWith(MockitoExtension.class)
class ShopTypeControllerTest {

    @InjectMocks
    private ShopTypeController shopTypeController;

    @Mock
    private IShopTypeService typeService;

    private List<ShopType> mockShopTypeList;
    private Result expectedResult;

    @BeforeEach
    void setUp() {
        ShopType type1 = new ShopType();
        type1.setId(1L);
        type1.setName("Food");

        ShopType type2 = new ShopType();
        type2.setId(2L);
        type2.setName("Entertainment");

        mockShopTypeList = Arrays.asList(type1, type2);
        expectedResult = Result.ok(mockShopTypeList);
    }

    @Test
    void queryTypeList_ShouldReturnResultFromService() {
        // Given
        when(typeService.queryList()).thenReturn(expectedResult);

        // When
        Result actual = shopTypeController.queryTypeList();

        // Then
        assertNotNull(actual);
        assertTrue(actual.getSuccess());
        assertEquals(expectedResult, actual);

        // Verify that the service was called exactly once
        verify(typeService, times(1)).queryList();
    }

    @Test
    void queryTypeList_ShouldReturnEmptyResult_WhenServiceReturnsEmptyList() {
        // Given
        Result emptyResult = Result.ok(Collections.emptyList());

        when(typeService.queryList()).thenReturn(emptyResult);

        // When
        Result actual = shopTypeController.queryTypeList();

        // Then
        assertNotNull(actual);
        assertTrue(actual.getSuccess());
        List<?> data = (List<?>) actual.getData();
        assertTrue(data.isEmpty());

        verify(typeService, times(1)).queryList();
    }
}