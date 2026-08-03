package com.akshara.api.bookimport.dto;

import com.akshara.api.book.entity.AvailabilityStatus;
import com.akshara.api.book.entity.BookFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CatalogueBookCommand {
    String title();
    String subtitle();
    String description();
    List<String> authors();
    String publisher();
    String languageCode();
    Integer pageCount();
    List<String> categories();
    String coverImageUrl();
    LocalDate publicationDate();
    String editionName();
    String isbn10();
    String isbn13();
    BookFormat format();
    BigDecimal price();
    Integer stockQuantity();
    AvailabilityStatus availabilityStatus();
    String sku();
    Boolean active();
    Boolean featured();
}
