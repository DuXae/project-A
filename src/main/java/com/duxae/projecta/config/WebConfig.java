package com.duxae.projecta.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Настройки слоя web.
 *
 * Поднимаем предел размера страницы: по умолчанию Spring Data режет size до 2000,
 * а странице списка товаров нужны все записи одним запросом (NFR-2).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    static final int MAX_PAGE_SIZE = 10_000;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver pageableResolver =
                new PageableHandlerMethodArgumentResolver();
        pageableResolver.setMaxPageSize(MAX_PAGE_SIZE);
        resolvers.add(pageableResolver);
    }
}
