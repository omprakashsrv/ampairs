package com.ampairs.product.repository

import com.ampairs.product.domain.model.TaxCode
import org.springframework.data.repository.CrudRepository

interface TaxCodeRepository : CrudRepository<TaxCode, String>