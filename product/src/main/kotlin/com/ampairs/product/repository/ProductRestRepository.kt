package com.ampairs.product.repository

import com.ampairs.product.domain.model.Product
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource

@RepositoryRestResource(path = "/product/v1")
interface ProductRestRepository : PagingAndSortingRepository<Product, String>