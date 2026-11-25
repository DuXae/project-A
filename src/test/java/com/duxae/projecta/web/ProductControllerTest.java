package com.duxae.projecta.web;

import com.duxae.projecta.domain.Product;
import com.duxae.projecta.service.ProductService;
import com.duxae.projecta.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void saveBulkReturns201WithSavedCount() throws Exception {
        when(productService.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/products/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[
                                  {"name":"Гвоздь 50мм","quantity":100},
                                  {"name":"Шуруп 3x20","quantity":250}
                                ]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saved").value(2))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].name").value("Гвоздь 50мм"))
                .andExpect(jsonPath("$.items[1].quantity").value(250));
    }

    @Test
    void saveBulkTrimsWhitespaceInName() throws Exception {
        when(productService.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/v1/products/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"name":"  Молоток  ","quantity":1}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].name").value("Молоток"));
    }

    @Test
    void saveBulkRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/products/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"name":"   ","quantity":5}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0]").value(containsString("name")));
    }

    @Test
    void saveBulkRejectsZeroQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/products/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"name":"Товар","quantity":0}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0]").value(containsString("quantity")));
    }

    @Test
    void saveBulkRejectsNegativeQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/products/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"name":"Товар","quantity":-7}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void saveBulkRejectsEmptyItems() throws Exception {
        mockMvc.perform(post("/api/v1/products/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void saveBulkRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/products/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void listReturnsSavedProducts() throws Exception {
        when(productService.findAll(any()))
                .thenReturn(new PageImpl<>(
                        List.of(new Product("Гвоздь", 10)),
                        PageRequest.of(0, 20),
                        1
                ));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name").value("Гвоздь"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void indexPageIsServed() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void productsPageIsServed() throws Exception {
        mockMvc.perform(get("/products.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void sizeAboveDefaultLimitIsAccepted() throws Exception {
        when(productService.findAll(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10_000), 0));

        mockMvc.perform(get("/api/v1/products").param("size", "10000"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10_000);
    }

    /**
     * Записи, сохранённые одной пачкой, имеют одинаковый createdAt.
     * Без вторичной сортировки по id порядок в списке непредсказуем.
     */
    @Test
    void defaultSortIsStable() throws Exception {
        when(productService.findAll(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).findAll(captor.capture());

        Sort.Order createdAt = captor.getValue().getSort().getOrderFor("createdAt");
        Sort.Order id = captor.getValue().getSort().getOrderFor("id");

        assertThat(createdAt).isNotNull();
        assertThat(createdAt.getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(id).as("stable tiebreaker by id").isNotNull();
        assertThat(id.getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
