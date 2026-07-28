package br.com.systemcommerce.storecontext;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class StoreContextWebConfig implements WebMvcConfigurer {

    private final RequireStoreContextInterceptor requireStoreContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requireStoreContextInterceptor);
    }
}
