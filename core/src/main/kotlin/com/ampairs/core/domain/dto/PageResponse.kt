package com.ampairs.core.domain.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.domain.Page

/**
 * Custom page response DTO that provides stable JSON structure for paginated data
 * Replaces direct Page serialization to avoid Spring Data warnings
 */
data class PageResponse<T>(
    @JsonProperty("content")
    val content: List<T>,
    
    @JsonProperty("page_number")
    val pageNumber: Int,
    
    @JsonProperty("page_size")
    val pageSize: Int,
    
    @JsonProperty("total_elements")
    val totalElements: Long,
    
    @JsonProperty("total_pages")
    val totalPages: Int,
    
    @JsonProperty("first")
    val first: Boolean,
    
    @JsonProperty("last")
    val last: Boolean,
    
    @JsonProperty("has_next")
    val hasNext: Boolean,
    
    @JsonProperty("has_previous")
    val hasPrevious: Boolean,
    
    @JsonProperty("empty")
    val empty: Boolean
) {
    companion object {
        /**
         * Create PageResponse from Spring Data Page
         */
        fun <T> from(page: Page<T>): PageResponse<T> {
            return PageResponse(
                content = page.content,
                pageNumber = page.number,
                pageSize = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                first = page.isFirst,
                last = page.isLast,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious(),
                empty = page.isEmpty
            )
        }
        
        /**
         * Create PageResponse with mapped content
         */
        fun <T, R> from(page: Page<T>, mapper: (T) -> R): PageResponse<R> {
            return PageResponse(
                content = page.content.map(mapper),
                pageNumber = page.number,
                pageSize = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                first = page.isFirst,
                last = page.isLast,
                hasNext = page.hasNext(),
                hasPrevious = page.hasPrevious(),
                empty = page.isEmpty
            )
        }
    }
}