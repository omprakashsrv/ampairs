package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.Company
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CompanyRepository : CrudRepository<Company, String>