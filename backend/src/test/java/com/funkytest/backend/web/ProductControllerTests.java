package com.funkytest.backend.web;

import java.math.BigDecimal;
import java.util.List;

import com.funkytest.backend.catalog.Category;
import com.funkytest.backend.catalog.Product;
import com.funkytest.backend.catalog.ProductRepository;
import com.funkytest.backend.catalog.ProductVariant;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(ProductController.class)
class ProductControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private ProductRepository products;

	@Test
	void returnsEveryProductWithItsCategoryAndVariants() {
		Category clothing = new Category("Clothing", "clothing");
		Product tshirt = new Product(clothing, "Cotton T-shirt", "cotton-tshirt");
		tshirt.addVariant(new ProductVariant("TS-RED-L", "red / L", new BigDecimal("19.90"), 12));
		given(products.findAllWithCategoryAndVariantsBy()).willReturn(List.of(tshirt));

		MvcTestResult result = mvc.get().uri("/api/products").exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$[0].name").isEqualTo("Cotton T-shirt");
		assertThat(result).bodyJson().extractingPath("$[0].slug").isEqualTo("cotton-tshirt");
		assertThat(result).bodyJson().extractingPath("$[0].categoryName").isEqualTo("Clothing");
		assertThat(result).bodyJson().extractingPath("$[0].categorySlug").isEqualTo("clothing");
		assertThat(result).bodyJson().extractingPath("$[0].active").asBoolean().isTrue();
		assertThat(result).bodyJson().extractingPath("$[0].variants.length()").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$[0].variants[0].sku").isEqualTo("TS-RED-L");
		assertThat(result).bodyJson()
			.extractingPath("$[0].variants[0].price")
			.convertTo(InstanceOfAssertFactories.BIG_DECIMAL)
			.isEqualByComparingTo("19.90");
	}

	@Test
	void returnsAnEmptyArrayWhenTheCatalogueIsEmpty() {
		given(products.findAllWithCategoryAndVariantsBy()).willReturn(List.of());

		assertThat(mvc.get().uri("/api/products")).hasStatusOk().bodyJson().isEqualTo("[]");
	}

}
